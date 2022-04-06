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

import dev.kobu.interpreter.ast.eval.Expr;
import dev.kobu.interpreter.ast.eval.ValueExpr;
import dev.kobu.interpreter.ast.eval.expr.FunctionCallExpr;
import dev.kobu.interpreter.ast.eval.expr.VarDeclExpr;
import dev.kobu.interpreter.ast.eval.expr.value.ArrayConstructorCallExpr;
import dev.kobu.interpreter.ast.eval.expr.value.TupleConstructorCallExpr;
import dev.kobu.interpreter.ast.eval.expr.value.RecordConstructorCallExpr;
import dev.kobu.interpreter.ast.symbol.ModuleScope;
import dev.kobu.interpreter.ast.symbol.Type;
import dev.kobu.interpreter.error.AnalyzerError;
import dev.kobu.interpreter.error.analyzer.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Variables")
public class VarTest extends AstTestBase {

    ModuleScope module;

    @BeforeEach
    void createModule() {
        module = module("mod");
    }

    @Test
    @DisplayName("Type inference -> \"str\" = string")
    void testStringTypeInference() {
        testVar(var(module, "str", stringVal("str")), stringType(), stringVal("str"));
    }

    @Test
    @DisplayName("Type inference -> 10 = number")
    void testNumberTypeInference() {
        testVar(var(module, "nmb", numberVal(10)), numberType(), numberVal(10));
    }

    @Test
    @DisplayName("Type inference -> true = boolean")
    void testBooleanTypeInference() {
        testVar(var(module, "bool", booleanVal(true)), booleanType(), booleanVal(true));
    }

    @Test
    @DisplayName("Type inference (new record) -> MyRecord{} = MyRecord")
    void testRecordTypeInference() {
        var recType = recordType(module, "MyRecord");
        recType.analyze(analyzerContext, evalContextProvider);

        testVar(var(module, "recVar", recordConstructor(recType)), recType, recordConstructor(recType));
    }

    @Test
    @DisplayName("Type inference (rule reference) -> MyRule = Rule")
    void testRuleTypeInference() {
        var recType = recordType(module, "RecType");
        var myRule = rule(module, "MyRule", recType);
        recType.analyze(analyzerContext, evalContextProvider);
        myRule.analyze(analyzerContext, evalContextProvider);

        testVar(var(module, "ruleVar", ref(module, "MyRule")), ruleRefType(), ruleRef(myRule));
    }

    @Test
    @DisplayName("Type inference (Record type reference) -> MyRecord = RecordType<MyRecord>")
    void testTypeRefInference() {
        var recType = recordType(module, "MyRecord");
        recType.analyze(analyzerContext, evalContextProvider);

        testVar(var(module, "typeVar", ref(module, "MyRecord")), recordTypeRefType(recType), typeRef(recType));
    }

    @Test
    @DisplayName("Type inference -> [\"str1\", \"str2\"] = string[]")
    void testStringArrayTypeInference() {
        var stringArrayVar = var(module, "strArr", arrayConstructor(
                stringVal("str1"), stringVal("str2")
        ));

        testVar(stringArrayVar, arrayType(stringType()), arrayConstructor(
                stringVal("str1"), stringVal("str2")
        ));
    }

    @Test
    @DisplayName("Type inference -> [null, \"str1\", null, \"str2\"] = string[]")
    void testStringArrayWithNullElemsTypeInference() {
        var stringArrayWithNullVar = var(module, "stringArrayWithNull", arrayConstructor(
                nullVal(), stringVal("str1"), nullVal(), stringVal("str2")
        ));

        testVar(stringArrayWithNullVar, arrayType(stringType()), arrayConstructor(
                nullVal(), stringVal("str1"), nullVal(), stringVal("str2")
        ));
    }

    @Test
    @DisplayName("Type inference -> [\"str1\", 10] = AnyVal[]")
    void testAnyValArrayTypeInference() {
        var anyValArrayVar = var(module, "anyValArr", arrayConstructor(
                stringVal("str1"), numberVal(10)
        ));

        testVar(anyValArrayVar, arrayType(anyValType()), arrayConstructor(
                stringVal("str1"), numberVal(10)
        ));
    }

    @Test
    @DisplayName("Type inference -> [null, null] = AnyType[]")
    void testArrayNullElementsTypeInference() {
        var arrayWithNullsVar = var(module, "arrayWithNulls", arrayConstructor(
                nullVal(), nullVal()
        ));

        testVar(arrayWithNullsVar, arrayType(anyType()), arrayConstructor(
                nullVal(), nullVal()
        ));
    }

    @Test
    @DisplayName("Type inference -> [] = AnyType[]")
    void testEmptyArrayTypeInference() {
        var emptyArrayVar = var(module, "emptyArray", arrayConstructor());

        testVar(emptyArrayVar, arrayType(anyType()), arrayConstructor());
    }

    @Test
    @DisplayName("Type inference -> [RecType{}, RecType{}] = RecType[]")
    void testRecordArrayTypeInference() {
        var recType = recordType(module, "RecType");
        recType.analyze(analyzerContext, evalContextProvider);

        var recTypeArrayVar = var(module, "recTypeArray", arrayConstructor(
                recordConstructor(recType), recordConstructor(recType)
        ));

        testVar(recTypeArrayVar, arrayType(recType), arrayConstructor(
                recordConstructor(recType), recordConstructor(recType)
        ));
    }

    @Test
    @DisplayName("Type inference -> [SubType{}, RecType{}, SubType{}] = RecType[] if SubType extends RecType")
    void testRecordArrayCommonTypeInference() {
        var recType = recordType(module, "RecType");
        var subType = recordType(module, "SubType", recType);
        recType.analyze(analyzerContext, evalContextProvider);
        subType.analyze(analyzerContext, evalContextProvider);

        var recTypeArrayVar = var(module, "recTypeArray", arrayConstructor(
                recordConstructor(subType), recordConstructor(recType), recordConstructor(subType)
        ));

        testVar(recTypeArrayVar, arrayType(recType), arrayConstructor(
                recordConstructor(subType), recordConstructor(recType), recordConstructor(subType)
        ));
    }

    @Test
    @DisplayName("Type inference -> [RecType{}, RecType2{}] = AnyRecord[]")
    void testRecordArrayAnyTypeInference() {
        var recType = recordType(module, "RecType");
        var recType2 = recordType(module, "RecType2");
        recType.analyze(analyzerContext, evalContextProvider);
        recType2.analyze(analyzerContext, evalContextProvider);

        var anyRecTypeArrayVar = var(module, "anyRecTypeArray", arrayConstructor(
                recordConstructor(recType), recordConstructor(recType2)
        ));

        testVar(anyRecTypeArrayVar, arrayType(anyRecordType()), arrayConstructor(
                recordConstructor(recType), recordConstructor(recType2)
        ));
    }

    @Test
    @DisplayName("Type inference -> [RecType{}, \"str1\"] = AnyType[]")
    void testAnyTypeArrayInference() {
        var recType = recordType(module, "RecType");
        recType.analyze(analyzerContext, evalContextProvider);

        var anyTypeArrayVar = var(module, "anyTypeArray", arrayConstructor(
                recordConstructor(recType), stringVal("str1")
        ));

        testVar(anyTypeArrayVar, arrayType(anyType()), arrayConstructor(
                recordConstructor(recType), stringVal("str1")
        ));
    }

    @Test
    @DisplayName("Type inference -> [[\"str1\", \"str2\"], [\"str3\", \"str4\"]] = string[][]")
    void testArrayCompTypeInference() {
        var stringArrayCompVar = var(module, "stringMatrix", arrayConstructor(
                arrayConstructor(stringVal("str1"), stringVal("str2")),
                arrayConstructor(stringVal("str3"), stringVal("str4"))
        ));

        testVar(stringArrayCompVar, arrayType(arrayType(stringType())), arrayConstructor(
                arrayConstructor(stringVal("str1"), stringVal("str2")),
                arrayConstructor(stringVal("str3"), stringVal("str4"))
        ));
    }

    @Test
    @DisplayName("Type inference -> [[\"str1\", \"str2\"], [1, 2]] = AnyVal[][]")
    void testArrayCompCommonTypeInference() {
        var anyValArrayCompVar = var(module, "anyValMatrix", arrayConstructor(
                arrayConstructor(stringVal("str1"), stringVal("str2")), //string[]
                arrayConstructor(numberVal(1), numberVal(2)) //number[]
        )); //AnyVal[][]

        testVar(anyValArrayCompVar, arrayType(arrayType(anyValType())), arrayConstructor(
                arrayConstructor(stringVal("str1"), stringVal("str2")),
                arrayConstructor(numberVal(1), numberVal(2))
        ));
    }

    @Test
    @DisplayName("Type inference -> (\"str1\", 10) = (string, number)")
    void testStringNumberPairTypeInference() {
        var stringNumberPairVar = var(module, "stringNumberPair",
                tupleConstructor(stringVal("str1"), numberVal(10)));

        testVar(stringNumberPairVar, tupleType(stringType(), numberType()),
                tupleConstructor(stringVal("str1"), numberVal(10)));
    }

    @Test
    @DisplayName("Type inference -> [(1, true), (2, \"str\")] = (number, AnyVal)[]")
    void testNumberAnyValPairArrayTypeInference() {
        var pairArrayVar = var(module, "pairArray", arrayConstructor(
                tupleConstructor(numberVal(1), booleanVal(true)),
                tupleConstructor(numberVal(2), stringVal("str"))
        ));

        testVar(pairArrayVar, arrayType(tupleType(numberType(), anyValType())), arrayConstructor(
                tupleConstructor(numberVal(1), booleanVal(true)),
                tupleConstructor(numberVal(2), stringVal("str"))
        ));
    }

    @Test
    @DisplayName("Type checker, invalid var -> var stringVar: string = 10 + 20")
    void testInvalidStringTypeChecker() {
        var stringVar = var(module, "stringVar", stringType(),
                add(numberVal(10), numberVal(20)));

        testVar(stringVar, new InvalidAssignExprTypeError(stringVar.getValueExpr().getSourceCodeRef(),
                stringType(), numberType()));
    }

    @Test
    @DisplayName("Type checker -> var anyValVar: AnyVal = 10 + 20")
    void testAnyValTypeChecker() {
        var anyValVar = var(module, "anyValVar", anyValType(),
                add(numberVal(10), numberVal(20)));

        testVar(anyValVar, anyValType(), numberVal(30));
    }

    @Test
    @DisplayName("Type checker -> var anyTypeVar: Any = 10 + 20")
    void testAnyTypeChecker() {
        var anyTypeVar = var(module, "anyTypeVar", anyType(),
                add(numberVal(10), numberVal(20)));

        testVar(anyTypeVar, anyType(), numberVal(30));
    }

    @Test
    @DisplayName("Type checker, invalid var -> var anyRecordVar: AnyRecord = 10 + 20")
    void testInvalidAnyRecordTypeChecker() {
        var anyRecordVar = var(module, "anyRecordVar", anyRecordType(),
                add(numberVal(10), numberVal(20)));

        testVar(anyRecordVar, new InvalidAssignExprTypeError(anyRecordVar.getValueExpr().getSourceCodeRef(),
                anyRecordType(), numberType()));
    }

    @Test
    @DisplayName("Type checker, invalid initialization -> var missingTypeVar = null")
    void testTypeInferenceWithNullVal() {
        var missingTypeVar = var(module, "missingTypeVar", nullVal());

        testVar(missingTypeVar, new InvalidVariableDeclError(missingTypeVar.getSourceCodeRef()));
    }

    @Test
    @DisplayName("Type checker, invalid var -> var missingTypeAndVal")
    void testMissingTypeAndVal() {
        var missingTypeVar = var(module, "missingTypeVar", (Expr) null);

        testVar(missingTypeVar, new InvalidVariableDeclError(missingTypeVar.getSourceCodeRef()));
    }

    @Test
    @DisplayName("Type checker -> var stringVar: string")
    void testVarDeclWithoutVal() {
        var stringVar = var(module, "stringVar", stringType());

        testVar(stringVar, stringType(), nullVal());
    }

    @Test
    @DisplayName("Type checker -> var stringVar: string = null")
    void testVarNullVal() {
        var stringVar = var(module, "stringVar", stringType(), nullVal());

        testVar(stringVar, stringType(), nullVal());
    }

    @Test
    @DisplayName("Type checker -> invalid assignment")
    void testInvalidAssignment() {
        var numberVar = var(module, "numberVar",
                add(numberVal(10), numberVal(20)));
        var assignment = assign(ref(module, "numberVar"), stringVal("str"));

        analyze(module, block(numberVar, assignment));
        eval(module, block(numberVar, assignment));
        assertErrors(new InvalidAssignExprTypeError(assignment.getExprRight().getSourceCodeRef(), numberType(), stringType()));
    }

    @Test
    @DisplayName("Type checker -> void assignment without type")
    void testVoidAssignmentWithoutType() {
        var myFunc = functionSymbol(module, "myFunc");
        myFunc.analyze(analyzerContext, evalContextProvider);

        var fnCall = functionCall(module, "myFunc");
        var myVar = var(module, "myVar", fnCall);
        analyze(module, block(myVar));
        assertErrors(new InvalidVariableDeclError(myVar.getSourceCodeRef()));
    }

    @Test
    @DisplayName("Type checker -> void assignment")
    void testVoidAssignment() {
        var myFunc = functionSymbol(module, "myFunc");
        myFunc.analyze(analyzerContext, evalContextProvider);

        var fnCall = functionCall(module, "myFunc");
        var myVar = var(module, "myVar", stringType(), fnCall);
        analyze(module, block(myVar));
        assertErrors(new InvalidAssignExprTypeError(fnCall.getSourceCodeRef(), stringType(), null));
    }

    @Test
    @DisplayName("Scope -> function scope")
    void testFunctionScope() {
        var function1 = functionSymbol(module, "myFunction", numberType());
        var myVar = var(module, "myVar", mult(
                add(numberVal(10), numberVal(23)),
                sub(numberVal(12.1), numberVal(6))));
        function1.setBlock(block(
                myVar,
                returnStatement(ref(module, "myVar"))
        ));
        var function2 = functionSymbol(module, "myFunction2", numberType());
        var returnStat = returnStatement(ref(module, "myVar"));
        function2.setBlock(block(returnStat));
        function1.analyze(analyzerContext, evalContextProvider);
        function2.analyze(analyzerContext, evalContextProvider);

        assertErrors(new UndefinedSymbolError(returnStat.getExpr().getSourceCodeRef(), myVar.getName()));
    }

    @Test
    @DisplayName("Scope -> symbol conflict")
    void testSymbolConflict() {
        var myVar1 = var(module, "myVar", stringVal("str1"));
        var myVar2 = var(module, "myVar", numberVal(10));

        analyze(module, block(myVar1, myVar2));
        assertErrors(new SymbolConflictError(myVar1.getVarSymbol(), myVar2.getVarSymbol()));
    }

    @Test
    @DisplayName("Scope -> local vars and functions cannot share the same name")
    void testLocalVarAndFunctionNameResolution() {
        var function = functionSymbol(module, "mySymbol", numberType());
        function.setBlock(block(returnStatement(numberVal(10))));
        function.analyze(analyzerContext, evalContextProvider);

        var myVar = var(module, "mySymbol", stringVal("str"));
        var myVar2 = var(module, "myVar", ref(module, "mySymbol"));
        FunctionCallExpr functionCall = functionCall(module, "mySymbol");
        var myVar3 = var(module, "myVar2", functionCall);

        analyze(module, block(myVar, myVar2, myVar3));
        assertErrors(
                new SymbolConflictError(function, myVar.getVarSymbol()),
                new InvalidFunctionRefError(functionCall.getFunctionRefExpr().getSourceCodeRef(), myVar.getVarSymbol().getType())
        );
    }

    @Test
    @DisplayName("Scope -> symbol shadowing")
    void testSymbolShadowing() {
        var myVar = var(module, "myVar", stringVal("str"));
        var myVar2 = var(module, "myVar", numberVal(10));
        var ifStat = ifStatement(booleanVal(true), block(
                myVar2,
                assign(ref(module, "myVar"), numberVal(20))
        ));
        var assign = assign(ref(module, "myVar"), stringVal("str2"));

        analyze(module, block(myVar, ifStat, assign));
        var evalContext = eval(module, block(myVar, ifStat, assign));

        assertNoErrors();
        assertVar(evalContext, myVar.getName(), stringType(), stringVal("str2"));
    }

    @Test
    @DisplayName("Scope -> var ref outside scope")
    void testRefOutsideScope() {
        var myVar = var(module, "myVar", stringVal("str"));
        var ifStat = ifStatement(booleanVal(true), block(myVar));
        var assign = assign(ref(module, "myVar"), stringVal("str2"));

        analyze(module, block(ifStat, assign));
        assertErrors(new UndefinedSymbolError(assign.getExprLeft().getSourceCodeRef(), myVar.getName()));
    }

    private void testVar(VarDeclExpr varDecl, Type expectedType, ValueExpr expectedValue) {
        analyze(module, block(varDecl));
        var evalContext = eval(module, block(varDecl));
        assertNoErrors();
        assertVar(evalContext, varDecl.getName(), expectedType, expectedValue);
    }

    private void testVar(VarDeclExpr varDecl, Type expectedType, RecordConstructorCallExpr recordContructor) {
        analyze(module, block(varDecl));
        var evalContext = eval(module, block(varDecl));
        assertNoErrors();
        assertVar(evalContext, varDecl.getName(), expectedType, record(recordContructor, evalContext));
    }

    private void testVar(VarDeclExpr varDecl, Type expectedType, ArrayConstructorCallExpr arrayConstructor) {
        analyze(module, block(varDecl));
        var evalContext = eval(module, block(varDecl));
        assertNoErrors();
        assertVar(evalContext, varDecl.getName(), expectedType, array(arrayConstructor, evalContext));
    }

    private void testVar(VarDeclExpr varDecl, Type expectedType, TupleConstructorCallExpr tupleConstructor) {
        analyze(module, block(varDecl));
        var evalContext = eval(module, block(varDecl));
        assertNoErrors();
        assertVar(evalContext, varDecl.getName(), expectedType, tuple(tupleConstructor, evalContext));
    }

    private void testVar(VarDeclExpr varDecl, AnalyzerError... expectedErrors) {
        analyze(module, block(varDecl));
        eval(module, block(varDecl));
        assertErrors(expectedErrors);
    }

}
