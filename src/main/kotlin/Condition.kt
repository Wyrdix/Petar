package fr.univ_lille.iut_info

interface Condition
class TrueCondition: Condition
data class AndCondition(val left: Condition, val right: Condition): Condition
data class OrCondition(val left: Condition, val right: Condition): Condition
data class NotCondition(val operand: Condition): Condition
data class EqualCondition(val left: Expression, val right: Expression): Condition