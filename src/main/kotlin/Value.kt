package fr.univ_lille.iut_info

import fr.univ_lille.iut_info.type.NumberType
import fr.univ_lille.iut_info.type.ObjectType
import fr.univ_lille.iut_info.type.StringType
import fr.univ_lille.iut_info.type.Type

interface Value {
    fun type(): Type
}

data class StringValue(val value: String) : Value {
    override fun type(): Type {
        return StringType.instance
    }
}

data class NumberValue(val value: Float) : Value {
    override fun type(): Type {
        return NumberType.instance
    }
}

data class ObjectValue(val type: ObjectType, val values: Map<String, Value>) : Value {
    override fun type(): Type {
        return type
    }
}