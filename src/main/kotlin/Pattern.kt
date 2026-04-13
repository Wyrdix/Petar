package fr.univ_lille.iut_info

import java.util.*

abstract class Pattern : Visitable<Pattern> {
    val id = UUID.randomUUID().node()
    abstract val name: String?
    abstract val modifier: PatternModifier
    abstract val condition: Expression?

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
    val value: Expression,
    override val name: String? = null,
    override val modifier: PatternModifier = PatternModifier.ONE,
    override val condition: Expression? = null
) : Pattern() {
    override fun accept(visitor: Visitor<Pattern>): Pattern {
        return this
    }

}

data class RegexPattern(
    val value: String,
    override val name: String? = null,
    override val modifier: PatternModifier = PatternModifier.ONE,
    override val condition: Expression? = null
) : Pattern() {
    override fun accept(visitor: Visitor<Pattern>): Pattern {
        return this
    }

}

data class ArrayPattern(
    val values: List<Pattern>,
    override val name: String? = null,
    override val modifier: PatternModifier = PatternModifier.ONE,
    override val condition: Expression? = null
) : Pattern() {
    override fun accept(visitor: Visitor<Pattern>): Pattern {
        return ArrayPattern(values.map(visitor::visit), name = this.name, modifier = this.modifier)
    }

}

data class UnorderedArrayPattern(
    val values: List<Pattern>,
    override val name: String? = null,
    override val modifier: PatternModifier = PatternModifier.ONE,
    override val condition: Expression? = null
) : Pattern() {
    override fun accept(visitor: Visitor<Pattern>): Pattern {
        return UnorderedArrayPattern(values.map(visitor::visit), name, modifier)
    }

}

data class PropertyPattern(
    val identifier: String,
    val fields: List<Pair<String, Pattern>>,
    override val name: String? = null,
    override val modifier: PatternModifier = PatternModifier.ONE,
    override val condition: Expression? = null
) : Pattern() {

    val fieldsMap
        get() = fields.associateBy({ it.first }, { it.second })

    override fun accept(visitor: Visitor<Pattern>): Pattern {
        return PropertyPattern(identifier, fields.map { Pair(it.first, visitor.visit(it.second)) }, name, modifier)
    }

}