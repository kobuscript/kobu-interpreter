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

import dev.kobu.interpreter.ast.eval.expr.RecordFieldExpr;
import dev.kobu.interpreter.ast.eval.expr.RefExpr;
import dev.kobu.interpreter.ast.eval.expr.value.NumberValueExpr;
import dev.kobu.interpreter.ast.symbol.ModuleScope;
import dev.kobu.interpreter.ast.symbol.RecordTypeAttribute;
import dev.kobu.interpreter.error.analyzer.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

@DisplayName("Records")
public class RecordTest extends AstTestBase {

    ModuleScope module;

    @BeforeEach
    void createModule() {
        module = module("mod");
    }

    @Test
    @DisplayName("Record type -> scalar attributes")
    void recordTypeScalarAttributes() {
        var recType = recordType(module, "MyRecord", attributeList(
                attribute(module, "attr1", numberType()),
                attribute(module, "attr2", stringType())
        ));
        recType.analyze(analyzerContext, evalContextProvider);

        var myRecVar = var(module, "myRec", recordConstructor(recType,
                recordField("attr1", numberVal(1)),
                recordField("attr2", stringVal("name"))));
        var attr1Var = var(module, "attr1", fieldAccess(ref(module, "myRec"), ref(module, "attr1")));
        var attr2Var = var(module, "attr2", fieldAccess(ref(module, "myRec"), ref(module, "attr2")));
        analyze(module, block(myRecVar, attr1Var, attr2Var));
        var evalContext = eval(module, block(myRecVar, attr1Var, attr2Var));
        assertNoErrors();
        assertVar(evalContext, attr1Var.getName(), numberType(), numberVal(1));
        assertVar(evalContext, attr2Var.getName(), stringType(), stringVal("name"));
    }

    @Test
    @DisplayName("Record type -> composition")
    void recordTypeComposition() {
        var recType = recordType(module, "MyRecord", attributeList(
                attribute(module, "attr1", numberType()),
                attribute(module, "attr2", stringType())
        ));
        var recType2 = recordType(module, "MyRecord2", attributeList(
                attribute(module, "rec", recType)
        ));
        recType.analyze(analyzerContext, evalContextProvider);
        recType2.analyze(analyzerContext, evalContextProvider);

        var myRec2Var = var(module, "myRec2", recordConstructor(recType2,
                recordField("rec", recordConstructor(recType,
                        recordField("attr1", numberVal(1))
                )))
        );
        var myRecVar = var(module, "myRec", fieldAccess(ref(module, "myRec2"), ref(module, "rec")));
        var attr1Var = var(module, "attr1", fieldAccess(ref(module, "myRec"), ref(module, "attr1")));
        var attr2Var = var(module, "attr2", fieldAccess(ref(module, "myRec"), ref(module, "attr2")));
        analyze(module, block(myRec2Var, myRecVar, attr1Var, attr2Var));
        var evalContext = eval(module, block(myRec2Var, myRecVar, attr1Var, attr2Var));
        assertNoErrors();
        assertVar(evalContext, attr1Var.getName(), numberType(), numberVal(1));
        assertVar(evalContext, attr2Var.getName(), stringType(), nullVal());
    }

    @Test
    @DisplayName("Record type -> duplicated attribute")
    void recordTypeDuplicatedAttr() {
        RecordTypeAttribute attr1 = attribute(module, "attr1", numberType());
        RecordTypeAttribute attr2 = attribute(module, "attr1", stringType());
        var recType = recordType(module, "MyRecord", attributeList(
                attr1,
                attr2
        ));
        recType.analyze(analyzerContext, evalContextProvider);
        assertErrors(new RecordTypeAttributeConflictError(attr1, attr2));
    }

    @Test
    @DisplayName("Record type -> inheritance")
    void recordTypeInheritance() {
        var recType = recordType(module, "MyRecord", attributeList(
                attribute(module, "attr1", numberType()),
                attribute(module, "attr2", stringType())
        ));
        var recType2 = recordType(module, "MyRecord2", recType, attributeList(
                attribute(module, "attr3", booleanType())
        ));
        recType.analyze(analyzerContext, evalContextProvider);
        recType2.analyze(analyzerContext, evalContextProvider);

        var myRecVar = var(module, "myRec", recordConstructor(recType2,
                recordField("attr1", numberVal(1)),
                recordField("attr2", stringVal("name")),
                recordField("attr3", booleanVal(true))
        ));
        var attr1Var = var(module, "attr1", fieldAccess(ref(module, "myRec"), ref(module, "attr1")));
        var attr2Var = var(module, "attr2", fieldAccess(ref(module, "myRec"), ref(module, "attr2")));
        var attr3Var = var(module, "attr3", fieldAccess(ref(module, "myRec"), ref(module, "attr3")));
        analyze(module, block(myRecVar, attr1Var, attr2Var, attr3Var));
        var evalContext = eval(module, block(myRecVar, attr1Var, attr2Var, attr3Var));
        assertNoErrors();
        assertVar(evalContext, attr1Var.getName(), numberType(), numberVal(1));
        assertVar(evalContext, attr2Var.getName(), stringType(), stringVal("name"));
        assertVar(evalContext, attr3Var.getName(), booleanType(), booleanVal(true));
    }

    @Test
    @DisplayName("Record -> Record instance with invalid attribute type")
    void recordConstructorAttrInvalidType() {
        var recType = recordType(module, "MyRecord", attributeList(
                attribute(module, "attr1", numberType()),
                attribute(module, "attr2", stringType())
        ));
        var recType2 = recordType(module, "MyRecord2", recType, attributeList(
                attribute(module, "attr3", booleanType())
        ));
        recType.analyze(analyzerContext, evalContextProvider);
        recType2.analyze(analyzerContext, evalContextProvider);

        RecordFieldExpr attr2 = recordField("attr2", booleanVal(true));
        var recVar = var(module, "rec", recordConstructor(recType2,
                attr2));
        analyze(module, block(recVar));
        assertErrors(new InvalidRecordFieldTypeError(attr2.getSourceCodeRef(), recType2, "attr2", booleanType()));
    }

    @Test
    @DisplayName("Record -> Record instance with undefined attribute")
    void recordConstructorAttrUndefined() {
        var recType = recordType(module, "MyRecord", attributeList(
                attribute(module, "attr1", numberType()),
                attribute(module, "attr2", stringType())
        ));
        var recType2 = recordType(module, "MyRecord2", recType, attributeList(
                attribute(module, "attr3", booleanType())
        ));
        recType.analyze(analyzerContext, evalContextProvider);
        recType2.analyze(analyzerContext, evalContextProvider);

        RecordFieldExpr attr = recordField("undef", booleanVal(true));
        var recVar = var(module, "rec", recordConstructor(recType2,
                attr));
        analyze(module, block(recVar));
        assertErrors(new InvalidRecordFieldError(attr.getSourceCodeRef(), recType2, "undef"));
    }

    @Test
    @DisplayName("Record type -> Inheritance with duplicated attributes")
    void recordTypeInheritanceAttributeError() {
        RecordTypeAttribute attr1 = attribute(module, "attr1", numberType());
        var recType = recordType(module, "MyRecord", attributeList(
                attr1,
                attribute(module, "attr2", stringType())
        ));
        RecordTypeAttribute attr1dup = attribute(module, "attr1", booleanType());
        var recType2 = recordType(module, "MyRecord2", recType, attributeList(
                attr1dup
        ));
        recType.analyze(analyzerContext, evalContextProvider);
        recType2.analyze(analyzerContext, evalContextProvider);
        assertErrors(new RecordTypeAttributeConflictError(attr1dup, attr1));
    }

    @Test
    @DisplayName("Record type -> inheritance with cyclic reference")
    void recordTypeInheritanceCyclicReference() {
        var recType = recordType(module, "MyRecord", attributeList(
                attribute(module, "attr1", numberType()),
                attribute(module, "attr2", stringType())
        ));
        var recType2 = recordType(module, "MyRecord2", recType, attributeList(
                attribute(module, "attr3", booleanType())
        ));
        var recType3 = recordType(module, "MyRecord3", recType2, attributeList(
                attribute(module, "attr4", numberType())
        ));
        setSuperType(recType, recType3);
        recType.analyze(analyzerContext, evalContextProvider);
        recType2.analyze(analyzerContext, evalContextProvider);
        recType3.analyze(analyzerContext, evalContextProvider);
        assertErrors(
                new CyclicRecordInheritanceError(recType.getSuperTypeSourceCodeRef(),
                        List.of("mod.MyRecord", "mod.MyRecord3", "mod.MyRecord2", "mod.MyRecord")),
                new CyclicRecordInheritanceError(recType2.getSuperTypeSourceCodeRef(),
                        List.of("mod.MyRecord2", "mod.MyRecord", "mod.MyRecord3", "mod.MyRecord2")),
                new CyclicRecordInheritanceError(recType3.getSuperTypeSourceCodeRef(),
                        List.of("mod.MyRecord3", "mod.MyRecord2", "mod.MyRecord", "mod.MyRecord3"))
        );
    }

    @Test
    @DisplayName("Record -> attribute assignment")
    void recordAttributeAssignment() {
        var recType = recordType(module, "MyRecord", attributeList(
                attribute(module, "attr1", numberType()),
                attribute(module, "attr2", stringType())
        ));
        var recType2 = recordType(module, "MyRecord2", recType, attributeList(
                attribute(module, "attr3", booleanType())
        ));
        recType.analyze(analyzerContext, evalContextProvider);
        recType2.analyze(analyzerContext, evalContextProvider);

        var recVar = var(module, "rec", recordConstructor(recType2,
                recordField("attr2", stringVal(""))));
        var assign1 = assign(fieldAccess(ref(module, "rec"), ref(module, "attr1")),
                numberVal(12));
        var assign2 = assign(fieldAccess(ref(module, "rec"), ref(module, "attr3")),
                booleanVal(true));
        var attr1Var = var(module, "attr1", fieldAccess(ref(module, "rec"), ref(module, "attr1")));
        var attr2Var = var(module, "attr2", fieldAccess(ref(module, "rec"), ref(module, "attr2")));
        var attr3Var = var(module, "attr3", fieldAccess(ref(module, "rec"), ref(module, "attr3")));

        analyze(module, block(recVar, assign1, assign2, attr1Var, attr2Var, attr3Var));
        var evalContext = eval(module, block(recVar, assign1, assign2, attr1Var, attr2Var, attr3Var));
        assertNoErrors();
        assertVar(evalContext, attr1Var.getName(), numberType(), numberVal(12));
        assertVar(evalContext, attr2Var.getName(), stringType(), stringVal(""));
        assertVar(evalContext, attr3Var.getName(), booleanType(), booleanVal(true));
    }

    @Test
    @DisplayName("Record -> attribute invalid assignment")
    void recordAttributeInvalidAssignment() {
        var recType = recordType(module, "MyRecord", attributeList(
                attribute(module, "attr1", numberType()),
                attribute(module, "attr2", stringType())
        ));
        var recType2 = recordType(module, "MyRecord2", recType, attributeList(
                attribute(module, "attr3", booleanType())
        ));
        recType.analyze(analyzerContext, evalContextProvider);
        recType2.analyze(analyzerContext, evalContextProvider);

        var recVar = var(module, "rec", recordConstructor(recType2,
                recordField("attr2", stringVal(""))));
        NumberValueExpr numberVal = numberVal(12);
        var assign1 = assign(fieldAccess(ref(module, "rec"), ref(module, "attr2")),
                numberVal);
        analyze(module, block(recVar, assign1));
        assertErrors(new InvalidAssignExprTypeError(numberVal.getSourceCodeRef(), stringType(), numberType()));
    }

    @Test
    @DisplayName("Record -> assign undefined attribute")
    void recordAttributeAssignUndefinedAttr() {
        var recType = recordType(module, "MyRecord", attributeList(
                attribute(module, "attr1", numberType()),
                attribute(module, "attr2", stringType())
        ));
        var recType2 = recordType(module, "MyRecord2", recType, attributeList(
                attribute(module, "attr3", booleanType())
        ));
        recType.analyze(analyzerContext, evalContextProvider);
        recType2.analyze(analyzerContext, evalContextProvider);

        var recVar = var(module, "rec", recordConstructor(recType2,
                recordField("attr2", stringVal(""))));
        NumberValueExpr numberVal = numberVal(12);
        RefExpr undefRef = ref(module, "undef");
        var assign1 = assign(fieldAccess(ref(module, "rec"), undefRef),
                numberVal);
        analyze(module, block(recVar, assign1));
        assertErrors(new UndefinedFieldError(undefRef.getSourceCodeRef(), recType2, "undef"));
    }

    @Test
    @DisplayName("Record type -> star attribute")
    void recordTypeStarAttribute() {
        var recType = recordType(module, "MyRecord", attributeList(
                attribute(module, "attr1", numberType()),
                attribute(module, "attr2", booleanType())
        ), stringType());
        recType.analyze(analyzerContext, evalContextProvider);

        var recVar = var(module, "rec", recordConstructor(recType,
                recordField("attr1", numberVal(10)),
                recordField("anotherAttr", stringVal("str1"))));
        var assign1 = assign(fieldAccess(ref(module, "rec"), ref(module, "attr1")),
                numberVal(42));
        var assign2 = assign(fieldAccess(ref(module, "rec"), ref(module, "extraField")),
                stringVal("str2"));
        var extraVar = var(module, "extra", fieldAccess(ref(module, "rec"), ref(module, "extraField")));
        analyze(module, block(recVar, assign1, assign2, extraVar));
        var evalContext = eval(module, block(recVar, assign1, assign2, extraVar));
        assertNoErrors();
        assertVar(evalContext, recVar.getName(), recType, record(recordConstructor(recType,
                recordField("attr1", numberVal(42)),
                recordField("anotherAttr", stringVal("str1")),
                recordField("extraField", stringVal("str2"))), evalContext));
        assertVar(evalContext, extraVar.getName(), stringType(), stringVal("str2"));
    }

    @Test
    @DisplayName("Record type -> star attribute invalid assignment")
    void recordTypeStarAttributeInvalidAssignment() {
        var recType = recordType(module, "MyRecord", attributeList(
                attribute(module, "attr1", numberType()),
                attribute(module, "attr2", booleanType())
        ), stringType());
        recType.analyze(analyzerContext, evalContextProvider);

        var recVar = var(module, "rec", recordConstructor(recType,
                recordField("attr1", numberVal(10)),
                recordField("anotherAttr", stringVal("str1"))));
        NumberValueExpr numberVal = numberVal(2);
        var assign = assign(fieldAccess(ref(module, "rec"), ref(module, "extraField")),
                numberVal);
        analyze(module, block(recVar, assign));
        assertErrors(new InvalidAssignExprTypeError(numberVal.getSourceCodeRef(), stringType(), numberType()));
    }

    @Test
    @DisplayName("Record type -> inherited star attribute")
    void recordTypeInheritanceStarAttribute() {
        var recType = recordType(module, "MyRecord", attributeList(
                attribute(module, "attr1", numberType()),
                attribute(module, "attr2", booleanType())
        ), stringType());
        var recType2 = recordType(module, "MyRecord2", recType);
        recType.analyze(analyzerContext, evalContextProvider);
        recType2.analyze(analyzerContext, evalContextProvider);

        var recVar = var(module, "rec", recordConstructor(recType2,
                recordField("attr1", numberVal(10)),
                recordField("anotherAttr", stringVal("str1"))));
        var assign1 = assign(fieldAccess(ref(module, "rec"), ref(module, "attr1")),
                numberVal(42));
        var assign2 = assign(fieldAccess(ref(module, "rec"), ref(module, "extraField")),
                stringVal("str2"));
        var extraVar = var(module, "extra", fieldAccess(ref(module, "rec"), ref(module, "extraField")));
        analyze(module, block(recVar, assign1, assign2, extraVar));
        var evalContext = eval(module, block(recVar, assign1, assign2, extraVar));
        assertNoErrors();
        assertVar(evalContext, recVar.getName(), recType2, record(recordConstructor(recType2,
                recordField("attr1", numberVal(42)),
                recordField("anotherAttr", stringVal("str1")),
                recordField("extraField", stringVal("str2"))), evalContext));
        assertVar(evalContext, extraVar.getName(), stringType(), stringVal("str2"));
    }

    @Test
    @DisplayName("Record type -> star attribute conflict")
    void recordTypeInheritanceStarAttributeError() {
        var recType = recordType(module, "MyRecord", attributeList(
                attribute(module, "attr1", numberType()),
                attribute(module, "attr2", booleanType())
        ), stringType());
        var recType2 = recordType(module, "MyRecord2", recType, attributeList(), numberType());
        recType.analyze(analyzerContext, evalContextProvider);
        recType2.analyze(analyzerContext, evalContextProvider);
        assertErrors(new RecordSuperTypeConflictError(recType2, recType));
    }

}
