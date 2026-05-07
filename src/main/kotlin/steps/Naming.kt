package fr.univ_lille.iut_info.steps

import fr.univ_lille.iut_info.*

interface INameNode {
    val parent: INameNode?

    fun addUsage(name: String, exp: Expression)

    fun getUsages(name: String): List<Expression>

    fun getAllUsages(): Map<String, List<Expression>>

    fun getUndefinedUsages(): Map<String, List<Expression>>

    fun unsageGet(name: String): Type?

    fun get(name: String): Type
}

class NameNode(override val parent: INameNode?) : INameNode {
    val nameMap: MutableMap<String, Type> = HashMap()
    val usages: MutableMap<String, List<Expression>> = HashMap()

    override fun addUsage(name: String, exp: Expression) {
        if (nameMap[name] == null && parent != null) parent.addUsage(name, exp)
        else {
            usages[name] = (usages[name] ?: emptyList()) + exp
        }
    }

    override fun getUsages(name: String): List<Expression> {
        return if (nameMap[name] == null && parent != null) parent.getUsages(name)
        else usages[name] ?: emptyList()
    }

    override fun getUndefinedUsages(): Map<String, List<Expression>> {
        return usages.filterKeys { !nameMap.containsKey(it) }
    }

    override fun getAllUsages(): Map<String, List<Expression>> {
        return usages.toMap()
    }

    override fun unsageGet(name: String): Type? {
        return nameMap[name] ?: parent?.unsageGet(name)
    }

    override fun get(name: String): Type {
        return unsageGet(name) ?: throw IllegalStateException("Could not find name: $name")
    }
}

interface INameContext {
    val program: Program
    val typeNameMap: MutableMap<String, Type>
    val patternNodeMap: MutableMap<Pattern, NameNode>
    val expressionNodeMap: MutableMap<Expression, NameNode>
    val root: NameNode
    val expressionParentMap: MutableMap<Expression, Expression?>
    val expressionChildrenMap: MutableMap<Expression, List<Expression>>
    val patternParentMap: MutableMap<Pattern, Pattern?>
    val patternChildrenMap: MutableMap<Pattern, List<Pattern>>

    fun getNameNode(expression: Expression): NameNode {
        return expressionNodeMap[expression] ?: throw IllegalStateException("Could not find name node for expression.")
    }

    fun getNameNode(expression: Pattern): NameNode {
        return patternNodeMap[expression] ?: throw IllegalStateException("Could not find name node for pattern.")
    }
}

fun INameContext.getTypeDependencies(type: Type): Set<String> {

    val processedDefinitions: MutableSet<String> = HashSet()
    val accumulation: MutableSet<String> = HashSet()


    fun auxiliary(type: Type): Set<String> {
        return when (type) {
            is ReferenceType -> setOf(type.value)
            is UnionType -> type.types.flatMap { auxiliary(it) }.toSet()
            is PrimitiveType.StringType -> setOf("String")
            is PrimitiveType.NumberType -> setOf("Number")
            is PrimitiveType.BooleanType -> setOf("Boolean")
            is PrimitiveType.UndefinedType -> setOf("undefined")
            is ArrayType -> auxiliary(type.type)
            is PropertyType -> (((type.inlineFields.map { it.second }).flatMap { auxiliary(it) }) + type.parent?.first).filterNotNull()
                .toSet()

            is AnyType -> setOf("Any")
            is BottomType -> emptySet()
            is AnyPatternType -> emptySet()
        }
    }

    accumulation.addAll(auxiliary(type))

    while (accumulation.size != processedDefinitions.size) {
        accumulation.addAll(
            accumulation.subtract(processedDefinitions)
                .mapNotNull { typeNameMap[it.apply { processedDefinitions.add(this) }] }
                .flatMap { auxiliary(it) })
    }

    return accumulation
}

fun initial(context: INameContext, expression: Expression, root: NameNode = context.root): Expression {
    val children = expression.children()
    children.forEach { context.expressionParentMap[it] = expression }
    context.expressionChildrenMap[expression] = children

    context.expressionNodeMap[expression] = root

    children.forEach { initial(context, it, root) }

    return expression
}

fun initial(context: INameContext, pattern: Pattern, root: NameNode = context.root): Pattern {
    val children = pattern.children()
    children.forEach { context.patternParentMap[it] = pattern }
    context.patternChildrenMap[pattern] = children

    context.patternNodeMap[pattern] = root

    children.forEach { initial(context, it, root) }

    val condition = pattern.condition
    if (condition != null) initial(context, condition, root)

    return pattern
}

fun fillNodes(context: INameContext, root: Expression): Expression {
    return root.visit {
        when (it) {
            is PatternMatchExpression -> fillNodes(context, it.right)
            is ExpressionAccess.Member if it.parent == null -> {
                context.getNameNode(it).addUsage(it.identifier, it)
            }

            is PropertyExpression if context.typeNameMap[it.identifier] == null -> {
                throw StepError(Step.NAME, it, "Can't instantiate property ${it.identifier} as it's not defined.")
            }

            is FunctionCallExpression if !FunctionPrototype.entries.map { proto -> proto.name }.contains(it.name) ->
                throw StepError(Step.NAME, it, "$it function is used but does not exist")

            else -> null
        }
        return@visit null
    }
}

fun fillNodes(context: INameContext, root: Pattern): Pattern {
    return root.visit { pattern ->
        val name = pattern.name
        val modifier = pattern.modifier
        val condition = pattern.condition
        var type: Type? = null

        when (pattern) {
            is ExpressionPattern -> {
                fillNodes(context, pattern.value)
            }

            is PropertyPattern -> {
                type = context.typeNameMap[pattern.identifier]

                if (type == null) throw StepError(
                    Step.NAME,
                    pattern,
                    "Cannot use property pattern '${pattern.identifier}' as no property with that name exist."
                )
            }

            else -> {}
        }

        if (name != null)
            context.patternNodeMap[pattern]?.nameMap[name] = Type.any

        if (condition != null) fillNodes(context, condition)

        return@visit null
    }
}