package fr.univ_lille.iut_info.parsing

import fr.univ_lille.iut_info.*
import fr.univ_lille.iut_info.alfr_parser.AlfrLexer
import fr.univ_lille.iut_info.alfr_parser.AlfrParser
import fr.univ_lille.iut_info.alfr_parser.AlfrParser.*
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.misc.Interval

class SpecificationParser {
    companion object {
        class CollectingAlfrLexer(val errors: MutableList<String>, input: CharStream) : AlfrLexer(input) {
            override fun notifyListeners(e: LexerNoViableAltException) {
                val text = _input.getText(Interval.of(_tokenStartCharIndex, _input.index()))
                val msg = "Token recognition error at: '" + getErrorDisplay(text) + "'"
                errors.add(msg)
                val listener = errorListenerDispatch as ProxyErrorListener
                listener.syntaxError(this, null, _tokenStartLine, _tokenStartCharPositionInLine, msg, e)
            }
        }

        fun parse(input: String): Pair<MutableList<String>, List<Statement>> {
            val stream: CharStream = ANTLRInputStream(input)
            val listener = CustomErrorListener()
            val lexer = CollectingAlfrLexer(listener.errors, stream)
            lexer.removeErrorListeners()
            val tokens = CommonTokenStream(lexer)
            val parser = AlfrParser(tokens)
            parser.errorHandler = BailErrorStrategy()
            parser.removeErrorListeners()
            parser.addErrorListener(listener)
            val statements = try {
                visitProgram(parser.program())
            } catch (e: Exception) {
                listener.errors.add("Failed to parse: ${e.message}")
                emptyList()
            }
            return Pair(listener.errors, statements)
        }

        fun visitProgram(ctx: ProgramContext): List<Statement> {
            return ctx.statement().map { visitStatement(it) }
        }

        fun visitStatement(ctx: StatementContext): Statement {
            val nodeDeclarationStatement = ctx.property_declaration_statement()
            val rewriteRuleStatement = ctx.rewrite_rule_statement()
            if (nodeDeclarationStatement != null) return visitProperty_declaration_statement(nodeDeclarationStatement)
            if (rewriteRuleStatement != null) return visitRewrite_rule_statement(rewriteRuleStatement)
            throw IllegalStateException("Statement type is not found.")
        }

        fun visitProperty_declaration_statement(ctx: Property_declaration_statementContext): PropertyDeclarationStatement {
            val type = visitProperty_type(ctx.property_type())
            return PropertyDeclarationStatement(type)
        }

        fun visitRewrite_rule_statement(ctx: Rewrite_rule_statementContext): ProductionRuleStatement {
            return ProductionRuleStatement(
                visitPattern(ctx.pattern()), visitExpression_object(ctx.result)
            )
        }

        fun visitProperty_type(ctx: Property_typeContext): PropertyType {
            val id = ctx.identifier.text
            val fields = ctx.fields.map { visitProperty_type_field(it) }
            val parent = ctx.property_type_parent()?.let { parent ->
                Pair(
                    parent.parent_identifier.text, parent.restrictions.map(this::visitProperty_type_field)
                )
            }
            return PropertyType(id, fields, parent = parent)
        }

        fun visitProperty_type_field(ctx: Property_type_fieldContext): Pair<String, Type> {
            val id = ctx.identifier.text
            val type = visitType(ctx.type())
            return Pair(
                id, when (ctx.QUESTION_MARK()) {
                    null -> type
                    else -> Type.optional(type)
                }
            )
        }

        fun visitType(ctx: TypeContext): Type {
            return visitEnclosed_type_1(ctx.enclosed_type_1())
        }

        fun visitEnclosed_type_1(ctx: Enclosed_type_1Context): Type {
            val enclosedType2 = ctx.enclosed_type_2()
            return when (val arrayType = ctx.array_type()) {
                null -> visitEnclosed_type_2(enclosedType2)
                else -> ArrayType(visitEnclosed_type_2(arrayType.enclosed_type_2()))
            }
        }

        fun visitEnclosed_type_2(ctx: Enclosed_type_2Context): Type {
            return when (val identifier = ctx.type_identifier()?.text) {
                null -> visitType(ctx.type())
                else -> when (identifier) {
                    "Any" -> AnyType()
                    else -> ReferenceType(identifier)
                }

            }
        }

        fun visitPattern(ctx: PatternContext): Pattern {
            val name = ctx.name?.text
            val modifier = when (ctx.at_least_one) {
                null -> when (ctx.any_number) {
                    null -> PatternModifier.ONE
                    else -> PatternModifier.ANY
                }

                else -> PatternModifier.AT_LEAST_ONE
            }
            val condition = ctx.condition?.let { visitExpression(ctx.condition) }
            val patternFields = PatternMeta(name, modifier, condition)

            val patternArray = ctx.pattern_array()?.let { visitPattern_array(it, patternFields) }
            val patternObject = ctx.pattern_property()?.let { visitPattern_object(it, patternFields) }
            val patternExpression = ctx.pattern_expression()?.let { visitPattern_expression(it, patternFields) }
            val patternRegex = ctx.pattern_regex()?.let { visitPattern_regex(it, patternFields) }
            return patternArray ?: patternObject ?: patternExpression ?: patternRegex
            ?: throw IllegalStateException("Unknown pattern context")
        }

        fun visitPattern_regex(
            ctx: Pattern_regexContext, fields: PatternMeta
        ): RegexPattern {
            val regex = ctx.REGEX_STRING().text
            return RegexPattern(regex.substring(2, regex.length - 1), fields)
        }

        fun visitPattern_expression(
            ctx: Pattern_expressionContext, fields: PatternMeta
        ): ExpressionPattern {
            return ExpressionPattern(visitExpression(ctx.expression()), fields)
        }

        fun visitPattern_object_field(ctx: Pattern_property_fieldContext): Pair<String, Pattern> {
            val text = ctx.id.text
            val pattern = visitPattern(ctx.pattern())
            return Pair(text, pattern)
        }

        fun visitPattern_object(
            ctx: Pattern_propertyContext, fields: PatternMeta
        ): PropertyPattern {
            val id = ctx.type_identifier().text
            val values = ctx.fields.map { visitPattern_object_field(it) }
            return PropertyPattern(id, values, fields)
        }

        fun visitPattern_array(
            ctx: Pattern_arrayContext, fields: PatternMeta
        ): ArrayPattern {
            val values = ctx.values.map { visitPattern(it) }
            return ArrayPattern(values, fields)
        }

        fun visitExpression(ctx: ExpressionContext?): Expression {
            if (ctx == null) return LiteralExpression.EBoolean(true)
            val enclosedExpression = ctx.enclosed_expression()
            val binaryExpression = ctx.binary_expression()
            if (binaryExpression != null) return visitBinary_expression(binaryExpression)
            if (enclosedExpression != null) return visitEnclosed_expression(enclosedExpression)
            throw IllegalStateException("Unknown expression context")
        }

        fun visitExpression_access(ctx: Expression_accessContext): ExpressionAccess {
            val parent = ctx.parent
            val access = ctx.id ?: ctx.access
            val index = ctx.index
            if (index != null) return ExpressionAccess.Index(visitExpression_access(parent), visitExpression(index))
            if (access != null) return ExpressionAccess.Member(parent?.let { visitExpression_access(it) }, access.text)
            throw IllegalStateException("Unknown expression access context")
        }

        fun visitExpression_literal(ctx: Expression_literalContext): LiteralExpression {
            val string = ctx.STRING()
            val number = ctx.NUMBER()
            val falseNode = ctx.FALSE()
            val trueNode = ctx.TRUE()
            val undefinedNode = ctx.UNDEFINED()
            if (string != null) return LiteralExpression.EString(string.text.substring(1, string.text.length - 1))
            if (number != null) return LiteralExpression.ENumber(number.text.toFloat())
            if (falseNode != null) return LiteralExpression.EBoolean(false)
            if (trueNode != null) return LiteralExpression.EBoolean(true)
            if (undefinedNode != null) return LiteralExpression.EUndefined()
            throw IllegalStateException("Unknown expression literal context")
        }

        fun visitBinary_expression(ctx: Binary_expressionContext): Expression {
            val left = visitEnclosed_expression(ctx.left)
            val right = visitExpression(ctx.right)
            if (ctx.AND() != null) return BinaryExpression.And(left, right)
            if (ctx.OR() != null) return BinaryExpression.Or(left, right)
            if (ctx.MULT() != null) return BinaryExpression.Multiply(left, right)
            if (ctx.PLUS() != null) return BinaryExpression.Plus(left, right)
            if (ctx.MINUS() != null) return BinaryExpression.Minus(left, right)
            if (ctx.DIVIDE() != null) return BinaryExpression.Divide(left, right)
            throw IllegalStateException("Unknown binary expression context")
        }

        fun visitUnary_expression(ctx: Unary_expressionContext): Expression {
            val operand = visitExpression(ctx.operand)
            if (ctx.PLUS() != null) return operand
            if (ctx.MINUS() != null) return UnaryExpression.Opposite(operand)
            if (ctx.NOT() != null) return UnaryExpression.Negate(operand)
            throw IllegalStateException("Unknown unary expression context")
        }

        fun visitEnclosed_expression(ctx: Enclosed_expressionContext): Expression {
            val expressionAccess = ctx.expression_access()?.let { visitExpression_access(it) }
            val expressionArray = ctx.expression_array()?.let { visitExpression_array(it) }
            val expressionObject = ctx.expression_property()?.let { visitExpression_object(it) }
            val expressionFunction = ctx.expression_function()?.let { visitExpression_function(it) }
            val expressionLiteral = ctx.expression_literal()?.let { visitExpression_literal(it) }
            val unaryExpression = ctx.unary_expression()?.let { visitUnary_expression(it) }
            val expression = ctx.expression()?.let { visitExpression(it) }
            return expressionAccess ?: expressionArray ?: expressionObject
            ?: expressionLiteral ?: unaryExpression ?: expression
            ?: expressionFunction ?: throw IllegalStateException("Unknown enclosed expression context")
        }

        fun visitExpression_function(ctx: Expression_functionContext): FunctionCallExpression {
            val id = ctx.FUNCTION_IDENTIFIER().text.substring(1)
            val args = ctx.args.map { visitPattern(it) }
            return FunctionCallExpression(id, args)
        }

        fun visitObject_field(ctx: Expression_property_fieldContext): Pair<String, Expression> {
            val text = ctx.id.text
            val expression = visitExpression(ctx.expression())
            return Pair(text, expression)
        }

        fun visitExpression_object(ctx: Expression_propertyContext): PropertyExpression {
            val id = ctx.type_identifier().text
            val parent = ctx.parent?.let { visitExpression(it) }
            val fields = ctx.fields.map { visitObject_field(it) }
            return PropertyExpression(id, fields, parent)
        }

        fun visitExpression_array(ctx: Expression_arrayContext): ArrayExpression {
            val values = ctx.values.map { visitExpression(it) }
            return ArrayExpression(values)
        }

    }
}