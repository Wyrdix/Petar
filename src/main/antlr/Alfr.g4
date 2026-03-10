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
    | IDENTIFIER;

rewrite_rule_statement: pattern KEYWORD_SPECIFY_REWRITE specify_condition? transformation;

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
    | LBRACK
        (values+=pattern_field_primitive_value COMMA)*
        (values+=pattern_field_primitive_value)
      RBRACK;

pattern_field_primitive_value :
    | STRING
    | NUMBER
    | root_pattern;

specify_condition: condition ARROW;

condition:
    expression EQUAL expression
    | condition ARROW condition
    | condition AND condition
    | condition OR condition
    | NOT condition
    | LPAREN condition RPAREN;

transformation:
    | KEYWORD_DELETE
    | root_transform;

root_transform: IDENTIFIER
    LPAREN
        (
            (fields_transform ',' transfrom_children) |
            fields_transform? |
            transfrom_children?
        )
    RPAREN;
transfrom_children: (roots+=root_transform COMMA)* roots+=root_transform+;

fields_transform:  (fields+=field_transform COMMA)* fields+=field_transform;

field_transform: IDENTIFIER EQUAL transform_field_value;

transform_field_value:
    | transform_field_primitive_value
    | LBRACK
        (values+=transform_field_primitive_value COMMA)*
        (values+=transform_field_primitive_value)
      RBRACK;

transform_field_primitive_value:
    | expression
    | root_transform;

expression_access: (paths+=IDENTIFIER COMMA)* paths+=IDENTIFIER;

expression: STRING | NUMBER | expression_access;

COLON: ':';
COMMA: ',';
EQUAL: '=';
LBRACK: '[';
RBRACK: ']';
LPAREN: '(';
RPAREN: ')';
AND: '&&';
OR: '||';
NOT: '!';

KEYWORD_DELETE: 'delete';
KEYWORD_SPECIFY_GROUP: '#Group';
KEYWORD_SPECIFY_NODE: '#Node';
KEYWORD_SPECIFY_REWRITE: '=>';
ARROW: '->';
SHARP: '#';

IDENTIFIER: [a-zA-Z] [a-zA-Z0-9]*;

TYPE_STRING: 'String';
TYPE_NUMBER: 'Number';

NUMBER: ([0-9]+'.'?|[0-9]*'.'[0-9]+);
STRING: '"' StringCharacters? '"';
fragment StringCharacters: StringCharacter+;
fragment StringCharacter: ~["\\\r\n];

WS: [ \t\n\r\f]+ -> skip ;