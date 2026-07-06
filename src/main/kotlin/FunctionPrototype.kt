package fr.univ_lille.iut_info

import fr.univ_lille.iut_info.memory.MemoryBoolean
import fr.univ_lille.iut_info.memory.MemoryElement
import fr.univ_lille.iut_info.memory.MemoryNumber
import fr.univ_lille.iut_info.memory.MemoryString
import fr.univ_lille.iut_info.steps.*

@Suppress("EnumEntryName")
enum class FunctionPrototype(
    val typing: (expression: FunctionCallExpression, context: ITypingContext) -> Type?,
    val evaluation: (expression: FunctionCallExpression, context: IEvaluatingContext, environment: EvaluationEnvironment) -> MemoryElement
) {
    exist(
        typing = { expression, _ ->
            val arguments = expression.arguments

            if (arguments.size < 2) throw StepError(
                Step.TYPE, expression,
                $$"$exist function needs at least two arguments: $exist(root, pattern...)"
            )

            Type.boolean
        },
        evaluation = { expression, context, environment ->
            MemoryBoolean(
                (count.evaluation(expression, context, environment) as MemoryNumber).value.toInt() >= 1
            )
        }),
    count(
        typing = { expression, context ->
            val arguments = expression.arguments

            if (arguments.size < 2) throw StepError(
                Step.TYPE, expression,
                $$"$count function needs at least two arguments: $count(root, pattern...)"
            )

            Type.boolean
        },
        evaluation = { expression, context, environment ->
            val localRoot = (expression.arguments.first() as ExpressionPattern).value.evaluate(context, environment)
            val patterns = expression.arguments.subList(1, expression.arguments.size)

            val quantity = patterns.sumOf { pattern ->
                var qty = 0
                localRoot.map { original ->
                    qty += (context.getAnnotations(original) + original).count {
                        pattern.match(context, it, environment).hasNext()
                    }
                    null
                }
                qty
            }
            MemoryNumber(quantity)
        }),
    type(
        typing = { expression, context ->
            val arguments = expression.arguments

            if (arguments.size != 1) throw StepError(
                Step.TYPE, expression,
                $$"$type function needs exactly one argument: $type(value)"
            )

            Type.string
        },
        evaluation = { expression, context, environment ->
            val localRoot = (expression.arguments.first() as ExpressionPattern).value.evaluate(context, environment)
            MemoryString(localRoot.type.toString())
        });
}