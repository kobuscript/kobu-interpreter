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

package dev.cgrscript.interpreter.ast.symbol;

import dev.cgrscript.interpreter.ast.AnalyzerContext;
import dev.cgrscript.interpreter.ast.eval.*;
import dev.cgrscript.interpreter.ast.eval.context.EvalContextProvider;
import dev.cgrscript.interpreter.ast.eval.context.EvalModeEnum;
import dev.cgrscript.interpreter.error.analyzer.FunctionMissingReturnStatError;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FunctionSymbol extends Symbol implements FunctionType, HasExpr {

    private final ModuleScope moduleScope;

    private final SourceCodeRef closeFunctionRef;

    private final String docText;

    private List<FunctionParameter> parameters;

    private Type returnType;

    private List<Evaluable> exprList;

    private Collection<SymbolDescriptor> symbolsModule;

    private SymbolDocumentation documentation;

    public FunctionSymbol(SourceCodeRef sourceCodeRef, SourceCodeRef closeFunctionRef, ModuleScope moduleScope,
                          String name, String docText) {
        super(moduleScope, sourceCodeRef, name);
        this.closeFunctionRef = closeFunctionRef;
        this.moduleScope = moduleScope;
        this.docText = docText;

        if (closeFunctionRef != null && moduleScope.getEvalMode() == EvalModeEnum.ANALYZER_SERVICE) {
            moduleScope.registerAutoCompletionSource(closeFunctionRef.getStartOffset(), new AutoCompletionSource() {
                @Override
                public List<SymbolDescriptor> requestSuggestions(List<ModuleScope> externalModules) {
                    return new ArrayList<>(symbolsModule);
                }

                @Override
                public boolean hasOwnCompletionScope() {
                    return false;
                }
            });
        }
    }

    @Override
    public Type getReturnType() {
        return returnType;
    }

    public List<FunctionParameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<FunctionParameter> parameters) {
        this.parameters = parameters;
    }

    public void setReturnType(Type returnType) {
        this.returnType = returnType;
    }

    public List<Evaluable> getExprList() {
        return exprList;
    }

    public void setExprList(List<Evaluable> exprList) {
        this.exprList = exprList;
    }

    public SourceCodeRef getCloseFunctionRef() {
        return closeFunctionRef;
    }

    @Override
    public void analyze(AnalyzerContext analyzerContext, EvalContextProvider evalContextProvider) {
        var context = evalContextProvider.newEvalContext(analyzerContext, moduleScope, this);
        var scope = context.getCurrentScope();

        symbolsModule = scope.getSymbolDescriptors(
                        SymbolTypeEnum.FUNCTION,
                        SymbolTypeEnum.MODULE_REF,
                        SymbolTypeEnum.RULE,
                        SymbolTypeEnum.TEMPLATE,
                        SymbolTypeEnum.FILE,
                        SymbolTypeEnum.KEYWORD);

        for (FunctionParameter parameter : parameters) {
            VariableSymbol variableSymbol = new VariableSymbol(moduleScope, parameter.getSourceCodeRef(), parameter.getName(),
                    parameter.getType());
            scope.define(analyzerContext, variableSymbol);
        }
        var branch = context.pushNewBranch();
        context.analyzeBlock(exprList);

        if (closeFunctionRef != null && returnType != null && !branch.hasReturnStatement()) {
            analyzerContext.getErrorScope().addError(new FunctionMissingReturnStatError(closeFunctionRef));
        }

        if (context.getEvalMode() == EvalModeEnum.ANALYZER_SERVICE) {
            String description = getName() + getDescription();
            documentation = new SymbolDocumentation(moduleScope.getModuleId(), SymbolTypeEnum.FUNCTION, description, docText);
        }

        context.popBranch();
    }

    public ValueExpr eval(AnalyzerContext analyzerContext, EvalContextProvider evalContextProvider, List<ValueExpr> args) {
        var context = evalContextProvider.newEvalContext(analyzerContext, moduleScope, this);
        var scope = context.getCurrentScope();
        for (int i = 0; i < parameters.size(); i++) {
            FunctionParameter parameter = parameters.get(i);
            ValueExpr arg = i < args.size() ? args.get(i) : null;

            VariableSymbol variableSymbol = new VariableSymbol(moduleScope, parameter.getSourceCodeRef(), parameter.getName(),
                    parameter.getType());
            scope.define(analyzerContext, variableSymbol);
            if (arg != null) {
                scope.setValue(parameter.getName(), arg);
            }
        }
        context.evalBlock(exprList);
        return context.getReturnValue();
    }

    @Override
    public SymbolDocumentation getDocumentation() {
        return documentation;
    }
}
