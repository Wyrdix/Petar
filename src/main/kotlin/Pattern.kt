package fr.univ_lille.iut_info

interface Pattern

data class StringPattern(val value: String) : Pattern

data class NumberPattern(val value: Float) : Pattern

data class ObjectPattern(val fields: Map<String, Pattern>, val children: ChildrenPattern? = null, val alias: String? = null) :
    Pattern

data class ChildrenPattern(val patterns: List<Pattern>) : Pattern

data class ListPattern(val patterns: List<Pattern>): Pattern