package fr.univ_lille.iut_info.steps

import fr.univ_lille.iut_info.*

class NameStep(override val program: Program) : ExecutionStep, INameContext {


    override val root: NameNode = NameNode(null)
    override val typeNameMap: MutableMap<String, Type> = HashMap()
    override val patternNodeMap: MutableMap<Pattern, NameNode> = HashMap()
    override val expressionNodeMap: MutableMap<Expression, NameNode> = HashMap()
    override val nameErrors: MutableList<String> = ArrayList()

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
        val rules = program.statements.filterIsInstance<ProductionRuleStatement>()

        types.forEach { typeNameMap[it.identifier] = it }

        val undefinedTypeUsages = types.flatMap { getTypeDependencies(it) }.filter { !typeNameMap.containsKey(it) }
        if (undefinedTypeUsages.isNotEmpty()) return undefinedTypeUsages.map { "$it is used in a property definition but not defined." }

        val nameContext = this
        rules.forEach {
            val ruleRoot = NameNode(root)
            initial(nameContext, it.pattern, ruleRoot)
            initial(nameContext, it.production, nameContext.getNameNode(it.pattern))
            fillNodes(nameContext, it.pattern)
            fillNodes(nameContext, it.production)
        }

        val undefinedUsages =
            (nameContext.patternNodeMap.values + nameContext.expressionNodeMap.values).flatMap { it.getUndefinedUsages().keys }

        if (undefinedUsages.isNotEmpty()) return undefinedUsages.map { "$it is used but not defined." }

        return nameErrors
    }
}