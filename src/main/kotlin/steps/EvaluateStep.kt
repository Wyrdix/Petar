package fr.univ_lille.iut_info.steps

import fr.univ_lille.iut_info.RewriteRuleStatement
import fr.univ_lille.iut_info.Statement
import fr.univ_lille.iut_info.memory.MemoryBoolean
import fr.univ_lille.iut_info.memory.MemoryElement
import fr.univ_lille.iut_info.visit

fun MemoryElement.evaluate(rule: RewriteRuleStatement): Pair<Boolean, MemoryElement> {
    val pattern = rule.pattern
    val condition = rule.condition
    val transform = rule.transform

    var used = false

    val transformed = visit { node ->
        val context = pattern.evaluate(node) ?: return@visit null

        val condition = (condition.evaluate(context) as MemoryBoolean).value
        if (!condition) return@visit null
        used = true

        return@visit transform.evaluate(context)
    }

    return Pair(used, transformed)
}

fun List<Statement>.evaluate(element: MemoryElement): MemoryElement {
    val ruleStatements = this.filterIsInstance<RewriteRuleStatement>()

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

class EvaluateStep(val typecheckStep: TypecheckStep) {

    val program
        get() = typecheckStep.program

    fun check(): List<String> {
        return emptyList()
    }

    fun evaluate(input: MemoryElement): MemoryElement {
        val evaluation = program.evaluate(input)
        return evaluation
    }

}