package fr.univ_lille.iut_info.parsing

import fr.univ_lille.iut_info.*
import fr.univ_lille.iut_info.alfr_parser.AlfrLexer
import fr.univ_lille.iut_info.alfr_parser.AlfrParser
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.misc.Interval

class Parser {
    companion object {

        class CollectingAlfrLexer(val errors: MutableList<String>, input: CharStream) : AlfrLexer(input) {
            override fun notifyListeners(e: LexerNoViableAltException?) {
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

        fun visitProgram(ctx: AlfrParser.ProgramContext): List<Statement> {
            return ctx.statement().map { visitStatement(it) }
        }


        fun visitStatement(ctx: AlfrParser.StatementContext): Statement {
            val groupDeclarationStatement = ctx.group_declaration_statement()
            val nodeDeclarationStatement = ctx.node_declaration_statement()
            val rewriteRuleStatement = ctx.rewrite_rule_statement()

            if (groupDeclarationStatement != null) return visitGroup_declaration_statement(groupDeclarationStatement)
            if (nodeDeclarationStatement != null) return visitNode_declaration_statement(nodeDeclarationStatement)
            if (rewriteRuleStatement != null) return visitRewrite_rule_statement(rewriteRuleStatement)
            throw IllegalStateException("Statement type is not found.")
        }


        fun visitGroup_declaration_statement(ctx: AlfrParser.Group_declaration_statementContext): GroupDeclarationStatement {
            return GroupDeclarationStatement(ctx.IDENTIFIER().text)
        }


        fun visitNode_declaration_statement(ctx: AlfrParser.Node_declaration_statementContext): NodeDeclarationStatement {
            return NodeDeclarationStatement(
                ctx.node_type().IDENTIFIER().text, visitNode_type(ctx.node_type()), ctx.groups.map { it.text }
            )
        }

        fun visitRewrite_rule_statement(ctx: AlfrParser.Rewrite_rule_statementContext): RewriteRuleStatement {
            return RewriteRuleStatement(
                visitPattern(ctx.pattern()),
                ctx.specify_condition()?.let { visitSpecify_condition(it) } ?: TrueCondition(),
                visitTransformation(ctx.transformation()))
        }


        fun visitNode_type(ctx: AlfrParser.Node_typeContext): ObjectAlfrType {
            val id = ctx.identifier.text
            val fields = ctx.fields.map { visitField(it) }
            return ObjectAlfrType(id, fields)
        }


        fun visitField(ctx: AlfrParser.FieldContext): Pair<String, Type> {
            val id = ctx.identifier.text
            val type = visitType(ctx.type())
            return Pair(id, type)
        }


        fun visitType(ctx: AlfrParser.TypeContext): Type {
            val primitive_type = visitPrimitive_type(ctx.primitive_type())
            val isArray = ctx.LBRACK() != null

            if (isArray) return ArrayType(primitive_type)
            return primitive_type
        }


        fun visitPrimitive_type(ctx: AlfrParser.Primitive_typeContext): Type {
            val identifier = ctx.IDENTIFIER()
            val typeNumber = ctx.TYPE_NUMBER()
            val typeString = ctx.TYPE_STRING()
            if (typeString != null) return StringType.Companion.instance
            if (typeNumber != null) return NumberType.Companion.instance
            if (identifier != null) return ReferenceType(identifier.text)
            throw IllegalStateException("Unknown primitive type.")
        }


        fun visitPattern(ctx: AlfrParser.PatternContext): Pattern {
            val listPattern = ctx.children_pattern()
            val rootPattern = ctx.root_pattern()

            if (listPattern != null) return visitChildren_pattern(listPattern)
            if (rootPattern != null) return visitRoot_pattern(rootPattern)
            throw IllegalStateException("Unknown pattern type.")
        }


        fun visitChildren_pattern(ctx: AlfrParser.Children_patternContext): ChildrenPattern {
            val roots = ctx.roots.map { visitRoot_pattern(it) }
            return ChildrenPattern(roots)
        }


        fun visitRoot_pattern(ctx: AlfrParser.Root_patternContext): Pattern {
            val childrenPattern = ctx.children_pattern()?.let { visitChildren_pattern(it) }
            val fieldsPattern = ctx.fields_pattern()?.let { visitFields_pattern(it) }
            val alias = ctx.specify_alias()?.let { visitSpecify_alias(it) }

            return ObjectPattern(fieldsPattern ?: emptyList(), childrenPattern, alias)
        }


        fun visitFields_pattern(ctx: AlfrParser.Fields_patternContext): List<Pair<String, Pattern>> {
            return ctx.fields.map { visitField_pattern(it) }
        }


        fun visitField_pattern(ctx: AlfrParser.Field_patternContext): Pair<String, Pattern> {
            val id = ctx.IDENTIFIER().text
            val pattern = visitPattern_field_value(ctx.pattern_field_value())

            return Pair(id, pattern)
        }


        fun visitSpecify_alias(ctx: AlfrParser.Specify_aliasContext): String {
            return ctx.IDENTIFIER().text
        }


        fun visitPattern_field_value(ctx: AlfrParser.Pattern_field_valueContext): Pattern {
            val values = ctx.values
            val patternFieldPrimitiveValue = ctx.pattern_field_primitive_value

            if (values != null) {
                return ListPattern(values.map { visitPattern_field_primitive_value(it) })
            }
            return visitPattern_field_primitive_value(patternFieldPrimitiveValue)
        }


        fun visitPattern_field_primitive_value(ctx: AlfrParser.Pattern_field_primitive_valueContext): Pattern {
            val string = ctx.STRING()
            val number = ctx.NUMBER()
            val rootPattern = ctx.root_pattern()

            if (string != null) return StringPattern(string.text)
            if (number != null) return NumberPattern(number.text.toFloat())
            if (rootPattern != null) return visitRoot_pattern(rootPattern)

            throw IllegalStateException("Unknown primitive pattern value")
        }


        fun visitSpecify_condition(ctx: AlfrParser.Specify_conditionContext): Condition {
            return visitCondition(ctx.condition())
        }


        fun visitCondition(ctx: AlfrParser.ConditionContext): Condition {
            val and = ctx.AND()
            val or = ctx.OR()
            val not = ctx.NOT()
            val arrow = ctx.ARROW()
            val equal = ctx.EQUAL()
            val parent = ctx.LPAREN()

            val conditions = ctx.condition().map { visitCondition(it) }
            val expressions = ctx.expression().map { visitExpression(it) }

            if (parent != null) return conditions[0]
            if (not != null) return NotCondition(conditions[0])
            if (equal != null) return EqualCondition(expressions[0], expressions[1])

            if (and != null || arrow != null) return AndCondition(conditions[0], conditions[1])
            if (or != null) return OrCondition(
                conditions[0], conditions[1]
            )
            throw IllegalStateException("Unknown condition expression")
        }


        fun visitTransformation(ctx: AlfrParser.TransformationContext): Transform {
            val keywordDelete = ctx.KEYWORD_DELETE()
            val rootTransform = ctx.root_transform()

            if (keywordDelete != null) return DeleteTransform()
            if (rootTransform != null) return visitRoot_transform(rootTransform)

            throw IllegalStateException("Unknown transform expression")
        }


        fun visitRoot_transform(ctx: AlfrParser.Root_transformContext): ObjectTransform {
            val fieldsTransform = ctx.fields_transform()?.let { visitFields_transform(it) }
            val childrenTransform = ctx.transfrom_children()?.let { visitTransform_children(it) }

            return ObjectTransform(fieldsTransform ?: emptyList(), childrenTransform)
        }


        fun visitTransform_children(ctx: AlfrParser.Transfrom_childrenContext): ChildrenTransform {
            val roots = ctx.root_transform().map { visitRoot_transform(it) }
            return ChildrenTransform(roots)
        }


        fun visitFields_transform(ctx: AlfrParser.Fields_transformContext): List<Pair<String, Transform>> {
            return ctx.fields.map { visitField_transform(it) }
        }


        fun visitField_transform(ctx: AlfrParser.Field_transformContext): Pair<String, Transform> {
            val id = ctx.IDENTIFIER().text
            val transform = ctx.transform_field_value().let { visitTransform_field_value(it) }
            return Pair(id, transform)
        }


        fun visitTransform_field_value(ctx: AlfrParser.Transform_field_valueContext): Transform {
            val values = ctx.values
            val transformFieldPrimitiveValueContext = ctx.transform_field_primitive_value

            if (values != null) {
                return ListTransform(values.map { visitTransform_field_primitive_value(it) })
            }
            return visitTransform_field_primitive_value(transformFieldPrimitiveValueContext)
        }


        fun visitTransform_field_primitive_value(ctx: AlfrParser.Transform_field_primitive_valueContext): Transform {
            val expression = ctx.expression()?.let { visitExpression(it) }
            val rootTransform = ctx.root_transform()?.let { visitRoot_transform(it) }

            if (expression != null) return ExpressionTransform(expression)
            return rootTransform!!
        }


        fun visitExpression_access(ctx: AlfrParser.Expression_accessContext): Expression {
            val chain = ctx.paths.map { it.text }

            return IdentifierExpression(chain)
        }


        fun visitExpression(ctx: AlfrParser.ExpressionContext): Expression {
            val expressionAccess = ctx.expression_access()
            val number = ctx.NUMBER()
            val string = ctx.STRING()

            if (expressionAccess != null) return visitExpression_access(expressionAccess)
            if (number != null) return ValueExpression(NumberValue(number.text.toFloat()))
            if (string != number) return ValueExpression(StringValue(string.text))

            throw IllegalStateException("Unknown expression type")
        }


    }
}