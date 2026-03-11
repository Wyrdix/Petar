package fr.univ_lille.iut_info

abstract class Type {
    companion object {
        val string: StringType
            get() = StringType.instance

        val number: NumberType
            get() = NumberType.instance
    }
}

data class ObjectAlfrType(
    val identifier: String, val children: List<Pair<String, Type>>
) : Type() {
    val childrenMap: Map<String, Type>
        get() = children.associateBy({ it.first }, { it.second })
}

class StringType private constructor() : Type() {
    val identifier: String
        get() = "String"

    override fun toString(): String {
        return "String"
    }

    companion object {
        val instance = StringType()
    }
}

class NumberType : Type() {
    val identifier: String
        get() = "Number"

    override fun toString(): String {
        return "Number"
    }

    companion object {
        val instance = NumberType()
    }
}

data class ReferenceType(val value: String) : Type()

data class ArrayType(val type: Type) : Type() {
    init {
        assert(type !is ArrayType)
    }

    override fun toString(): String {
        return "$type[]"
    }
}