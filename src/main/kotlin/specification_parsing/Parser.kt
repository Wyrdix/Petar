package fr.univ_lille.iut_info.parsing

import fr.univ_lille.iut_info.*
import fr.univ_lille.iut_info.alfr_parser.AlfrLexer
import fr.univ_lille.iut_info.alfr_parser.AlfrParser
import fr.univ_lille.iut_info.alfr_parser.AlfrParser.*
import fr.univ_lille.iut_info.expression.*
import fr.univ_lille.iut_info.pattern.ArrayPattern
import fr.univ_lille.iut_info.pattern.LiteralPattern
import fr.univ_lille.iut_info.pattern.ObjectPattern
import fr.univ_lille.iut_info.pattern.Pattern
import fr.univ_lille.iut_info.type.*
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.misc.Interval

class Parser {
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
            val groupDeclarationStatement = ctx.group_declaration_statement()
            val nodeDeclarationStatement = ctx.node_declaration_statement()
            val rewriteRuleStatement = ctx.rewrite_rule_statement()
            if (groupDeclarationStatement != null) return visitGroup_declaration_statement(groupDeclarationStatement)
            if (nodeDeclarationStatement != null) return visitNode_declaration_statement(nodeDeclarationStatement)
            if (rewriteRuleStatement != null) return visitRewrite_rule_statement(rewriteRuleStatement)
            throw IllegalStateException("Statement type is not found.")
        }

        fun visitGroup_declaration_statement(ctx: Group_declaration_statementContext): GroupDeclarationStatement {
            return GroupDeclarationStatement(ctx.IDENTIFIER().text)
        }

        fun visitNode_declaration_statement(ctx: Node_declaration_statementContext): NodeDeclarationStatement {
            return NodeDeclarationStatement(
                ctx.node_type().IDENTIFIER().text, visitNode_type(ctx.node_type()), ctx.groups.map { it.text })
        }

        fun visitRewrite_rule_statement(ctx: Rewrite_rule_statementContext): RewriteRuleStatement {
            return RewriteRuleStatement(
                visitPattern(ctx.pattern()), visitExpression(ctx.condition), visitExpression(ctx.result)
            )
        }

        fun visitNode_type(ctx: Node_typeContext): ObjectType {
            val id = ctx.identifier.text
            val fields = ctx.fields.map { visitField(it) }
            return ObjectType(id, fields)
        }

        fun visitField(ctx: FieldContext): Pair<String, Type> {
            val id = ctx.identifier.text
            val type = visitType(ctx.type())
            return Pair(id, type)
        }

        fun visitType(ctx: TypeContext): Type {
            val primitive_type = visitPrimitive_type(ctx.primitive_type())
            val isArray = ctx.LBRACK() != null
            if (isArray) return ArrayType(primitive_type)
            return primitive_type
        }

        fun visitPrimitive_type(ctx: Primitive_typeContext): Type {
            val identifier = ctx.IDENTIFIER()
            val typeNumber = ctx.TYPE_NUMBER()
            val typeString = ctx.TYPE_STRING()
            if (typeString != null) return StringType.instance
            if (typeNumber != null) return NumberType.instance
            if (identifier != null) return ReferenceType(identifier.text)
            throw IllegalStateException("Unknown primitive type.")
        }

        fun visitPattern(ctx: PatternContext): Pattern {
            val name = ctx.name?.text
            val patternArray = ctx.pattern_array()?.let { visitPattern_array(it, name) }
            val patternObject = ctx.pattern_object()?.let { visitPattern_object(it, name) }
            val patternLiteral = ctx.pattern_literal()?.let { visitPattern_literal(it, name) }
            return patternArray ?: patternObject ?: patternLiteral
            ?: throw IllegalStateException("Unknown pattern context")
        }

        fun visitPattern_literal(ctx: Pattern_literalContext, name: String?): LiteralPattern {
            val string = ctx.STRING()
            val number = ctx.NUMBER()
            val falseNode = ctx.FALSE()
            val trueNode = ctx.TRUE()
            if (string != null) return LiteralPattern.PString(string.text.substring(0, string.text.length - 1), name)
            if (number != null) return LiteralPattern.PNumber(number.text.toFloat(), name)
            if (falseNode != null) return LiteralPattern.PBoolean(false, name)
            if (trueNode != null) return LiteralPattern.PBoolean(true, name)
            throw IllegalStateException("Unknown pattern literal context")
        }

        fun visitPattern_object_field(ctx: Pattern_object_fieldContext): Pair<String, Pattern> {
            val text = ctx.id.text
            val pattern = visitPattern(ctx.pattern())
            return Pair(text, pattern)
        }

        fun visitPattern_object(ctx: Pattern_objectContext, name: String?): ObjectPattern {
            val id = ctx.IDENTIFIER().text
            val fields = ctx.fields.map { visitPattern_object_field(it) }
            return ObjectPattern(id, fields, name)
        }

        fun visitPattern_array(ctx: Pattern_arrayContext, name: String?): ArrayPattern {
            val values = ctx.values.map { visitPattern(it) }
            return ArrayPattern(values, name)
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
            val id = ctx.id
            val index = ctx.index
            if (index != null) return ExpressionAccess.Index(visitExpression_access(parent), visitExpression(index))
            if (id != null) return ExpressionAccess.Member(parent?.let { visitExpression_access(it) }, id.text)
            throw IllegalStateException("Unknown expression access context")
        }

        fun visitExpression_literal(ctx: Expression_literalContext): LiteralExpression {
            val string = ctx.STRING()
            val number = ctx.NUMBER()
            val falseNode = ctx.FALSE()
            val trueNode = ctx.TRUE()
            if (string != null) return LiteralExpression.EString(string.text.substring(0, string.text.length - 1))
            if (number != null) return LiteralExpression.ENumber(number.text.toFloat())
            if (falseNode != null) return LiteralExpression.EBoolean(false)
            if (trueNode != null) return LiteralExpression.EBoolean(true)
            throw IllegalStateException("Unknown expression literal context")
        }

        fun visitBinary_expression(ctx: Binary_expressionContext): BinaryExpression {
            val left = visitEnclosed_expression(ctx.left)
            val right = visitExpression(ctx.right)
            if (ctx.AND() != null) return BinaryExpression.And(left, right)
            if (ctx.OR() != null) return BinaryExpression.Or(left, right)
            if (ctx.MULT() != null) return BinaryExpression.Multiply(left, right)
            if (ctx.PLUS() != null) return BinaryExpression.Plus(left, right)
            if (ctx.MINUS() != null) return BinaryExpression.Minus(left, right)
            if (ctx.DIVIDE() != null) return BinaryExpression.Divide(left, right)
            if (ctx.EQUAL() != null) return BinaryExpression.Equal(left, right)
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
            val expressionObject = ctx.expression_object()?.let { visitExpression_object(it) }
            val expressionLiteral = ctx.expression_literal()?.let { visitExpression_literal(it) }
            val unaryExpression = ctx.unary_expression()?.let { visitUnary_expression(it) }
            val expression = ctx.expression()?.let { visitExpression(it) }
            return expressionAccess ?: expressionArray ?: expressionObject ?: expressionLiteral ?: unaryExpression
            ?: expression ?: throw IllegalStateException("Unknown enclosed expression context")
        }

        fun visitObject_field(ctx: Object_fieldContext): Pair<String, Expression> {
            val text = ctx.id.text
            val expression = visitExpression(ctx.expression())
            return Pair(text, expression)
        }

        fun visitExpression_object(ctx: Expression_objectContext): ObjectExpression {
            val id = ctx.IDENTIFIER().text
            val fields = ctx.fields.map { visitObject_field(it) }
            return ObjectExpression(id, fields)
        }

        fun visitExpression_array(ctx: Expression_arrayContext): ArrayExpression {
            val values = ctx.values.map { visitExpression(it) }
            return ArrayExpression(values)
        }

    }
}