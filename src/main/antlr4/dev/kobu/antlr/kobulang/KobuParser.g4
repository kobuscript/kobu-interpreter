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

parser grammar KobuParser;

options { tokenVocab=KobuLexer; superClass=dev.kobu.antlr.KobuParserBase; }

prog : module importExpr* stat* EOF ;

module : 'module' moduleId MODULE_ID_END? MODULE_ID_BREAK
         | 'module' MODULE_ID_END? MODULE_ID_BREAK {notifyErrorListenersPrevToken("module path expected");}
         ;

moduleId : MODULE_ID ( MODULE_SEPARATOR MODULE_ID )*
           | MODULE_ID MODULE_SEPARATOR {notifyErrorListenersPrevToken("invalid module path");}
           ;

importExpr : 'import' moduleId moduleScope? MODULE_ID_END? MODULE_ID_BREAK
             | 'import' MODULE_ID_END? MODULE_ID_BREAK {notifyErrorListenersPrevToken("module path expected");}
             ;

moduleScope : MODULE_AS MODULE_ID  ;

execStat : singleStat
           | blockStat
           | functionReturnStat
           ;

singleStat: varDecl
            | assignment
            | breakStat
            | continueStat
            | invalidKeyword
            | expr
            | emptyExpr
            ;

invalidKeyword : keyword=( 'def' | 'fun' | 'template' | 'type' | 'rule' | 'file' ) ;

emptyExpr: SEMI ;

blockStat: ifStat
           | forStat
           | enhancedForStat
           | whileStat
           ;

functionReturnStat : RETURN exprWrapper
                     | RETURN SEMI
                     | RETURN {notifyErrorListenersPrevToken("';' expected");}
                     ;

stat : deftype
       | deftemplate
       | defrule
       | deffile
       | nativeDecl
       | invalidDef
       | functionDecl
       | invalidStat
       ;

invalidDef : 'def' elem=( INVALID_DEF | DEF_BREAK ) ;

invalidStat : ID {notifyErrorListenersPrevToken("'def' or 'fun' expected");} ;

functionDecl : 'fun' ID typeParameters? LP functionDeclParam? RP COLON functionDeclRet LCB execStat* RCB
               | 'fun' ID typeParameters? LP functionDeclParam? RP LCB {notifyMissingFunctionReturnType();}
               | 'fun' ID typeParameters? LP functionDeclParam? RP COLON functionDeclRet LCB execStat* {notifyErrorListenersPrevToken("'}' expected");}
               | 'fun' ID typeParameters? LP functionDeclParam? RP COLON functionDeclRet {notifyErrorListenersPrevToken("'{' expected");}
               | 'fun' ID typeParameters? LP functionDeclParam? RP COLON {notifyErrorListenersPrevToken("return type expected");}
               | 'fun' ID typeParameters? LP functionDeclParam? RP {notifyErrorListenersPrevToken("':' expected");}
               | 'fun' ID typeParameters? LP functionDeclParam? {notifyErrorListenersPrevToken("')' expected");}
               | 'fun' ID typeParameters? {notifyErrorListenersPrevToken("'(' expected");}
               | 'fun' {notifyErrorListenersPrevToken("function name expected");}
               ;

nativeDecl : 'def' 'native' ID typeParameters? LP functionDeclParam? RP COLON functionDeclRet SEMI?;

functionDeclRet : ( 'void' | type ) ;

functionDeclParam : ID '?'? COLON type ( COMMA functionDeclParam )?
                    | ID '?'? COLON? {notifyErrorListenersPrevToken("parameter type expected");}
                    ;

ifStat : 'if' LP expr? RP LCB execStat* RCB elseIfStat? elseStat?
         | 'if' LP expr? RP LCB execStat* {notifyErrorListenersPrevToken("'}' expected");}
         | 'if' LP expr? RP {notifyErrorListenersPrevToken("'{' expected");}
         | 'if' LP expr? {notifyErrorListenersPrevToken("')' expected");}
         | 'if' {notifyErrorListenersPrevToken("'(' expected");}
         ;

elseIfStat : 'else' 'if' LP expr RP LCB execStat* RCB elseIfStat?
             | 'else' 'if' LP expr RP LCB execStat* {notifyErrorListenersPrevToken("'}' expected");}
             | 'else' 'if' LP expr RP {notifyErrorListenersPrevToken("'{' expected");}
             | 'else' 'if' LP RP {notifyErrorListenersPrevToken("boolean expression expected");}
             | 'else' 'if' LP expr? {notifyErrorListenersPrevToken("')' expected");}
             ;

elseStat : 'else' LCB execStat* RCB
           | 'else' LCB execStat* {notifyErrorListenersPrevToken("'}' expected");}
           | 'else' {notifyErrorListenersPrevToken("'{' expected");}
           ;

forStat : 'for' LP varDeclList? SEMI expr? SEMI assignmentSequece? RP LCB execStat* RCB
          | 'for' LP varDeclList? SEMI expr? SEMI assignmentSequece? RP LCB execStat* {notifyErrorListenersPrevToken("'}' expected");}
          | 'for' LP varDeclList? SEMI expr? SEMI assignmentSequece? RP {notifyErrorListenersPrevToken("'{' expected");}
          | 'for' LP varDeclList? SEMI expr? SEMI assignmentSequece? {notifyErrorListenersPrevToken("')' expected");}
          | 'for' LP varDeclList? SEMI expr? {notifyErrorListenersPrevToken("';' expected");}
          | 'for' LP varDeclList? {notifyErrorListenersPrevToken("';' expected");}
          ;

enhancedForStat : 'for' LP VAR ID ( COLON type )? OF expr RP LCB execStat* RCB
                  | 'for' LP VAR ID ( COLON type )? OF expr RP LCB execStat* {notifyErrorListenersPrevToken("'}' expected");}
                  | 'for' LP VAR ID ( COLON type )? OF expr RP {notifyErrorListenersPrevToken("'{' expected");}
                  | 'for' LP VAR ID ( COLON type )? OF expr {notifyErrorListenersPrevToken("')' expected");}
                  | 'for' LP VAR ID ( COLON type )? OF RP? {notifyErrorListenersPrevToken("expression expected");}
                  | 'for' LP VAR ID ( COLON type )? {notifyErrorListenersPrevToken("'of' expected");}
                  | 'for' LP VAR {notifyErrorListenersPrevToken("identifier expected");}
                  ;

whileStat : 'while' LP expr RP LCB execStat* RCB
            | 'while' LP expr RP LCB execStat* {notifyErrorListenersPrevToken("'}' expected");}
            | 'while' LP expr RP {notifyErrorListenersPrevToken("'{' expected");}
            | 'while' LP expr {notifyErrorListenersPrevToken("')' expected");}
            | 'while' LP RP {notifyErrorListenersPrevToken("boolean expression expected");}
            | 'while' {notifyErrorListenersPrevToken("'(' expected");}
            ;

breakStat: BREAK ;

continueStat : CONTINUE ;

exprSequence : exprWrapper ( COMMA exprWrapper )* COMMA? ;

deftype : 'def' 'type' ID typeParameters? inheritance? LCB attributes? RCB
          | 'def' 'type' ID typeParameters? inheritance? LCB attributes? {notifyErrorListenersPrevToken("'}' expected");}
          | 'def' 'type' ID typeParameters? inheritance? LCB {notifyErrorListenersPrevToken("'}' expected");}
          | 'def' 'type' ID typeParameters? inheritance? {notifyErrorListenersPrevToken("'{' expected");}
          | 'def' 'type' {notifyErrorListenersPrevToken("type name expected");}
          ;

inheritance : 'extends' typeName typeArgs? ;

attributes : ( STAR | ID ) COLON type ( COMMA? attributes )?
             | ( STAR | ID ) COLON type COMMA
             | ( STAR | ID ) COLON? {notifyErrorListenersPrevToken("attribute type expected");}
             ;

record : typeName typeArgs? LCB recordField? RCB
         | typeName typeArgs? LCB recordField? {notifyErrorListenersPrevToken("'}' expected");}
         ;

recordField : ID COLON exprWrapper ( COMMA? recordField )?
              | ID COLON exprWrapper COMMA
              | ID COLON? {notifyErrorListenersPrevToken("value expected");};

deftemplate : 'def' 'template' ID ruleExtends? 'for' queryExpr joinExpr* ( 'when' expr )? TEMPLATE_BEGIN template? TEMPLATE_END
              | 'def' 'template' ID ruleExtends? 'for' queryExpr joinExpr* ( 'when' expr )? TEMPLATE_BEGIN template? RCB? {notifyErrorListenersPrevToken("'|>' expected");}
              | 'def' 'template' ID ruleExtends? 'for' queryExpr joinExpr* 'when' {notifyErrorListenersPrevToken("boolean expression expected");}
              | 'def' 'template' ID ruleExtends? 'for' queryExpr {notifyErrorListenersPrevToken("'<|' expected");}
              | 'def' 'template' ID ruleExtends? 'for' {notifyErrorListenersPrevToken("query expected");}
              | 'def' 'template' ID ruleExtends? {notifyErrorListenersPrevToken("'for' expected");}
              | 'def' 'template' {notifyErrorListenersPrevToken("rule name expected");}
              ;

deffile : 'def' 'file' ID ruleExtends? 'for' queryExpr joinExpr* ( 'when' expr )? PATH_ARROW pathExpr PATH_END
          | 'def' 'file' ID ruleExtends? 'for' queryExpr joinExpr* ( 'when' expr )? PATH_ARROW pathExpr {notifyMissingEndStatement();}
          | 'def' 'file' ID ruleExtends? 'for' queryExpr joinExpr* 'when' {notifyErrorListenersPrevToken("boolean expression expected");}
          | 'def' 'file' ID ruleExtends? 'for' queryExpr {notifyErrorListenersPrevToken("'->' expected");}
          | 'def' 'file' ID ruleExtends? 'for' {notifyErrorListenersPrevToken("query expected");}
          | 'def' 'file' ID ruleExtends? {notifyErrorListenersPrevToken("'for' expected");}
          | 'def' 'file' {notifyErrorListenersPrevToken("rule name expected");}
          ;

pathExpr : pathSegmentExpr ( SLASH pathExpr )? ;

pathSegmentExpr : pathStaticSegmentExpr
                  | pathVariableExpr
                  ;

pathStaticSegmentExpr : PATH_SEGMENT ;
pathVariableExpr : PATH_VARIABLE_BEGIN expr? PATH_VARIABLE_END
                   | PATH_VARIABLE_BEGIN expr {notifyErrorListenersPrevToken("'}' expected");}
                   ;

defrule : 'def' 'rule' ID ruleExtends? 'for' queryExpr joinExpr* ( 'when' expr )? LCB block RCB
          | 'def' 'rule' ID ruleExtends? 'for' queryExpr joinExpr* ( 'when' expr )? LCB block {notifyErrorListenersPrevToken("'}' expected");}
          | 'def' 'rule' ID ruleExtends? 'for' queryExpr joinExpr* 'when' {notifyErrorListenersPrevToken("boolean expression expected");}
          | 'def' 'rule' ID ruleExtends? 'for' queryExpr {notifyErrorListenersPrevToken("'{' expected");}
          | 'def' 'rule' ID ruleExtends? 'for' {notifyErrorListenersPrevToken("query expected");}
          | 'def' 'rule' ID ruleExtends? {notifyErrorListenersPrevToken("'for' expected");}
          | 'def' 'rule' {notifyErrorListenersPrevToken("rule name expected");}
          ;

ruleExtends : EXTENDS typeName ;

queryExpr : 'any'? type queryExprAlias? queryExprSegment?
            | 'any' {notifyErrorListenersPrevToken("type expected");}
            ;

queryExprAlias : AS ID
                 | AS {notifyErrorListenersPrevToken("alias expected");}
                 ;

queryExprSegment : DIV queryFieldExpr queryExprAlias? queryExprSegment?
                   | DIV {notifyErrorListenersPrevToken("field or selector expected");}
                   ;

queryFieldExpr : ID queryExprArraySelect? ;

queryExprArraySelect : LSB queryExprArrayItem RSB ;

queryExprArrayItem : arrayIndexExpr       #queryExprArrayItemIndex
                     | STAR               #queryExprArrayItemAll
                     ;

joinExpr : 'join' queryExpr joinOfExpr? ;

joinOfExpr : 'of' expr ;

block : execStat* ;

varDecl : VAR varDeclBody
          | VAR {notifyErrorListenersPrevToken("identifier expected");}
          ;

varDeclBody : ID ( COLON type )? ( '=' exprWrapper )?
              | ID ( COLON type )? '=' {notifyErrorListenersPrevToken("value expected");}
              ;

varDeclList : VAR varDeclBody ( COMMA varDeclBody )* ;

template : templateExpr template? ;

templateExpr : templateStaticContentExpr
               | templateContentExpr
               ;

templateStaticContentExpr : CONTENT ;

templateContentExpr : TEMPLATE_EXPR_BEGIN expr? TEMPLATE_EXPR_END
                      | TEMPLATE_EXPR_BEGIN expr {notifyErrorListenersPrevToken("'}' expected");}
                      ;

exprWrapper : expr | assignPostIncDec | assignPreIncDec ;

expr : record                                                                                       #recordExpr
       | LSB exprSequence? RSB                                                                      #arrayExpr
       | LSB exprSequence? {notifyErrorListenersPrevToken("']' expected");}                         #arrayErr1
       | LP exprWrapper COMMA exprSequence RP                                                       #tupleExpr
       | LP exprWrapper COMMA exprSequence {notifyErrorListenersPrevToken("')' expected");}         #tupleErr1
       | LP exprWrapper COMMA {notifyErrorListenersPrevToken("value expected");}                    #tupleErr2
       | LP exprWrapper {notifyErrorListenersPrevToken("',' or ')' expected");}                     #tupleErr3
       | LP {notifyErrorListenersPrevToken("value expected");}                                      #tupleErr4
       | expr AS typeName                                                                           #castExpr
       | expr AS {notifyErrorListenersPrevToken("expression expected");}                            #castErr1
       | expr LSB arrayIndexExpr RSB                                                                #arrayAccessExpr
       | expr DOT expr                                                                              #fieldAccessExpr
       | expr DOT                                                                                   #fieldAccessErr
       | anonymousFunction                                                                          #anonymousFunctionExpr
       | expr typeArgs? LP exprSequence? RP                                                         #functionCallExpr
       | expr typeArgs? LP exprSequence? {notifyErrorListenersPrevToken("')' expected");}           #functionCallErr
       | expr ( STAR | DIV | MOD ) expr                                                             #factorExpr
       | expr ( STAR | DIV | MOD )                                                                  #factorErr
       | expr ( PLUS | MINUS ) expr                                                                 #addSubExpr
       | expr ( PLUS | MINUS )                                                                      #addSubErr
       | expr ( EQUALS | NOT_EQUALS | LESS | LESS_OR_EQUALS | GREATER | GREATER_OR_EQUALS ) expr    #eqExpr
       | expr ( EQUALS | NOT_EQUALS | LESS | LESS_OR_EQUALS | GREATER | GREATER_OR_EQUALS )         #eqErr
       | expr ( AND | OR ) expr                                                                     #logicExpr
       | expr ( AND | OR )                                                                          #logicErr
       | NOT expr                                                                                   #notExpr
       | NOT                                                                                        #notErr
       | TRUE                                                                                       #trueExpr
       | FALSE                                                                                      #falseExpr
       | NULL                                                                                       #nullExpr
       | ID                                                                                         #idExpr
       | stringLiteral                                                                              #stringExpr
       | NUMBER                                                                                     #numberExpr
       | LP expr RP                                                                                 #parenthesizedExpr
       ;

anonymousFunction : ID FN_ARROW anonymousFunctionBody                              #singleArgAnonymousFunction
                    | anonymousFunctionHeader FN_ARROW anonymousFunctionBody       #fullArgsAnonymousFunction;

anonymousFunctionHeader : LP anonymousFunctionParams RP ;
anonymousFunctionParams : ID QM? ( COLON type )? ( COMMA anonymousFunctionParams )? ;

anonymousFunctionBody : expr
                        | LCB execStat* RCB
                        ;

arrayIndexExpr : expr ':' expr  #arrayIndexSliceExpr
                 | ':' expr     #arrayIndexSliceEndExpr
                 | expr ':'     #arrayIndexSliceBeginExpr
                 | exprWrapper  #arrayIndexItemExpr
                 ;

assignment : expr '=' expr                       #assignElemValue
             | assignPostIncDec                  #assignPostIncDecStat
             | assignPreIncDec                   #assignPreIncDecStat
             ;

assignPostIncDec : expr ( INC | DEC ) ;

assignPreIncDec : ( INC | DEC) expr ;

assignmentSequece : assignment ( COMMA assignment )* ;

typeParameters : LESS typeParameter GREATER ;
typeParameter: ID ( COMMA typeParameter )? ;

typeArgs : LESS typeArg GREATER ;
typeArg: type ( COMMA typeArg )? ;

type : typeName                      #typeNameExpr
       | functionType                #functionTypeExpr
       | type LSB RSB                #arrayType
       | LP type ( COMMA type )+ RP  #tupleType
       | LP functionType RP          #parenthesizedFunctionTypeExpr
       ;

functionType : LP functionTypeParameter? RP FN_ARROW type
               | LP functionTypeParameter? RP FN_ARROW {notifyErrorListenersPrevToken("return type expected");}
               ;
functionTypeParameter : type QM? ( COMMA functionTypeParameter )? ;

typeName : ID ( DOT ID )? typeArgs?
           | ANY {notifyErrorListenersPrevToken("'any' not allowed here. Did you mean 'Any'?");}
           ;

stringLiteral : OPEN_QUOTE STRING_CONTENT? CLOSE_QUOTE
                | OPEN_QUOTE STRING_CONTENT? STRING_BREAK {notifyErrorListenersPrevToken("illegal line end in string literal");}
                ;
