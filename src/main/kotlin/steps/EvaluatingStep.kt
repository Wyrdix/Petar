package fr.univ_lille.iut_info.steps

import fr.univ_lille.iut_info.AnnotationRuleStatement
import fr.univ_lille.iut_info.MutableBiMap
import fr.univ_lille.iut_info.PropertyType
import fr.univ_lille.iut_info.memory.MemoryElement
import fr.univ_lille.iut_info.memory.MemoryPath
import fr.univ_lille.iut_info.visit

class EvaluatingStep(override val typecheckStep: TypecheckStep) : IEvaluatingContext, ITypingContext by typecheckStep,
    ExecutionStep {
    override val pathMemory: MutableBiMap<MemoryPath, MemoryElement> = MutableBiMap()
    override var output: fr.univ_lille.iut_info.memory.MemoryObject? = null

    override fun run(): List<String> {
        val rootInput = program.data.input?.let { it(this) } ?: return emptyList()

        val rules = program.statements.filterIsInstance<AnnotationRuleStatement>()

        initial(rootInput, MemoryPath.root(this));

        var changed = true
        while (changed) {
            changed = false
            for (statement in rules) {
                rootInput.visit { input ->
                    val pattern = statement.pattern
                    val production = statement.production
                    val restriction = calculateRestriction(input)
                    if (restriction.map { it.identifier }.contains(production.identifier)) return@visit null
                    val environments = pattern.match(this, input).iterator()
                    if (!environments.hasNext()) return@visit null
                    val env = environments.next()
                    val annotation = production.evaluate(this, env)
                    if (annotation is fr.univ_lille.iut_info.memory.MemoryObject) {
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

    fun calculateRestriction(element: MemoryElement): List<PropertyType> {
        if (element !is fr.univ_lille.iut_info.memory.MemoryObject) return emptyList()
        val annotations = getAnnotations(element)
        return listOf(element.type) + annotations.flatMap { calculateRestriction(it) }
    }
}