package fr.univ_lille.iut_info

import fr.univ_lille.iut_info.steps.EvaluationEnvironment
import fr.univ_lille.iut_info.steps.IEvaluatingContext
import fr.univ_lille.iut_info.steps.evaluate
import fr.univ_lille.iut_info.steps.match

@Suppress("EnumEntryName")
enum class FunctionPrototype(
    vararg val args: Type,
    val returnType: Type,
    val arbitraryLast: Boolean = false,
    val evaluation: (expression: FunctionCallExpression, context: IEvaluatingContext, environment: EvaluationEnvironment) -> MemoryElement
) {
    exist(
        Type.any,
        Type.anyPattern,
        returnType = Type.boolean,
        arbitraryLast = true,
        evaluation = { expression, context, environment ->
            MemoryBoolean(
                (count.evaluation(expression, context, environment) as MemoryNumber).value.toInt() >= 1
            )
        }),
    count(
        Type.any,
        Type.anyPattern,
        returnType = Type.number,
        arbitraryLast = true,
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
        Type.any, returnType = Type.string, evaluation = { expression, context, environment ->
            val localRoot = (expression.arguments.first() as ExpressionPattern).value.evaluate(context, environment)
            MemoryString(localRoot.type.toString())
        });
}