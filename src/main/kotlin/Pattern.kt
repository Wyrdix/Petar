package fr.univ_lille.iut_info

import fr.univ_lille.iut_info.memory.MemoryArray
import fr.univ_lille.iut_info.memory.MemoryElement
import fr.univ_lille.iut_info.memory.MemoryObject
import java.util.*

abstract class Pattern : Visitable<Pattern> {
    val id = UUID.randomUUID().node()
    abstract val name: String?
    abstract val modifier: PatternModifier
    abstract val condition: Expression?

    fun singleton(element: MemoryElement): Map<String, MemoryElement> {
        val first = name
        return if (first == null) emptyMap() else mapOf(Pair(first, element))
    }

    abstract fun evaluate(element: MemoryElement): Map<String, MemoryElement>?

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

    override fun evaluate(element: MemoryElement): Map<String, MemoryElement>? {
        TODO("Not yet implemented")
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

    override fun evaluate(element: MemoryElement): Map<String, MemoryElement>? {
        TODO("Not yet implemented")
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

    override fun evaluate(element: MemoryElement): Map<String, MemoryElement>? {
        return if (element !is MemoryArray || element.value.size != this.values.size) null else {
            val evaluations = this.values.mapIndexed { index, pattern -> pattern.evaluate(element.value[index]) }
            if (evaluations.contains(null)) null else {
                evaluations.filterNotNull().union(listOf(singleton(element))).flatMap { it.entries }
                    .associateBy({ it.key }, { it.value })
            }
        }
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

    override fun evaluate(element: MemoryElement): Map<String, MemoryElement>? {
        return if (element !is MemoryArray || element.value.size != this.values.size) null else {
            val evaluations = this.values.mapIndexed { index, pattern -> pattern.evaluate(element.value[index]) }
            if (evaluations.contains(null)) null else {
                evaluations.filterNotNull().union(listOf(singleton(element))).flatMap { it.entries }
                    .associateBy({ it.key }, { it.value })
            }
        }
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

    override fun evaluate(element: MemoryElement): Map<String, MemoryElement>? {
        return if (element !is MemoryObject || element.type.identifier != this.identifier) null else {
            val evaluations = this.fieldsMap.map { (key, pattern) -> pattern.evaluate(element.value[key]!!) }
            if (evaluations.contains(null)) null else {
                evaluations.filterNotNull().union(listOf(singleton(element))).flatMap { it.entries }
                    .associateBy({ it.key }, { it.value })
            }
        }
    }
}