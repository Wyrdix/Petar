package fr.univ_lille.iut_info.steps

import fr.univ_lille.iut_info.*

fun Expression.identifiers(): List<String> {
    return this.mapNotNull { node ->
        if (node is ExpressionAccess.Member && node.parent == null) node.identifier else null
    }
}

fun Pattern.identifiers(): List<String> {
    return this.mapNotNull { it.name }
}

class NameStep(val program: Program) : ExecutionStep {
    val names: MutableMap<String, ObjectType> = HashMap()
    val types: Map<String, Type>
        get() = names.entries.map { (key, value) -> Pair(key, value) }
            .union(
                setOf(
                    Pair("String", Type.string), Pair("Number", Type.number), Pair("Boolean", Type.boolean)
                )
            ).associateBy({ it.first }, { it.second })

    var roots: List<ObjectType> = emptyList()

    override fun run(): List<String> {

        val errors = listOf(
            program.statements.filterIsInstance<TypeDeclarationStatement>().map { it.type }
                .flatMap(this::checkIdentified),
            program.statements.filterIsInstance<TypeDeclarationStatement>().flatMap(this::checkObjectParent),
            program.statements.filterIsInstance<TypeDeclarationStatement>().map { Pair(it.identifier, it.type) }
                .flatMap { checkObjectType(it.first, it.second) },
            program.statements.asSequence().filterIsInstance<RewriteRuleStatement>().map { (pattern, _, _) -> pattern }
                .filterIsInstance<ObjectPattern>().flatMap(this::checkObjectPattern).toList(),
            program.statements.filterIsInstance<RewriteRuleStatement>().map { (_, _, transform) -> transform }
                .filterIsInstance<ObjectExpression>().flatMap(this::checkTransform),
            program.statements.filterIsInstance<RewriteRuleStatement>()
                .flatMap(this::checkRewriteRuleNameDefinitionAndUsage)

        ).flatten().toSet().toMutableList()

        roots =
            program.name.names.values
                .filter {
                    it.identifier.lowercase() == "root"
                            || it.parents.find { parent -> parent.identifier.lowercase() == "root" } != null
                }
        if (roots.isEmpty()) errors.addLast("NameError: No Root type could be found. Either defined a Node named 'Root' to represent the whole document, the root node can also be used as a parent to other nodes.")

        if (errors.isEmpty()) resolveReference()

        return errors
    }

    fun resolveReference() {
        val types = types

        types.values.filterIsInstance<ObjectType>().flatMap { it.childrenMap.values }
            .map { if (it is ArrayType) it.type else it }.filterIsInstance<ReferenceType>().forEach {
                it.cache = types[it.value]
            }
    }

    fun checkIdentified(identified: ObjectType): List<String> {
        return if (names.containsKey(identified.identifier)) {
            val error = "NameError: ${identified.identifier} is defined multiple times."
            listOf(error)
        } else {
            names[identified.identifier] = identified
            emptyList()
        }
    }

    fun checkObjectParent(type: TypeDeclarationStatement): List<String> {
        val notExisting = type.type.parents.filterNot { names.containsKey(it.identifier) }
            .map { "NameError: Type ${type.identifier} has a parent named ${it}, which is not declared." }

        if (notExisting.isNotEmpty()) return notExisting

        val exists = type.type.childrenMap.keys
        val used: List<String> = type.type.parents.flatMap { it.identifiers() }

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

        val existing: List<String> = rule.pattern.identifiers()

        val used: MutableList<String> = ArrayList()

        used.addAll(rule.condition.identifiers())
        used.addAll(rule.transform.identifiers())

        val duplicateKeys =
            existing.associateBy({ it }, { key -> existing.count({ it == key }) }).filterValues { it > 1 }.keys

        return listOf(
            duplicateKeys.map { "NameError: The variable $it is defined multiple times in a pattern." },
            used.toSet().filter { !(existing.contains(it)) }
                .map { "NameError: Variable $it is used but is not defined." },
        ).flatten()
    }
}