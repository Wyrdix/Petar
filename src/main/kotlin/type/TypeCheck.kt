package fr.univ_lille.iut_info.type

import fr.univ_lille.iut_info.RewriteRuleStatement
import fr.univ_lille.iut_info.name.NameAnalysis
import fr.univ_lille.iut_info.pattern.ArrayPattern
import fr.univ_lille.iut_info.pattern.LiteralPattern
import fr.univ_lille.iut_info.pattern.ObjectPattern
import fr.univ_lille.iut_info.pattern.Pattern
import fr.univ_lille.iut_info.visitable.visit

fun typeEquality(got: Type, expected: Type): Boolean {
    if (got is ReferenceType) {
        val cache = got.cache
        if (cache != null)
            return typeEquality(cache, expected)
        if (!got.group) throw IllegalStateException("Reference type should have cache before type checking")
    }
    if (expected is ReferenceType) {
        val cache = expected.cache
        if (cache != null)
            return typeEquality(got, cache)
        if (!expected.group) throw IllegalStateException("Reference type should have cache before type checking")
    }
    if (got is StringType) return expected is StringType
    if (got is NumberType) return expected is NumberType
    if (got is BooleanType) return expected is BooleanType
    if (got is ObjectType) {
        if (expected is ObjectType) return got.identifier == expected.identifier
        if (expected is ReferenceType) return got.interfaces.contains(expected.value)
        return false
    }
    return false
}

class TypeCheck(val analysis: NameAnalysis) {

    val program
        get() = analysis.program

    fun check(): List<String> {
        val rules = program.filterIsInstance<RewriteRuleStatement>()

        return rules.map { it.pattern }.flatMap(this::resolvePatternVariableType)
    }

    fun resolvePatternVariableType(pattern: Pattern): MutableList<String> {
        val errors: MutableList<String> = ArrayList()

        pattern.visit { node, _ ->
            val expectedType = node.type
            var gotType: Type? = null
            when (node) {
                is LiteralPattern.PBoolean -> gotType = Type.boolean
                is LiteralPattern.PNumber -> gotType = Type.number
                is LiteralPattern.PString -> gotType = Type.string
                is ObjectPattern -> {
                    gotType = analysis.types[node.identifier] as ObjectType

                    val childrenMap = gotType.childrenMap
                    val fieldsMap = node.fieldsMap

                    fieldsMap.forEach { (key, pattern) ->
                        pattern.type = childrenMap[key]
                    }


                }

                is ArrayPattern -> {
                    if (expectedType == null) return@visit null
                    if (expectedType !is ArrayType) {
                        TODO()
                    }
                    val elementType = expectedType.type
                    node.values.forEach { pattern -> pattern.type = elementType }
                }

                else -> {}
            }
            if (expectedType != null && gotType != null && !typeEquality(gotType, expectedType)) {
                errors.add("TypeError: Expected $expectedType got $gotType")
            }
            node.type = gotType

            return@visit null

        }

        return errors
    }

}