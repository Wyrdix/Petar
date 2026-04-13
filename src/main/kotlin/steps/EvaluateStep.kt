package fr.univ_lille.iut_info.steps

import com.google.gson.JsonObject
import fr.univ_lille.iut_info.ProductionRuleStatement
import fr.univ_lille.iut_info.Statement
import fr.univ_lille.iut_info.memory.MemoryBoolean
import fr.univ_lille.iut_info.memory.MemoryElement
import fr.univ_lille.iut_info.memory.createMemoryElement
import fr.univ_lille.iut_info.memory.safeCheck
import fr.univ_lille.iut_info.visit

fun MemoryElement.evaluate(rule: ProductionRuleStatement): Pair<Boolean, MemoryElement> {
    val pattern = rule.pattern
    val condition = rule.condition
    val transform = rule.production

    var used = false

    val transformed = visit { element ->
        val context = pattern.evaluate(element) ?: return@visit null

        val condition = (condition.evaluate(context) as MemoryBoolean).value
        if (!condition) return@visit null
        used = true

        return@visit transform.evaluate(context)
    }

    return Pair(used, transformed)
}

fun List<Statement>.evaluate(element: MemoryElement): MemoryElement {
    val ruleStatements = this.filterIsInstance<ProductionRuleStatement>()

    var accumulation: Pair<Boolean, MemoryElement> = Pair(true, element)

    while (accumulation.first && ruleStatements.isNotEmpty()) {
        accumulation = ruleStatements.fold(Pair(false, accumulation.second)) { acc, statement ->
            val alreadyChanged = acc.first
            val evaluated = acc.second.evaluate(statement)
            val freshlyChanged = evaluated.first
            Pair(alreadyChanged || freshlyChanged, evaluated.second)
        }
    }
    return accumulation.second
}

class EvaluateStep(val typecheckStep: TypecheckStep, val input: JsonObject) : ExecutionStep {

    val program
        get() = typecheckStep.program

    var evaluation: MemoryElement? = null

    override fun run(): List<String> {

        val suitableRoots = program.name.roots.filter { it.safeCheck(input) }

        if (suitableRoots.isEmpty()) {
            return listOf(
                "EvaluationError: No suitable root were found (available roots are [${
                    program.name.roots.joinToString(separator = ",") { it.identifier }
                }])"
            )
        } else if (suitableRoots.size > 1) {
            return listOf(
                "EvaluationError: Multiple suitable roots were found (suitable roots are [${
                    suitableRoots.joinToString(separator = ",") { it.identifier }
                }])"
            )
        }

        val input = createMemoryElement(program.name.roots[0], input)
        evaluation = program.statements.evaluate(input)
        return emptyList()
    }

}