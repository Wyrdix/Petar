package fr.univ_lille.iut_info

class Environment {

    val types: Map<String, StringType> = HashMap()

    fun resultTypeIdentifier(value: String): Type {
        if (value == "String") return StringType.instance
        if (value == "Number") return NumberType.instance

        return types[value] ?: throw IllegalStateException("Type identifier '$value' do not exist.")
    }
}