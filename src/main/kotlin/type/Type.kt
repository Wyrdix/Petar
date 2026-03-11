package fr.univ_lille.iut_info.type

abstract class Type {
    companion object {
        val string: StringType
            get() = StringType.instance

        val number: NumberType
            get() = NumberType.instance
    }
}

class StringType private constructor() : Type() {
    override fun toString(): String {
        return "String"
    }

    companion object {
        val instance = StringType()
    }
}

class NumberType : Type() {
    override fun toString(): String {
        return "Number"
    }

    companion object {
        val instance = NumberType()
    }
}

data class ObjectType(
    val identifier: String, val children: List<Pair<String, Type>>
) : Type() {
    val childrenMap: Map<String, Type>
        get() = children.associateBy({ it.first }, { it.second })
}

data class ReferenceType(val value: String) : Type() {
    var cache: Type? = null
}

data class ArrayType(val type: Type) : Type() {
    init {
        assert(type !is ArrayType)
    }

    override fun toString(): String {
        return "$type[]"
    }
}