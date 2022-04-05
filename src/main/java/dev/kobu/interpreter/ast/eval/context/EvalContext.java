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

package dev.kobu.interpreter.ast.eval.context;

import dev.kobu.config.ProjectProperty;
import dev.kobu.database.Database;
import dev.kobu.interpreter.ast.AnalyzerContext;
import dev.kobu.interpreter.ast.eval.*;
import dev.kobu.interpreter.ast.symbol.*;
import dev.kobu.interpreter.ast.symbol.function.KobuFunction;
import dev.kobu.interpreter.ast.symbol.function.NativeFunctionSymbol;
import dev.kobu.interpreter.error.AnalyzerError;
import dev.kobu.interpreter.error.analyzer.UnreachableCodeError;
import dev.kobu.interpreter.error.eval.UserDefinedError;
import dev.kobu.interpreter.file_system.KobuFileSystem;
import dev.kobu.interpreter.input.InputReader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EvalContext {

    private final EvalContextProvider provider;

    private final AnalyzerContext analyzerContext;

    private final EvalModeEnum evalMode;

    private final ModuleScope moduleScope;

    private final KobuFileSystem fileSystem;

    private final Database database;

    private final InputReader inputReader;

    private final Map<String, String> properties = new HashMap<>();

    private UserDefinedFunction function;

    private RuleContext ruleContext;

    private Branch currentBranch;

    private LocalScope currentScope;

    private Type returnType;

    private boolean voidReturnType;

    private ValueExpr returnValue;

    private ErrorValue errorValue;

    private UserDefinedError lastUserError;

    protected EvalContext(EvalContextProvider provider, AnalyzerContext analyzerContext, EvalModeEnum evalMode,
                          ModuleScope moduleScope, KobuFileSystem fileSystem, Database database,
                          InputReader inputReader,
                          UserDefinedFunction function) {
        this.provider = provider;
        this.analyzerContext = analyzerContext;
        this.evalMode = evalMode;
        this.moduleScope = moduleScope;
        this.fileSystem = fileSystem;
        this.database = database;
        this.inputReader = inputReader;
        this.function = function;
        loadProperties();
        pushNewScope();
    }

    protected EvalContext(EvalContextProvider provider, AnalyzerContext analyzerContext, EvalModeEnum evalMode,
                          ModuleScope moduleScope, KobuFileSystem fileSystem, Database database,
                          InputReader inputReader,
                          RuleContext ruleContext) {
        this.provider = provider;
        this.analyzerContext = analyzerContext;
        this.evalMode = evalMode;
        this.moduleScope = moduleScope;
        this.fileSystem = fileSystem;
        this.database = database;
        this.inputReader = inputReader;
        this.ruleContext = ruleContext;
        loadProperties();
        pushNewScope();
    }

    protected EvalContext(EvalContextProvider provider, AnalyzerContext analyzerContext, EvalModeEnum evalMode,
                          ModuleScope moduleScope, KobuFileSystem fileSystem, Database database,
                          InputReader inputReader) {
        this.provider = provider;
        this.analyzerContext = analyzerContext;
        this.evalMode = evalMode;
        this.moduleScope = moduleScope;
        this.fileSystem = fileSystem;
        this.database = database;
        this.inputReader = inputReader;
        loadProperties();
        pushNewScope();
    }

    public EvalContextProvider getProvider() {
        return provider;
    }

    public EvalContext newEvalContext() {
        return getProvider().newEvalContext(this);
    }

    public EvalContext newEvalContext(RuleContext ruleContext) {
        return getProvider().newEvalContext(this, ruleContext);
    }

    public EvalModeEnum getEvalMode() {
        return evalMode;
    }

    public LocalScope getCurrentScope() {
        return currentScope;
    }

    public Database getDatabase() {
        return database;
    }

    public KobuFileSystem getFileSystem() {
        return fileSystem;
    }

    public InputReader getInputParser() {
        return inputReader;
    }

    public LocalScope pushNewScope() {
        this.currentScope = new LocalScope(moduleScope, currentScope);
        return this.currentScope;
    }

    public LocalScope popScope() {
        if (this.currentScope.getEnclosingScope() != moduleScope) {
            this.currentScope = (LocalScope) this.currentScope.getEnclosingScope();
        }
        return this.currentScope;
    }

    public Branch pushNewBranch() {
        this.currentBranch = new Branch(this.currentBranch);
        return this.currentBranch;
    }

    public Branch getCurrentBranch() {
        return this.currentBranch;
    }

    public Branch popBranch() {
        if (this.currentBranch.getParent() != null) {
            var interrupt = this.currentBranch.getInterrupt();
            this.currentBranch = this.currentBranch.getParent();
            this.currentBranch.setInterrupt(interrupt);
            this.currentBranch.updateReturnStatement();
        }
        return this.currentBranch;
    }

    public ValueExpr evalFunction(KobuFunction function, List<ValueExpr> args, SourceCodeRef sourceCodeRef) {
        if (function instanceof UserDefinedFunction) {
            UserDefinedFunction userFunction = (UserDefinedFunction) function;
            return userFunction.eval(analyzerContext, provider, args);
        } else if (function instanceof BuiltinFunctionSymbol) {
            BuiltinFunctionSymbol builtinFunctionSymbol = (BuiltinFunctionSymbol) function;
            return builtinFunctionSymbol.getFunctionImpl().run(this, args, sourceCodeRef);
        } else if (function instanceof NativeFunctionSymbol) {
            NativeFunctionSymbol nativeFunctionSymbol = (NativeFunctionSymbol) function;
            return nativeFunctionSymbol.getFunctionImpl().run(this, args, sourceCodeRef);
        }
        throw new IllegalArgumentException("Unrecognized function type: " + function.getClass().getName());
    }

    public ValueExpr evalMethod(ValueExpr object, KobuFunction function, List<ValueExpr> args, SourceCodeRef sourceCodeRef) {
        if (function instanceof BuiltinFunctionSymbol) {
            BuiltinFunctionSymbol builtinFunctionSymbol = (BuiltinFunctionSymbol) function;
            return builtinFunctionSymbol.getFunctionImpl().run(this, object, args, sourceCodeRef);
        }
        throw new IllegalArgumentException("Unrecognized method type: " + function.getClass().getName());
    }

    public void analyzeBlock(List<Evaluable> block) {
        if (block == null) {
            return;
        }
        pushNewScope();
        var branch = pushNewBranch();

        for (Evaluable evaluable : block) {
            if (branch.hasTerminalStatement()) {
                branch.setHasUnreachableCode(true);
                analyzerContext.getErrorScope().addError(new UnreachableCodeError(evaluable.getSourceCodeRef()));
                break;
            }
            evaluable.analyze(this);
        }

        popBranch();
        popScope();
    }

    public InterruptTypeEnum evalBlock(List<Evaluable> block) {
        if (block == null) {
            return null;
        }
        pushNewScope();
        var branch = pushNewBranch();

        InterruptTypeEnum interrupt = null;

        try {
            for (Evaluable evaluable : block) {
                if (branch.hasTerminalStatement()) {
                    break;
                }
                if (getCurrentBranch().getInterrupt() != null) {
                    interrupt = getCurrentBranch().getInterrupt();
                    break;
                }
                if (evaluable instanceof Statement) {
                    ((Statement) evaluable).evalStat(this);
                } else if (evaluable instanceof Expr) {
                    ((Expr) evaluable).evalExpr(this);
                }
            }
        } finally {
            popBranch();
            popScope();
        }

        return interrupt;
    }

    public Type getReturnType() {
        return returnType;
    }

    public void setReturnType(Type returnType) {
        this.returnType = returnType;
    }

    public boolean voidReturnType() {
        return voidReturnType;
    }

    public void setVoidReturnType() {
        this.voidReturnType = true;
    }

    public ValueExpr getReturnValue() {
        return returnValue;
    }

    public void setReturnValue(ValueExpr returnValue) {
        this.returnValue = returnValue;
    }

    public ErrorValue getErrorValue() {
        return errorValue;
    }

    public void setErrorValue(ErrorValue errorValue) {
        this.errorValue = errorValue;
    }

    public ModuleScope getModuleScope() {
        return moduleScope;
    }

    public UserDefinedFunction getFunction() {
        return function;
    }

    public RuleContext getRuleContext() {
        return ruleContext;
    }

    public String getProperty(String name) {
        return properties.get(name);
    }

    public AnalyzerContext getAnalyzerContext() {
        return analyzerContext;
    }

    public void addAnalyzerError(AnalyzerError error) {
        analyzerContext.getErrorScope().addError(error);
    }

    public UserDefinedError getLastUserError() {
        return lastUserError;
    }

    public void setLastUserError(UserDefinedError lastUserError) {
        this.lastUserError = lastUserError;
    }

    public int getNewGlobalDefinitionOffset() {
        if (evalMode == EvalModeEnum.EXECUTION) {
            return 0;
        }
        if (getFunction() != null) {
            return getFunction().getCloseBlockSourceRef().getStartOffset() + 1;
        } else if (getRuleContext() != null) {
            return getRuleContext().getRuleSymbol().getCloseRuleRef().getStartOffset() + 1;
        }

        throw new UnsupportedOperationException("getNewGlobalDefinitionOffset() must be invoked from a function or rule context");
    }

    private void loadProperties() {
        if (moduleScope.getProperties() != null) {
            for (ProjectProperty property : moduleScope.getProperties()) {
                properties.put(property.getName(), property.getValue());
            }
        }
    }
}
