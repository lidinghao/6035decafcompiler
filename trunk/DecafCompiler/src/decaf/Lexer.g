header {package decaf;}

options 
{
  mangleLiteralPrefix = "TK_";
  language="Java";
}

class DecafScanner extends Lexer;
options 
{
  k=2;
}

tokens 
{
  // keyword tokens
  "class";
  "if";
  "else";
  "for";
  "return";
  "break";
  "continue";
  "callout";
  "true";
  "false";
  "int";
  "boolean";
  "void";
  "Program";
}

LCURLY options { paraphrase = "{"; } : "{";
RCURLY options { paraphrase = "}"; } : "}";
LPAREN options { paraphrase = "("; } : "(";
RPAREN options { paraphrase = ")"; } : ")";
LSQUARE options { paraphrase = "["; } : "[";
RSQUARE options { paraphrase = "]"; } : "]";

WS_ options { paraphrase = "whitespace"; } : (' ' | '\t' | '\n' {newline();}) {_ttype = Token.SKIP; };

SL_COMMENT options { paraphrase = "comment"; } : "//" (~'\n')* '\n' {_ttype = Token.SKIP; newline (); };

STRING options { paraphrase = "string literal"; } : '"' (ESC | VALIDCHARS)* '"';
CHAR options { paraphrase = "char literal"; } : '\'' (ESC | VALIDCHARS) '\'';
COMMA options { paraphrase = "comma"; } : ',' ;
SEMI options { paraphrase = "semicolon"; } : ';' ;

// Arithmetic operators
PLUS options { paraphrase = "plus sign"; } : "+" ;
MINUS options { paraphrase = "minus sign"; } : "-";
MULDIV options { paraphrase = "mul or div operation"; } : "*" | "/" ;
MOD options { paraphrase = "modulus operation"; } : "%" ;

// Assignment operators
ASSIGNPLUSEQ : "+=" ;
ASSIGNMINUSEQ : "-=" ;
ASSIGNEQ : '=' ;

// Relation operators
LESS : "<" ;
MORE : ">" ;
LEQ : "<=" ; 
GEQ : ">=" ;
CEQ : "==" ;
NEQ : "!=" ;
AND : "&&" ;
OR : "||" ;
NOT : "!" ;

// Identifiers and Literals
ID options { paraphrase = "identifier"; } : (ALPHA)(ALPHA_NUM)* ;
INTLIT options { paraphrase = "integer literal"; } : DECIMAL | HEX ;

// Protected tokens
protected ESC :  '\\' ('n'|'"'|'t'|'r'|'\''|'\\') ;
protected VALIDCHARS : ('\u0020'..'\u0021' | '\u0023'..'\u0026' | '\u0028'..'\u005B' | '\u005d'..'\u007e') ;
protected ALPHA : 'a'..'z' | 'A'..'Z' | '_' ;
protected DIGIT : '0'..'9' ;
protected HEX_DIGIT : DIGIT | 'a'..'f' | 'A'..'F' ;
protected ALPHA_NUM : ALPHA | DIGIT ;
protected DECIMAL : (DIGIT)+ ;
protected HEX : "0x"(HEX_DIGIT)+ ;
