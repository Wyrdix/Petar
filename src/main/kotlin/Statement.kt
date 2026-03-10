package fr.univ_lille.iut_info

interface Statement

data class GroupDeclarationStatement(val identifier: String) : Statement
data class NodeDeclarationStatement(val identifier: String, val type: ObjectAlfrType, val groups: List<String>) :
    Statement

data class RewriteRuleStatement(val pattern: Pattern, val condition: Condition, val transform: Transform) : Statement