package fr.univ_lille.iut_info.memory

import fr.univ_lille.iut_info.PropertyType
import fr.univ_lille.iut_info.Visitable
import fr.univ_lille.iut_info.Visitor
import fr.univ_lille.iut_info.steps.IEvaluatingContext
import fr.univ_lille.iut_info.visit

class MemoryPath private constructor(val context: IEvaluatingContext, val nodes: List<MemoryNode>): Visitable<MemoryPath> {

    private fun goto(node: MemoryNode): MemoryPath {
        return MemoryPath(context, nodes + listOf(node));
    }

    fun resolve(): MemoryElement {
        return context.pathMemory[this]!!;
    }

    override fun toString(): String {
        return nodes.joinToString(separator = "") { it.toString() }
    }

    override fun hashCode(): Int {
        return toString().hashCode()
    }

    fun parent(): MemoryPath {
        if(nodes.isEmpty()) return this
        return MemoryPath(context, nodes.subList(0, nodes.size-1))
    }

    fun children(): List<MemoryPath> {
        val prefix = this.toString()
        return context.pathMemory.keys.filter {
            val path = it.toString()
            path.startsWith(prefix) && it.nodes.size == nodes.size + 1
        }
    }

    fun goto(key: String): MemoryPath {
        return goto(KeyNode(key, nodes.isEmpty()));
    }

    fun goto(index: Int): MemoryPath {
        return goto(IndexNode(index));
    }

    fun goto(type: PropertyType): MemoryPath {
        if(nodes.lastOrNull() is TypeNode) throw IllegalStateException("Cannot have two type node path consecutively.")
        return goto(TypeNode(type ));
    }

    override fun accept(visitor: Visitor<MemoryPath>): MemoryPath {

        children().forEach(visitor::visit)

        return this;
    }

    companion object {
        fun root(context: IEvaluatingContext): MemoryPath {
            return MemoryPath(context, emptyList())
        }

        fun parse(context: IEvaluatingContext, raw: String, prefix: MemoryPath = root(context)): MemoryPath? {
            if(raw.isEmpty()) return root(context)
            if(raw.startsWith("[")) {
                val end = raw.indexOf(']')
                val index = raw.substring(1, end).toInt()
                return parse(context, raw.substring(end+1), prefix.goto(index))
            }
            if(raw.startsWith('(')) {
                val end = raw.indexOf(')')
                val type = raw.substring(1, end).let { context.typeNameMap[it] as? PropertyType }
                if(type == null) return null
                return parse(context, raw.substring(end+1),prefix.goto(type))
            }
            if(raw.contains('.')) {
                val end = raw.indexOf('.');
                val key = raw.substring(1, end)
                return parse(context, raw.substring(end+1), prefix.goto(key))
            }
            return prefix.goto(raw)
        }
    }

    sealed interface MemoryNode
    data class KeyNode(val key: String, val root: Boolean = false): MemoryNode {
        override fun toString(): String {
            return "${if (root) "" else "." }$key"
        }
    }
    data class IndexNode(val index: Int): MemoryNode {
        override fun toString(): String {
            return "[$index]"
        }
    }
    data class TypeNode(val type: PropertyType): MemoryNode  {
        override fun toString(): String {
            return "(${type.identifier})"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MemoryPath

        return nodes == other.nodes
    }
}