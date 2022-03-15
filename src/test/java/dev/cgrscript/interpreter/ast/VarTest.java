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

import dev.cgrscript.interpreter.ast.eval.Expr;
import dev.cgrscript.interpreter.ast.eval.ValueExpr;
import dev.cgrscript.interpreter.ast.eval.expr.VarDeclExpr;
import dev.cgrscript.interpreter.ast.eval.expr.value.ArrayConstructorCallExpr;
import dev.cgrscript.interpreter.ast.eval.expr.value.RecordConstructorCallExpr;
import dev.cgrscript.interpreter.ast.symbol.ModuleScope;
import dev.cgrscript.interpreter.ast.symbol.Type;
import dev.cgrscript.interpreter.error.AnalyzerError;
import dev.cgrscript.interpreter.error.analyzer.InvalidAssignExprTypeError;
import dev.cgrscript.interpreter.error.analyzer.InvalidVariableDeclError;
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
    @DisplayName("Type inference (Record type reference) -> MyRecord = Type")
    void testTypeRefInference() {
        var recType = recordType(module, "MyRecord");
        recType.analyze(analyzerContext, evalContextProvider);

        testVar(var(module, "typeVar", ref(module, "MyRecord")), recordTypeRefType(), typeRef(recType));
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
    @DisplayName("Typ checker -> var stringVar: string")
    void testVarDeclWithoutVal() {
        var stringVar = var(module, "stringVar", stringType());

        testVar(stringVar, stringType(), nullVal());
    }

    private void testVar(VarDeclExpr varDecl, Type expectedType, ValueExpr expectedValue) {
        var evalContext = evalContext(module);
        varDecl.analyze(evalContext);
        evalContext = evalContext(module);
        varDecl.evalStat(evalContext);
        assertNoErrors();
        assertVar(evalContext, varDecl.getName(), expectedType, expectedValue);
    }

    private void testVar(VarDeclExpr varDecl, Type expectedType, RecordConstructorCallExpr recordContructor) {
        var evalContext = evalContext(module);
        varDecl.analyze(evalContext);
        evalContext = evalContext(module);
        varDecl.evalStat(evalContext);
        assertNoErrors();
        assertVar(evalContext, varDecl.getName(), expectedType, record(recordContructor, evalContext));
    }

    private void testVar(VarDeclExpr varDecl, Type expectedType, ArrayConstructorCallExpr arrayConstructor) {
        var evalContext = evalContext(module);
        varDecl.analyze(evalContext);
        evalContext = evalContext(module);
        varDecl.evalStat(evalContext);
        assertNoErrors();
        assertVar(evalContext, varDecl.getName(), expectedType, array(arrayConstructor, evalContext));
    }

    private void testVar(VarDeclExpr varDecl, AnalyzerError... expectedErrors) {
        var evalContext = evalContext(module);
        varDecl.analyze(evalContext);
        evalContext = evalContext(module);
        varDecl.evalStat(evalContext);
        assertErrors(expectedErrors);
    }

}
