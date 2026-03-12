grammar Alfr;

program: statement+ EOF;

statement: group_declaration_statement | node_declaration_statement | rewrite_rule_statement;

group_declaration_statement: KEYWORD_SPECIFY_GROUP identifier=IDENTIFIER;

node_declaration_statement: KEYWORD_SPECIFY_NODE schema=node_type (COLON (groups+=IDENTIFIER ',')* groups+=IDENTIFIER)?;

node_type:
    identifier=IDENTIFIER
    LPAREN((fields+=field COMMA)* fields+=field)? RPAREN;

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

pattern: root_pattern | children_pattern;

children_pattern: (roots+=root_pattern COMMA)* roots+=root_pattern+;

root_pattern: identifier=IDENTIFIER
    LPAREN
        (
            (fields_pattern ',' children_pattern) |
            fields_pattern? |
            children_pattern?
        )
    RPAREN alias=specify_alias?;

fields_pattern:  (fields+=field_pattern COMMA)* fields+=field_pattern;

field_pattern: IDENTIFIER EQUAL pattern_field_value;

specify_alias: SHARP IDENTIFIER;

pattern_field_value:
    | pattern_field_primitive_value
    | LBRACK RBRACK
    | LBRACK
        (values+=pattern_field_primitive_value COMMA)*
        (values+=pattern_field_primitive_value)
      RBRACK;

pattern_field_primitive_value :
    | STRING
    | NUMBER
    | root_pattern;

// Expressions

expression_access:
    | id=IDENTIFIER
    | parent=expression_access LBRACK index=expression RBRACK
    | parent=expression_access DOT id=IDENTIFIER;

expression_literal:
    | STRING
    | NUMBER
    | TRUE
    | FALSE;

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
KEYWORD_SPECIFY_GROUP: '#Group';
KEYWORD_SPECIFY_NODE: '#Node';
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