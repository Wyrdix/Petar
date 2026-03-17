package fr.univ_lille.iut_info

interface Identified {
    val identifier: String
}

interface Statement

data class NodeDeclarationStatement(override val identifier: String, val type: ObjectType) :
    Statement, Identified

data class RewriteRuleStatement(val pattern: Pattern, val condition: Expression, val transform: Expression) : Statement