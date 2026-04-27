package fr.univ_lille.iut_info.steps

import fr.univ_lille.iut_info.*
import fr.univ_lille.iut_info.IIterator.Companion.toIIterator
import fr.univ_lille.iut_info.PatternModifier.*

data class EvaluationEnvironment(
    val definitions: Map<String, MemoryElement> = emptyMap(), val guards: List<Expression> = emptyList()
)

interface IEvaluatingContext : ITypingContext {
    val typecheckStep: ITypingContext

    val memoryParentMap: MutableMap<MemoryElement, Pair<String, MemoryElement>>
    val memoryAnnotationRoot: MutableMap<MemoryElement, MemoryElement>
    val memoryAnnotationMap: MutableMap<MemoryElement, List<MemoryElement>>

    fun getParent(element: MemoryElement) = memoryParentMap[element]
    fun getAnnotationRoot(element: MemoryElement) = memoryAnnotationRoot[element]
    fun getAnnotations(element: MemoryElement) = memoryAnnotationMap[element] ?: emptyList()

    fun addAnnotation(element: MemoryElement, annotation: MemoryElement) {
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
    return EvaluationEnvironment(definitions + Pair(key, element), guards)
}

fun Pattern.applyEffects(
    context: IEvaluatingContext, element: MemoryElement?, environment: EvaluationEnvironment
): EvaluationEnvironment {
    val name = name;
    return if (name != null) {
        if (this.modifier == ONE) if (element != null) environment.add(name, element) else environment
        else {
            val arrayType = context.getCheckedPatternType(this)!! as ArrayType
            val existing =
                environment.definitions[name].let {
                    when (it) {
                        null -> MemoryArray(arrayType, emptyList())
                        is MemoryArray -> it
                        else -> MemoryArray(arrayType, listOf(it))
                    }
                }

            val array = MemoryArray(arrayType, (if (element == null) emptyList() else listOf(element)) + existing.value)
            environment.add(name, array)
        }
    } else environment
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

                is BinaryExpression.Divide -> if ((right as MemoryNumber).value.toInt() == 0) throw IllegalStateException(
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
            is ExpressionAccess.Index -> this.parent.evaluate(context, environment)
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
    context: IEvaluatingContext, element: MemoryElement, environment: EvaluationEnvironment
): IIterator<EvaluationEnvironment> {

    return when (this) {
        is ExpressionPattern -> {
            val stored = this.value.evaluate(context, environment)
            if (stored == element) IIterator.singleton(applyEffects(context, element, environment))
            else IIterator.empty()
        }

        is RegexPattern if element is MemoryString && this.typeCheck(
            context, element.type
        ) -> if (element.value.matches(Regex(value))) IIterator.singleton(
            this.applyEffects(
                context, element, environment
            )
        ) else IIterator.empty()

        is ArrayPattern if element is MemoryArray && this.typeCheck(context, element.type) -> arrayMatching(
            context,
            values,
            element.value,
            environment
        )

        is PropertyPattern if element is MemoryObject && this.typeCheck(context, element.type) -> {
            val transformer: (Map.Entry<String, Pattern>) -> IIterator<EvaluationEnvironment> = { (key, value) ->
                val keyedElement = element.value[key] ?: MemoryUndefined()
                value.match(
                    context, keyedElement, this.applyEffects(context, keyedElement, environment)
                )
            }
            (context.getAnnotations(element).map { it as MemoryObject } + this).toIIterator().flatMapI {
                fields.entries.toIIterator().flatMapI(transformer)
            }
        }

        else -> IIterator.empty()
    }

}

fun arrayMatching(
    context: IEvaluatingContext,
    patterns: List<Pattern>,
    elements: List<MemoryElement>,
    environment: EvaluationEnvironment,
    modifierAccumulation: PatternModifier? = null
): IIterator<EvaluationEnvironment> {
    if (patterns.isEmpty()) return IIterator.singleton(environment)

    val patternHead = patterns[0]
    val patternTail = patterns.subList(1, patterns.size)
    val elementsHead = if (elements.isEmpty()) null else elements[0]
    val elementsTails = elements.subList(1, elements.size)

    return when (modifierAccumulation ?: patternHead.modifier) {
        ANY if elementsHead == null -> arrayMatching(
            context, patternTail, elements, patternHead.applyEffects(context, null, environment)
        )

        ANY -> IIterator.flat(
            patternHead.match(context, elementsHead!!, environment).flatMapI {
                arrayMatching(
                    context, patternTail, elementsTails, patternHead.applyEffects(context, elementsHead, environment)
                )
            }, arrayMatching(
                context, patternTail, elements, patternHead.applyEffects(context, null, environment)
            )
        )

        ONE if elementsHead != null -> patternHead.match(
            context, elementsHead, patternHead.applyEffects(context, elementsHead, environment)
        ).flatMapI {
            arrayMatching(
                context, patternTail, elementsTails, patternHead.applyEffects(context, elementsHead, environment)
            )
        }

        AT_LEAST_ONE ->
            patternHead.match(context, elementsHead!!, environment).flatMapI {
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