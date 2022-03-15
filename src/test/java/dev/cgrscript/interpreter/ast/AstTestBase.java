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

import dev.cgrscript.database.Database;
import dev.cgrscript.interpreter.ast.eval.*;
import dev.cgrscript.interpreter.ast.eval.context.EvalContext;
import dev.cgrscript.interpreter.ast.eval.context.EvalContextProvider;
import dev.cgrscript.interpreter.ast.eval.context.EvalModeEnum;
import dev.cgrscript.interpreter.ast.eval.expr.*;
import dev.cgrscript.interpreter.ast.eval.expr.value.*;
import dev.cgrscript.interpreter.ast.eval.statement.ElseIfStatement;
import dev.cgrscript.interpreter.ast.eval.statement.ForStatement;
import dev.cgrscript.interpreter.ast.eval.statement.IfStatement;
import dev.cgrscript.interpreter.ast.eval.statement.WhileStatement;
import dev.cgrscript.interpreter.ast.query.Query;
import dev.cgrscript.interpreter.ast.query.QueryTypeClause;
import dev.cgrscript.interpreter.ast.symbol.*;
import dev.cgrscript.interpreter.error.AnalyzerError;
import dev.cgrscript.interpreter.file_system.ScriptRef;
import dev.cgrscript.interpreter.input.FileFetcher;
import dev.cgrscript.interpreter.input.InputReader;
import dev.cgrscript.interpreter.module.ModuleIndex;
import dev.cgrscript.interpreter.writer.FileSystemWriterHandler;
import dev.cgrscript.interpreter.writer.OutputWriter;
import dev.cgrscript.interpreter.writer.OutputWriterLogTypeEnum;
import dev.cgrscript.interpreter.writer.OutputWriterModeEnum;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Base class for AST unit tests. Provides methods for AST construction and validation.
 */
public abstract class AstTestBase {

    private int idGen;

    final AnalyzerContext analyzerContext = new AnalyzerContext();

    final EvalContextProvider evalContextProvider;

    public AstTestBase() {
        Database database = new Database();
        InputReader inputReader = new InputReader(new FileFetcher());
        OutputWriter outputWriter = new OutputWriter(
                OutputWriterModeEnum.LOG_ONLY,
                OutputWriterLogTypeEnum.NORMAL,
                new FileSystemWriterHandler());
        evalContextProvider = new EvalContextProvider(EvalModeEnum.EXECUTION, database, inputReader, outputWriter);
    }

    @BeforeEach
    void pushErrorScope() {
        analyzerContext.pushErrorScope();
    }

    void assertErrors(AnalyzerError... expectedErrors) {
        var actualErrors = analyzerContext.getAllErrors();
        assertTrue(actualErrors.containsAll(Arrays.asList(expectedErrors)) && actualErrors.size() == expectedErrors.length,
                () -> printErrors(expectedErrors));
    }

    void assertNoErrors() {
        assertEquals(0, analyzerContext.getAllErrors().size(), this::printErrors);
    }

    void assertVar(EvalContext evalContext, String varName, Type type, ValueExpr value) {
        var symbol = evalContext.getCurrentScope().resolve(varName);
        assertTrue(symbol instanceof VariableSymbol);
        Type varType = ((VariableSymbol) symbol).getType();
        assertNotNull(varType, () -> "var '" + varName + "' has no type");
        assertEquals(type.getName(), varType.getName(), () -> "var '" + varName + "' has type '" + varType.getName()
                + "'. Expected: '" + type.getName() + "'");
        var actualValue = evalContext.getCurrentScope().getValue(varName);
        var actualValueStr = actualValue != null ? actualValue.getStringValue() : null;
        assertEquals(new ValueWrapper(value), new ValueWrapper(actualValue),
                () -> "var '" + varName + "' has value " + actualValueStr
                    + ". Expected: " + value.getStringValue());
    }

    EvalContext evalContext(ModuleScope module) {
        return evalContextProvider.newEvalContext(analyzerContext, module);
    }

    ValueExpr run(FunctionSymbol function, ValueExpr... args) {
        return function.eval(analyzerContext, evalContextProvider, Arrays.asList(args));
    }

    ModuleScope module(String moduleId) {
        var scriptRef = new MockScriptRef(moduleId);
        return new ModuleScope(moduleId, scriptRef, "/", new ArrayList<>(), new HashMap<>(),
                new ModuleIndex(), EvalModeEnum.EXECUTION);
    }

    void importModule(ModuleScope module, ModuleScope imported) {
        importModule(module, imported, null);
    }

    void importModule(ModuleScope module, ModuleScope imported, String alias) {
        module.addModule(analyzerContext, imported, alias, sourceCodeRef());
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

    Type arrayType(Type elementType) {
        return new ArrayType(elementType);
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

    NullValueExpr nullVal() {
        return new NullValueExpr(sourceCodeRef());
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
        var recordType = new RecordTypeSymbol(sourceCodeRef(), name, module, null);
        module.define(analyzerContext, recordType);
        return recordType;
    }

    RecordTypeSymbol recordType(ModuleScope module, String name, List<RecordTypeAttribute> attributes) {
        var recordType = new RecordTypeSymbol(sourceCodeRef(), name, module, null);
        module.define(analyzerContext, recordType);
        attributes.forEach(recordType::addAttribute);
        return recordType;
    }

    RecordTypeSymbol recordType(ModuleScope module, String name, List<RecordTypeAttribute> attributes, Type starAttrType) {
        var recordType = new RecordTypeSymbol(sourceCodeRef(), name, module, null);
        module.define(analyzerContext, recordType);
        attributes.forEach(recordType::addAttribute);
        recordType.setStarAttribute(new RecordTypeStarAttribute(sourceCodeRef(), starAttrType));
        return recordType;
    }

    RecordTypeSymbol recordType(ModuleScope module, String name, RecordTypeSymbol superType) {
        var recordType = new RecordTypeSymbol(sourceCodeRef(), name, module, null);
        module.define(analyzerContext, recordType);
        recordType.setSuperType(new RecordSuperType(sourceCodeRef(), superType));
        return recordType;
    }

    RecordTypeSymbol recordType(ModuleScope module, String name,
                                RecordTypeSymbol superType, List<RecordTypeAttribute> attributes) {
        var recordType = new RecordTypeSymbol(sourceCodeRef(), name, module, null);
        module.define(analyzerContext, recordType);
        attributes.forEach(recordType::addAttribute);
        recordType.setSuperType(new RecordSuperType(sourceCodeRef(), superType));
        return recordType;
    }

    RecordTypeSymbol recordType(ModuleScope module, String name,
                                RecordTypeSymbol superType, List<RecordTypeAttribute> attributes, Type starAttrType) {
        var recordType = new RecordTypeSymbol(sourceCodeRef(), name, module, null);
        module.define(analyzerContext, recordType);
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

    RecordValueExpr record(RecordConstructorCallExpr constructor, EvalContext evalContext) {
        return (RecordValueExpr) constructor.evalExpr(evalContext);
    }

    ArrayConstructorCallExpr arrayConstructor(Expr... elements) {
        return new ArrayConstructorCallExpr(sourceCodeRef(), Arrays.asList(elements));
    }

    ArrayValueExpr array(ArrayConstructorCallExpr constructor, EvalContext evalContext) {
        return (ArrayValueExpr) constructor.evalExpr(evalContext);
    }

    PairConstructorCallExpr pairConstructor(Expr left, Expr right) {
        return new PairConstructorCallExpr(sourceCodeRef(), left, right);
    }

    RuleRefValueExpr ruleRef(RuleSymbol rule) {
        return new RuleRefValueExpr(rule);
    }

    RecordTypeRefValueExpr typeRef(RecordTypeSymbol recordTypeSymbol) {
        return new RecordTypeRefValueExpr(recordTypeSymbol);
    }

    CastExpr cast(Type targetType, Expr expr) {
        return new CastExpr(sourceCodeRef(), targetType, expr);
    }

    FieldAccessExpr fieldAccess(Expr left, Expr right) {
        return new FieldAccessExpr(sourceCodeRef(), left, right);
    }

    FunctionParameter functionParameter(String name, Type type) {
        return new FunctionParameter(sourceCodeRef(), name, type, false);
    }

    FunctionParameter functionParameter(String name, Type type, boolean optional) {
        return new FunctionParameter(sourceCodeRef(), name, type, optional);
    }

    FunctionSymbol functionSymbol(ModuleScope module, String name, FunctionParameter... parameters) {
        var fn = new FunctionSymbol(sourceCodeRef(), sourceCodeRef(), module, name, null);
        fn.setParameters(Arrays.asList(parameters));
        module.define(analyzerContext, fn);
        return fn;
    }

    FunctionSymbol functionSymbol(ModuleScope module, String name, Type returnType, FunctionParameter... parameters) {
        var fn = new FunctionSymbol(sourceCodeRef(), sourceCodeRef(), module, name, null);
        fn.setParameters(Arrays.asList(parameters));
        fn.setReturnType(returnType);
        module.define(analyzerContext, fn);
        return fn;
    }

    FunctionArgExpr functionArg(Expr expr) {
        return new FunctionArgExpr(sourceCodeRef(), expr);
    }

    FunctionCallExpr functionCall(ModuleScope module, String name, FunctionArgExpr... args) {
        return new FunctionCallExpr(module, sourceCodeRef(), name, Arrays.asList(args));
    }

    RuleSymbol rule(ModuleScope module, String name, Type targetType) {
        var rule = new RuleSymbol(sourceCodeRef(), name, sourceCodeRef(), module, RuleTypeEnum.RULE, null);
        var query = new Query(sourceCodeRef(), new QueryTypeClause(module, sourceCodeRef(), sourceCodeRef(), targetType,
                false, "rec"));
        rule.setQuery(query);
        module.define(analyzerContext, rule);
        return rule;
    }

    String printErrors(AnalyzerError... expected) {
        return "\n Expected:\n" + Arrays.stream(expected)
                .map(AnalyzerError::toString)
                .collect(Collectors.joining("\n")) + "\n "
                + "Found:\n" + analyzerContext.getAllErrors().stream()
                .map(AnalyzerError::toString)
                .collect(Collectors.joining("\n"));
    }

    String printErrors() {
        return "\n Expected: no errors.\n "
                + "Found: " + analyzerContext.getAllErrors().stream()
                .map(AnalyzerError::toString)
                .collect(Collectors.joining("\n"));
    }

    private class MockSourceCodeRef extends SourceCodeRef {

        private final int id = idGen++;

        public MockSourceCodeRef() {
            super(null);
        }

        @Override
        public String toString() {
            return "MockSourceCodeRef: " + id;
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

    private static class ValueWrapper {

        private final ValueExpr value;

        private ValueWrapper(ValueExpr value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ValueWrapper) {
                ValueExpr other = ((ValueWrapper) obj).value;
                if (value instanceof RecordValueExpr && other instanceof RecordValueExpr) {
                    RecordValueExpr r1 = (RecordValueExpr) value;
                    RecordValueExpr r2 = (RecordValueExpr) other;
                    return r1.getType().equals(r2.getType()) && r1.getStringValue().equals(r2.getStringValue());
                }
                if (value instanceof ArrayValueExpr && other instanceof ArrayValueExpr) {
                    List<ValueWrapper> l1 = ((ArrayValueExpr)value).getValue().stream().map(ValueWrapper::new).collect(Collectors.toList());
                    List<ValueWrapper> l2 = ((ArrayValueExpr)value).getValue().stream().map(ValueWrapper::new).collect(Collectors.toList());
                    return l1.equals(l2);
                }
                return value.equals(other);
            }
            return false;
        }
    }

}
