package fr.univ_lille.iut_info

interface Pattern

data class StringPattern(val value: String) : Pattern

data class NumberPattern(val value: Float) : Pattern

data class ObjectPattern(val fields: List<Pair<String, Pattern>>, val children: ChildrenPattern? = null, val alias: String? = null) :
    Pattern {
    val fieldsMap: Map<String, Pattern>
        get() = fields.associateBy({ it.first }, { it.second })
}

data class ChildrenPattern(val patterns: List<Pattern>) : Pattern

data class ListPattern(val patterns: List<Pattern>): Pattern