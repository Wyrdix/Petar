parser grammar PetarParser;

options {
	tokenVocab = PetarLexer;
}

program: statement* EOF;

statement:
	property_declaration_statement
	| rewrite_rule_statement;

property_declaration_statement:
	KEYWORD_SPECIFY_PROP schema = property_type;

type_identifier: IDENTIFIER | KEYWORD_SPECIFY_PROP;
variable_identifier: IDENTIFIER | KEYWORD_SPECIFY_PROP;
parameter_identifier: IDENTIFIER | KEYWORD_SPECIFY_PROP;

property_type:
	identifier = type_identifier LPAREN (
		(fields += property_type_field COMMA)* fields += property_type_field
	)? RPAREN (COLON property_type_parent)?;

property_type_parent:
	parent_identifier = type_identifier (
		LPAREN (
			(restrictions += property_type_field COMMA)* restrictions += property_type_field
		)? RPAREN
	)?;

property_type_field:
	identifier = parameter_identifier (QUESTION_MARK?) COLON type;

array_type: enclosed_type_2 LBRACK RBRACK;

unordered_type:
	enclosed_type_2 LUNORDERED_ARRAY RUNORDERED_ARRAY;

enclosed_type_1:
	| enclosed_type_2
	| array_type
	| unordered_type;

enclosed_type_2: LPAREN type RPAREN | ref = type_identifier;

type: enclosed_type_1;

rewrite_rule_statement:
	pattern KEYWORD_SPECIFY_REWRITE result = expression_property;

// Patterns
pattern: (
        pattern_nest
        | pattern_property
		| pattern_array
		| pattern_unordered_array
		| pattern_regex
		| pattern_expression
	) (at_least_one = PLUS | any_number = MULT |) (
		'#' name = variable_identifier
	)? (ARROW condition = expression)?;

pattern_nest: LPAREN pattern RPAREN;

pattern_expression: expression;

pattern_property_field: id = parameter_identifier EQUAL pattern;

pattern_property:
	type_identifier (LPAREN (
		(fields += pattern_property_field COMMA)* (
			fields += pattern_property_field
		)
	)? RPAREN)?;

pattern_array:
	LBRACK ((values += pattern COMMA)* (values += pattern)) RBRACK;

pattern_unordered_array:
	LUNORDERED_ARRAY (
		(values += pattern COMMA)* (values += pattern)
	) RUNORDERED_ARRAY;

pattern_regex: REGEX_STRING;

// Expressions

expression_access:
	id = variable_identifier
	| parent = expression_access LBRACK index = expression RBRACK
	| parent = expression_access LBRACK start = expression? COLON stop = expression? RBRACK
	| parent = expression_access DOT access = parameter_identifier;

binary_expression:
	left = enclosed_expression op = AND right = expression
	| left = enclosed_expression op = OR right = expression
	| left = enclosed_expression op = LOWER right = expression
	| left = enclosed_expression op = GREATER right = expression
	| left = enclosed_expression op = LOWER_EQ right = expression
	| left = enclosed_expression op = GREATER_EQ right = expression
	| left = enclosed_expression op = MULT right = expression
	| left = enclosed_expression op = DIVIDE right = expression
	| left = enclosed_expression op = PLUS right = expression
	| left = enclosed_expression op = MINUS right = expression
	| left = enclosed_expression op = EQUAL right_pattern = pattern;

unary_expression:
	MINUS operand = expression
	| PLUS operand = expression
	| NOT operand = expression;

enclosed_expression:
	LPAREN expression RPAREN
	| expression_literal
	| unary_expression
	| expression_access
	| expression_array
	| expression_unordered_array
	| expression_property
	| expression_function;

expression: binary_expression | enclosed_expression;

expression_property_field:
	id = parameter_identifier EQUAL expression;

expression_literal: STRING | NUMBER | TRUE | FALSE | UNDEFINED;

expression_property:
	type_identifier LPAREN (
		(fields += expression_property_field COMMA)* (
			fields += expression_property_field
		)
	)? RPAREN (COLON parent = expression)?;

expression_array:
	LBRACK ((values += expression COMMA)* (values += expression))? RBRACK;

expression_unordered_array:
	LUNORDERED_ARRAY (
		(values += expression COMMA)* (values += expression)?
	) RUNORDERED_ARRAY;

expression_function: FUNCTION_IDENTIFIER LPAREN ( (args+=pattern COMMA)* args+=pattern )? RPAREN;