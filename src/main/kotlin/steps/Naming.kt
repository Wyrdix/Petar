package fr.univ_lille.iut_info.steps

import fr.univ_lille.iut_info.*

interface INameNode {
    val parent: INameNode?

    fun addUsage(name: String, exp: Expression)

    fun getUsages(name: String): List<Expression>

    fun getUsages(): Map<String, List<Expression>>

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

    override fun getUsages(): Map<String, List<Expression>> {
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
    val typeNameMap: MutableMap<String, Type>
    val patternNodeMap: MutableMap<Pattern, NameNode>
    val expressionNodeMap: MutableMap<Expression, NameNode>
    val root: NameNode;
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

fun getTypeDependencies(type: Type): List<String> {
    return when (type) {
        is ReferenceType -> listOf(type.value)
        is NullableType -> getTypeDependencies(type.type)
        is PrimitiveType.StringType -> listOf("String")
        is PrimitiveType.NumberType -> listOf("Number")
        is PrimitiveType.BooleanType -> listOf("Boolean")
        is ArrayType -> getTypeDependencies(type.type)
        is PropertyType -> (((type.children.map { it.second }).flatMap { getTypeDependencies(it) }) + type.parent?.first).filterNotNull()

        else -> emptyList()
    }
}

fun initial(context: INameContext, parent: Expression, root: NameNode = context.root): Expression {

    class ChildrenCollector : Visitor<Expression> {
        val children: ArrayList<Expression> = ArrayList()
        override fun visit(obj: Expression): Expression {
            children.add(obj)
            context.expressionNodeMap[obj] = context.expressionNodeMap[parent] ?: root
            if (obj is PatternMatchExpression) {
                context.expressionNodeMap[obj] = NameNode(context.expressionNodeMap[obj])
            }
            return initial(context, obj)
        }
    }

    val collector = ChildrenCollector()
    parent.accept(collector)
    collector.children.forEach { context.expressionParentMap[it] = parent }
    context.expressionChildrenMap[parent] = collector.children.toList()

    return parent
}

fun initial(context: INameContext, parent: Pattern, root: NameNode = context.root): Pattern {

    class ChildrenCollector : Visitor<Pattern> {
        val children: ArrayList<Pattern> = ArrayList()
        override fun visit(obj: Pattern): Pattern {
            children.add(obj)
            context.patternNodeMap[obj] = context.patternNodeMap[parent] ?: root
            if (obj is ExpressionPattern) {
                context.patternNodeMap[obj] = NameNode(context.patternNodeMap[obj])
            }
            return initial(context, obj)
        }
    }

    val collector = ChildrenCollector()
    parent.accept(collector)
    collector.children.forEach { context.patternParentMap[it] = parent }
    context.patternChildrenMap[parent] = collector.children.toList()

    return parent
}

fun fillNodes(context: INameContext, root: Expression): Expression {
    return root.visit {
        if (it is PatternMatchExpression) fillNodes(context, it.right);
        if (it is ExpressionAccess.Member && it.parent == null) {
            context.getNameNode(it).addUsage(it.identifier, it)
        }
        return@visit null
    }
}

fun fillNodes(context: INameContext, root: Pattern): Pattern {
    return root.visit { pattern ->
        val name = pattern.name
        val modifier = pattern.modifier
        val condition = pattern.condition
        var type: Type? = null;

        when (pattern) {
            is ExpressionPattern -> {
                fillNodes(context, pattern.value)
            }

            is PropertyPattern -> {
                type = context.typeNameMap[pattern.identifier] ?: Type.bottom

                pattern.values.forEach { (key, pattern) ->
                    val name = pattern.name
                    val modifier = pattern.modifier
                    if (name != null && type is PropertyType) if (modifier == PatternModifier.ONE) context.patternNodeMap[pattern]?.nameMap[name] =
                        type.childrenMap[key] ?: Type.bottom
                    else context.patternNodeMap[pattern]?.nameMap[name] =
                        Type.array(type.childrenMap[key] ?: Type.bottom)
                    fillNodes(context, pattern)
                }
            }

            is ArrayPattern -> {
                pattern.values.forEach { fillNodes(context, it) }
            }

            is UnorderedArrayPattern -> {
                pattern.values.forEach { fillNodes(context, it) }
            }
        }

        if (name != null && type != null) if (modifier == PatternModifier.ONE) context.patternNodeMap[pattern]?.nameMap[name] =
            type
        else context.patternNodeMap[pattern]?.nameMap[name] = Type.array(type)

        if (condition != null) fillNodes(context, condition)

        return@visit null
    }
}