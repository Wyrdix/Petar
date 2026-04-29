package fr.univ_lille.iut_info.steps

import fr.univ_lille.iut_info.MemoryElement
import fr.univ_lille.iut_info.MemoryObject
import fr.univ_lille.iut_info.ProductionRuleStatement
import fr.univ_lille.iut_info.visit

class EvaluatingStep(override val typecheckStep: TypecheckStep) : IEvaluatingContext, ITypingContext by typecheckStep,
    ExecutionStep {
    override val memoryParentMap: MutableMap<MemoryElement, Pair<String, MemoryElement>> = HashMap()
    override val memoryAnnotationRoot: MutableMap<MemoryElement, MemoryElement> = HashMap()
    override val memoryAnnotationMap: MutableMap<MemoryElement, List<MemoryObject>> = HashMap()
    override var output: MemoryObject? = null

    override fun run(): List<String> {
        val rootInput = program.data.input?.let { it(this) } ?: return emptyList()

        val rules = program.statements.filterIsInstance<ProductionRuleStatement>()

        var changed = true
        while (changed) {
            changed = false
            for (statement in rules) {
                rootInput.visit { input ->
                    val pattern = statement.pattern
                    val production = statement.production
                    val restriction = if (input is MemoryObject) listOf(input.type.identifier) else emptyList()
                    if ((restriction + getAnnotations(input).map { it.type.identifier }).find { it == production.identifier } != null) return@visit null
                    val environments = pattern.match(this, input)
                    if (!environments.hasNext()) return@visit null
                    val env = environments.next()
                    val annotation = production.evaluate(this, env)
                    if (annotation is MemoryObject) {
                        changed = true
                        addAnnotation(env.choices[input] ?: input, annotation)
                    }
                    return@visit null
                }
            }
        }

        output = rootInput


        return emptyList()
    }
}