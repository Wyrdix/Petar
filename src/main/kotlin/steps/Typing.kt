package fr.univ_lille.iut_info.steps

import fr.univ_lille.iut_info.*

interface ITypingContext : INameContext {
    val propertyResolved: HashMap<PropertyType, Map<String, Type>>
    val nameContext: INameContext
    val expressionSynthesized: HashMap<Expression, Type>
    val expressionChecked: HashMap<Expression, Type>
    val patternSynthesized: HashMap<Pattern, Type>
    val patternChecked: HashMap<Pattern, Type>
    fun typeSynthesis(exp: Expression, type: Type): Type
    fun typePatternSynthesis(pattern: Pattern, type: Type?): Type?
    fun typeChecked(exp: Expression, condition: Boolean, type: Type): Boolean
    fun typePatternChecked(pattern: Pattern, condition: Boolean, type: Type): Boolean
    fun getSynthesizedType(exp: Expression): Type?
    fun getPatternSynthesizedType(pattern: Pattern): Type?
    fun getCheckedType(exp: Expression): Type?
    fun getCheckedPatternType(pattern: Pattern): Type?
    fun getType(name: String): Type
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
    if (this is ReferenceType) return resolveReference(context).isAssignableFrom(context, other)
    val resolvedOther = other.resolveReference(context)
    if (resolvedOther != other) return isAssignableFrom(context, resolvedOther)
    if (other is UnionType) return other.types.all { isAssignableFrom(context, it) }

    if (this is AnyType && other !is PrimitiveType.UndefinedType) return true
    if (this is PrimitiveType.UndefinedType && other is PrimitiveType.UndefinedType) return true
    if (this is BottomType) return false

    if (this is UnionType) {
        return this.types.any { it.isAssignableFrom(context, other) }
    }

    if (this is PropertyType) {
        return other.ascendants(context).contains(this.identifier)
    }

    if (this is ArrayType && other is ArrayType) {
        return this.type.isAssignableFrom(context, other.type)
    }

    return other.javaClass.isAssignableFrom(javaClass) && javaClass.isAssignableFrom(other.javaClass)
}

fun Pattern.typeCheck(context: ITypingContext, type: Type, listPattern: Boolean = false): Boolean {
    if (this.modifier != PatternModifier.ONE && !listPattern) {
        return context.typePatternChecked(this, true, Type.bottom)
    }

    val checkedType = context.getCheckedPatternType(this)
    if (checkedType != null) return context.typePatternChecked(this, true, type)

    if (type is ReferenceType) return typeCheck(context, type.resolveReference(context))

    val synthesizedType = this.typeSynthesis(context)
    if (synthesizedType != null) return synthesizedType.isAssignableFrom(context, type)

    if (this is ArrayPattern) {
        if (type !is ArrayType) return false
        return context.typePatternChecked(this, this.values.all { this.typeCheck(context, type.type, true) }, type)
    }

    return false
}

fun Pattern.typeSynthesis(context: ITypingContext, listPattern: Boolean = false): Type? {
    if (this.modifier != PatternModifier.ONE && !listPattern) {
        context.typePatternChecked(this, true, Type.bottom)
        return Type.bottom
    }
    val alreadySynthesized = context.getPatternSynthesizedType(this)
    if (alreadySynthesized != null) return alreadySynthesized

    if (this is ExpressionPattern) {
        return context.typePatternSynthesis(
            this, this.value.typeSynthesis(context)
        )
    }

    if (this is RegexPattern) {
        return context.typePatternSynthesis(this, Type.string)
    }

    if (this is PropertyPattern) {
        val type = context.getType(this.identifier)
        if (type !is PropertyType) return null
        if (!type.fields.keys.containsAll(this.fields.keys)) return null

        if (!type.fields.all {
                val fieldValue = this.fields[it.key]
                fieldValue?.typeCheck(context, it.value) ?: true
            }) return Type.bottom
        return type
    }

    return null
}

fun Expression.typeCheck(context: ITypingContext, type: Type): Boolean {
    val checkedType = context.getCheckedType(this)
    if (checkedType != null) return context.typeChecked(this, true, type)

    if (type is ReferenceType) return typeCheck(context, type.resolveReference(context))

    val synthesizedType = this.typeSynthesis(context)
    if (synthesizedType != null) return type.isAssignableFrom(context, synthesizedType)

    if (this is BinaryExpression) {
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

            else -> false
        }
    }

    if (this is UnaryExpression) {
        return when (this) {
            is UnaryExpression.Negate -> context.typeChecked(
                this, this.operand.typeCheck(context, Type.boolean), Type.boolean
            )

            is UnaryExpression.Opposite -> context.typeChecked(
                this, this.operand.typeCheck(context, Type.number), Type.number
            )

            else -> false
        }
    }

    if (this is PatternMatchExpression) return context.typeChecked(
        this, Type.boolean.isAssignableFrom(context, type), Type.boolean
    )

    if (this is ArrayExpression) {
        if (type !is ArrayType) return false
        return context.typeChecked(this, this.values.all { this.typeCheck(context, type.type) }, type)
    }

    return false
}

fun Expression.typeSynthesis(context: ITypingContext): Type? {
    val alreadySynthesized = context.getSynthesizedType(this)
    if (alreadySynthesized != null) return alreadySynthesized

    if (this is LiteralExpression) {
        return context.typeSynthesis(
            this, when (this) {
                is LiteralExpression.EBoolean -> Type.boolean
                is LiteralExpression.EString -> Type.string
                is LiteralExpression.ENumber -> Type.number
                is LiteralExpression.EUndefined -> Type.undefined
                else -> null
            }!!
        )
    }

    if (this is ExpressionAccess) {
        return when (this) {
            is ExpressionAccess.Member if this.parent == null -> context.typeSynthesis(
                this, context.getNameNode(this).get(this.identifier)
            )

            is ExpressionAccess.Index -> {
                val arrayType = this.parent.typeSynthesis(context)
                if (arrayType !is ArrayType) null
                else if (this.expression.typeCheck(context, Type.number)) context.typeSynthesis(this, arrayType.type)
                else null
            }

            else -> null
        }
    }


    if (this is PropertyExpression) {
        val type = context.getType(this.identifier)
        if (type !is PropertyType) return null

        val typeFields = type.fields
        val declaredFields = fields
        val inferredFields = typeFields.filterValues { it.isAssignableFrom(context, Type.undefined) }
            .mapValues { LiteralExpression.EUndefined() } + declaredFields
        val parentType = type.parent?.let { Type.reference(it.first) }

        return if (inferredFields.keys == typeFields.keys) {
            val expressionTypeMap = inferredFields.mapValues { Pair(it.value, typeFields[it.key]!!) }
            if (!expressionTypeMap.values.all { it.first.typeCheck(context, it.second) }) Type.bottom
            else if ((parentType == null) != (parent == null) || (parentType != null && !(parent?.typeCheck(
                    context, parentType
                ) ?: true))
            ) Type.bottom
            else type
        } else Type.bottom
    }
    return null
}