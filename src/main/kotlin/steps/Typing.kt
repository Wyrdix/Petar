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
                    expressionChecked[exp] = Type.bottom
                    return false
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
                    patternChecked[pattern] = Type.bottom
                    return false
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
        return typeNameMap[name] ?: throw IllegalStateException("Could not find type by name: $name.")
    }


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

fun PropertyType.check(context: ITypingContext) {
    if (context.propertyResolved[this] != null) return

    val map: HashMap<String, Type> = HashMap()
    val parent = this.parent?.first
    if (parent != null) {
        val type = context.getType(parent)
        if (type !is PropertyType) throw IllegalStateException("Type parent is not a property type")
        type.check(context)
        map.putAll(type.getAllFields(context))
    }

    val fields = this.inlineFields.associate { it }
    if (fields.size != this.inlineFields.size) throw IllegalStateException("Children list contains twice the same key")

    if (fields.keys.intersect(map.keys)
            .isNotEmpty()
    ) throw IllegalStateException("Parent and children contains same keys, parent type specialisation should occur in the parent call.")

    fields.values.find { it is ReferenceType && !context.typeNameMap.containsKey(it.value) }?.let {
        throw IllegalStateException("Type $it is used but not defined.")
    }

    val specialisations = this.parent?.second?.associate { it } ?: emptyMap()
    if (specialisations.size != (this.parent?.second?.size
            ?: 0)
    ) throw IllegalStateException("Specialisation field contains twice the same key.")

    if (!map.keys.containsAll(specialisations.keys)) throw IllegalStateException("Some specialisation do not exist in the parent type.")

    for ((key, spe) in specialisations) {
        val parentKeyType = map[key]
        if (!parentKeyType!!.isAssignableFrom(
                context, spe
            )
        ) throw IllegalStateException("Type specialization for key '$key' is not assignable from parent type ($parentKeyType is not assignable from $spe)")
    }

    context.propertyResolved[this] = map + specialisations + fields
}

fun Type.isAssignableFrom(context: ITypingContext, other: Type): Boolean {

    val resolvedOther = other.resolveReference(context)
    if (resolvedOther != other) return isAssignableFrom(context, resolvedOther)
    if (other is UnionType) return other.types.all { isAssignableFrom(context, it) }

    return when (this) {
        is ReferenceType -> resolveReference(context).isAssignableFrom(context, other)
        is AnyPatternType,
        is AnyType -> other !is PrimitiveType.UndefinedType

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
        return context.typePatternChecked(this, true, Type.bottom)
    }

    val checkedType = context.getCheckedPatternType(this)
    if (checkedType != null) return context.typePatternChecked(this, true, type)

    if (type is ReferenceType) return typeCheck(context, type.resolveReference(context))

    val synthesizedType = this.typeSynthesis(context, listPattern)
    if (synthesizedType != null) return context.typePatternChecked(
        this, type.isAssignableFrom(context, synthesizedType), type
    )

    when {
        type is AnyPatternType -> {
            return context.typePatternChecked(this, true, Type.anyPattern)
        }

        this is ArrayPattern -> {
            if (type !is ArrayType) return false
            return context.typePatternChecked(this, this.values.all { it.typeCheck(context, type.type, true) }, type)
        }

        type is AnyType -> {
            return context.typePatternChecked(this, true, Type.any)
        }

        else -> return false
    }
}

fun Pattern.typeSynthesis(context: ITypingContext, listPattern: Boolean = false): Type? {
    if (this.modifier != PatternModifier.ONE && !listPattern) {
        context.typePatternChecked(this, true, Type.bottom)
        return Type.bottom
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
            if (type !is PropertyType) null
            else if (!type.fields.keys.containsAll(this.fields.keys)) null
            else if (!type.fields.all {
                    val fieldValue = this.fields[it.key]
                    fieldValue?.typeCheck(context, it.value) ?: true
                }) Type.bottom
            else type
        }

        else -> null
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
                    if (it is PropertyType) it.getAllFields(context)[this.identifier] ?: Type.bottom else Type.bottom
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
            else context.typeSynthesis(this, Type.bottom)
        }

        is PropertyExpression -> {
            val type = context.getType(this.identifier)
            if (type !is PropertyType) null
            else {
                val typeFields = type.fields
                val declaredFields = fields
                val inferredFields = typeFields.filterValues { it.isAssignableFrom(context, Type.undefined) }
                    .mapValues { LiteralExpression.EUndefined() } + declaredFields
                val parentType = type.parent?.let { Type.reference(it.first) }

                if (inferredFields.keys == typeFields.keys) {
                    val expressionTypeMap = inferredFields.mapValues { Pair(it.value, typeFields[it.key]!!) }
                    if (!expressionTypeMap.values.all { it.first.typeCheck(context, it.second) }) context.typeSynthesis(
                        this, Type.bottom
                    )
                    else if ((parentType == null) != (parent == null) || (parentType != null && !(parent?.typeCheck(
                            context, parentType
                        ) ?: true))
                    ) context.typeSynthesis(this, Type.bottom)
                    else context.typeSynthesis(this, type)
                } else context.typeSynthesis(this, Type.bottom)
            }
        }

        else -> null
    }
}