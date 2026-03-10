package fr.univ_lille.iut_info

abstract class Type {
    companion object {
        val string: StringType
            get() = StringType.instance

        val number: NumberType
            get() = NumberType.instance
    }
}

class ObjectAlfrType(
    val identifier: String, val children: Map<String, Type>
) : Type()

class StringType private constructor() : Type() {
    val identifier: String
        get() = "String"

    companion object {
        val instance = StringType()
    }
}

class NumberType : Type() {
    val identifier: String
        get() = "Number"

    companion object {
        val instance = NumberType()
    }
}

class ReferenceType(value: String) : Type()

class ArrayType(val type: Type) : Type() {
    init {
        assert(type !is ArrayType)
    }
}