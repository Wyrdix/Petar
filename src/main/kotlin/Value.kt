package fr.univ_lille.iut_info

interface Value {
    fun type(): Type
}

class StringValue(value: String) : Value {
    override fun type(): Type {
        return StringType.instance
    }
}

class NumberValue(value: Float) : Value {
    override fun type(): Type {
        return NumberType.instance
    }
}

class ObjectValue(val type: ObjectAlfrType, val values: Map<String, Value>) : Value {
    override fun type(): Type {
        return type
    }
}