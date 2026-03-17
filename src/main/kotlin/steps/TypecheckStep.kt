package fr.univ_lille.iut_info.steps

import fr.univ_lille.iut_info.*

fun typeEquality(got: Type, expected: Type): Boolean {
    if (got is ReferenceType) {
        val cache = got.cache
        if (cache != null)
            return typeEquality(cache, expected)
        throw IllegalStateException("Reference type should have cache before type checking")
    }
    if (expected is ReferenceType) {
        val cache = expected.cache
        if (cache != null)
            return typeEquality(got, cache)
        throw IllegalStateException("Reference type should have cache before type checking")
    }
    if (got is StringType) return expected is StringType
    if (got is NumberType) return expected is NumberType
    if (got is BooleanType) return expected is BooleanType
    if (got is ObjectType) {
        if (expected is ObjectType) return got.identifier == expected.identifier
        return false
    }
    return false
}

class TypecheckStep(val analysis: NameStep) : ExecutionStep {

    val program
        get() = analysis.program

    override fun run(): List<String> {
        val rules = program.statements.filterIsInstance<RewriteRuleStatement>()

        rules.forEach { if (it.pattern is ObjectPattern) it.pattern.typecheck(analysis.types[it.pattern.identifier]!!) }
        rules.forEach { it.condition.typecheck(analysis.types, Type.boolean) }
        rules.forEach {
            val gotType = it.transform.getBottomUpType(analysis.types)
            if (gotType != null)
                it.transform.typecheck(analysis.types, gotType.resolve())
        }

        return emptyList()
    }

}