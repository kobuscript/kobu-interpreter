/*
MIT License

Copyright (c) 2022 Luiz Mineo

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

lexer grammar CgrScriptLexer;

channels { WSCHANNEL, COMMENTCHANNEL }

options { superClass=dev.cgrscript.antlr.CgrScriptLexerBase; }

fragment NEW_LINE : '\n' ;

MODULE : 'module' -> pushMode(MODULE_MODE) ;

IMPORT : 'import' -> pushMode(MODULE_MODE) ;

DEF : 'def' -> pushMode(DEF_MODE) ;

EXTENDS : 'extends' ;

FUN : 'fun' ;
QM : '?' ;
RETURN : 'return' ;
IF : 'if' ;
ELSE : 'else' ;
FOR : 'for' ;
WHILE : 'while' ;
BREAK : 'break' ;
CONTINUE : 'continue' ;

VOID : 'void' ;
NULL : 'null' ;

VAR : 'var' ;
ASSIGN : '=' ;
SEMI : ';' ;
COMMA : ',' ;
COLON : ':' ;
AS : 'as' ;
ANY : 'any' ;

WHEN : 'when' ;
NOT : 'not' ;
AND : 'and' ;
OR : 'or' ;

TRUE : 'true' ;
FALSE : 'false' ;

INC : '++' ;
DEC : '--' ;
STAR : '*' ;
DIV : '/' ;
PLUS : '+' ;
MINUS : '-' ;
OF : 'of' ;

EQUALS : '==' ;
NOT_EQUALS : '!=' ;
LESS : '<' ;
LESS_OR_EQUALS : '<=' ;
GREATER : '>' ;
GREATER_OR_EQUALS : '>=' ;

JOIN : 'join' ;
DOT : '.' ;

LP : '(' ;
RP : ')' ;

LSB : '[' ;
RSB : ']' ;

COMMENT_BLOCK : '/*' .*? '*/' -> channel(COMMENTCHANNEL) ;
COMMENT_LINE : '//' .*? '\n' -> channel(COMMENTCHANNEL) ;

LCB : '{' ;
TEMPLATE_BEGIN : '<$' {this.SetTemplateMode(true);} -> pushMode(TEMPLATE_MODE) ;
TEMPLATE_EXPR_END : {this.IsTemplateMode()}? '}' -> popMode ;
FILE_PATH_EXPR : '->' {this.SetPathMode(true);} -> pushMode(PATH_MODE) ;
PATH_VARIABLE_END : {this.IsPathMode()}? '}' -> popMode ;
RCB : '}' ;

OPEN_QUOTE : '"' -> pushMode(STRING_MODE) ;

NUMBER : '-'? INT '.' [0-9]+  // 1.35, 0.3, -4.5
        | '-'? INT // -3, 45
        ;
fragment INT : '0' | [1-9] [0-9]* ; // no leading zeros

ID : [a-zA-Z_][a-zA-Z0-9_]* ;
WS : [ \t\r\n]+ -> channel(WSCHANNEL) ;
BAD_CHARACTER : .+? ;

mode STRING_MODE;

STRING_CONTENT : (ESC | ~[\n"\\])+ ;
fragment ESC : '\\' ["\\/bfnrt] ;
STRING_BREAK : NEW_LINE -> popMode ;
CLOSE_QUOTE : '"' -> popMode ;

mode MODULE_MODE;

MODULE_AS : 'as' ;
MODULE_ID : [a-zA-Z_][a-zA-Z0-9_]* ;
MODULE_SEPARATOR : '.' ;
MODULE_ID_BREAK : NEW_LINE -> popMode ;
MODULE_ID_END : ';' ;
MODULEWS : [ \t\r]+ -> channel(WSCHANNEL) ;

mode DEF_MODE;

DEFTYPE : 'type' -> popMode ;
DEFTEMPLATE : 'template' -> popMode ;
DEFRULE : 'rule' -> popMode ;
DEFFILE : 'file' -> popMode ;
DEFNATIVE : 'native' -> popMode ;
INVALID_DEF : [a-zA-Z0-9]+ -> popMode ;
DEF_BREAK : NEW_LINE -> popMode ;
DEFWS : [ \t\r]+ -> channel(WSCHANNEL) ;

mode PATH_MODE;

SLASH : '/' ;
PATH_VARIABLE_BEGIN : '${' -> pushMode(DEFAULT_MODE) ;
PATH_SEGMENT : [a-zA-Z0-9_\\-\\.]+ ;
PATH_END : ';' {this.SetPathMode(false);} -> popMode ;
PATHWS : [ \t\r\n]+ -> channel(WSCHANNEL) ;

mode TEMPLATE_MODE;

TEMPLATE_END : '$>' {this.SetTemplateMode(false);} -> popMode ;
CONTENT : (CONT_ESC | ~[$\\] )+ ;
fragment CONT_ESC : '\\' ([$\\]) ;

TEMPLATE_EXPR_BEGIN : '${' -> pushMode(DEFAULT_MODE) ;