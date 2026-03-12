package fr.univ_lille.iut_info.pattern

import fr.univ_lille.iut_info.type.Type
import fr.univ_lille.iut_info.visitable.Visitable
import fr.univ_lille.iut_info.visitable.Visitor

abstract class Pattern : Visitable<Pattern> {
    abstract val name: String?
    var type: Type? = null
}

abstract class LiteralPattern : Pattern() {

    override fun accept(visitor: Visitor<Pattern>): Pattern {
        return this
    }

    data class PString(val value: String, override val name: String? = null) : LiteralPattern()
    data class PNumber(val value: Float, override val name: String? = null) : LiteralPattern()
    data class PBoolean(val value: Boolean, override val name: String? = null) : LiteralPattern()
}

data class ArrayPattern(val values: List<Pattern>, override val name: String? = null) : Pattern() {
    override fun accept(visitor: Visitor<Pattern>): Pattern {
        return ArrayPattern(values.map(visitor::visit), name)
    }
}

data class ObjectPattern(
    val identifier: String,
    val fields: List<Pair<String, Pattern>>,
    override val name: String? = null
) : Pattern() {

    val fieldsMap
        get() = fields.associateBy({ it.first }, { it.second })

    override fun accept(visitor: Visitor<Pattern>): Pattern {
        return ObjectPattern(identifier, fields.map { Pair(it.first, visitor.visit(it.second)) }, name)
    }
}