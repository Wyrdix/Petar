package fr.univ_lille.iut_info

import java.util.*

data class PatternMeta(
    val name: String? = null, val modifier: PatternModifier = PatternModifier.ONE, val condition: Expression? = null
)

sealed class Pattern(open val meta: PatternMeta) : Visitable<Pattern>, TextualRangeLocated {
    val id = UUID.randomUUID().toString()
    val name: String?
        get() = meta.name
    val modifier: PatternModifier
        get() = meta.modifier
    val condition: Expression?
        get() = meta.condition

    override var textual: TextualRange? = null

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Pattern

        return id == other.id
    }
}

enum class PatternModifier {
    ONE, ANY, AT_LEAST_ONE
}

class ExpressionPattern(
    val value: Expression, override val meta: PatternMeta
) : Pattern(meta) {
    override fun accept(visitor: Visitor<Pattern>): Pattern {
        return this
    }

}

class RegexPattern(
    val value: String, override val meta: PatternMeta
) : Pattern(meta) {
    override fun accept(visitor: Visitor<Pattern>): Pattern {
        return this
    }

}

class ArrayPattern(
    val values: List<Pattern>, override val meta: PatternMeta
) : Pattern(meta) {
    override fun accept(visitor: Visitor<Pattern>): Pattern {
        return ArrayPattern(values.map(visitor::visit), meta)
    }

}

class PropertyPattern(
    val identifier: String, val inlineFields: List<Pair<String, Pattern>>, override val meta: PatternMeta
) : Pattern(meta) {

    val fields
        get() = inlineFields.associate { it }

    override fun accept(visitor: Visitor<Pattern>): Pattern {
        return PropertyPattern(identifier, inlineFields.map { Pair(it.first, visitor.visit(it.second)) }, meta)
    }

}