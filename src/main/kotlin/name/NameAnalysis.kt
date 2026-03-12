package fr.univ_lille.iut_info.name

import fr.univ_lille.iut_info.Identified
import fr.univ_lille.iut_info.NodeDeclarationStatement
import fr.univ_lille.iut_info.RewriteRuleStatement
import fr.univ_lille.iut_info.Statement
import fr.univ_lille.iut_info.expression.ExpressionAccess
import fr.univ_lille.iut_info.expression.ObjectExpression
import fr.univ_lille.iut_info.pattern.ObjectPattern
import fr.univ_lille.iut_info.type.ObjectType
import fr.univ_lille.iut_info.type.ReferenceType
import fr.univ_lille.iut_info.type.Type
import fr.univ_lille.iut_info.visitable.visit

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
                .filterIsInstance<ObjectPattern>().flatMap(this::checkObjectPattern).toList(),
            program.filterIsInstance<RewriteRuleStatement>().map { (_, _, transform) -> transform }
                .filterIsInstance<ObjectExpression>().flatMap(this::checkTransform),
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

    fun checkTransform(type: ObjectExpression): List<String> {
        val duplicateKeys =
            type.fields.associateBy({ it.first }, { (key, _) -> type.fields.count({ it.first == key }) })
                .filterValues { it > 1 }.keys

        return duplicateKeys.map { "NameError: A pattern transformation contains a duplicate field $it." }

    }

    fun checkRewriteRuleNameDefinitionAndUsage(rule: RewriteRuleStatement): List<String> {

        val existing: MutableList<String> = ArrayList()

        rule.pattern.visit { node, rec ->
            val name = node.name
            if (name != null) {
                existing.add(name)
            }
            return@visit null
        }

        val used: MutableList<String> = ArrayList()

        rule.condition.visit { node, rec ->
            if (node is ExpressionAccess.Member && node.parent == null) {
                used.add(node.identifier)
            }
            return@visit null
        }

        rule.transform.visit { node, rec ->
            if (node is ExpressionAccess.Member && node.parent == null) {
                used.add(node.identifier)
            }
            return@visit null
        }

        val duplicateKeys =
            existing.associateBy({ it }, { key -> existing.count({ it == key }) }).filterValues { it > 1 }.keys

        return listOf(
            duplicateKeys.map { "NameError: The variable $it is defined multiple times in a pattern." },
            used.toSet().filter { !(existing.contains(it)) }
                .map { "NameError: Variable $it is used but is not defined." },
        ).flatten()
    }
}