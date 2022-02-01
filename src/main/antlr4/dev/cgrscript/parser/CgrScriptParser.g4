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

importExpr : 'import' moduleId SEMI? ;

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

functionReturnStat : RETURN expr
                     | RETURN SEMI
                     | RETURN {notifyErrorListenersPrevToken("';' expected");}
                     ;

stat : deftype
       | deftemplate
       | defrule
       | deffile
       | nativeDecl
       | functionDecl
       ;

functionDecl : 'fun' ID LP functionDeclParam? RP COLON functionDeclRet LK execStat* RK
               | 'fun' ID LP functionDeclParam? RP LK execStat* RK {notifyMissingReturnStatement();}
               ;

nativeDecl : 'def' 'native' ID LP functionDeclParam? RP COLON functionDeclRet ;

functionDeclRet : ( 'void' | type ) ;

functionDeclParam : ID '?'? COLON type ( COMMA functionDeclParam )?
                    | ID '?'? {notifyErrorListeners("Missing type on function parameter");}
                    ;

ifStat : 'if' LP expr RP LK execStat* RK elseIfStat? elseStat? ;

elseIfStat : 'else' 'if' LP expr RP LK execStat* RK elseIfStat? ;

elseStat : 'else' LK execStat* RK ;

forStat : 'for' LP varDeclList? SEMI exprSequence SEMI assignmentSequece RP LK execStat* RK ;

whileStat : 'while' LP expr RP LK execStat* RK ;

breakStat: BREAK ;

continueStat : CONTINUE ;

exprSequence : expr ( COMMA expr )* ;

deftype : 'def' 'type' ID inheritance? LK attributes? RK #recordType ;

inheritance : 'extends' ID ;

attributes : ( STAR | ID ) COLON type ( COMMA attributes )? ;

record : ID LK recordField? RK ;

recordField : ID COLON expr ( COMMA recordField )? ;

deftemplate : 'def' 'template' ID ruleExtends? 'for' 'any'? queryExpr joinExpr* ( 'when' expr )? TEMPLATE_BEGIN template TEMPLATE_END ;

deffile : 'def' 'file' ID ruleExtends? 'for' 'any'? queryExpr joinExpr* ( 'when' expr )? FILE_PATH_EXPR pathExpr PATH_END
          | 'def' 'file' ID ruleExtends? 'for' 'any'? queryExpr joinExpr* ( 'when' expr )? FILE_PATH_EXPR pathExpr {notifyMissingEndStatement();};

pathExpr : pathSegmentExpr ( SLASH pathExpr )? ;

pathSegmentExpr : PATH_SEGMENT                                 #pathStaticSegmentExpr
                  | PATH_VARIABLE_BEGIN expr PATH_VARIABLE_END #pathVariableExpr
                  ;

defrule : 'def' 'rule' ID ruleExtends? 'for' queryExpr joinExpr* ( 'when' expr )? LK block RK ;

ruleExtends : EXTENDS ID ;

queryExpr : 'any'? type queryExprAlias? queryExprSegment? ;

queryExprAlias : 'as' ID ;

queryExprSegment : DIV queryPipeExpr queryExprAlias? queryExprSegment? ;

queryPipeExpr : ID queryExprArraySelect?                                    #queryFieldExpr
                | functionCallExpr queryExprArraySelect?                    #queryFunctionCallExpr
                ;

queryExprArraySelect : LB queryExprArrayItem RB ;

queryExprArrayItem : arrayIndexExpr       #queryExprArrayItemIndex
                     | STAR               #queryExprArrayItemAll
                     ;

joinExpr : 'join' queryExpr joinOfExpr? ;

joinOfExpr : 'of' expr ;

block : execStat* ;

varDecl : VAR varDeclBody #varDeclStat
          ;

varDeclBody : ID ( COLON type )? ( '=' expr )? ;

varDeclList : VAR varDeclBody ( COMMA varDeclBody )* ;

template : templateExpr template? ;

templateExpr : CONTENT                                       #templateStaticContentExpr
               | TEMPLATE_EXPR_BEGIN expr TEMPLATE_EXPR_END  #templateContentExpr
               ;

expr : record                                                                                       #recordExpr
       | LB exprSequence? RB                                                       #arrayExpr
       | LP expr COMMA expr RP                                             #pairExpr
       | functionCallExpr                                                                           #functionCallProxyExpr
       | expr LB arrayIndexExpr RB                                                 #arrayAccessExpr
       | NOT expr                                                                                   #notExpr
       | assignPostIncDec                                                                           #assignPostIncDecExpr
       | assignPreIncDec                                                                            #assignPreIncDecExpr
       | expr DOT expr                                                                  #fieldAccessExpr
       | expr ( STAR | DIV ) expr                                                                   #factorExpr
       | expr ( ADD | SUB ) expr                                                                    #addSubExpr
       | expr ( EQUALS | NOT_EQUALS | LESS | LESS_OR_EQUALS | GREATER | GREATER_OR_EQUALS ) expr    #eqExpr
       | expr ( AND | OR ) expr                                                                     #logicExpr
       | ID                                                                                         #idExpr
       | STRING                                                                                     #stringExpr
       | NUMBER                                                                                     #numberExpr
       | BOOLEAN                                                                                    #booleanExpr
       | NULL                                                                                       #nullExpr
       | LP expr RP                                                                #parenthesizedExpr
       ;

functionCallExpr : ID LP exprSequence? RP ;

arrayIndexExpr : expr ':' expr  #arrayIndexSliceExpr
                 | ':' expr     #arrayIndexSliceEndExpr
                 | expr ':'     #arrayIndexSliceBeginExpr
                 | expr         #arrayIndexItemExpr
                 ;

assignment : expr '=' expr                       #assignElemValue
             | assignPostIncDec                  #assignPostIncDecStat
             | assignPreIncDec                   #assignPreIncDecStat
             ;

assignPostIncDec : ID ( INC | DEC ) ;

assignPreIncDec : ( INC | DEC) ID ;

assignmentSequece : assignment ( COMMA assignment )* ;

type : ID                                                #singleType
       | type LB RB                     #arrayType
       | LP type COMMA type RP  #pairType
       ;

