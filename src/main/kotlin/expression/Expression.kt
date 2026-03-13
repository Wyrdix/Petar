package fr.univ_lille.iut_info.expression

import fr.univ_lille.iut_info.parsing.*
import fr.univ_lille.iut_info.type.*
import fr.univ_lille.iut_info.visitable.Visitable
import fr.univ_lille.iut_info.visitable.Visitor
import kotlin.math.floor

typealias Context = Map<String, MemoryElement>
typealias TypecheckContext = Map<String, Type>

abstract class Expression : Visitable<Expression> {
    var checkedType: Type? = null
    abstract fun evaluate(context: Context): MemoryElement?
    abstract fun typecheck(context: TypecheckContext, expected: Type): Boolean
    abstract fun getBottomUpType(context: TypecheckContext): Type?
}

abstract class ExpressionAccess : Expression() {
    abstract val parent: ExpressionAccess?

    data class Index(
        override val parent: ExpressionAccess, val expression: Expression
    ) : ExpressionAccess() {
        override fun accept(visitor: Visitor<Expression>): Expression {
            return Index(visitor.visit(parent) as ExpressionAccess, visitor.visit(expression))
        }

        override fun evaluate(context: Context): MemoryElement? {
            val parentElement = parent.evaluate(context)
            if (parentElement !is MemoryArray) return null

            val indexElement = expression.evaluate(context)
            if (indexElement !is MemoryNumber) return null

            val array = parentElement.value
            val rawIndex = indexElement.value

            if (floor(rawIndex.toDouble()) != rawIndex.toDouble()) return null
            val intIndex = rawIndex.toInt()
            if (intIndex >= array.size || intIndex < -array.size) return null

            val index = (intIndex + array.size) % array.size
            return array[index]
        }

        override fun getBottomUpType(context: TypecheckContext): Type? {
            val parent = parent.getBottomUpType(context)

            if (parent !is ArrayType) return null
            return parent.type
        }

        override fun typecheck(context: TypecheckContext, expected: Type): Boolean {

            val parentElement = parent.typecheck(context, ArrayType(expected))
            if (!parentElement) return false

            val indexElement = expression.typecheck(context, Type.number)
            if (!indexElement) return false

            checkedType = expected
            return true
        }
    }

    data class Member(
        override val parent: ExpressionAccess?, val identifier: String
    ) : ExpressionAccess() {
        override fun accept(visitor: Visitor<Expression>): Expression {
            return Member(parent?.run(visitor::visit) as ExpressionAccess?, identifier)
        }

        override fun evaluate(context: Context): MemoryElement? {
            val parent = this.parent?.evaluate(context) ?: return context[identifier]

            if (parent !is MemoryObject) return null
            return parent.value[identifier]
        }

        override fun getBottomUpType(context: TypecheckContext): Type? {

            if (parent == null) return context[identifier]

            val parent = parent.getBottomUpType(context)

            if (parent !is ObjectType) return null
            return parent.childrenMap[identifier]
        }

        override fun typecheck(context: TypecheckContext, expected: Type): Boolean {
            if (parent == null) {
                val got = context[identifier] ?: return false
                return typeEquality(got, expected)
            }

            val parent = parent.getBottomUpType(context) ?: return false
            if (parent !is ObjectType) return false
            val got = parent.childrenMap[identifier] ?: return false
            if (!typeEquality(got, expected)) return false

            checkedType = expected
            return false
        }
    }
}

abstract class LiteralExpression : Expression() {

    override fun accept(visitor: Visitor<Expression>): Expression {
        return this
    }

    data class EString(val value: String) : LiteralExpression() {
        override fun evaluate(context: Context): MemoryElement {
            return MemoryString(value)
        }

        override fun typecheck(context: TypecheckContext, expected: Type): Boolean {
            if (expected !is StringType) return false
            checkedType = expected
            return true
        }

        override fun getBottomUpType(context: TypecheckContext): Type {
            return Type.string
        }
    }

    data class ENumber(val value: Float) : LiteralExpression() {
        override fun evaluate(context: Context): MemoryElement {
            return MemoryNumber(value)
        }

        override fun typecheck(context: TypecheckContext, expected: Type): Boolean {
            if (expected !is NumberType) return false
            checkedType = expected
            return true
        }

        override fun getBottomUpType(context: TypecheckContext): Type {
            return Type.number
        }
    }

    data class EBoolean(val value: Boolean) : LiteralExpression() {
        override fun evaluate(context: Context): MemoryElement {
            return MemoryBoolean(value)
        }

        override fun typecheck(context: TypecheckContext, expected: Type): Boolean {
            if (expected !is BooleanType) return false
            checkedType = expected
            return true
        }

        override fun getBottomUpType(context: TypecheckContext): Type {
            return Type.boolean
        }
    }
}

abstract class BinaryExpression : Expression() {
    abstract val left: Expression
    abstract val right: Expression
    abstract val operandType: Type?
    abstract val resultType: Type

    override fun getBottomUpType(context: TypecheckContext): Type? {
        return resultType
    }

    override fun typecheck(context: TypecheckContext, expected: Type): Boolean {
        val operandType = this@BinaryExpression.operandType ?: return false
        if (!left.typecheck(context, operandType) || !right.typecheck(context, operandType)) return false
        if (!typeEquality(resultType, expected)) return false
        checkedType = expected
        return true
    }


    data class And(override val left: Expression, override val right: Expression) : BinaryExpression() {
        override val resultType: Type = Type.boolean
        override val operandType: Type = Type.boolean

        override fun accept(visitor: Visitor<Expression>): Expression {
            return And(visitor.visit(left), visitor.visit(right))
        }

        override fun evaluate(context: Context): MemoryElement? {
            val left = left.evaluate(context)
            val right = right.evaluate(context)

            if (left == null || right == null) return null
            if (left.type() != operandType || right.type() != operandType) return null

            return MemoryBoolean((left as MemoryBoolean).value && (right as MemoryBoolean).value)
        }
    }

    data class Or(override val left: Expression, override val right: Expression) : BinaryExpression() {
        override val resultType: Type = Type.boolean
        override val operandType: Type = Type.boolean

        override fun accept(visitor: Visitor<Expression>): Expression {
            return Or(visitor.visit(left), visitor.visit(right))
        }

        override fun evaluate(context: Context): MemoryElement? {
            val left = left.evaluate(context)
            val right = right.evaluate(context)

            if (left == null || right == null) return null
            if (left.type() != operandType || right.type() != operandType) return null

            return MemoryBoolean((left as MemoryBoolean).value || (right as MemoryBoolean).value)
        }
    }

    data class Multiply(override val left: Expression, override val right: Expression) : BinaryExpression() {
        override val resultType: Type = Type.number
        override val operandType: Type = Type.number

        override fun accept(visitor: Visitor<Expression>): Expression {
            return Multiply(visitor.visit(left), visitor.visit(right))
        }

        override fun evaluate(context: Context): MemoryElement? {
            val left = left.evaluate(context)
            val right = right.evaluate(context)

            if (left == null || right == null) return null
            if (left.type() != operandType || right.type() != operandType) return null

            return MemoryNumber((left as MemoryNumber).value * (right as MemoryNumber).value)
        }
    }

    data class Divide(override val left: Expression, override val right: Expression) : BinaryExpression() {
        override val resultType: Type = Type.number
        override val operandType: Type = Type.number

        override fun accept(visitor: Visitor<Expression>): Expression {
            return Divide(visitor.visit(left), visitor.visit(right))
        }


        override fun evaluate(context: Context): MemoryElement? {
            val left = left.evaluate(context)
            val right = right.evaluate(context)

            if (left == null || right == null) return null
            if (left.type() != operandType || right.type() != operandType) return null
            if ((right as MemoryNumber).value == 0f) return null

            return MemoryNumber((left as MemoryNumber).value * right.value)
        }
    }

    data class Plus(override val left: Expression, override val right: Expression) : BinaryExpression() {
        override val resultType: Type = Type.number
        override val operandType: Type = Type.number

        override fun accept(visitor: Visitor<Expression>): Expression {
            return Plus(visitor.visit(left), visitor.visit(right))
        }

        override fun evaluate(context: Context): MemoryElement? {
            val left = left.evaluate(context)
            val right = right.evaluate(context)

            if (left == null || right == null) return null
            if (left.type() != operandType || right.type() != operandType) return null

            return MemoryNumber((left as MemoryNumber).value + (right as MemoryNumber).value)
        }
    }

    data class Minus(override val left: Expression, override val right: Expression) : BinaryExpression() {
        override val resultType: Type = Type.number
        override val operandType: Type = Type.number

        override fun accept(visitor: Visitor<Expression>): Expression {
            return Minus(visitor.visit(left), visitor.visit(right))
        }

        override fun evaluate(context: Context): MemoryElement? {
            val left = left.evaluate(context)
            val right = right.evaluate(context)

            if (left == null || right == null) return null
            if (left.type() != operandType || right.type() != operandType) return null

            return MemoryNumber((left as MemoryNumber).value - (right as MemoryNumber).value)
        }
    }

    data class Equal(override val left: Expression, override val right: Expression) : BinaryExpression() {
        override val resultType: Type = Type.boolean
        override val operandType: Type? = null

        override fun accept(visitor: Visitor<Expression>): Expression {
            return Equal(visitor.visit(left), visitor.visit(right))
        }

        override fun evaluate(context: Context): MemoryElement? {
            val left = left.evaluate(context)
            val right = right.evaluate(context)

            if (left == null || right == null) return null

            return MemoryBoolean(left.toString() == right.toString())
        }

        override fun typecheck(context: TypecheckContext, expected: Type): Boolean {

            val leftType = left.getBottomUpType(context)?.resolve() ?: return false
            val rightType = right.getBottomUpType(context)?.resolve() ?: return false


            if (!left.typecheck(context, leftType) || !right.typecheck(
                    context,
                    rightType
                )
            ) return false
            if (!typeEquality(resultType, expected)) return false
            checkedType = expected
            return true
        }
    }
}

abstract class UnaryExpression : Expression() {
    abstract val operand: Expression
    abstract val operandAndResultType: Type

    override fun getBottomUpType(context: TypecheckContext): Type? {
        return operandAndResultType
    }

    override fun typecheck(context: TypecheckContext, expected: Type): Boolean {
        val operand = operand

        if (!operand.typecheck(context, operandAndResultType)) return false
        if (!typeEquality(operandAndResultType, expected)) return false

        checkedType = expected
        return true
    }

    data class Negate(override val operand: Expression) : UnaryExpression() {
        override val operandAndResultType: Type
            get() = Type.boolean

        override fun accept(visitor: Visitor<Expression>): Expression {
            return Negate(visitor.visit(operand))
        }

        override fun evaluate(context: Context): MemoryElement? {
            val operand = operand.evaluate(context) ?: return null

            if (operand !is MemoryBoolean) return null
            return MemoryBoolean(!operand.value)
        }
    }

    data class Opposite(override val operand: Expression) : UnaryExpression() {
        override val operandAndResultType: Type
            get() = Type.number

        override fun accept(visitor: Visitor<Expression>): Expression {
            return Opposite(visitor.visit(operand))
        }

        override fun evaluate(context: Context): MemoryElement? {
            val operand = operand.evaluate(context) ?: return null

            if (operand !is MemoryNumber) return null
            return MemoryNumber(-operand.value)
        }
    }
}

data class ArrayExpression(val values: List<Expression>) : Expression() {
    override fun accept(visitor: Visitor<Expression>): Expression {
        return ArrayExpression(values.map(visitor::visit))
    }

    override fun evaluate(context: Context): MemoryElement? {
        val nullableEvaluatedValues = values.map { it.evaluate(context) }
        if (nullableEvaluatedValues.contains(null)) return null
        val type = checkedType ?: return null

        val evaluatedValues = nullableEvaluatedValues.map { value -> value!! }
        return MemoryArray(type as ArrayType, evaluatedValues)
    }

    override fun getBottomUpType(context: TypecheckContext): Type? {
        val nullableTypedValues = values.map { it.getBottomUpType(context) }
        if (nullableTypedValues.contains(null)) return null

        val typedValues = nullableTypedValues.filterNotNull()
        val tree = typedValues.map {
            when (it) {
                is ObjectType -> setOf(it) + it.interfaces.map {
                    val reference = ReferenceType(it)
                    reference.group = true
                    reference
                }

                else -> setOf(it)
            }
        }

        if (tree.isEmpty()) TODO()

        val intersection = tree[0].intersect(tree.subList(1, tree.size).flatten().toSet())
        if (intersection.isEmpty()) return null

        val elementType = intersection.maxByOrNull { a -> if (a is ReferenceType) -1 else 1 }!!
        return ArrayType(elementType)
    }

    override fun typecheck(context: TypecheckContext, expected: Type): Boolean {
        if (expected !is ArrayType) return false

        val typedValues = values.map { it.typecheck(context, expected.type.resolve()) }
        if (typedValues.contains(false)) return false
        checkedType = expected
        return true
    }
}

data class ObjectExpression(val identifier: String, val fields: List<Pair<String, Expression>>) : Expression() {

    val mapFields
        get() = fields.associateBy({ it.first }, { it.second })

    override fun accept(visitor: Visitor<Expression>): Expression {
        return ObjectExpression(identifier, fields.map { Pair(it.first, visitor.visit(it.second)) })
    }

    override fun evaluate(context: Context): MemoryElement? {
        val nullableEvaluatedFields = mapFields.mapValues { (_, value) -> value.evaluate(context) }
        if (nullableEvaluatedFields.containsValue(null)) return null
        val type = checkedType ?: return null

        val evaluatedFields = nullableEvaluatedFields.mapValues { (_, value) -> value!! }
        return MemoryObject(type as ObjectType, evaluatedFields)
    }

    override fun getBottomUpType(context: TypecheckContext): Type? {
        return context[identifier]
    }

    override fun typecheck(context: TypecheckContext, expected: Type): Boolean {

        if (expected !is ObjectType) return false
        if (identifier != expected.identifier) return false

        val typeChildrenMap = expected.childrenMap
        val mapFields = this.mapFields

        if (mapFields.keys != typeChildrenMap.keys) return false

        val typedValues = mapFields.map { (key, pattern) ->
            pattern.typecheck(context, typeChildrenMap[key]!!.resolve())
        }
        if (typedValues.contains(false)) return false
        checkedType = expected
        return true
    }

}