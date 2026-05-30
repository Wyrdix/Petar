package fr.univ_lille.iut_info.serializer

import fr.univ_lille.iut_info.memory.MemoryElement
import fr.univ_lille.iut_info.Type
import fr.univ_lille.iut_info.steps.IEvaluatingContext
import fr.univ_lille.iut_info.steps.ITypingContext

interface Serializer<K> {
    fun serialize(data: MemoryElement, context: IEvaluatingContext?): K

    fun deserialize(root: K, context: ITypingContext, typecheck: Type? = null): MemoryElement
}