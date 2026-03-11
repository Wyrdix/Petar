package fr.univ_lille.iut_info.name

import fr.univ_lille.iut_info.*

class NameAnalysis(val program: List<Statement>) {
    val names: MutableMap<String, Identified> = HashMap()

    fun check(): List<String> {
        return listOf(
            program.filterIsInstance<Identified>().flatMap(this::checkIdentified),
            program.filterIsInstance<NodeDeclarationStatement>().map { Pair(it.identifier, it.type) }
                .flatMap { checkObjectType(it.first, it.second) },
            program.asSequence().filterIsInstance<RewriteRuleStatement>().map { (pattern, _, _) -> pattern }
                .flatMap { pattern -> if (pattern is ChildrenPattern) pattern.patterns else listOf(pattern) }
                .filterIsInstance<ObjectPattern>()
                .flatMap(this::checkObjectPattern).toList(),
            program.filterIsInstance<RewriteRuleStatement>().map { (_, _, transform) -> transform }
                .filterIsInstance<ObjectTransform>()
                .flatMap(this::checkTransform)
        ).flatten().toSet().toList()
    }

    fun checkIdentified(identified: Identified): List<String> {
        return if (names.containsKey(identified.identifier)) {
            val error = "NameError: ${identified.identifier} is defined multiple times."
            listOf(error)
        } else emptyList()
    }

    fun checkObjectType(name: String, type: ObjectAlfrType): List<String> {
        val duplicateKeys =
            type.children.associateBy({ it.first }, { (key, _) -> type.children.count({ it.first == key }) })
                .filterValues { it > 1 }.keys

        return duplicateKeys.map { "NameError: $name contains a duplicate field $it" }
    }

    fun checkObjectPattern(type: ObjectPattern): List<String> {
        val duplicateKeys =
            type.fields.associateBy({ it.first }, { (key, _) -> type.fields.count({ it.first == key }) })
                .filterValues { it > 1 }.keys

        return duplicateKeys.map { "NameError: A patterns contains a duplicate field $it" }
    }

    fun checkTransform(type: ObjectTransform): List<String> {
        val duplicateKeys =
            type.fields.associateBy({ it.first }, { (key, _) -> type.fields.count({ it.first == key }) })
                .filterValues { it > 1 }.keys

        return duplicateKeys.map { "NameError: A pattern transformation contains a duplicate field $it" }

    }
}