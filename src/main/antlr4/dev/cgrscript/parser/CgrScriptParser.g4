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

parser grammar CgrScriptParser;

options { tokenVocab=CgrScriptLexer; superClass=dev.cgrscript.antlr.CgrScriptParserBase; }

prog : module importExpr* stat* ;

module : 'module' moduleId SEMI? ;

moduleId : ID ( DOT ID )* ;

importExpr : 'import' moduleId moduleScope? SEMI? ;

moduleScope : 'as' ID ;

execStat : singleStat
           | blockStat
           | functionReturnStat
           ;

singleStat: varDecl
            | assignment
            | breakStat
            | continueStat
            | exprStat
            ;

exprStat: expr
          | emptyExpr
          ;

emptyExpr: SEMI ;

blockStat: ifStat
           | forStat
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
       | 'def' {notifyErrorListenersPrevToken("'type', 'template', 'rule', 'file' or 'native' expected");}
       | functionDecl
       ;

functionDecl : 'fun' ID LP functionDeclParam? RP COLON functionDeclRet LCB execStat* RCB
               | 'fun' ID LP functionDeclParam? RP LCB {notifyMissingFunctionReturnType();}
               | 'fun' ID LP functionDeclParam? RP COLON functionDeclRet LCB execStat* {notifyErrorListenersPrevToken("'}' expected");}
               | 'fun' ID LP functionDeclParam? RP COLON {notifyErrorListenersPrevToken("return type expected");}
               | 'fun' ID LP functionDeclParam? RP {notifyErrorListenersPrevToken("':' expected");}
               | 'fun' ID LP functionDeclParam? {notifyErrorListenersPrevToken("')' expected");}
               | 'fun' ID {notifyErrorListenersPrevToken("'(' expected");}
               | 'fun' {notifyErrorListenersPrevToken("function name expected");}
               ;

nativeDecl : 'def' 'native' ID LP functionDeclParam? RP COLON functionDeclRet ;

functionDeclRet : ( 'void' | type ) ;

functionDeclParam : ID '?'? COLON type ( COMMA functionDeclParam )?
                    | ID '?'? COLON? {notifyErrorListenersPrevToken("parameter type expected");}
                    ;

ifStat : 'if' LP expr RP LCB execStat* RCB elseIfStat? elseStat? ;

elseIfStat : 'else' 'if' LP expr RP LCB execStat* RCB elseIfStat? ;

elseStat : 'else' LCB execStat* RCB ;

forStat : 'for' LP varDeclList? SEMI exprSequence SEMI assignmentSequece RP LCB execStat* RCB ;

whileStat : 'while' LP expr RP LCB execStat* RCB ;

breakStat: BREAK ;

continueStat : CONTINUE ;

exprSequence : exprWrapper ( COMMA exprWrapper )* ;

deftype : 'def' 'type' ID inheritance? LCB attributes? RCB
          | 'def' 'type' ID inheritance? LCB attributes? {notifyErrorListenersPrevToken("'}' expected");}
          | 'def' 'type' ID inheritance? LCB {notifyErrorListenersPrevToken("'}' expected");}
          | 'def' 'type' ID inheritance? {notifyErrorListenersPrevToken("'{' expected");}
          | 'def' 'type' {notifyErrorListenersPrevToken("type name expected");}
          ;

inheritance : 'extends' ID ;

attributes : ( STAR | ID ) COLON type ( COMMA attributes )?
             | ( STAR | ID ) COLON type COMMA
             | ( STAR | ID ) COLON? {notifyErrorListenersPrevToken("attribute type expected");}
             ;

record : typeName LCB recordField? RCB ;

recordField : ID COLON exprWrapper ( COMMA recordField )? ;

deftemplate : 'def' 'template' ID ruleExtends? 'for' queryExpr joinExpr* ( 'when' expr )? TEMPLATE_BEGIN template TEMPLATE_END
              | 'def' 'template' ID ruleExtends? 'for' queryExpr joinExpr* ( 'when' expr )? TEMPLATE_BEGIN template {notifyErrorListenersPrevToken("'}->' expected");}
              | 'def' 'template' ID ruleExtends? 'for' queryExpr joinExpr* 'when' {notifyErrorListenersPrevToken("boolean expression expected");}
              | 'def' 'template' ID ruleExtends? 'for' queryExpr {notifyErrorListenersPrevToken("'<-{' expected");}
              | 'def' 'template' ID ruleExtends? 'for' {notifyErrorListenersPrevToken("query clause expected");}
              | 'def' 'template' ID ruleExtends? {notifyErrorListenersPrevToken("'for' expected");}
              | 'def' 'template' {notifyErrorListenersPrevToken("rule name expected");}
              ;

deffile : 'def' 'file' ID ruleExtends? 'for' queryExpr joinExpr* ( 'when' expr )? FILE_PATH_EXPR pathExpr PATH_END
          | 'def' 'file' ID ruleExtends? 'for' queryExpr joinExpr* ( 'when' expr )? FILE_PATH_EXPR pathExpr {notifyMissingEndStatement();}
          | 'def' 'file' ID ruleExtends? 'for' queryExpr joinExpr* 'when' {notifyErrorListenersPrevToken("boolean expression expected");}
          | 'def' 'file' ID ruleExtends? 'for' queryExpr {notifyErrorListenersPrevToken("'->' expected");}
          | 'def' 'file' ID ruleExtends? 'for' {notifyErrorListenersPrevToken("query clause expected");}
          | 'def' 'file' ID ruleExtends? {notifyErrorListenersPrevToken("'for' expected");}
          | 'def' 'file' {notifyErrorListenersPrevToken("rule name expected");}
          ;

pathExpr : pathSegmentExpr ( SLASH pathExpr )? ;

pathSegmentExpr : PATH_SEGMENT                                 #pathStaticSegmentExpr
                  | PATH_VARIABLE_BEGIN expr PATH_VARIABLE_END #pathVariableExpr
                  ;

defrule : 'def' 'rule' ID ruleExtends? 'for' queryExpr joinExpr* ( 'when' expr )? LCB block RCB
          | 'def' 'rule' ID ruleExtends? 'for' queryExpr joinExpr* ( 'when' expr )? LCB block {notifyErrorListenersPrevToken("'}' expected");}
          | 'def' 'rule' ID ruleExtends? 'for' queryExpr joinExpr* 'when' {notifyErrorListenersPrevToken("boolean expression expected");}
          | 'def' 'rule' ID ruleExtends? 'for' queryExpr {notifyErrorListenersPrevToken("'{' expected");}
          | 'def' 'rule' ID ruleExtends? 'for' {notifyErrorListenersPrevToken("query clause expected");}
          | 'def' 'rule' ID ruleExtends? {notifyErrorListenersPrevToken("'for' expected");}
          | 'def' 'rule' {notifyErrorListenersPrevToken("rule name expected");}
          ;

ruleExtends : EXTENDS ID ;

queryExpr : 'any'? type queryExprAlias? queryExprSegment?
            | 'any' {notifyErrorListenersPrevToken("type expected");}
            ;

queryExprAlias : 'as' ID
                 | 'as' {notifyErrorListenersPrevToken("alias expected");}
                 ;

queryExprSegment : DIV queryPipeExpr queryExprAlias? queryExprSegment?
                   | DIV {notifyErrorListenersPrevToken("field or selector expected");}
                   ;

queryPipeExpr : ID queryExprArraySelect?                                    #queryFieldExpr
                | functionCallExpr queryExprArraySelect?                    #queryFunctionCallExpr
                ;

queryExprArraySelect : LSB queryExprArrayItem RSB ;

queryExprArrayItem : arrayIndexExpr       #queryExprArrayItemIndex
                     | STAR               #queryExprArrayItemAll
                     ;

joinExpr : 'join' queryExpr joinOfExpr? ;

joinOfExpr : 'of' expr ;

block : execStat* ;

varDecl : VAR varDeclBody
          ;

varDeclBody : ID ( COLON type )? ( '=' exprWrapper )? ;

varDeclList : VAR varDeclBody ( COMMA varDeclBody )* ;

template : templateExpr template? ;

templateExpr : CONTENT                                       #templateStaticContentExpr
               | TEMPLATE_EXPR_BEGIN expr TEMPLATE_EXPR_END  #templateContentExpr
               ;

exprWrapper : expr | assignPostIncDec | assignPreIncDec ;

expr : record                                                                                       #recordExpr
       | LSB exprSequence? RSB                                                                      #arrayExpr
       | LP exprWrapper COMMA exprWrapper RP                                                        #pairExpr
       | functionCallExpr                                                                           #functionCallProxyExpr
       | expr LSB arrayIndexExpr RSB                                                                #arrayAccessExpr
       | expr DOT expr                                                                              #fieldAccessExpr
       | expr ( STAR | DIV ) expr                                                                   #factorExpr
       | expr ( PLUS | MINUS ) expr                                                                 #addSubExpr
       | expr ( EQUALS | NOT_EQUALS | LESS | LESS_OR_EQUALS | GREATER | GREATER_OR_EQUALS ) expr    #eqExpr
       | expr ( AND | OR ) expr                                                                     #logicExpr
       | NOT expr                                                                                   #notExpr
       | TRUE                                                                                       #trueExpr
       | FALSE                                                                                      #falseExpr
       | NULL                                                                                       #nullExpr
       | ID                                                                                         #idExpr
       | STRING                                                                                     #stringExpr
       | NUMBER                                                                                     #numberExpr
       | LP expr RP                                                                                 #parenthesizedExpr
       ;

functionCallExpr : ID LP exprSequence? RP ;

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

type : typeName                 #typeNameExpr
       | type LSB RSB           #arrayType
       | LP type COMMA type RP  #pairType
       ;

typeName : ID ( DOT ID )? ;
