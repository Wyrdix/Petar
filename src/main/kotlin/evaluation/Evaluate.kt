package fr.univ_lille.iut_info.evaluation

import fr.univ_lille.iut_info.RewriteRuleStatement
import fr.univ_lille.iut_info.Statement
import fr.univ_lille.iut_info.parsing.MemoryBoolean
import fr.univ_lille.iut_info.parsing.MemoryElement
import fr.univ_lille.iut_info.visitable.visit

fun MemoryElement.evaluate(rule: RewriteRuleStatement): Pair<Boolean, MemoryElement> {
    val pattern = rule.pattern
    val condition = rule.condition
    val transform = rule.transform

    var used = false

    val transformed = visit { node, _ ->
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