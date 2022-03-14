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

package dev.cgrscript.interpreter;

import dev.cgrscript.interpreter.ast.eval.*;
import dev.cgrscript.interpreter.ast.eval.context.EvalModeEnum;
import dev.cgrscript.interpreter.ast.eval.expr.*;
import dev.cgrscript.interpreter.ast.eval.expr.value.BooleanValueExpr;
import dev.cgrscript.interpreter.ast.eval.expr.value.NumberValueExpr;
import dev.cgrscript.interpreter.ast.eval.expr.value.RecordConstructorCallExpr;
import dev.cgrscript.interpreter.ast.eval.expr.value.StringValueExpr;
import dev.cgrscript.interpreter.ast.eval.statement.ElseIfStatement;
import dev.cgrscript.interpreter.ast.eval.statement.ForStatement;
import dev.cgrscript.interpreter.ast.eval.statement.IfStatement;
import dev.cgrscript.interpreter.ast.eval.statement.WhileStatement;
import dev.cgrscript.interpreter.ast.symbol.*;
import dev.cgrscript.interpreter.file_system.ScriptRef;
import dev.cgrscript.interpreter.module.ModuleIndex;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public abstract class AstTestBase {

    private int idGen;

    ModuleScope module(String moduleId) {
        var scriptRef = new MockScriptRef(moduleId);
        return new ModuleScope(moduleId, scriptRef, "/", new ArrayList<>(), new HashMap<>(),
                new ModuleIndex(), EvalModeEnum.EXECUTION);
    }

    Type anyType() {
        return BuiltinScope.ANY_TYPE;
    }

    Type stringType() {
        return BuiltinScope.STRING_TYPE;
    }

    Type numberType() {
        return BuiltinScope.NUMBER_TYPE;
    }

    Type booleanType() {
        return BuiltinScope.BOOLEAN_TYPE;
    }

    Type recordTypeRefType() {
        return BuiltinScope.RECORD_TYPE_REF_TYPE;
    }

    Type ruleRefType() {
        return BuiltinScope.RULE_REF_TYPE;
    }

    Type anyRecordType() {
        return BuiltinScope.ANY_RECORD_TYPE;
    }

    Type anyValType() {
        return BuiltinScope.ANY_VAL_TYPE;
    }

    Type templateType() {
        return BuiltinScope.TEMPLATE_TYPE;
    }

    StringValueExpr stringVal(String value) {
        return new StringValueExpr(sourceCodeRef(), value);
    }

    NumberValueExpr numberVal(Number value) {
        return new NumberValueExpr(sourceCodeRef(), value);
    }

    BooleanValueExpr booleanVal(Boolean value) {
        return new BooleanValueExpr(sourceCodeRef(), value);
    }

    SourceCodeRef sourceCodeRef() {
        return new MockSourceCodeRef();
    }

    VarDeclExpr var(ModuleScope module, String name) {
        var varSymbol = new VariableSymbol(module, sourceCodeRef(), name, null);
        return new VarDeclExpr(varSymbol);
    }

    VarDeclExpr var(ModuleScope module, String name, Type type) {
        var varSymbol = new VariableSymbol(module, sourceCodeRef(), name, type);
        return new VarDeclExpr(varSymbol);
    }

    VarDeclExpr var(ModuleScope module, String name, Expr value) {
        var varSymbol = new VariableSymbol(module, sourceCodeRef(), name, null);
        var varDecl = new VarDeclExpr(varSymbol);
        varDecl.setValueExpr(value);
        return varDecl;
    }

    VarDeclExpr var(ModuleScope module, String name, Type type, Expr value) {
        var varSymbol = new VariableSymbol(module, sourceCodeRef(), name, type);
        var varDecl = new VarDeclExpr(varSymbol);
        varDecl.setValueExpr(value);
        return varDecl;
    }

    AddExpr add(Expr left, Expr right) {
        return new AddExpr(sourceCodeRef(), left, right);
    }

    SubExpr sub(Expr left, Expr right) {
        return new SubExpr(sourceCodeRef(), left, right);
    }

    MultExpr mult(Expr left, Expr right) {
        return new MultExpr(sourceCodeRef(), left, right);
    }

    DivExpr div(Expr left, Expr right) {
        return new DivExpr(sourceCodeRef(), left, right);
    }

    NotExpr not(Expr expr) {
        return new NotExpr(sourceCodeRef(), expr);
    }

    LogicExpr and(Expr left, Expr right) {
        return new LogicExpr(sourceCodeRef(), left, LogicOperatorEnum.AND, right);
    }

    LogicExpr or(Expr left, Expr right) {
        return new LogicExpr(sourceCodeRef(), left, LogicOperatorEnum.OR, right);
    }

    EqExpr equals(Expr left, Expr right) {
        return new EqExpr(sourceCodeRef(), left, EqOperatorEnum.EQUALS, right);
    }

    EqExpr notEquals(Expr left, Expr right) {
        return new EqExpr(sourceCodeRef(), left, EqOperatorEnum.NOT_EQUALS, right);
    }

    EqExpr less(Expr left, Expr right) {
        return new EqExpr(sourceCodeRef(), left, EqOperatorEnum.LESS, right);
    }

    EqExpr lessOrEquals(Expr left, Expr right) {
        return new EqExpr(sourceCodeRef(), left, EqOperatorEnum.LESS_OR_EQUALS, right);
    }

    EqExpr greater(Expr left, Expr right) {
        return new EqExpr(sourceCodeRef(), left, EqOperatorEnum.GREATER, right);
    }

    EqExpr greaterOrEquals(Expr left, Expr right) {
        return new EqExpr(sourceCodeRef(), left, EqOperatorEnum.GREATER_OR_EQUALS, right);
    }

    PostIncDecExpr postInc(Expr expr) {
        return new PostIncDecExpr(sourceCodeRef(), expr, IncDecOperatorEnum.INC);
    }

    PostIncDecExpr postDec(Expr expr) {
        return new PostIncDecExpr(sourceCodeRef(), expr, IncDecOperatorEnum.DEC);
    }

    PreIncDecExpr preInc(Expr expr) {
        return new PreIncDecExpr(sourceCodeRef(), expr, IncDecOperatorEnum.INC);
    }

    PreIncDecExpr preDec(Expr expr) {
        return new PreIncDecExpr(sourceCodeRef(), expr, IncDecOperatorEnum.DEC);
    }

    RefExpr ref(ModuleScope module, String name) {
        return new RefExpr(module, sourceCodeRef(), name);
    }

    List<Evaluable> block(Evaluable... evaluable) {
        return Arrays.asList(evaluable);
    }

    IfStatement ifStatement(Expr condExpr, List<Evaluable> block) {
        return new IfStatement(sourceCodeRef(), condExpr, block);
    }

    ElseIfStatement elseIf(IfStatement ifStatement, Expr condExpr, List<Evaluable> block) {
        var elseIf = new ElseIfStatement(sourceCodeRef(), condExpr, block);
        ifStatement.setElseIf(elseIf);
        return elseIf;
    }

    ElseIfStatement elseIf(ElseIfStatement elseIf, Expr condExpr, List<Evaluable> block) {
        var nextElseIf = new ElseIfStatement(sourceCodeRef(), condExpr, block);
        elseIf.setElseIf(nextElseIf);
        return nextElseIf;
    }

    WhileStatement whileStatement(Expr condExpr, List<Evaluable> block) {
        return new WhileStatement(sourceCodeRef(), condExpr, block);
    }

    List<VarDeclExpr> varDeclList(VarDeclExpr... varDecl) {
        return Arrays.asList(varDecl);
    }

    List<Expr> exprList(Expr... expr) {
        return Arrays.asList(expr);
    }

    List<Statement> statementList(Statement... statement) {
        return Arrays.asList(statement);
    }

    ForStatement forStatement(List<VarDeclExpr> varDeclList, List<Expr> condExprList,
                              List<Statement> stepList, List<Evaluable> block) {
        return new ForStatement(sourceCodeRef(), varDeclList, condExprList, stepList, block);
    }

    RecordTypeAttribute attribute(ModuleScope module, String name, Type type) {
        return new RecordTypeAttribute(module, sourceCodeRef(), name, type);
    }

    List<RecordTypeAttribute> attributeList(RecordTypeAttribute... attribute) {
        return Arrays.asList(attribute);
    }

    RecordTypeSymbol recordType(ModuleScope module, String name) {
        return new RecordTypeSymbol(sourceCodeRef(), name, module, null);
    }

    RecordTypeSymbol recordType(ModuleScope module, String name, List<RecordTypeAttribute> attributes) {
        var recordType = new RecordTypeSymbol(sourceCodeRef(), name, module, null);
        attributes.forEach(recordType::addAttribute);
        return recordType;
    }

    RecordTypeSymbol recordType(ModuleScope module, String name, List<RecordTypeAttribute> attributes, Type starAttrType) {
        var recordType = new RecordTypeSymbol(sourceCodeRef(), name, module, null);
        attributes.forEach(recordType::addAttribute);
        recordType.setStarAttribute(new RecordTypeStarAttribute(sourceCodeRef(), starAttrType));
        return recordType;
    }

    RecordTypeSymbol recordType(ModuleScope module, String name, RecordTypeSymbol superType) {
        var recordType = new RecordTypeSymbol(sourceCodeRef(), name, module, null);
        recordType.setSuperType(new RecordSuperType(sourceCodeRef(), superType));
        return recordType;
    }

    RecordTypeSymbol recordType(ModuleScope module, String name,
                                RecordTypeSymbol superType, List<RecordTypeAttribute> attributes) {
        var recordType = new RecordTypeSymbol(sourceCodeRef(), name, module, null);
        attributes.forEach(recordType::addAttribute);
        recordType.setSuperType(new RecordSuperType(sourceCodeRef(), superType));
        return recordType;
    }

    RecordTypeSymbol recordType(ModuleScope module, String name,
                                RecordTypeSymbol superType, List<RecordTypeAttribute> attributes, Type starAttrType) {
        var recordType = new RecordTypeSymbol(sourceCodeRef(), name, module, null);
        attributes.forEach(recordType::addAttribute);
        recordType.setStarAttribute(new RecordTypeStarAttribute(sourceCodeRef(), starAttrType));
        recordType.setSuperType(new RecordSuperType(sourceCodeRef(), superType));
        return recordType;
    }

    RecordFieldExpr recordField(String name, Expr expr) {
        return new RecordFieldExpr(sourceCodeRef(), name, expr);
    }

    RecordConstructorCallExpr recordConstructor(Type recordType) {
        return new RecordConstructorCallExpr(sourceCodeRef(), recordType);
    }

    RecordConstructorCallExpr recordConstructor(Type recordType, RecordFieldExpr... fields) {
        var recordConstructor = new RecordConstructorCallExpr(sourceCodeRef(), recordType);
        for (RecordFieldExpr field : fields) {
            recordConstructor.addField(field);
        }
        return recordConstructor;
    }

    private class MockSourceCodeRef extends SourceCodeRef {

        private final int id = idGen++;

        public MockSourceCodeRef() {
            super(null);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            MockSourceCodeRef that = (MockSourceCodeRef) o;
            return id == that.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), id);
        }

    }

    private static class MockScriptRef implements ScriptRef {

        private final String moduleId;

        private MockScriptRef(String moduleId) {
            this.moduleId = moduleId;
        }

        @Override
        public String getAbsolutePath() {
            return "/" + moduleId.replace('.', '/');
        }

        @Override
        public InputStream newInputStream() throws IOException {
            return null;
        }

        @Override
        public String extractModuleId() {
            return moduleId;
        }
    }

}
