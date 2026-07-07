package fr.univ_lille.iut_info.steps

import fr.univ_lille.iut_info.*
import fr.univ_lille.iut_info.IIterator.Companion.toIIterator
import fr.univ_lille.iut_info.PatternModifier.*
import fr.univ_lille.iut_info.memory.*

data class EvaluationEnvironment(
    val definitions: Map<String, MemoryElement> = emptyMap(),
    val choices: Map<MemoryElement, MemoryElement> = emptyMap()
)

interface IEvaluatingContext : ITypingContext {
    val typecheckStep: ITypingContext
    val pathMemory: MutableBiMap<MemoryPath, MemoryElement>

    var output: MemoryObject?

    fun getAnnotations(element: MemoryElement) =
        pathMemory.getReversed(element)!!.children().filter { it.nodes.last() is MemoryPath.TypeNode }
            .map { it.resolve() }

    fun addAnnotation(element: MemoryElement, annotation: MemoryObject) {
        initial(annotation, pathMemory.getReversed(element)!!.goto(annotation.type))
    }

    fun isAnnotation(element: MemoryElement) =
        pathMemory.getReversed(element)?.nodes?.lastOrNull() is MemoryPath.TypeNode
}

fun IEvaluatingContext.initial(element: MemoryElement, path: MemoryPath): MemoryElement {

    val existing = pathMemory.getReversed(element)

    if (existing != null) {
        val reference = MemoryReference(element.type, existing)
        pathMemory[path] = reference
        return reference
    }

    val newValue = when (element) {
        is MemoryBoolean, is MemoryNumber, is MemoryString, is MemoryUndefined, is MemoryReference -> element
        is MemoryObject -> MemoryObject(
            element.type,
            element.value.mapValues { (key, value) -> initial(value, path.goto(key)) }).apply { id = element.id }

        is MemoryArray -> MemoryArray(
            element.type,
            element.value.mapIndexed { index, value -> initial(value, path.goto(index)) }).apply { id = element.id }
    }

    pathMemory[path] = newValue
    return newValue
}

fun EvaluationEnvironment.add(key: String, element: MemoryElement): EvaluationEnvironment {
    return EvaluationEnvironment(definitions + Pair(key, element))
}

fun EvaluationEnvironment.choice(parent: MemoryElement, choice: MemoryElement): EvaluationEnvironment {
    return EvaluationEnvironment(definitions, choices + (parent to choice))
}

fun Pattern.applyEffects(
    context: IEvaluatingContext, element: MemoryElement?, environment: EvaluationEnvironment
): EvaluationEnvironment {
    val name = name
    return (if (name != null) {
        if (this.modifier == ONE) if (element != null) environment.add(name, element) else environment
        else {
            val arrayType = context.getCheckedPatternType(context.patternParentMap[this]!!)!! as ArrayType
            val existing = environment.definitions[name].let {
                when (it) {
                    null -> MemoryArray(arrayType, emptyList())
                    is MemoryArray -> it
                    else -> MemoryArray(arrayType, listOf(it))
                }
            }

            val array = MemoryArray(
                arrayType,
                existing.value + (if (element == null) emptyList() else listOf(element))
            )
            environment.add(name, array)
        }
    } else environment).let { environment ->
        if (element != null && element is MemoryObject) {
            if (context.isAnnotation(element))
                environment.choice(context.pathMemory.getReversed(element)!!.parent().resolve(), element)
            else environment
        } else environment
    }
}

fun Expression.evaluate(context: IEvaluatingContext, environment: EvaluationEnvironment): MemoryElement {
    return when (this) {
        is ArrayExpression -> MemoryArray(
            context.getCheckedType(this) as ArrayType, values.map { it.evaluate(context, environment) })

        is BinaryExpression -> {
            val left = left.evaluate(context, environment)
            val right = right.evaluate(context, environment)
            when (this) {
                is BinaryExpression.And -> MemoryNumber((left as MemoryNumber).value.toDouble() + (right as MemoryNumber).value.toDouble())

                is BinaryExpression.Divide -> if ((right as MemoryNumber).value.toInt() == 0) throw StepError(
                    Step.EVALUATION, this, "Division by 0"
                )
                else MemoryNumber((left as MemoryNumber).value.toDouble() / right.value.toDouble())

                is BinaryExpression.Minus -> MemoryNumber((left as MemoryNumber).value.toDouble() - (right as MemoryNumber).value.toDouble())

                is BinaryExpression.Multiply -> MemoryNumber((left as MemoryNumber).value.toDouble() * (right as MemoryNumber).value.toDouble())

                is BinaryExpression.Or -> MemoryBoolean((left as MemoryBoolean).value || (right as MemoryBoolean).value)

                is BinaryExpression.Plus -> MemoryBoolean((left as MemoryBoolean).value && (right as MemoryBoolean).value)

                is BinaryExpression.Lower -> MemoryBoolean((left as MemoryNumber).value.toDouble() < (right as MemoryNumber).value.toDouble())
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
            val fieldMap = this.fields.mapValues { (_, value) -> value.evaluate(context, environment) }
            val parentMap = (parent?.evaluate(
                context, environment
            ) as MemoryObject?)?.value ?: emptyMap()
            MemoryObject(
                context.getCheckedType(this) as PropertyType, fieldMap + parentMap
            )
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

fun Pattern.condition(
    context: IEvaluatingContext, environment: EvaluationEnvironment
): Boolean {
    return children().all {
        it.condition(context, environment)
    } && condition?.evaluate(context, environment)?.let { (it as MemoryBoolean).value } ?: true
}

fun Pattern.match(
    context: IEvaluatingContext, element: MemoryElement, environment: EvaluationEnvironment = EvaluationEnvironment()
): IIterator<EvaluationEnvironment> {

    if (element is MemoryReference) {
        return match(context, element.reference.resolve(), environment)
    }

    return when (this) {
        is PatternNesting -> {
            pattern.match(context, element, environment).map { applyEffects(context, element, it) }
        }

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
            context, values, element.value, environment
        )

        is PrimitiveTypePattern -> {
            if (element.type == context.typeNameMap[identifier]!!) IIterator.singleton(
                this.applyEffects(
                    context, element, environment
                )
            )
            else IIterator.empty()
        }

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

        AT_LEAST_ONE if elementsHead != null -> patternHead.safeMatch(context, elementsHead, environment).flatMapI {
            arrayMatching(
                context, patterns, elementsTails, patternHead.applyEffects(context, elementsHead, environment), ANY
            )
        }

        else -> IIterator.empty()
    }
}