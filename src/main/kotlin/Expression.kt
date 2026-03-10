package fr.univ_lille.iut_info

interface Expression;

data class ValueExpression(val value: Value) : Expression
data class IdentifierExpression(val identifiers: List<String>) : Expression