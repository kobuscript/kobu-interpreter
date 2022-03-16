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

package dev.cgrscript.interpreter.ast;

import dev.cgrscript.interpreter.ast.eval.statement.IfStatement;
import dev.cgrscript.interpreter.ast.symbol.ModuleScope;
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
        myFunc.setExprList(block(
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
        myFunc.setExprList(block(ifStatement));
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
        myFunc.setExprList(block(ifStatement));
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
        myFunc.setExprList(block(
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

    }

    @Test
    @DisplayName("'for' statement without variables and step")
    void testForCondOnlyEvaluation() {

    }

    @Test
    @DisplayName("'for' statement with multiple expressions")
    void testForMultipleExprsEvaluation() {

    }

}
