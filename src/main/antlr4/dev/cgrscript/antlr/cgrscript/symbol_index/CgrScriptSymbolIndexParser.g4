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

parser grammar CgrScriptSymbolIndexParser;

options { tokenVocab=CgrScriptSymbolIndexLexer; }

prog : module importExpr* stat* EOF ;

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
            | expr
            | emptyExpr
            ;

emptyExpr: SEMI ;

blockStat: ifStat
           | forStat
           | whileStat
           ;

functionReturnStat : RETURN exprWrapper
                     | RETURN SEMI
                     ;

stat : deftype
       | deftemplate
       | defrule
       | deffile
       | nativeDecl
       | functionDecl
       ;

functionDecl : 'fun' ID LP functionDeclParam? RP COLON functionDeclRet LCB execStat* RCB ;

nativeDecl : 'def' 'native' ID LP functionDeclParam? RP COLON functionDeclRet SEMI?;

functionDeclRet : ( 'void' | type ) ;

functionDeclParam : ID '?'? COLON type ( COMMA functionDeclParam )? ;

ifStat : 'if' LP expr RP LCB execStat* RCB elseIfStat? elseStat? ;

elseIfStat : 'else' 'if' LP expr RP LCB execStat* RCB elseIfStat? ;

elseStat : 'else' LCB execStat* RCB ;

forStat : 'for' LP varDeclList? SEMI exprSequence SEMI assignmentSequece RP LCB execStat* RCB ;

whileStat : 'while' LP expr RP LCB execStat* RCB ;

breakStat: BREAK ;

continueStat : CONTINUE ;

exprSequence : exprWrapper ( COMMA exprWrapper )* COMMA? ;

deftype : 'def' 'type' ID inheritance? LCB attributes? RCB ;

inheritance : 'extends' ID ;

attributes : ( STAR | ID ) COLON type ( COMMA attributes )?
             | ( STAR | ID ) COLON type COMMA ;

record : typeName LCB recordField? RCB ;

recordField : ID COLON exprWrapper ( COMMA recordField )?
              | ID COLON exprWrapper COMMA
              ;

deftemplate : 'def' 'template' ID ruleExtends? 'for' queryExpr joinExpr* ( 'when' expr )? TEMPLATE_BEGIN template TEMPLATE_END ;

deffile : 'def' 'file' ID ruleExtends? 'for' queryExpr joinExpr* ( 'when' expr )? FILE_PATH_EXPR pathExpr PATH_END ;

pathExpr : pathSegmentExpr ( SLASH pathExpr )? ;

pathSegmentExpr : pathStaticSegmentExpr
                  | pathVariableExpr
                  ;

pathStaticSegmentExpr : PATH_SEGMENT ;
pathVariableExpr : PATH_VARIABLE_BEGIN expr? PATH_VARIABLE_END ;

defrule : 'def' 'rule' ID ruleExtends? 'for' queryExpr joinExpr* ( 'when' expr )? LCB block RCB ;

ruleExtends : EXTENDS ID ;

queryExpr : 'any'? type queryExprAlias? queryExprSegment? ;

queryExprAlias : 'as' ID ;

queryExprSegment : DIV queryPipeExpr queryExprAlias? queryExprSegment? ;

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

templateExpr : templateStaticContentExpr
               | templateContentExpr
               ;

templateStaticContentExpr : CONTENT ;

templateContentExpr : TEMPLATE_EXPR_BEGIN expr? TEMPLATE_EXPR_END ;

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
