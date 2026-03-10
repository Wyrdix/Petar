package fr.univ_lille.iut_info

interface Pattern

class StringPattern(val value: String) : Pattern

class NumberPattern(val value: Float) : Pattern

class ObjectPattern(val fields: Map<String, Pattern>, val children: ChildrenPattern? = null, val alias: String? = null) :
    Pattern

class ChildrenPattern(val patterns: List<Pattern>) : Pattern

class ListPattern(val patterns: List<Pattern>): Pattern