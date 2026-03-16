package fr.univ_lille.iut_info.name

import fr.univ_lille.iut_info.*
import fr.univ_lille.iut_info.expression.ExpressionAccess
import fr.univ_lille.iut_info.expression.ObjectExpression
import fr.univ_lille.iut_info.pattern.ObjectPattern
import fr.univ_lille.iut_info.type.ArrayType
import fr.univ_lille.iut_info.type.ObjectType
import fr.univ_lille.iut_info.type.ReferenceType
import fr.univ_lille.iut_info.type.Type
import fr.univ_lille.iut_info.visitable.visit

class NameAnalysis(val program: List<Statement>) {
    val names: MutableMap<String, Identified> = HashMap()
    val types: Map<String, Type>
        get() = names.entries.filter { it.value is NodeDeclarationStatement }
            .map { (key, value) -> Pair(key, value as NodeDeclarationStatement) }.map { Pair(it.first, it.second.type) }
            .union(
                setOf(
                    Pair("String", Type.string), Pair("Number", Type.number)
                )
            ).associateBy({ it.first }, { it.second })

    fun check(): List<String> {
        return listOf(
            program.filterIsInstance<Identified>().flatMap(this::checkIdentified),
            program.filterIsInstance<NodeDeclarationStatement>().flatMap(this::checkObjectParent),
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
        val types = types

        types.values.filterIsInstance<ObjectType>().flatMap { it.childrenMap.values }
            .map { if (it is ArrayType) it.type else it }.filterIsInstance<ReferenceType>().forEach {
                it.cache = types[it.value]
            }
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

    fun checkObjectParent(node: NodeDeclarationStatement): List<String> {
        val notExisting = node.type.parents.filterNot { names.containsKey(it.identifier) }
            .map { "NameError: Node ${node.identifier} has a parent named ${it}, which is not declared." }

        if (notExisting.isNotEmpty()) return notExisting

        val exists = node.type.childrenMap.keys
        val used: MutableList<String> = ArrayList()

        node.type.parents.forEach {
            it.visit { node, _ ->
                if (node is ExpressionAccess.Member && node.parent == null) {
                    used.add(node.identifier)
                }
                return@visit null
            }
        }

        val usedNotExisting = used.filter { !exists.contains(it) }

        return usedNotExisting.map { "NameError: Name $it is used in parent construction, but it's not an object field." }
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

        rule.pattern.visit { node, _ ->
            val name = node.name
            if (name != null) {
                existing.add(name)
            }
            return@visit null
        }

        val used: MutableList<String> = ArrayList()

        rule.condition.visit { node, _ ->
            if (node is ExpressionAccess.Member && node.parent == null) {
                used.add(node.identifier)
            }
            return@visit null
        }

        rule.transform.visit { node, _ ->
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