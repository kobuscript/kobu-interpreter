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

import dev.kobu.interpreter.ast.eval.statement.IfStatement;
import dev.kobu.interpreter.ast.eval.statement.ReturnStatement;
import dev.kobu.interpreter.ast.symbol.FunctionParameter;
import dev.kobu.interpreter.ast.symbol.FunctionType;
import dev.kobu.interpreter.ast.symbol.ModuleScope;
import dev.kobu.interpreter.error.analyzer.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Functions")
public class FunctionTest extends AstTestBase {

    ModuleScope module;

    @BeforeEach
    void createModule() {
        module = module("mod");
    }

    @Test
    @DisplayName("Analyzer -> function evaluation")
    void testFunctionEvaluation() {
        var myFunc = functionSymbol(module, "myFunc",
                stringType(),
                functionParameter("p1", stringType()));
        myFunc.setBlock(block(
                var(module, "myVar", add(ref(module, "p1"), stringVal("_suffix"))),
                returnStatement(ref(module, "myVar"))
        ));
        myFunc.analyze(analyzerContext, evalContextProvider);

        var myVar = var(module, "myVar", functionCall(module, "myFunc",
                functionArg(stringVal("str"))));
        var myVar2 = var(module, "myVar2", functionCall(module, "myFunc",
                functionArg(stringVal("str2"))));

        analyze(module, block(myVar, myVar2));
        var evalContext = eval(module, block(myVar, myVar2));
        assertNoErrors();
        assertVar(evalContext, "myVar", stringType(), stringVal("str_suffix"));
        assertVar(evalContext, "myVar2", stringType(), stringVal("str2_suffix"));
    }

    @Test
    @DisplayName("Type checker -> return statement type checker")
    void testReturnTypeChecker() {
        var myFunc = functionSymbol(module, "myFunc",
                stringType(),
                functionParameter("p1", stringType()));
        ReturnStatement returnStatement = returnStatement(add(numberVal(1), numberVal(3)));
        myFunc.setBlock(block(returnStatement));
        myFunc.analyze(analyzerContext, evalContextProvider);
        assertErrors(new InvalidReturnTypeError(returnStatement.getSourceCodeRef(), myFunc, numberType()));
    }

    @Test
    @DisplayName("Analyzer -> missing return statement")
    void testMissingReturnStatement() {
        var myFunc = functionSymbol(module, "myFunc",
                stringType(),
                functionParameter("p1", stringType()));
        myFunc.analyze(analyzerContext, evalContextProvider);
        assertErrors(new FunctionMissingReturnStatError(myFunc.getCloseBlockSourceRef()));
    }

    @Test
    @DisplayName("Analyzer -> missing return statement: 'if' branch")
    void testMissingReturnStatementIfBranch() {
        var myFunc = functionSymbol(module, "myFunc", stringType());
        IfStatement ifStatement = ifStatement(booleanVal(true), block(returnStatement(stringVal("str"))));
        myFunc.setBlock(block(ifStatement));
        myFunc.analyze(analyzerContext, evalContextProvider);
        assertErrors(new FunctionMissingReturnStatError(myFunc.getCloseBlockSourceRef()));
    }

    @Test
    @DisplayName("Analyzer -> missing return statement: 'if' branch w/ return + 'else' branch")
    void testMissingReturnStatementIfBranchElseBranch() {
        var myFunc = functionSymbol(module, "myFunc", stringType());
        IfStatement ifStat = ifStatement(booleanVal(true), block(returnStatement(stringVal("str"))));
        elseStatement(ifStat, block());
        myFunc.setBlock(block(ifStat));
        myFunc.analyze(analyzerContext, evalContextProvider);
        assertErrors(new FunctionMissingReturnStatError(myFunc.getCloseBlockSourceRef()));
    }

    @Test
    @DisplayName("Analyzer -> missing return statement: 'if' branch w/ return + 'else' branch w/ return")
    void testMissingReturnStatementIfElseBranchWithReturn() {
        var myFunc = functionSymbol(module, "myFunc", stringType());
        IfStatement ifStat = ifStatement(booleanVal(true), block(returnStatement(stringVal("str"))));
        elseStatement(ifStat, block(returnStatement(stringVal("str2"))));
        myFunc.setBlock(block(ifStat));
        myFunc.analyze(analyzerContext, evalContextProvider);
        assertNoErrors();
    }

    @Test
    @DisplayName("Analyzer -> return statement: 'if' branch w/ return + 'else' branch + top level return statement")
    void testReturnStatementIfBranchWithReturn() {
        var myFunc = functionSymbol(module, "myFunc", stringType());
        IfStatement ifStat = ifStatement(booleanVal(true), block(returnStatement(stringVal("str"))));
        elseStatement(ifStat, block());
        myFunc.setBlock(block(ifStat, returnStatement(stringVal("str2"))));
        myFunc.analyze(analyzerContext, evalContextProvider);
        assertNoErrors();
    }

    @Test
    @DisplayName("Analyzer -> return statement: loop branch w/ return")
    void testMissingReturnStatementLoopBranchWithReturn() {
        var myFunc = functionSymbol(module, "myFunc", stringType());
        myFunc.setBlock(block(whileStatement(booleanVal(true), block(returnStatement(stringVal("str"))))));
        myFunc.analyze(analyzerContext, evalContextProvider);
        assertErrors(new FunctionMissingReturnStatError(myFunc.getCloseBlockSourceRef()));
    }

    @Test
    @DisplayName("Analyzer -> unreachable code")
    void testUnreachableCode() {
        var myFunc = functionSymbol(module, "myFunc", stringType());
        IfStatement ifStat = ifStatement(booleanVal(true), block(returnStatement(stringVal("str"))));
        elseStatement(ifStat, block(returnStatement(stringVal("str2"))));
        ReturnStatement returnStatement = returnStatement(stringVal("str3"));
        myFunc.setBlock(block(ifStat, returnStatement));
        myFunc.analyze(analyzerContext, evalContextProvider);
        assertErrors(new UnreachableCodeError(returnStatement.getSourceCodeRef()));
    }

    @Test
    @DisplayName("Analyzer -> return statement with value in void function")
    void testReturnStatementWithValueVoidFunction() {
        var myFunc = functionSymbol(module, "myFunc");
        ReturnStatement returnStatement = returnStatement(numberVal(10));
        myFunc.setBlock(block(returnStatement));
        myFunc.analyze(analyzerContext, evalContextProvider);
        assertErrors(new ReturnStatInVoidFunctionError(returnStatement.getSourceCodeRef(), myFunc));
    }

    @Test
    @DisplayName("Analyzer -> return statement without value in void function")
    void testReturnStatementVoidFunction() {
        var myFunc = functionSymbol(module, "myFunc");
        ReturnStatement returnStatement = returnStatement();
        myFunc.setBlock(block(ifStatement(booleanVal(true), block(returnStatement))));
        myFunc.analyze(analyzerContext, evalContextProvider);
        assertNoErrors();
    }

    @Test
    @DisplayName("Analyzer -> duplicated parameters")
    void testDuplicatedParameters() {
        FunctionParameter param1 = functionParameter("param", stringType());
        FunctionParameter param2 = functionParameter("param", booleanType());
        var myFunc = functionSymbol(module, "myFunc",
                param1,
                param2);
        myFunc.analyze(analyzerContext, evalContextProvider);
        assertErrors(new DuplicatedFunctionParamError(param1, param2));
    }

    @Test
    @DisplayName("Analyzer -> invalid optional parameter")
    void testInvalidOptionalParameter() {
        FunctionParameter p2 = functionParameter("p2", stringType());
        var myFunc = functionSymbol(module, "myFunc",
                functionParameter("p1", numberType(), true),
                p2);
        myFunc.analyze(analyzerContext, evalContextProvider);
        assertErrors(new InvalidRequiredFunctionParamError(p2));
    }

    @Test
    @DisplayName("Type checker -> function call, wrong number of args")
    void testFunctionCallMismatchArgsTypeChecker() {
        var myFunc = functionSymbol(module, "myFunc",
                functionParameter("p1", stringType()),
                functionParameter("p2", anyValType()));
        myFunc.analyze(analyzerContext, evalContextProvider);

        var funcCall = functionCall(module, "myFunc", functionArg(stringVal("str")));
        analyze(module, block(funcCall));
        assertErrors(new InvalidFunctionCallError(funcCall.getSourceCodeRef(), (FunctionType) myFunc.getType(),
                functionArgs(functionArg(stringVal("str")))));
    }

    @Test
    @DisplayName("Type checker -> function call args")
    void testFunctionCallArgsTypeChecker() {
        var myFunc = functionSymbol(module, "myFunc",
                functionParameter("p1", stringType()),
                functionParameter("p2", anyValType()));
        myFunc.analyze(analyzerContext, evalContextProvider);

        var funcCall = functionCall(module, "myFunc",
                functionArg(stringVal("str")),
                functionArg(numberVal(23)));
        analyze(module, block(funcCall));
        eval(module, block(funcCall));
        assertNoErrors();
    }

    @Test
    @DisplayName("Type checker -> function call optional args")
    void testFunctionCallOptionalArgsTypeChecker() {
        var myFunc = functionSymbol(module, "myFunc",
                functionParameter("p1", stringType()),
                functionParameter("p2", anyValType(), true));
        myFunc.analyze(analyzerContext, evalContextProvider);

        var funcCall = functionCall(module, "myFunc",
                functionArg(stringVal("str")));
        var funcCall2 = functionCall(module, "myFunc",
                functionArg(stringVal("str")),
                functionArg(numberVal(23)));

        analyze(module, block(funcCall, funcCall2));
        eval(module, block(funcCall, funcCall2));
        assertNoErrors();
    }

    @Test
    @DisplayName("Type checker -> function call return value")
    void testFunctionCallReturnTypeChecker() {
        var myFunc = functionSymbol(module, "myFunc", stringType());
        myFunc.setBlock(block(returnStatement(stringVal("str"))));
        myFunc.analyze(analyzerContext, evalContextProvider);

        var fnCall = functionCall(module, "myFunc");
        var myVar = var(module, "myVar", numberType(), fnCall);
        analyze(module, block(myVar));
        assertErrors(new InvalidAssignExprTypeError(fnCall.getSourceCodeRef(), numberType(), stringType()));
    }

    @Test
    @DisplayName("Scope -> call function from imported module")
    void testFunctionCallImportedModule() {
        var mod2 = module("mod2");
        var myFunc = functionSymbol(mod2, "myFunc", stringType());
        myFunc.setBlock(block(returnStatement(stringVal("str"))));
        myFunc.analyze(analyzerContext, evalContextProvider);

        importModule(module, mod2);

        var fnCall = functionCall(module, "myFunc");
        var myVar = var(module, "myVar", fnCall);
        analyze(module, block(myVar));
        var evalContext = eval(module, block(myVar));
        assertNoErrors();
        assertVar(evalContext, myVar.getName(), stringType(), stringVal("str"));

    }

    @Test
    @DisplayName("Scope -> call function from indirect dependency")
    void testFunctionCallIndirectDependency() {
        var mod2 = module("mod2");
        var mod3 = module("mod3");
        var myFunc = functionSymbol(mod3, "myFunc", stringType());
        myFunc.setBlock(block(returnStatement(stringVal("str"))));
        myFunc.analyze(analyzerContext, evalContextProvider);

        importModule(mod2, mod3);
        importModule(module, mod2);

        var fnCall = functionCall(module, "myFunc");
        var myVar = var(module, "myVar", fnCall);
        analyze(module, block(myVar));
        assertErrors(new UndefinedFunctionName(fnCall, null, "myFunc", 0));
    }

    @Test
    @DisplayName("Scope -> call undefined function")
    void testUndefinedFunctionCall() {
        var fnCall = functionCall(module, "myFunc");
        var myVar = var(module, "myVar", fnCall);
        analyze(module, block(myVar));
        assertErrors(new UndefinedFunctionName(fnCall, null, "myFunc", 0));
    }

}
