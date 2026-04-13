package fr.univ_lille.iut_info

import com.google.gson.JsonObject
import fr.univ_lille.iut_info.steps.EvaluateStep
import fr.univ_lille.iut_info.steps.ExecutionStep
import fr.univ_lille.iut_info.steps.NameStep
import fr.univ_lille.iut_info.steps.TypecheckStep


interface Statement

data class PropertyDeclarationStatement(
    val identifier: String,
    val type: PropertyType
) : Statement

data class ProductionRuleStatement(
    val pattern: Pattern,
    val production: Expression
) : Statement

class Program(val statements: List<Statement>) {

    val name = NameStep(this)
    val type = TypecheckStep(name)

    val steps: List<ExecutionStep> = listOf(name, type)

    fun compile(): List<String> {
        for (step in steps) {
            val errors = step.run()
            if (errors.isNotEmpty()) return errors
        }
        return emptyList()
    }

    fun evaluate(input: JsonObject): Pair<List<String>, MemoryElement?> {
        val evaluateStep = EvaluateStep(type, input)
        val errors = evaluateStep.run()

        return Pair(errors, evaluateStep.evaluation)
    }
}