package fr.univ_lille.iut_info

interface Identified {
    val identifier: String
}
interface Statement

data class GroupDeclarationStatement(override val identifier: String) : Statement, Identified
data class NodeDeclarationStatement(override val identifier: String, val type: ObjectAlfrType, val groups: List<String>) :
    Statement, Identified

data class RewriteRuleStatement(val pattern: Pattern, val condition: Condition, val transform: Transform) : Statement