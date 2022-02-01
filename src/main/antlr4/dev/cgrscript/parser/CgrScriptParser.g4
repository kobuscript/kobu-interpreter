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

module : 'module' moduleId END_STATEMENT? ;

moduleId : ID ( FIELD_SEPARATOR ID )* ;

importExpr : 'import' moduleId END_STATEMENT? ;

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

emptyExpr: END_STATEMENT ;

blockStat: ifStat
           | forStat
           | whileStat
           ;

functionReturnStat : RETURN expr
                     | RETURN END_STATEMENT
                     | RETURN {notifyErrorListenersPrevToken("';' expected");}
                     ;

stat : deftype
       | deftemplate
       | defrule
       | deffile
       | nativeDecl
       | functionDecl
       ;

functionDecl : 'fun' ID OPEN_GROUP functionDeclParam? CLOSE_GROUP COLON functionDeclRet OPEN_BLOCK execStat* CLOSE_BLOCK
               | 'fun' ID OPEN_GROUP functionDeclParam? CLOSE_GROUP OPEN_BLOCK execStat* CLOSE_BLOCK {notifyMissingReturnStatement();}
               ;

nativeDecl : 'def' 'native' ID OPEN_GROUP functionDeclParam? CLOSE_GROUP COLON functionDeclRet ;

functionDeclRet : ( 'void' | type ) ;

functionDeclParam : ID '?'? COLON type ( ARG_SEPARATOR functionDeclParam )?
                    | ID '?'? {notifyErrorListeners("Missing type on function parameter");}
                    ;

ifStat : 'if' OPEN_GROUP expr CLOSE_GROUP OPEN_BLOCK execStat* CLOSE_BLOCK elseIfStat? elseStat? ;

elseIfStat : 'else' 'if' OPEN_GROUP expr CLOSE_GROUP OPEN_BLOCK execStat* CLOSE_BLOCK elseIfStat? ;

elseStat : 'else' OPEN_BLOCK execStat* CLOSE_BLOCK ;

forStat : 'for' OPEN_GROUP varDeclList? END_STATEMENT exprSequence END_STATEMENT assignmentSequece CLOSE_GROUP OPEN_BLOCK execStat* CLOSE_BLOCK ;

whileStat : 'while' OPEN_GROUP expr CLOSE_GROUP OPEN_BLOCK execStat* CLOSE_BLOCK ;

breakStat: BREAK ;

continueStat : CONTINUE ;

exprSequence : expr ( ARG_SEPARATOR expr )* ;

deftype : 'def' 'type' ID inheritance? OPEN_BLOCK attributes? CLOSE_BLOCK #recordType ;

inheritance : 'extends' ID ;

attributes : ( STAR | ID ) COLON type ( ARG_SEPARATOR attributes )? ;

record : ID OPEN_BLOCK recordField? CLOSE_BLOCK ;

recordField : ID COLON expr ( ARG_SEPARATOR recordField )? ;

deftemplate : 'def' 'template' ID ruleExtends? 'for' 'any'? queryExpr joinExpr* ( 'when' expr )? TEMPLATE_BEGIN template TEMPLATE_END ;

deffile : 'def' 'file' ID ruleExtends? 'for' 'any'? queryExpr joinExpr* ( 'when' expr )? FILE_PATH_EXPR pathExpr PATH_END
          | 'def' 'file' ID ruleExtends? 'for' 'any'? queryExpr joinExpr* ( 'when' expr )? FILE_PATH_EXPR pathExpr {notifyMissingEndStatement();};

pathExpr : pathSegmentExpr ( PATH_SEPARATOR pathExpr )? ;

pathSegmentExpr : PATH_SEGMENT                                 #pathStaticSegmentExpr
                  | PATH_VARIABLE_BEGIN expr PATH_VARIABLE_END #pathVariableExpr
                  ;

defrule : 'def' 'rule' ID ruleExtends? 'for' queryExpr joinExpr* ( 'when' expr )? OPEN_BLOCK block CLOSE_BLOCK ;

ruleExtends : EXTENDS ID ;

queryExpr : 'any'? type queryExprAlias? queryExprSegment? ;

queryExprAlias : 'as' ID ;

queryExprSegment : DIV queryPipeExpr queryExprAlias? queryExprSegment? ;

queryPipeExpr : ID queryExprArraySelect?                                    #queryFieldExpr
                | functionCallExpr queryExprArraySelect?                    #queryFunctionCallExpr
                ;

queryExprArraySelect : OPEN_ARRAY queryExprArrayItem CLOSE_ARRAY ;

queryExprArrayItem : arrayIndexExpr       #queryExprArrayItemIndex
                     | STAR               #queryExprArrayItemAll
                     ;

joinExpr : 'join' queryExpr joinOfExpr? ;

joinOfExpr : 'of' expr ;

block : execStat* ;

varDecl : VAR varDeclBody #varDeclStat
          ;

varDeclBody : ID ( COLON type )? ( '=' expr )? ;

varDeclList : VAR varDeclBody ( ARG_SEPARATOR varDeclBody )* ;

template : templateExpr template? ;

templateExpr : CONTENT                                       #templateStaticContentExpr
               | TEMPLATE_EXPR_BEGIN expr TEMPLATE_EXPR_END  #templateContentExpr
               ;

expr : record                                                                                       #recordExpr
       | OPEN_ARRAY exprSequence? CLOSE_ARRAY                                                       #arrayExpr
       | OPEN_GROUP expr ARG_SEPARATOR expr CLOSE_GROUP                                             #pairExpr
       | functionCallExpr                                                                           #functionCallProxyExpr
       | expr OPEN_ARRAY arrayIndexExpr CLOSE_ARRAY                                                 #arrayAccessExpr
       | NOT expr                                                                                   #notExpr
       | assignPostIncDec                                                                           #assignPostIncDecExpr
       | assignPreIncDec                                                                            #assignPreIncDecExpr
       | expr FIELD_SEPARATOR expr                                                                  #fieldAccessExpr
       | expr ( STAR | DIV ) expr                                                                   #factorExpr
       | expr ( ADD | SUB ) expr                                                                    #addSubExpr
       | expr ( EQUALS | NOT_EQUALS | LESS | LESS_OR_EQUALS | GREATER | GREATER_OR_EQUALS ) expr    #eqExpr
       | expr ( AND | OR ) expr                                                                     #logicExpr
       | ID                                                                                         #idExpr
       | STRING                                                                                     #stringExpr
       | NUMBER                                                                                     #numberExpr
       | BOOLEAN                                                                                    #booleanExpr
       | NULL                                                                                       #nullExpr
       | OPEN_GROUP expr CLOSE_GROUP                                                                #parenthesizedExpr
       ;

functionCallExpr : ID OPEN_GROUP exprSequence? CLOSE_GROUP ;

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

assignmentSequece : assignment ( ARG_SEPARATOR assignment )* ;

type : ID                                                #singleType
       | type OPEN_ARRAY CLOSE_ARRAY                     #arrayType
       | OPEN_GROUP type ARG_SEPARATOR type CLOSE_GROUP  #pairType
       ;

