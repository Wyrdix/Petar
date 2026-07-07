package fr.univ_lille.iut_info

import fr.univ_lille.iut_info.memory.MemoryObject
import fr.univ_lille.iut_info.steps.*


data class ProgramData(
    val statements: List<Statement>,
    val input: ((typing: ITypingContext) -> MemoryObject)? = null
)

sealed class Statement : TextualRangeLocated {
    override var textual: TextualRange? = null
}

data class PropertyDeclarationStatement(
    val type: PropertyType
) : Statement()

data class AnnotationRuleStatement(
    val pattern: Pattern, val acts: List<AnnotationAct>
) : Statement()

data class AnnotationAct(val attaching: ExpressionAccess?, val attachment: PropertyExpression)

class Program(val data: ProgramData) {

    val statements
        get() = data.statements

    val name = NameStep(this)
    val type = TypecheckStep(name)
    val evaluate = EvaluatingStep(type)

    val steps: List<ExecutionStep> = listOf(name, type)

    fun compile(): List<String> {
        for (step in steps) {
            val errors = step.run()
            if (errors.isNotEmpty()) return errors
        }
        return emptyList()
    }

    fun evaluate(): MemoryObject? {
        evaluate.run()
        return evaluate.output
    }
}