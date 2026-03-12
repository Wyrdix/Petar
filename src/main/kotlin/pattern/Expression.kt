package fr.univ_lille.iut_info.pattern

import fr.univ_lille.iut_info.visitable.Visitable
import fr.univ_lille.iut_info.visitable.Visitor

interface Pattern : Visitable<Pattern> {
    val name: String?;
}

interface LiteralPattern : Pattern {

    override fun accept(visitor: Visitor<Pattern>): Pattern {
        return this
    }

    data class PString(val value: String, override val name: String? = null) : LiteralPattern
    data class PNumber(val value: Float, override val name: String? = null) : LiteralPattern
    data class PBoolean(val value: Boolean, override val name: String? = null) : LiteralPattern
}

data class ArrayPattern(val values: List<Pattern>, override val name: String? = null) : Pattern {
    override fun accept(visitor: Visitor<Pattern>): Pattern {
        return ArrayPattern(values.map(visitor::visit), name)
    }
}

data class ObjectPattern(
    val identifier: String,
    val fields: List<Pair<String, Pattern>>,
    override val name: String? = null
) : Pattern {
    override fun accept(visitor: Visitor<Pattern>): Pattern {
        return ObjectPattern(identifier, fields.map { Pair(it.first, visitor.visit(it.second)) }, name)
    }
}