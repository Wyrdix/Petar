package fr.univ_lille.iut_info

import java.util.*

data class PatternFields(
    val name: String? = null, val modifier: PatternModifier = PatternModifier.ONE, val condition: Expression? = null
)

abstract class Pattern(open val fields: PatternFields) : Visitable<Pattern> {
    val id = UUID.randomUUID().node()
    val name: String? = fields.name
    val modifier: PatternModifier = fields.modifier
    val condition: Expression? = fields.condition

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Expression

        return id == other.id
    }
}

enum class PatternModifier {
    ONE, ANY, AT_LEAST_ONE
}

data class ExpressionPattern(
    val value: Expression, override val fields: PatternFields
) : Pattern(fields) {
    override fun accept(visitor: Visitor<Pattern>): Pattern {
        return this
    }

}

data class RegexPattern(
    val value: String, override val fields: PatternFields
) : Pattern(fields) {
    override fun accept(visitor: Visitor<Pattern>): Pattern {
        return this
    }

}

data class ArrayPattern(
    val values: List<Pattern>, override val fields: PatternFields
) : Pattern(fields) {
    override fun accept(visitor: Visitor<Pattern>): Pattern {
        return ArrayPattern(values.map(visitor::visit), fields)
    }

}

data class UnorderedArrayPattern(
    val values: List<Pattern>, override val fields: PatternFields
) : Pattern(fields) {
    override fun accept(visitor: Visitor<Pattern>): Pattern {
        return UnorderedArrayPattern(values.map(visitor::visit), fields)
    }

}

data class PropertyPattern(
    val identifier: String, val values: List<Pair<String, Pattern>>, override val fields: PatternFields
) : Pattern(fields) {

    val fieldsMap
        get() = values.associateBy({ it.first }, { it.second })

    override fun accept(visitor: Visitor<Pattern>): Pattern {
        return PropertyPattern(identifier, values.map { Pair(it.first, visitor.visit(it.second)) }, fields)
    }

}