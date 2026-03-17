grammar Alfr;

program: statement+ EOF;

statement: type_declaration_statement | rewrite_rule_statement;

type_declaration_statement: KEYWORD_SPECIFY_NODE schema=object_type;

object_type:
    identifier=IDENTIFIER
    LPAREN((fields+=field COMMA)* fields+=field)? RPAREN
    (COLON (views+=expression_object ',')* views+=expression_object)?;

field: identifier=IDENTIFIER COLON type;

type:
    | primitive_type
    | primitive_type LBRACK RBRACK;

primitive_type :
    | TYPE_STRING
    | TYPE_NUMBER
    | TYPE_BOOLEAN
    | IDENTIFIER;

rewrite_rule_statement: pattern KEYWORD_SPECIFY_REWRITE (condition=expression ARROW)? result=expression;

// Patterns

pattern: (pattern_literal | pattern_object | pattern_array) ('#' name=IDENTIFIER)?;

pattern_literal:
    | STRING
    | NUMBER
    | TRUE
    | FALSE;

pattern_object_field:
    id=IDENTIFIER EQUAL pattern;

pattern_object:
    IDENTIFIER
        LPAREN
            (
                (fields+=pattern_object_field COMMA)*
                (fields+=pattern_object_field)
            )?
        RPAREN;

pattern_array: LBRACK RBRACK
    | LBRACK
            (values+=pattern COMMA)*
            (values+=pattern)
    RBRACK;

// Expressions

expression_access:
    | id=IDENTIFIER
    | parent=expression_access LBRACK index=expression RBRACK
    | parent=expression_access DOT id=IDENTIFIER;

binary_expression:
    | left=enclosed_expression op=AND right=expression
    | left=enclosed_expression op=OR right=expression
    | left=enclosed_expression op=MULT right=expression
    | left=enclosed_expression op=DIVIDE right=expression
    | left=enclosed_expression op=PLUS right=expression
    | left=enclosed_expression op=MINUS right=expression
    | left=enclosed_expression op=EQUAL right=expression;

unary_expression:
    | MINUS operand=expression
    | PLUS operand=expression
    | NOT operand=expression;

enclosed_expression:
    | LPAREN expression RPAREN
    | expression_literal
    | unary_expression
    | expression_access
    | expression_array
    | expression_object;

expression:
    | binary_expression
    | enclosed_expression;

object_field:
    id=IDENTIFIER EQUAL expression;

expression_literal:
    | STRING
    | NUMBER
    | TRUE
    | FALSE;

expression_object:
    IDENTIFIER
        LPAREN
            (
                (fields+=object_field COMMA)*
                (fields+=object_field)
            )?
        RPAREN;

expression_array: LBRACK RBRACK
    | LBRACK
            (values+=expression COMMA)*
            (values+=expression)
    RBRACK;

TRUE: 'True';
FALSE: 'False';
COLON: ':';
DOT: '.';
COMMA: ',';
EQUAL: '=';
LBRACK: '[';
RBRACK: ']';
LPAREN: '(';
RPAREN: ')';
AND: '&&';
OR: '||';
NOT: '!';
PLUS: '+';
MINUS: '-';
MULT: '*';
DIVIDE: '/';

KEYWORD_DELETE: 'delete';
KEYWORD_SPECIFY_NODE: 'type';
KEYWORD_SPECIFY_REWRITE: '=>';
ARROW: '->';
SHARP: '#';

IDENTIFIER: [a-zA-Z] [a-zA-Z0-9]*;

TYPE_STRING: 'String';
TYPE_NUMBER: 'Number';
TYPE_BOOLEAN: 'Boolean';

NUMBER: ([0-9]+'.'?|[0-9]*'.'[0-9]+);
STRING: '"' StringCharacters? '"';
fragment StringCharacters: StringCharacter+;
fragment StringCharacter: ~["\\\r\n];

WS: [ \t\n\r\f]+ -> skip ;