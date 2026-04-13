package fr.univ_lille.iut_info.steps

import fr.univ_lille.iut_info.*

interface ITypingContext : INameContext {
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
    if (this is ReferenceType) return cache ?: context.getType(value)
    return this
}

fun Type.ascendants(context: ITypingContext): List<String> {
    if (this !is PropertyType) return emptyList()
    if (this.parent != null) return context.getType(this.parent.first).ascendants(context) + this.identifier
    return listOf(this.identifier)
}

fun Type.isAssignableFrom(context: ITypingContext, other: Type): Boolean {
    if (this is ReferenceType) resolveReference(context).isAssignableFrom(context, other)
    val resolvedOther = other.resolveReference(context)

    if (this is AnyType && resolvedOther !is NullableType) return true

    if (this is NullableType && this.type.isAssignableFrom(context, other)) {
        return true
    }

    if (this is PropertyType && other.ascendants(context).contains(this.identifier)) {
        return true
    }

    if (this is ArrayType && resolvedOther is ArrayType) {
        return this.type.isAssignableFrom(context, resolvedOther.type)
    }


    return resolvedOther.javaClass.isAssignableFrom(javaClass) && javaClass.isAssignableFrom(resolvedOther.javaClass)
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
        if (!type.childrenMap.keys.containsAll(this.fieldsMap.keys)) return null

        return if (type.childrenMap.all {
                val fieldValue = this.fieldsMap[it.key]
                fieldValue?.typeCheck(context, it.value) ?: true
            }) type else Type.bottom
    }

    return null
}

fun Expression.typeCheck(context: ITypingContext, type: Type): Boolean {
    val checkedType = context.getCheckedType(this)
    if (checkedType != null) return context.typeChecked(this, true, type)

    if (type is ReferenceType) return typeCheck(context, type.resolveReference(context))

    val synthesizedType = this.typeSynthesis(context)
    if (synthesizedType != null) return synthesizedType.isAssignableFrom(context, type)

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
        if (!type.childrenMap.keys.containsAll(this.mapFields.keys)) return null

        return if (type.childrenMap.all {
                val fieldValue = this.mapFields[it.key] ?: LiteralExpression.EUndefined()
                fieldValue.typeCheck(context, it.value)
            }) type else Type.bottom
    }
    return null
}