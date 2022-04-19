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
            | constDecl
            | assignment
            | breakStat
            | continueStat
            | throwStat
            | invalidKeyword
            | expr
            | emptyExpr
            ;

invalidKeyword : keyword=( 'def' | 'fun' | DEFTEMPLATE | TYPE | 'rule' | 'action' ) ;

emptyExpr: SEMI ;

blockStat: ifStat
           | forStat
           | enhancedForStat
           | whileStat
           | tryCatchStat
           ;

functionReturnStat : RETURN exprWrapper
                     | RETURN SEMI
                     | RETURN {notifyErrorListenersPrevToken("';' expected");}
                     ;

stat : typerecord
       | typetemplate
       | invalidType
       | globalConstDecl
       | deftemplate
       | defrule
       | defaction
       | nativeDecl
       | invalidDef
       | functionDecl
       | invalidStat
       ;

invalidDef : 'def' elem=( INVALID_DEF | DEF_BREAK ) ;
invalidType : TYPE INVALID_TYPE {notifyErrorListenersPrevToken("'record' or 'template' expected");} ;

invalidStat : ID {notifyErrorListenersPrevToken("'type', 'def' or 'fun' expected");} ;

functionDecl : 'private'? 'fun' ID typeParameters? LP functionDeclParam? RP COLON functionDeclRet LCB execStat* RCB ;

nativeDecl : 'def' 'native' ID typeParameters? LP functionDeclParam? RP COLON functionDeclRet SEMI?;

functionDeclRet : ( 'void' | type ) ;

functionDeclParam : ID '?'? COLON type ( COMMA functionDeclParam )?
                    | ID '?'? COLON? {notifyErrorListenersPrevToken("parameter type expected");}
                    ;

ifStat : 'if' LP expr? RP LCB execStat* RCB elseIfStat? elseStat? ;

elseIfStat : 'else' 'if' LP expr RP LCB execStat* RCB elseIfStat? ;

elseStat : 'else' LCB execStat* RCB ;

forStat : 'for' LP varDeclList? SEMI expr? SEMI assignmentSequece? RP LCB execStat* RCB ;

enhancedForStat : 'for' LP VAR ID ( COLON type )? OF expr RP LCB execStat* RCB ;

whileStat : 'while' LP expr RP LCB execStat* RCB ;

breakStat: BREAK ;

continueStat : CONTINUE ;

throwStat : THROW exprWrapper ;

tryCatchStat : TRY LCB execStat* RCB catchStat ;

catchStat : CATCH LP ID COLON type RP LCB execStat* RCB catchStat? ;

exprSequence : exprWrapper ( COMMA exprWrapper )* COMMA? ;

typerecord : TYPE TYPE_RECORD ID typeParameters? inheritance? LCB attributes? RCB ;

typetemplate : TYPE TYPE_TEMPLATE ID templateInheritance? ( LCB RCB? )? ;

inheritance : 'extends' typeName ;

templateInheritance : 'extends' typeName ;

attributes : ( STAR | ID ) COLON type ( COMMA? attributes )? ;

record : typeName LCB recordField? RCB ;

recordField : ID COLON exprWrapper ( COMMA? recordField )? ;

deftemplate : 'def' DEFTEMPLATE ID ruleExtends? 'for' queryExpr joinExpr* ( 'when' expr )? TEMPLATE_BEGIN template? TEMPLATE_END templateTargetType? ;

templateTargetType : AS typeName ;

defaction : 'def' 'action' ID ruleExtends? 'for' queryExpr joinExpr* ( 'when' expr )? LCB block RCB ;

defrule : 'def' 'rule' ID ruleExtends? 'for' queryExpr joinExpr* ( 'when' expr )? LCB block RCB ;

ruleExtends : EXTENDS typeName ;

queryExpr : 'any'? type queryExprAlias? queryExprSegment? ;

queryExprAlias : AS ID ;

queryExprSegment : DIV queryFieldExpr queryExprAlias? queryExprSegment? ;

queryFieldExpr : ID queryExprArraySelect? ;

queryExprArraySelect : LSB queryExprArrayItem RSB ;

queryExprArrayItem : arrayIndexExpr       #queryExprArrayItemIndex
                     | STAR               #queryExprArrayItemAll
                     ;

joinExpr : 'join' queryExpr joinOfExpr? ;

joinOfExpr : 'of' expr ;

block : execStat* ;

globalConstDecl : PRIVATE? CONST varDeclBody ;

constDecl : CONST varDeclBody ;

varDecl : VAR varDeclBody ;

varDeclBody : ID ( COLON type )? ( '=' exprWrapper )? ;

varDeclList : VAR varDeclBody ( COMMA varDeclBody )* ;

template : templateExpr template? ;

templateExpr : templateStaticContentExpr
               | templateContentExpr
               ;

templateStaticContentExpr : CONTENT ;

templateContentExpr : ( TEMPLATE_EXPR_BEGIN | TEMPLATE_SHIFT_EXPR_BEGIN ) expr? TEMPLATE_EXPR_END ;

exprWrapper : expr | assignPostIncDec | assignPreIncDec ;

expr : record                                                                                       #recordExpr
       | LSB exprSequence? RSB                                                                      #arrayExpr
       | LP exprWrapper COMMA exprSequence RP                                                       #tupleExpr
       | expr LSB arrayIndexExpr RSB                                                                #arrayAccessExpr
       | expr DOT expr                                                                              #fieldAccessExpr
       | expr AS type                                                                               #castExpr
       | anonymousFunction                                                                          #anonymousFunctionExpr
       | expr typeArgs? LP exprSequence? RP                                                         #functionCallExpr
       | expr INSTANCEOF type                                                                       #instanceOfExpr
       | expr ( STAR | DIV | MOD ) expr                                                             #factorExpr
       | expr ( PLUS | MINUS ) expr                                                                 #addSubExpr
       | expr ( EQUALS | NOT_EQUALS | LESS | LESS_OR_EQUALS | GREATER | GREATER_OR_EQUALS ) expr    #eqExpr
       | expr ( AND | OR ) expr                                                                     #logicExpr
       | NOT expr                                                                                   #notExpr
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

anonymousFunctionHeader : LP anonymousFunctionParams? RP ;
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

functionType : LP functionTypeParameter? RP FN_ARROW type ;
functionTypeParameter : type QM? ( COMMA functionTypeParameter )? ;

typeName : ID ( DOT ID )? typeArgs?
           | ANY {notifyErrorListenersPrevToken("'any' not allowed here. Did you mean 'Any'?");}
           ;

stringLiteral : OPEN_QUOTE STRING_CONTENT? CLOSE_QUOTE
                | OPEN_QUOTE STRING_CONTENT? STRING_BREAK {notifyErrorListenersPrevToken("illegal line end in string literal");}
                ;
