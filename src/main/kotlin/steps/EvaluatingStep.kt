package fr.univ_lille.iut_info.steps

import fr.univ_lille.iut_info.MemoryElement

class EvaluatingStep(override val typecheckStep: TypecheckStep) : IEvaluatingContext, ITypingContext by typecheckStep,
    ExecutionStep {
    override val memoryParentMap: MutableMap<MemoryElement, Pair<String, MemoryElement>> = HashMap()
    override val memoryAnnotationRoot: MutableMap<MemoryElement, MemoryElement> = HashMap()
    override val memoryAnnotationMap: MutableMap<MemoryElement, List<MemoryElement>> = HashMap()

    override fun run(): List<String> {
        return emptyList()
    }
}