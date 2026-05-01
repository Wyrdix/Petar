package fr.univ_lille.iut_info

import fr.univ_lille.iut_info.steps.EvaluationEnvironment
import fr.univ_lille.iut_info.steps.IEvaluatingContext

enum class FunctionPrototype(
    vararg val args: Type,
    val returnType: Type,
    val arbitraryLast: Boolean = false,
    val evaluation: (expression: FunctionCallExpression, context: IEvaluatingContext, environment: EvaluationEnvironment) -> MemoryElement
) {

}