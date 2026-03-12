package fr.univ_lille.iut_info.name

import fr.univ_lille.iut_info.*
import fr.univ_lille.iut_info.expression.Expression
import fr.univ_lille.iut_info.expression.IdentifierExpression
import fr.univ_lille.iut_info.expression.ValueExpression
import fr.univ_lille.iut_info.type.ObjectType
import fr.univ_lille.iut_info.type.ReferenceType
import fr.univ_lille.iut_info.type.Type


fun Pattern.variables(): List<String> {
    if (this is StringPattern) return emptyList()
    if (this is NumberPattern) return emptyList()
    if (this is ChildrenPattern) return this.patterns.flatMap { it.variables() }
    if (this is ObjectPattern) return listOf(
        if (this.alias != null) listOf(alias) else emptyList(),
        fields.flatMap { it.second.variables() },
        if (this.children != null) children.variables() else emptyList()
    ).flatten()
    if (this is ListPattern) return this.patterns.flatMap { it.variables() }
    throw IllegalStateException("Unknown pattern type")
}

fun Transform.variables(): List<String> {
    if (this is DeleteTransform) return emptyList()
    if (this is ExpressionTransform) return expression.variables()
    if (this is ObjectTransform) {
        return listOf(
            fields.flatMap { it.second.variables() }, if (this.children != null) children.variables() else emptyList()
        ).flatten()
    }
    if (this is ChildrenTransform) return this.transforms.flatMap { it.variables() }
    if (this is ListTransform) return this.transforms.flatMap { it.variables() }
    throw IllegalStateException("Unknown transform type")
}

fun Expression.variables(): List<String> {
    if (this is ValueExpression) return emptyList()
    if (this is IdentifierExpression) return listOf(identifiers[0])
    throw IllegalStateException("Unknown expression type")
}

fun Condition.variables(): List<String> {
    if (this is TrueCondition) return emptyList()
    if (this is AndCondition) return left.variables() + right.variables()
    if (this is OrCondition) return left.variables() + right.variables()
    if (this is EqualCondition) return left.variables() + right.variables()
    if (this is NotCondition) return operand.variables()
    throw IllegalStateException("Unknown condition type")
}

class NameAnalysis(val program: List<Statement>) {
    val names: MutableMap<String, Identified> = HashMap()
    val types: Map<String, Type>
        get() =
            names.entries.filterIsInstance<Map.Entry<String, NodeDeclarationStatement>>()
                .map { Pair(it.key, it.value.type) }
                .union(
                    setOf(
                        Pair("String", Type.string),
                        Pair("Number", Type.number)
                    )
                ).associateBy({ it.first }, { it.second })

    fun check(): List<String> {
        return listOf(
            program.filterIsInstance<Identified>().flatMap(this::checkIdentified),
            program.filterIsInstance<NodeDeclarationStatement>().flatMap(this::checkObjectGroup),
            program.filterIsInstance<NodeDeclarationStatement>().map { Pair(it.identifier, it.type) }
                .flatMap { checkObjectType(it.first, it.second) },
            program.asSequence().filterIsInstance<RewriteRuleStatement>().map { (pattern, _, _) -> pattern }
                .flatMap { pattern -> if (pattern is ChildrenPattern) pattern.patterns else listOf(pattern) }
                .filterIsInstance<ObjectPattern>().flatMap(this::checkObjectPattern).toList(),
            program.filterIsInstance<RewriteRuleStatement>().map { (_, _, transform) -> transform }
                .filterIsInstance<ObjectTransform>().flatMap(this::checkTransform),
            program.filterIsInstance<RewriteRuleStatement>()
                .flatMap(this::checkRewriteRuleNameDefinitionAndUsage)).flatten().toSet().toList()
    }

    fun resolveReference() {
        val types = types;
        types.values.filterIsInstance<ReferenceType>().forEach { it.cache = types[it.value] }
    }

    fun checkIdentified(identified: Identified): List<String> {
        return if (names.containsKey(identified.identifier)) {
            val error = "NameError: ${identified.identifier} is defined multiple times."
            listOf(error)
        } else {
            names[identified.identifier] = identified
            emptyList()
        }
    }

    fun checkObjectGroup(node: NodeDeclarationStatement): List<String> {
        return node.groups.filterNot { names.containsKey(it) }
            .map { "NameError: Node ${node.identifier} is part of group ${it}, which is not declared." }
    }

    fun checkObjectType(name: String, type: ObjectType): List<String> {
        val duplicateKeys =
            type.children.associateBy({ it.first }, { (key, _) -> type.children.count({ it.first == key }) })
                .filterValues { it > 1 }.keys

        return duplicateKeys.map { "NameError: $name contains a duplicate field $it." }
    }

    fun checkObjectPattern(type: ObjectPattern): List<String> {
        val duplicateKeys =
            type.fields.associateBy({ it.first }, { (key, _) -> type.fields.count({ it.first == key }) })
                .filterValues { it > 1 }.keys

        return duplicateKeys.map { "NameError: A patterns contains a duplicate field $it." }
    }

    fun checkTransform(type: ObjectTransform): List<String> {
        val duplicateKeys =
            type.fields.associateBy({ it.first }, { (key, _) -> type.fields.count({ it.first == key }) })
                .filterValues { it > 1 }.keys

        return duplicateKeys.map { "NameError: A pattern transformation contains a duplicate field $it." }

    }

    fun checkRewriteRuleNameDefinitionAndUsage(rule: RewriteRuleStatement): List<String> {
        val existing = rule.pattern.variables()
        val usedByCondition = rule.condition.variables()
        val usedByTransform = rule.transform.variables()

        val duplicateKeys =
            existing.associateBy({ it }, { key -> existing.count({ it == key }) }).filterValues { it > 1 }.keys

        return listOf(
            duplicateKeys.map { "NameError: The variable $it is defined multiple times in a pattern." },
            usedByCondition.filter { !(existing.contains(it)) }
                .map { "NameError: Variable $it is used in a condition but is not defined." },
            usedByTransform.filter { !(existing.contains(it)) }
                .map { "NameError: Variable $it is used in a transformation but is not defined." }).flatten()
    }
}