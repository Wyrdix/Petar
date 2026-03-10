package fr.univ_lille.iut_info

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

data class ObjectValue(val type: ObjectAlfrType, val values: Map<String, Value>) : Value {
    override fun type(): Type {
        return type
    }
}