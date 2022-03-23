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

package dev.kobu.interpreter.ast;

import dev.kobu.interpreter.ast.eval.expr.AddExpr;
import dev.kobu.interpreter.ast.eval.expr.value.ArrayConstructorCallExpr;
import dev.kobu.interpreter.ast.eval.expr.value.StringValueExpr;
import dev.kobu.interpreter.ast.eval.statement.IfStatement;
import dev.kobu.interpreter.ast.symbol.ModuleScope;
import dev.kobu.interpreter.error.analyzer.InvalidAssignExprTypeError;
import dev.kobu.interpreter.error.analyzer.InvalidTypeError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Control structures")
public class ControlStructureTest extends AstTestBase {

    ModuleScope module;

    @BeforeEach
    void createModule() {
        module = module("mod");
    }

    @Test
    @DisplayName("'if' statement evaluation")
    void testIfEvaluation() {
        var myFunc = functionSymbol(module, "myFunc", stringType(),
                functionParameter("param", stringType()));
        myFunc.setBlock(block(
                ifStatement(equals(ref(module, "param"), stringVal("str1")),
                        block(returnStatement(stringVal("str1")))),
                returnStatement(nullVal())
        ));
        myFunc.analyze(analyzerContext, evalContextProvider);

        var myVar = var(module, "myVar",
                functionCall(module, "myFunc", functionArg(stringVal("str1"))));
        var myVar2 = var(module, "myVar2",
                functionCall(module, "myFunc", functionArg(stringVal(""))));
        analyze(module, block(myVar, myVar2));
        var evalContext = eval(module, block(myVar, myVar2));

        assertNoErrors();
        assertVar(evalContext, "myVar", stringType(), stringVal("str1"));
        assertVar(evalContext, "myVar2", stringType(), nullVal());
    }

    @Test
    @DisplayName("'if / else' statement evaluation")
    void testIfElseEvaluation() {
        var myFunc = functionSymbol(module, "myFunc", stringType(),
                functionParameter("param", stringType()));
        IfStatement ifStatement = ifStatement(equals(ref(module, "param"), stringVal("str1")),
                block(returnStatement(stringVal("str1"))));
        elseStatement(ifStatement, block(returnStatement(stringVal("str2"))));
        myFunc.setBlock(block(ifStatement));
        myFunc.analyze(analyzerContext, evalContextProvider);

        var myVar = var(module, "myVar",
                functionCall(module, "myFunc", functionArg(stringVal("str1"))));
        var myVar2 = var(module, "myVar2",
                functionCall(module, "myFunc", functionArg(stringVal(""))));
        analyze(module, block(myVar, myVar2));
        var evalContext = eval(module, block(myVar, myVar2));

        assertNoErrors();
        assertVar(evalContext, "myVar", stringType(), stringVal("str1"));
        assertVar(evalContext, "myVar2", stringType(), stringVal("str2"));
    }

    @Test
    @DisplayName("'if / else if' statement evaluation")
    void testElseIfEvaluation() {
        var myFunc = functionSymbol(module, "myFunc", stringType(),
                functionParameter("param", stringType()));
        IfStatement ifStatement = ifStatement(equals(ref(module, "param"), stringVal("str1")),
                block(returnStatement(stringVal("str1"))));
        var elseIf = elseIf(ifStatement, equals(ref(module, "param"), stringVal("str2")),
                block(returnStatement(stringVal("str2"))));
        elseIf = elseIf(elseIf, equals(ref(module, "param"), stringVal("str3")),
                block(returnStatement(stringVal("str3"))));
        elseStatement(ifStatement, block(returnStatement(stringVal("str4"))));
        myFunc.setBlock(block(ifStatement));
        myFunc.analyze(analyzerContext, evalContextProvider);

        var myVar = var(module, "myVar",
                functionCall(module, "myFunc", functionArg(stringVal("str1"))));
        var myVar2 = var(module, "myVar2",
                functionCall(module, "myFunc", functionArg(stringVal("str2"))));
        var myVar3 = var(module, "myVar3",
                functionCall(module, "myFunc", functionArg(stringVal("str3"))));
        var myVar4 = var(module, "myVar4",
                functionCall(module, "myFunc", functionArg(nullVal())));
        analyze(module, block(myVar, myVar2, myVar3, myVar4));
        var evalContext = eval(module, block(myVar, myVar2, myVar3, myVar4));

        assertNoErrors();
        assertVar(evalContext, "myVar", stringType(), stringVal("str1"));
        assertVar(evalContext, "myVar2", stringType(), stringVal("str2"));
        assertVar(evalContext, "myVar3", stringType(), stringVal("str3"));
        assertVar(evalContext, "myVar4", stringType(), stringVal("str4"));
    }

    @Test
    @DisplayName("'while' statement evaluation")
    void testWhileEvaluation() {
        var myFunc = functionSymbol(module, "factorial", numberType(),
                functionParameter("n", numberType()));
        myFunc.setBlock(block(
                var(module, "idx", numberVal(1)),
                var(module, "result", numberVal(1)),
                whileStatement(lessOrEquals(ref(module, "idx"), ref(module, "n")), block(
                        assign(ref(module, "result"), mult(ref(module,"result"), ref(module, "idx"))),
                        postInc(ref(module, "idx"))
                )),
                returnStatement(ref(module, "result"))
        ));
        myFunc.analyze(analyzerContext, evalContextProvider);

        var myVar = var(module, "myVar", functionCall(module, "factorial", functionArg(numberVal(2))));
        var myVar2 = var(module, "myVar2", functionCall(module, "factorial", functionArg(numberVal(5))));
        var myVar3 = var(module, "myVar3", functionCall(module, "factorial", functionArg(numberVal(8))));

        analyze(module, block(myVar, myVar2, myVar3));
        var evalContext = eval(module, block(myVar, myVar2, myVar3));
        assertNoErrors();
        assertVar(evalContext, myVar.getName(), numberType(), numberVal(2));
        assertVar(evalContext, myVar2.getName(), numberType(), numberVal(120));
        assertVar(evalContext, myVar3.getName(), numberType(), numberVal(40320));
    }

    @Test
    @DisplayName("'for' statement evaluation")
    void testForEvaluation() {
        var myFunc = functionSymbol(module, "factorial", numberType(),
                functionParameter("n", numberType()));
        var varDeclList = varDeclList(var(module, "idx", numberVal(1)));
        var cond = lessOrEquals(ref(module, "idx"), ref(module, "n"));
        var stepList = statementList(postInc(ref(module, "idx")));
        myFunc.setBlock(block(
                var(module, "result", numberVal(1)),
                forStatement(varDeclList, cond, stepList, block(
                        assign(ref(module, "result"), mult(ref(module,"result"), ref(module, "idx")))
                )),
                returnStatement(ref(module, "result"))
        ));
        myFunc.analyze(analyzerContext, evalContextProvider);

        var myVar = var(module, "myVar", functionCall(module, "factorial", functionArg(numberVal(2))));
        var myVar2 = var(module, "myVar2", functionCall(module, "factorial", functionArg(numberVal(5))));
        var myVar3 = var(module, "myVar3", functionCall(module, "factorial", functionArg(numberVal(8))));

        analyze(module, block(myVar, myVar2, myVar3));
        var evalContext = eval(module, block(myVar, myVar2, myVar3));
        assertNoErrors();
        assertVar(evalContext, myVar.getName(), numberType(), numberVal(2));
        assertVar(evalContext, myVar2.getName(), numberType(), numberVal(120));
        assertVar(evalContext, myVar3.getName(), numberType(), numberVal(40320));
    }

    @Test
    @DisplayName("'for' statement without variables and step")
    void testForCondOnlyEvaluation() {
        var myFunc = functionSymbol(module, "factorial", numberType(),
                functionParameter("n", numberType()));
        var varDeclList = varDeclList();
        var cond = lessOrEquals(ref(module, "idx"), ref(module, "n"));
        var stepList = statementList();
        myFunc.setBlock(block(
                var(module, "idx", numberVal(1)),
                var(module, "result", numberVal(1)),
                forStatement(varDeclList, cond, stepList, block(
                        assign(ref(module, "result"), mult(ref(module,"result"), ref(module, "idx"))),
                        postInc(ref(module, "idx"))
                )),
                returnStatement(ref(module, "result"))
        ));
        myFunc.analyze(analyzerContext, evalContextProvider);

        var myVar = var(module, "myVar", functionCall(module, "factorial", functionArg(numberVal(2))));
        var myVar2 = var(module, "myVar2", functionCall(module, "factorial", functionArg(numberVal(5))));
        var myVar3 = var(module, "myVar3", functionCall(module, "factorial", functionArg(numberVal(8))));

        analyze(module, block(myVar, myVar2, myVar3));
        var evalContext = eval(module, block(myVar, myVar2, myVar3));
        assertNoErrors();
        assertVar(evalContext, myVar.getName(), numberType(), numberVal(2));
        assertVar(evalContext, myVar2.getName(), numberType(), numberVal(120));
        assertVar(evalContext, myVar3.getName(), numberType(), numberVal(40320));
    }

    @Test
    @DisplayName("'for' statement: invalid condition expression")
    void testForInvalidCondExpr() {
        var varDeclList = varDeclList(var(module, "i", numberVal(0)));
        var condExpr = add(ref(module, "i"), numberVal(2));
        var stepList = statementList(postInc(ref(module, "i")));
        var forStat = forStatement(varDeclList, condExpr, stepList, block());

        analyze(module, block(forStat));
        assertErrors(new InvalidTypeError(condExpr.getSourceCodeRef(), booleanType(), numberType()));
    }

    @Test
    @DisplayName("'enhanced for' statement evaluation")
    void testEnhancedForEvaluation() {
        var sumVar = var(module, "sum", numberVal(0));
        var enhancedFor = enhancedForStatement(module, "elem",
                arrayConstructor(numberVal(1), numberVal(10), numberVal(100)), block(
                         assign(ref(module, "sum"), add(ref(module, "sum"), ref(module, "elem")))
                ));
        analyze(module, block(sumVar, enhancedFor));
        var evalContext = eval(module, block(sumVar, enhancedFor));
        assertNoErrors();
        assertVar(evalContext, sumVar.getName(), numberType(), numberVal(111));
    }

    @Test
    @DisplayName("'enhanced for' statement: invalid expression")
    void testEnhancedForInvalidExpr() {
        StringValueExpr str = stringVal("str");
        var enhancedFor = enhancedForStatement(module, "elem",
                str, block());
        analyze(module, block(enhancedFor));
        assertErrors(new InvalidTypeError(str.getSourceCodeRef(), arrayType(anyType()), stringType()));
    }

    @Test
    @DisplayName("'enhanced for' statement: invalid type")
    void testEnhancedForInvalidType() {
        var sumVar = var(module, "sum", numberVal(0));
        ArrayConstructorCallExpr arrayExpr = arrayConstructor(numberVal(1), numberVal(10), numberVal(100));
        AddExpr addExpr = add(ref(module, "sum"), ref(module, "elem"));
        var enhancedFor = enhancedForStatement(module, "elem", stringType(),
                arrayExpr, block(
                        assign(ref(module, "sum"), addExpr)
                ));
        analyze(module, block(sumVar, enhancedFor));
        assertErrors(
                new InvalidTypeError(arrayExpr.getSourceCodeRef(), arrayType(stringType()), arrayType(numberType())),
                new InvalidAssignExprTypeError(addExpr.getSourceCodeRef(), numberType(), stringType())
        );
    }

    @Test
    @DisplayName("'while' statement: invalid condition expression")
    void testWhileInvalidCondExpr() {
        var condExpr = add(stringVal("str"), numberVal(2));
        var whileStat = whileStatement(condExpr, block());
        analyze(module, block(whileStat));
        assertErrors(new InvalidTypeError(condExpr.getSourceCodeRef(), booleanType(), stringType()));
    }

    @Test
    @DisplayName("'continue' statement")
    void testContinue() {
        var oddNumbersVar = var(module, "oddNumbers", arrayType(numberType()), arrayConstructor());
        var countVar = var(module, "count", numberVal(0));
        var condExpr = lessOrEquals(ref(module, "count"), numberVal(10));
        var whileStat = whileStatement(condExpr, block(
                ifStatement(equals(mod(ref(module, "count"), numberVal(2)), numberVal(0)), block(
                        postInc(ref(module, "count")),
                        continueStatement()
                )),
                functionCall(module, fieldAccess(ref(module, "oddNumbers"), ref(module, "add")),
                        functionArg(ref(module, "count"))),
                postInc(ref(module, "count"))
        ));

        analyze(module, block(oddNumbersVar, countVar, whileStat));
        var evalContext = eval(module, block(oddNumbersVar, countVar, whileStat));
        assertNoErrors();
        assertVar(evalContext, oddNumbersVar.getName(), arrayType(numberType()), array(arrayConstructor(
                numberVal(1), numberVal(3), numberVal(5), numberVal(7), numberVal(9)
        ), evalContext));
    }

    @Test
    @DisplayName("'break' statement")
    void testBreak() {
        var countVar = var(module, "count", numberVal(0));
        var condExpr = less(ref(module, "count"), numberVal(10));
        var whileStat = whileStatement(condExpr, block(
                ifStatement(equals(ref(module, "count"), numberVal(5)), block(
                        breakStatement()
                )),
                postInc(ref(module, "count"))
        ));
        analyze(module, block(countVar, whileStat));
        var evalContext = eval(module, block(countVar, whileStat));
        assertNoErrors();
        assertVar(evalContext, countVar.getName(), numberType(), numberVal(5));
    }
}
