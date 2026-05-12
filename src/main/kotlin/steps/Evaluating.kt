package fr.univ_lille.iut_info.steps

import fr.univ_lille.iut_info.*
import fr.univ_lille.iut_info.IIterator.Companion.toIIterator
import fr.univ_lille.iut_info.PatternModifier.*

data class EvaluationEnvironment(
    val definitions: Map<String, MemoryElement> = emptyMap(),
    val choices: Map<MemoryObject, MemoryObject> = emptyMap()
)

interface IEvaluatingContext : ITypingContext {
    val typecheckStep: ITypingContext

    val memoryParentMap: MutableMap<MemoryElement, Pair<String, MemoryElement>>
    val memoryAnnotationRoot: MutableMap<MemoryElement, MemoryElement>
    val memoryAnnotationMap: MutableMap<MemoryElement, List<MemoryObject>>

    fun getParent(element: MemoryElement) = memoryParentMap[element]
    fun getAnnotationRoot(element: MemoryElement) = memoryAnnotationRoot[element]
    fun getAnnotations(element: MemoryElement) = memoryAnnotationMap[element] ?: emptyList()
    var output: MemoryObject?

    fun addAnnotation(element: MemoryElement, annotation: MemoryObject) {
        memoryAnnotationRoot[annotation] = element
        memoryAnnotationMap[element] = getAnnotations(element) + annotation
    }

    fun cacheParents(parent: MemoryElement) {
        when (parent) {
            is MemoryNumber, is MemoryString, is MemoryBoolean, is MemoryUndefined -> {}
            is MemoryArray -> parent.value.forEachIndexed { index, element ->
                memoryParentMap[element] = Pair(index.toString(), parent)
                cacheParents(element)
            }

            is MemoryObject -> parent.value.forEach { (key, value) ->
                memoryParentMap[value] = Pair(key, parent)
                cacheParents(value)
            }
        }
    }
}

fun EvaluationEnvironment.add(key: String, element: MemoryElement): EvaluationEnvironment {
    return EvaluationEnvironment(definitions + Pair(key, element))
}

fun EvaluationEnvironment.choice(parent: MemoryObject, choice: MemoryObject): EvaluationEnvironment {
    return EvaluationEnvironment(definitions, choices + (parent to choice))
}

fun Pattern.applyEffects(
    context: IEvaluatingContext, element: MemoryElement?, environment: EvaluationEnvironment
): EvaluationEnvironment {
    val name = name;
    return (if (name != null) {
        if (this.modifier == ONE) if (element != null) environment.add(name, element) else environment
        else {
            val arrayType = context.getCheckedPatternType(context.patternParentMap[this]!!)!! as ArrayType
            val existing =
                environment.definitions[name].let {
                    when (it) {
                        null -> MemoryArray(arrayType, emptyList())
                        is MemoryArray -> it
                        else -> MemoryArray(arrayType, listOf(it))
                    }
                }

            val array = MemoryArray(arrayType, existing.value + (if (element == null) emptyList() else listOf(element)))
            environment.add(name, array)
        }
    } else environment).let { environment ->
        if (element != null && element is MemoryObject) {
            val parent = context.memoryAnnotationRoot[element]
            if (parent != null && parent is MemoryObject) environment.choice(parent, element)
            else environment
        } else
            environment
    }
}

fun Expression.evaluate(context: IEvaluatingContext, environment: EvaluationEnvironment): MemoryElement {
    return when (this) {
        is ArrayExpression -> MemoryArray(
            context.getCheckedType(this) as ArrayType, values.map { evaluate(context, environment) })

        is BinaryExpression -> {
            val left = left.evaluate(context, environment)
            val right = right.evaluate(context, environment)
            when (this) {
                is BinaryExpression.And -> MemoryNumber((left as MemoryNumber).value.toDouble() + (right as MemoryNumber).value.toDouble())

                is BinaryExpression.Divide -> if ((right as MemoryNumber).value.toInt() == 0) throw StepError(
                    Step.EVALUATION, this,
                    "Division by 0"
                )
                else MemoryNumber((left as MemoryNumber).value.toDouble() / (right as MemoryNumber).value.toDouble())

                is BinaryExpression.Minus -> MemoryNumber((left as MemoryNumber).value.toDouble() - (right as MemoryNumber).value.toDouble())

                is BinaryExpression.Multiply -> MemoryNumber((left as MemoryNumber).value.toDouble() * (right as MemoryNumber).value.toDouble())

                is BinaryExpression.Or -> MemoryBoolean((left as MemoryBoolean).value || (right as MemoryBoolean).value)

                is BinaryExpression.Plus -> MemoryBoolean((left as MemoryBoolean).value && (right as MemoryBoolean).value)
            }
        }

        is ExpressionAccess -> when (this) {
            is ExpressionAccess.Index -> {
                val array = this.parent.evaluate(context, environment) as MemoryArray
                val index = (this.expression.evaluate(context, environment) as MemoryNumber).value.toInt()

                if (index < 0 || index >= array.value.size) throw StepError(
                    Step.EVALUATION, this, "Index out of bounds"
                )
                else array.value[index]
            }

            is ExpressionAccess.Member -> when (this.parent) {
                null -> environment.definitions[this.identifier] ?: MemoryUndefined()
                else -> (this.parent.evaluate(context, environment) as MemoryObject).value[this.identifier]
                    ?: MemoryUndefined()
            }
        }

        is LiteralExpression -> when (this) {
            is LiteralExpression.EBoolean -> MemoryBoolean(value)
            is LiteralExpression.ENumber -> MemoryNumber(value)
            is LiteralExpression.EString -> MemoryString(value)
            is LiteralExpression.EUndefined -> MemoryUndefined()
        }

        is PatternMatchExpression -> MemoryBoolean(
            this.right.match(
                context, this.left.evaluate(context, environment), environment
            ).hasNext()
        )

        is PropertyExpression -> {
            MemoryObject(
                context.getCheckedType(this) as PropertyType,
                this.fields.mapValues { (_, value) -> value.evaluate(context, environment) })
        }

        is FunctionCallExpression -> {
            val prototype = FunctionPrototype.entries.find { it.name == this.name }!!
            prototype.evaluation(this, context, environment)
        }

        is UnaryExpression -> when (this) {
            is UnaryExpression.Negate -> MemoryBoolean(
                !(operand.evaluate(context, environment) as MemoryBoolean).value
            )

            is UnaryExpression.Opposite -> MemoryNumber(
                -(operand.evaluate(
                    context, environment
                ) as MemoryNumber).value.toDouble()
            )
        }
    }
}

fun Pattern.match(
    context: IEvaluatingContext,
    element: MemoryElement,
    environment: EvaluationEnvironment = EvaluationEnvironment()
): IIterator<EvaluationEnvironment> {
    return when (this) {
        is ExpressionPattern -> {
            val stored = this.value.evaluate(context, environment)
            if (stored.isSimilarTo(element, null)) IIterator.singleton(applyEffects(context, element, environment))
            else IIterator.empty()
        }

        is RegexPattern if element is MemoryString -> if (element.value.matches(Regex(value))) IIterator.singleton(
            this.applyEffects(
                context, element, environment
            )
        ) else IIterator.empty()

        is ArrayPattern if element is MemoryArray -> arrayMatching(
            context,
            values,
            element.value,
            environment
        )

        is PropertyPattern -> {
            (context.getAnnotations(element).map { it } + element).filterIsInstance<MemoryObject>()
                .filter { it.type.ascendants(context).contains(this.identifier) }.toIIterator().flatMapI {
                    fields.entries.toIIterator().foldI(
                        IIterator.singleton(this.applyEffects(context, it, environment))
                    ) { acc, (key, value) ->
                        val keyedElement = it.value[key] ?: MemoryUndefined()
                        acc.flatMapI { env ->
                            val match = value.match(
                                context, keyedElement, env
                            )
                            match
                        }

                    }
                }
        }

        else -> IIterator.empty()
    }

}

fun arrayMatching(
    context: IEvaluatingContext,
    patterns: List<Pattern>,
    elements: List<MemoryElement>,
    environment: EvaluationEnvironment = EvaluationEnvironment(),
    modifierAccumulation: PatternModifier? = null
): IIterator<EvaluationEnvironment> {
    fun Pattern.safeMatch(
        context: IEvaluatingContext,
        element: MemoryElement,
        environment: EvaluationEnvironment = EvaluationEnvironment()
    ): IIterator<EvaluationEnvironment> {
        return try {
            match(context, element, environment)
        } catch (_: Error) {
            IIterator.empty()
        }
    }


    if (patterns.isEmpty() && elements.isEmpty()) return IIterator.singleton(environment)
    if (patterns.isEmpty()) return IIterator.empty()

    val patternHead = patterns[0]
    val patternTail = patterns.subList(1, patterns.size)
    val elementsHead = if (elements.isEmpty()) null else elements[0]
    val elementsTails = if (elements.isEmpty()) emptyList() else elements.subList(1, elements.size)

    return when (modifierAccumulation ?: patternHead.modifier) {
        ANY if elementsHead == null -> arrayMatching(
            context, patternTail, elements, patternHead.applyEffects(context, null, environment), modifierAccumulation
        )

        ANY -> IIterator.flat(
            patternHead.safeMatch(context, elementsHead!!, environment).flatMapI {
                arrayMatching(
                    context,
                    patterns,
                    elementsTails,
                    patternHead.applyEffects(context, elementsHead, environment),
                    modifierAccumulation
                )
            }, arrayMatching(
                context,
                patternTail,
                elements,
                patternHead.applyEffects(context, null, environment),
                modifierAccumulation
            )
        )

        ONE if elementsHead != null -> patternHead.safeMatch(
            context, elementsHead, patternHead.applyEffects(context, elementsHead, environment)
        ).flatMapI {
            arrayMatching(
                context,
                patternTail,
                elementsTails,
                patternHead.applyEffects(context, elementsHead, environment),
                modifierAccumulation
            )
        }

        AT_LEAST_ONE if elementsHead != null ->
            patternHead.safeMatch(context, elementsHead, environment).flatMapI {
                arrayMatching(
                    context,
                    patterns,
                    elementsTails,
                    patternHead.applyEffects(context, elementsHead, environment),
                    ANY
                )
            }

        else -> IIterator.empty()
    }
}