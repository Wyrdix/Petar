package fr.univ_lille.iut_info.serializer

import fr.univ_lille.iut_info.MemoryElement
import fr.univ_lille.iut_info.Type
import fr.univ_lille.iut_info.steps.ITypingContext

interface Serializer<K> {
    fun serialize(data: MemoryElement): K

    fun deserialize(root: K, type: Type, context: ITypingContext): MemoryElement
}