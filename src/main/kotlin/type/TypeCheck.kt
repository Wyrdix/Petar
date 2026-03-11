package fr.univ_lille.iut_info.type

import fr.univ_lille.iut_info.*
import fr.univ_lille.iut_info.name.NameAnalysis

fun Pattern.variables(): Map<String, ObjectPattern> {
    if (this is StringPattern) return emptyMap()
    if (this is NumberPattern) return emptyMap()
    if (this is ChildrenPattern) return this.patterns.flatMap { it.variables().entries }
        .associateBy({ it.key }, { it.value })
    if (this is ObjectPattern) return listOf(
        (if (this.alias != null) mapOf(
            Pair(
                this.alias, this
            )
        )
        else emptyMap()).entries,
        fields.flatMap { it.second.variables().entries },
        if (this.children != null) children.variables().entries else emptySet()
    ).flatten().associateBy({ it.key }, { it.value })
    if (this is ListPattern) return this.patterns.flatMap { it.variables().entries }
        .associateBy({ it.key }, { it.value })
    throw IllegalStateException("Unknown pattern type")
}


class TypeCheck(val analysis: NameAnalysis) {

    val program
        get() = analysis.program

    fun check(): List<String> {
        val rules = program.filterIsInstance<RewriteRuleStatement>()
        rules.map { it.pattern }.forEach(this::resolvePatternVariableType)

        return emptyList()
    }

    fun resolvePatternVariableType(pattern: Pattern) {
        val variables = pattern.variables()
        pattern.variables = variables.mapValues { (_, value) -> analysis.types[value.identifier]!! }
    }

}