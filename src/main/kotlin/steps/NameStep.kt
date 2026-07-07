package fr.univ_lille.iut_info.steps

import fr.univ_lille.iut_info.*

class NameStep(override val program: Program) : ExecutionStep, INameContext {
    override val root: NameNode = NameNode(null)
    override val typeNameMap: MutableMap<String, Type> = HashMap()
    override val patternNodeMap: MutableMap<Pattern, NameNode> = HashMap()
    override val expressionNodeMap: MutableMap<Expression, NameNode> = HashMap()

    override val expressionParentMap: MutableMap<Expression, Expression?> = HashMap()
    override val expressionChildrenMap: MutableMap<Expression, List<Expression>> = HashMap()
    override val patternParentMap: MutableMap<Pattern, Pattern?> = HashMap()
    override val patternChildrenMap: MutableMap<Pattern, List<Pattern>> = HashMap()

    init {
        typeNameMap[Type.undefined.toString()] = Type.undefined
        typeNameMap[Type.string.toString()] = Type.string
        typeNameMap[Type.number.toString()] = Type.number
        typeNameMap[Type.boolean.toString()] = Type.boolean
        typeNameMap[Type.any.toString()] = Type.any
    }

    override fun run(): List<String> {


        val types = program.statements.filterIsInstance<PropertyDeclarationStatement>().map { it.type }
        val rules = program.statements.filterIsInstance<AnnotationRuleStatement>()

        types.forEach { typeNameMap[it.identifier] = it }

        types.forEach { type ->
            val deps = getTypeDependencies(type).filter { !typeNameMap.containsKey(it) }
            deps.forEach {
                throw StepError(
                    Step.NAME,
                    type,
                    "$it is used in a property definition but not defined."
                )
            }
        }

        val checkNode: (NameNode) -> Unit = { node ->

            val undefinedUsages = node.getUndefinedUsages()

            undefinedUsages.entries.flatMap { entry -> entry.value.map { Pair(entry.key, it) } }.forEach {
                throw StepError(
                    Step.NAME,
                    it.second,
                    "'${it.first}' identifier is used but not defined."
                )
            }
        }

        rules.forEach { rule ->
            initial(this, rule.pattern, root)
            fillNodes(this, rule.pattern)

            rule.acts.forEach { act ->
                act.attaching?.let { initial(this, it, getNameNode(rule.pattern)) }
                initial(this, act.attachment, getNameNode(rule.pattern))
            }

            rule.acts.forEach { act ->
                act.attaching?.let { fillNodes(this, it) }
                fillNodes(this, act.attachment)
            }
            checkNode(getNameNode(rule.pattern))
        }

        checkNode(root)

        (patternNodeMap.values + expressionNodeMap.values).forEach(checkNode)

        return emptyList()
    }
}