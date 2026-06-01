package fr.univ_lille.iut_info.steps

import fr.univ_lille.iut_info.*

interface ITypingContext : INameContext {
    val propertyResolved: HashMap<PropertyType, Map<String, Type>>
    val nameContext: INameContext
    val expressionSynthesized: HashMap<Expression, Type>
    val expressionChecked: HashMap<Expression, Type>
    val patternSynthesized: HashMap<Pattern, Type>
    val patternChecked: HashMap<Pattern, Type>

    fun typeSynthesis(exp: Expression, type: Type): Type {
        expressionSynthesized[exp] = type
        return type
    }

    fun typePatternSynthesis(pattern: Pattern, type: Type?): Type? {
        if (type != null) patternSynthesized[pattern] = type
        return type
    }

    fun typeChecked(exp: Expression, condition: Boolean, type: Type): Boolean {
        if (condition) {
            val alreadyChecked = expressionChecked[exp]
            if (alreadyChecked != null) {

                if (!alreadyChecked.isAssignableFrom(this, type)) expressionChecked[exp] = type
                else if (!type.isAssignableFrom(this, alreadyChecked)) {
                    throw StepError(Step.TYPE, exp, "Expression needs to be of type $type and $alreadyChecked")
                }
            } else expressionChecked[exp] = type
        }
        return condition
    }

    fun typePatternChecked(pattern: Pattern, condition: Boolean, type: Type): Boolean {
        if (condition) {
            val alreadyChecked = patternChecked[pattern]
            if (alreadyChecked != null) {

                if (!alreadyChecked.isAssignableFrom(this, type)) patternChecked[pattern] =
                    if (pattern.modifier == PatternModifier.ONE) type else Type.array(type)
                else if (!type.isAssignableFrom(this, alreadyChecked)) {
                    throw StepError(Step.TYPE, pattern, "Pattern needs to be of type $type and $alreadyChecked")
                }
            } else patternChecked[pattern] = if (pattern.modifier == PatternModifier.ONE) type else Type.array(type)
        }
        return condition
    }

    fun getSynthesizedType(exp: Expression): Type? {
        return expressionSynthesized[exp]
    }

    fun getPatternSynthesizedType(pattern: Pattern): Type? {
        return patternSynthesized[pattern]
    }

    fun getCheckedType(exp: Expression): Type? {
        return expressionChecked[exp] ?: expressionSynthesized[exp]
    }

    fun getCheckedPatternType(pattern: Pattern): Type? {
        return patternChecked[pattern] ?: patternSynthesized[pattern]
    }

    fun getType(name: String): Type {
        return typeNameMap[name] ?: throw IllegalStateException("Could not find type by name: $name $.")
    }

}

fun parseType(context: ITypingContext, type: String) : Type? {
    context.typeNameMap[type]?.let { return it }
    if(type.endsWith("[]")) {
        return parseType(context, type.substring(0, type.length-2))?.let { Type.array(it) }
    }
    return null
}

fun Type.resolveReference(context: ITypingContext): Type {
    if (this is ReferenceType) return context.getType(value)
    return this
}

fun Type.ascendants(context: ITypingContext): List<String> {
    if (this !is PropertyType) return emptyList()
    if (this.parent != null) return context.getType(this.parent.first).ascendants(context) + this.identifier
    return listOf(this.identifier)
}

fun PropertyType.getAllFields(context: ITypingContext): Map<String, Type> {
    return context.propertyResolved[this]!!
}

fun Type?.findAssignableFrom(context: ITypingContext): List<Type?> {
    if (this == null) return listOf(null)
    if (this !is PropertyType) return listOf(this)

    val types = context.typeNameMap.values
    return types.filter {
        val ascendants = it.ascendants(context)
        ascendants.contains(identifier)
    }
}

fun PropertyType.check(context: ITypingContext) {
    if (context.propertyResolved[this] != null) return

    val map: HashMap<String, Type> = HashMap()
    val parent = this.parent?.first
    if (parent != null) {
        val type = context.getType(parent)
        if (type !is PropertyType) throw StepError(Step.TYPE, this, "Parent type is not a property type.")
        type.check(context)
        map.putAll(type.getAllFields(context))
    }

    val fields = this.inlineFields.associate { it }
    if (fields.size != this.inlineFields.size) throw StepError(Step.TYPE, this, "One field is present twice.")

    if (fields.keys.intersect(map.keys)
            .isNotEmpty()
    ) throw StepError(
        Step.TYPE,
        this,
        "Parent and children contains same keys, parent type specialisation should occur in the parent call."
    )

    fields.values.find { it is ReferenceType && !context.typeNameMap.containsKey(it.value) }?.let {
        throw StepError(
            Step.TYPE,
            this, "Type $it is used but not defined."
        )
    }

    val specialisations = this.parent?.second?.associate { it } ?: emptyMap()
    if (specialisations.size != (this.parent?.second?.size
            ?: 0)
    ) throw StepError(
        Step.TYPE,
        this, "Specialisation field contains twice the same key."
    )

    if (!map.keys.containsAll(specialisations.keys)) throw StepError(
        Step.TYPE,
        this, "Some specialisation do not exist in the parent type."
    )

    for ((key, spe) in specialisations) {
        val parentKeyType = map[key]
        if (!parentKeyType!!.isAssignableFrom(
                context, spe
            )
        ) throw StepError(
            Step.TYPE,
            this,
            "Type specialization for key '$key' is not assignable from parent type ($parentKeyType is not assignable from $spe)"
        )
    }

    context.propertyResolved[this] = map + specialisations + fields
}

fun Type.isAssignableFrom(context: ITypingContext, other: Type): Boolean {

    val resolvedOther = other.resolveReference(context)
    if (resolvedOther != other) return isAssignableFrom(context, resolvedOther)
    if (other is UnionType) return other.types.all { isAssignableFrom(context, it) }

    return when (this) {
        is ReferenceType -> resolveReference(context).isAssignableFrom(context, other)
        is AnyPatternType, is AnyType -> other !is PrimitiveType.UndefinedType

        is BottomType -> false
        is UnionType -> this.types.any { it.isAssignableFrom(context, other) }

        is PropertyType -> other.ascendants(context).contains(this.identifier)

        is ArrayType -> other is ArrayType && this.type.isAssignableFrom(context, other.type)

        is PrimitiveType.BooleanType -> other is PrimitiveType.BooleanType
        is PrimitiveType.NumberType -> other is PrimitiveType.NumberType
        is PrimitiveType.StringType -> other is PrimitiveType.StringType
        is PrimitiveType.UndefinedType -> other is PrimitiveType.UndefinedType
    }
}

fun Pattern.typeCheck(context: ITypingContext, type: Type, listPattern: Boolean = false): Boolean {
    if (this.modifier != PatternModifier.ONE && !listPattern) {
        throw StepError(Step.TYPE, this, "Cannot use a list pattern (* or +) outside of a list pattern context.")
    }

    val checkedType = context.getCheckedPatternType(this)
    if (checkedType != null) return context.typePatternChecked(this, true, type)

    if (type is ReferenceType) return typeCheck(context, type.resolveReference(context))

    val synthesizedType = this.typeSynthesis(context, listPattern)
    if (synthesizedType != null)
        if (type.isAssignableFrom(context, synthesizedType)) return context.typePatternChecked(
            this, true, type
        )
        else
            throw StepError(Step.TYPE, this, "$type is not assignable from $synthesizedType")

    return when {
        type is AnyPatternType -> context.typePatternChecked(this, true, Type.anyPattern)

        this is ArrayPattern ->
            if (type !is ArrayType) false
            else context.typePatternChecked(
                this,
                this.values.all { it.typeCheck(context, type.type, true) },
                type
            )


        type is AnyType -> context.typePatternChecked(this, true, Type.any)

        else -> false
    }.let {
        val name = this.name
        if (!it || name == null) return@let it
        else {
            context.getNameNode(this).nameMap[name] = context.getCheckedPatternType(this)!!
            return@let true
        }
    }
}

fun Pattern.typeSynthesis(context: ITypingContext, listPattern: Boolean = false): Type? {
    if (this.modifier != PatternModifier.ONE && !listPattern) {
        throw StepError(
            Step.TYPE,
            this,
            "Pattern use a list modifier (+ or *) but is not used in a list context."
        )
    }
    val alreadySynthesized = context.getPatternSynthesizedType(this)
    if (alreadySynthesized != null) return alreadySynthesized

    return when (this) {
        is ExpressionPattern -> context.typePatternSynthesis(
            this, this.value.typeSynthesis(context)
        )

        is RegexPattern -> context.typePatternSynthesis(this, Type.string)

        is PropertyPattern -> {
            val type = context.getType(this.identifier)
            if (type !is PropertyType) throw StepError(
                Step.TYPE,
                this,
                "Property pattern isn't compatible with required pattern type"
            )
            else if (!type.getAllFields(context).keys.containsAll(this.fields.keys)) throw StepError(
                Step.TYPE,
                this,
                "Extraneous fields (${
                    this.fields.keys.subtract(type.getAllFields(context).keys).joinToString(separator = ", ") { it }
                }, available ones : ${type.getAllFields(context).keys.joinToString(separator = ", ") { it }})"
            )
            else
                type.getAllFields(context).forEach {
                    val fieldValue = this.fields[it.key]
                    if (!(fieldValue?.typeCheck(context, it.value) ?: true)) {
                        throw StepError(
                            Step.TYPE,
                            fieldValue,
                            "Pattern is not of type ${it.value} (it is of type ${context.patternChecked[fieldValue] ?: context.patternSynthesized[fieldValue]})"
                        )
                    }
                }.let { type }
        }

        else -> null
    }.let {
        val name = this.name
        if (it == null || name == null) return@let it
        else {
            context.getNameNode(this).nameMap[name] = it
            return@let it
        }
    }
}

fun Expression.typeCheck(context: ITypingContext, type: Type): Boolean {
    val checkedType = context.getCheckedType(this)
    if (checkedType != null) return context.typeChecked(this, true, type)

    if (type is ReferenceType) return typeCheck(context, type.resolveReference(context))

    val synthesizedType = this.typeSynthesis(context)
    if (synthesizedType != null) return type.isAssignableFrom(context, synthesizedType)

    return when (this) {
        is BinaryExpression.And, is BinaryExpression.Or -> context.typeChecked(
            this, this.left.typeCheck(
                context, Type.boolean
            ) && this.right.typeCheck(
                context, Type.boolean
            ), Type.boolean
        )

        is BinaryExpression.Multiply, is BinaryExpression.Divide, is BinaryExpression.Plus, is BinaryExpression.Minus -> context.typeChecked(
            this, this.left.typeCheck(
                context, Type.number
            ) && this.right.typeCheck(
                context, Type.number
            ), Type.number
        )

        is BinaryExpression.Lower -> context.typeChecked(
            this, this.left.typeCheck(
                context, Type.number
            ) && this.right.typeCheck(
                context, Type.number
            ), Type.boolean
        )


        is UnaryExpression.Negate -> context.typeChecked(
            this, this.operand.typeCheck(context, Type.boolean), Type.boolean
        )

        is UnaryExpression.Opposite -> context.typeChecked(
            this, this.operand.typeCheck(context, Type.number), Type.number
        )


        is PatternMatchExpression -> context.typeChecked(
            this, Type.boolean.isAssignableFrom(context, type), Type.boolean
        )

        is ArrayExpression -> if (type !is ArrayType) false
        else context.typeChecked(this, this.values.all { this.typeCheck(context, type.type) }, type)

        is ExpressionAccess.Index, is ExpressionAccess.Member, is LiteralExpression.EBoolean, is LiteralExpression.ENumber, is LiteralExpression.EString, is LiteralExpression.EUndefined, is PropertyExpression, is FunctionCallExpression -> false
    }
}

fun Expression.typeSynthesis(context: ITypingContext): Type? {
    val alreadySynthesized = context.getSynthesizedType(this)
    if (alreadySynthesized != null) return alreadySynthesized

    return when (this) {
        is LiteralExpression -> {
            context.typeSynthesis(
                this, when (this) {
                    is LiteralExpression.EBoolean -> Type.boolean
                    is LiteralExpression.EString -> Type.string
                    is LiteralExpression.ENumber -> Type.number
                    is LiteralExpression.EUndefined -> Type.undefined
                }
            )
        }

        is ExpressionAccess.Member -> {
            val parent = this.parent
            if (parent == null) context.typeSynthesis(
                this, context.getNameNode(this).get(this.identifier)
            ) else parent.typeSynthesis(context).let {
                context.typeSynthesis(
                    this,
                    if (it is PropertyType) it.getAllFields(context)[this.identifier]
                        ?: throw StepError(
                            Step.TYPE,
                            it,
                            "Unknown ${it.identifier} field : ${this.identifier}"
                        ) else throw StepError(
                        Step.TYPE,
                        it ?: this,
                        "Expected identifier to be an instance of a property type."
                    )
                )
            }
        }

        is ExpressionAccess.Index -> {
            val arrayType = this.parent.typeSynthesis(context)
            if (arrayType !is ArrayType) null
            else if (this.expression.typeCheck(context, Type.number)) context.typeSynthesis(
                this, arrayType.type
            )
            else null
        }

        is FunctionCallExpression -> {
            val prototype = FunctionPrototype.valueOf(this.name)

            val argsType =
                if (prototype.arbitraryLast) prototype.args.toList() + List(0.coerceAtLeast(this.arguments.size - prototype.args.size)) { prototype.args.last() }
                else prototype.args.toList()

            val argTypeCheck = argsType.zip(this.arguments).all { (type, pattern) ->
                val typeCheck = pattern.typeCheck(context, type)
                typeCheck
            }

            if (argTypeCheck) context.typeSynthesis(this, prototype.returnType)
            else throw StepError(
                Step.TYPE,
                this,
                "Function arguments do not type check."
            )
        }

        is PropertyExpression -> {
            val type = context.getType(this.identifier)
            if (type !is PropertyType) null
            else {
                val typeFields = type.fields
                val declaredFields = fields
                val inferredFields = typeFields.filterValues { it.isAssignableFrom(context, Type.undefined) }
                    .mapValues { LiteralExpression.EUndefined() } + declaredFields
                val parentType =
                    type.parent?.let { Type.reference(it.first) }?.resolveReference(context) as PropertyType?

                val parent = parent
                if (inferredFields.keys == typeFields.keys) {
                    val expressionTypeMap = inferredFields.mapValues { Pair(it.value, typeFields[it.key]!!) }
                    var errorPair: Pair<Expression, Type>? = null
                    if (expressionTypeMap.values.any {
                            val typeCheck = it.first.typeCheck(context, it.second)
                            if (!typeCheck) errorPair = it
                            !typeCheck
                        }) throw StepError(
                        Step.TYPE,
                        errorPair!!.first,
                        "Expression is supposed to be of type ${errorPair.second}."
                    )
                    else {
                        val uselessParent =
                            parent == null && (parentType == null || parentType.getAllFields(context).isEmpty())
                        val usefulParent = parent != null && parentType != null && parent.typeCheck(
                            context, parentType
                        )
                        if (!uselessParent && !usefulParent) {
                            if (parent != null && parentType == null) {
                                throw StepError(
                                    Step.TYPE,
                                    this,
                                    "Extraneous parent expression."
                                )
                            }
                            throw StepError(
                                Step.TYPE,
                                this,
                                "Missing parent expression."
                            )

                        } else context.typeSynthesis(this, type)
                    }
                } else throw StepError(
                    Step.TYPE,
                    this,
                    "Missing some property fields : ${
                        typeFields.keys.subtract(inferredFields.keys).joinToString(separator = ", ") { it }
                    }"
                )
            }
        }

        else -> null
    }
}