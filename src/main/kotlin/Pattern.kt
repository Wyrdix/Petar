package fr.univ_lille.iut_info

import fr.univ_lille.iut_info.memory.*

abstract class Pattern : Visitable<Pattern> {
    abstract val name: String?

    var checkedType: Type? = null

    fun singleton(element: MemoryElement): Map<String, MemoryElement> {
        val first = name
        return if (first == null) emptyMap() else mapOf(Pair(first, element))
    }

    abstract fun evaluate(element: MemoryElement): Map<String, MemoryElement>?

    abstract fun typecheck(expected: Type): Boolean
}

abstract class LiteralPattern : Pattern() {

    override fun accept(visitor: Visitor<Pattern>): Pattern {
        return this
    }

    data class PString(val value: String, override val name: String? = null) : LiteralPattern() {
        override fun evaluate(element: MemoryElement): Map<String, MemoryElement>? {
            return if (element is MemoryString && element.value == value) singleton(element) else null
        }

        override fun typecheck(expected: Type): Boolean {
            val typechecking = expected is StringType

            if(typechecking) checkedType = expected
            return typechecking
        }
    }

    data class PNumber(val value: Float, override val name: String? = null) : LiteralPattern() {
        override fun evaluate(element: MemoryElement): Map<String, MemoryElement>? {
            return if (element is MemoryNumber && element.value == value) singleton(element) else null
        }

        override fun typecheck(expected: Type): Boolean {
            val typechecking = expected is NumberType

            if(typechecking) checkedType = expected
            return typechecking
        }
    }

    data class PBoolean(val value: Boolean, override val name: String? = null) : LiteralPattern() {
        override fun evaluate(element: MemoryElement): Map<String, MemoryElement>? {
            return if (element is MemoryBoolean && element.value == value) singleton(element) else null
        }


        override fun typecheck(expected: Type): Boolean {
            val typechecking = expected is BooleanType

            if(typechecking) checkedType = expected
            return typechecking
        }
    }
}

data class ArrayPattern(val values: List<Pattern>, override val name: String? = null) : Pattern() {
    override fun accept(visitor: Visitor<Pattern>): Pattern {
        return ArrayPattern(values.map(visitor::visit), name)
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

    override fun typecheck(expected: Type): Boolean {
        if (expected !is ArrayType) return false
        val elementType = expected.type
        val typechecking = !values.map { pattern -> pattern.typecheck(elementType) }.contains(false)
        if(typechecking) checkedType = expected
        return typechecking
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

    override fun evaluate(element: MemoryElement): Map<String, MemoryElement>? {
        return if (element !is MemoryObject || element.type.identifier != this.identifier) null else {
            val evaluations = this.fieldsMap.map { (key, pattern) -> pattern.evaluate(element.value[key]!!) }
            if (evaluations.contains(null)) null else {
                evaluations.filterNotNull().union(listOf(singleton(element))).flatMap { it.entries }
                    .associateBy({ it.key }, { it.value })
            }
        }
    }

    override fun typecheck(expected: Type): Boolean {
        if (expected !is ObjectType) return false
        if (identifier != expected.identifier) return false

        val typeChildrenMap = expected.childrenMap
        val fieldsMap = fieldsMap

        if (fieldsMap.keys.intersect(typeChildrenMap.keys).size != fieldsMap.size) return false

        val typechecking = !fieldsMap.map { (key, pattern) ->
            pattern.typecheck(typeChildrenMap[key]!!)
        }.contains(false)

        if(typechecking) checkedType = expected
        return typechecking
    }
}