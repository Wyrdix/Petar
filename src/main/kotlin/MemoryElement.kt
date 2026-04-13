package fr.univ_lille.iut_info

abstract class MemoryElement : Visitable<MemoryElement> {
    abstract fun type(): Type

    companion object {
        fun string(value: String): MemoryString {
            return MemoryString(value)
        }

        fun number(value: Number): MemoryNumber {
            return MemoryNumber(value)
        }

        fun boolean(value: Boolean): MemoryBoolean {
            return MemoryBoolean(value)
        }

        fun undefined(): MemoryUndefined {
            return MemoryUndefined()
        }

        fun property(type: PropertyType, value: Map<String, MemoryElement>): MemoryObject {
            return MemoryObject(type, value)
        }

        fun array(type: ArrayType, value: List<MemoryElement>): MemoryArray {
            return MemoryArray(type, value)
        }
    }
}

class MemoryUndefined : MemoryElement() {

    override fun type(): Type {
        return Type.undefined
    }

    override fun toString(): String {
        return "MemoryUndefined()"
    }

    override fun accept(visitor: Visitor<MemoryElement>): MemoryElement {
        return this
    }
}

data class MemoryString(val value: String) : MemoryElement() {
    override fun type(): Type {
        return Type.string
    }

    override fun toString(): String {
        return "MemoryString(value=$value)"
    }

    override fun accept(visitor: Visitor<MemoryElement>): MemoryElement {
        return this
    }
}

data class MemoryNumber(val value: Number) : MemoryElement() {
    override fun type(): Type {
        return Type.number
    }

    override fun toString(): String {
        return "MemoryNumber(value=$value)"
    }

    override fun accept(visitor: Visitor<MemoryElement>): MemoryElement {
        return this
    }
}

data class MemoryBoolean(val value: Boolean) : MemoryElement() {
    override fun type(): Type {
        return Type.boolean
    }

    override fun toString(): String {
        return "MemoryBoolean(value=$value)"
    }

    override fun accept(visitor: Visitor<MemoryElement>): MemoryElement {
        return this
    }
}

data class MemoryObject(val type: PropertyType, val value: Map<String, MemoryElement>) : MemoryElement() {
    override fun type(): Type {
        return type
    }

    override fun toString(): String {
        return "MemoryObject(type=$type, value=${value.toSortedMap()})"
    }

    override fun accept(visitor: Visitor<MemoryElement>): MemoryElement {
        return MemoryObject(type, value.mapValues { (_, value) -> visitor.visit(value) })
    }
}

data class MemoryArray(val type: ArrayType, val value: List<MemoryElement>) : MemoryElement() {

    override fun type(): Type {
        return type
    }

    override fun toString(): String {
        return "MemoryArray(type=$type, value=$value)"
    }

    override fun accept(visitor: Visitor<MemoryElement>): MemoryElement {
        return MemoryArray(type, value.map { visitor.visit(it) })
    }
}