lexer grammar PetarLexer;

channels {
	COMMENTS
}

FUNCTION_IDENTIFIER: DOLLAR IDENTIFIER;

UNDEFINED: 'undefined';
TRUE: 'True';
FALSE: 'False';
COLON: ':';
DOT: '.';
COMMA: ',';
EQUAL: '=';
LBRACK: '[';
RBRACK: ']';
DOLLAR: '$';
LUNORDERED_ARRAY: '[|';
RUNORDERED_ARRAY: '|]';
LCURB: '{';
RCURB: '}';
LPAREN: '(';
RPAREN: ')';
AND: '&&';
OR: '||';
BAR: '|';
NOT: '!';
PLUS: '+';
MINUS: '-';
MULT: '*';
DIVIDE: '/';

KEYWORD_SPECIFY_PROP: 'property';
KEYWORD_SPECIFY_REWRITE: '=>';
KEYWORD_SPECIFY_FAIL_REWRITE: '=X=>';
ARROW: '->';
SHARP: '#';
QUESTION_MARK: '?';

IDENTIFIER: [a-zA-Z_] [a-zA-Z0-9_]*;

NUMBER: ([0-9]+ '.'? | [0-9]* '.' [0-9]+);

REGEX_STRING: 'r"' RegexCharacters? '"';
fragment RegexCharacters: RegexCharacter+;
fragment RegexCharacter: ~["\\\r\n] | ('\\' ["+*(){}|]);

STRING: '"' StringCharacters? '"';
fragment StringCharacters: StringCharacter+;
fragment StringCharacter: ~["\\\r\n];

WS: [ \t\n\r\f]+ -> skip;
COMMENT: '/*' .*? '*/' -> channel(COMMENTS);
LINE_COMMENT: '//' ~[\r\n]* -> channel(COMMENTS);