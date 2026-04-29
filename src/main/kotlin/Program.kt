package fr.univ_lille.iut_info

import fr.univ_lille.iut_info.steps.*


data class ProgramData(val statements: List<Statement>, val input: ((typing: ITypingContext) -> MemoryObject)? = null)

interface Statement

data class PropertyDeclarationStatement(
    val type: PropertyType
) : Statement

data class ProductionRuleStatement(
    val pattern: Pattern, val production: PropertyExpression
) : Statement

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