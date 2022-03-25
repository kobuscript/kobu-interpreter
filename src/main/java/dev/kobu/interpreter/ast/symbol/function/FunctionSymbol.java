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

package dev.kobu.interpreter.ast.symbol.function;

import dev.kobu.interpreter.ast.AnalyzerContext;
import dev.kobu.interpreter.ast.eval.*;
import dev.kobu.interpreter.ast.eval.context.EvalContextProvider;
import dev.kobu.interpreter.ast.eval.context.EvalModeEnum;
import dev.kobu.interpreter.ast.symbol.*;
import dev.kobu.interpreter.ast.symbol.generics.TypeParameter;
import dev.kobu.interpreter.error.analyzer.DuplicatedFunctionParamError;
import dev.kobu.interpreter.error.analyzer.FunctionMissingReturnStatError;
import dev.kobu.interpreter.error.analyzer.InvalidRequiredFunctionParamError;

import java.util.*;
import java.util.stream.Collectors;

public class FunctionSymbol extends Symbol implements NamedFunction, UserDefinedFunction, HasExpr {

    private final ModuleScope moduleScope;

    private final SourceCodeRef closeBlockSourceRef;

    private final String docText;

    private List<FunctionParameter> parameters;

    private Type returnType;

    private List<Evaluable> block;

    private Collection<SymbolDescriptor> symbolsModule;

    private SymbolDocumentation documentation;

    private FunctionType type;

    private List<TypeParameter> typeParameters;

    public FunctionSymbol(SourceCodeRef sourceCodeRef, SourceCodeRef closeBlockSourceRef, ModuleScope moduleScope,
                          String name, String docText) {
        super(moduleScope, sourceCodeRef, name);
        this.closeBlockSourceRef = closeBlockSourceRef;
        this.moduleScope = moduleScope;
        this.docText = docText;

        if (closeBlockSourceRef != null && moduleScope.getEvalMode() == EvalModeEnum.ANALYZER_SERVICE) {
            moduleScope.registerAutoCompletionSource(closeBlockSourceRef.getStartOffset(), new AutoCompletionSource() {
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

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public Map<String, Type> providedTypeArguments() {
        return new HashMap<>();
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

    @Override
    public List<Evaluable> getBlock() {
        return block;
    }

    @Override
    public boolean inferReturnType() {
        return false;
    }

    public void setBlock(List<Evaluable> block) {
        this.block = block;
    }

    @Override
    public SourceCodeRef getCloseBlockSourceRef() {
        return closeBlockSourceRef;
    }

    public List<TypeParameter> getTypeParameters() {
        return typeParameters;
    }

    public void setTypeParameters(List<TypeParameter> typeParameters) {
        this.typeParameters = typeParameters;
    }

    public void buildType() {
        this.type = new FunctionType(
                parameters.stream().map(FunctionParameter::toFunctionTypeParameter).collect(Collectors.toList()),
                returnType);
    }

    @Override
    public void analyze(AnalyzerContext analyzerContext, EvalContextProvider evalContextProvider) {
        var context = evalContextProvider.newEvalContext(analyzerContext, moduleScope, this);
        var scope = context.getCurrentScope();

        if (moduleScope.getEvalMode() == EvalModeEnum.ANALYZER_SERVICE) {
            symbolsModule = scope.getSymbolDescriptors(
                    SymbolTypeEnum.FUNCTION,
                    SymbolTypeEnum.MODULE_REF,
                    SymbolTypeEnum.RULE,
                    SymbolTypeEnum.TEMPLATE,
                    SymbolTypeEnum.FILE,
                    SymbolTypeEnum.KEYWORD);
        }

        Map<String, FunctionParameter> paramsMap = new HashMap<>();
        FunctionParameter lastOptionalParam = null;
        for (FunctionParameter parameter : parameters) {
            FunctionParameter currentParam = paramsMap.get(parameter.getName());
            if (currentParam != null) {
                analyzerContext.getErrorScope().addError(new DuplicatedFunctionParamError(currentParam, parameter));
                continue;
            }
            paramsMap.put(parameter.getName(), parameter);

            if (parameter.isOptional()) {
                lastOptionalParam = parameter;
            } else {
                if (lastOptionalParam != null) {
                    analyzerContext.getErrorScope().addError(new InvalidRequiredFunctionParamError(parameter));
                }
            }

            VariableSymbol variableSymbol = new VariableSymbol(moduleScope, parameter.getSourceCodeRef(), parameter.getName(),
                    parameter.getType());
            scope.define(analyzerContext, variableSymbol);
        }
        var branch = context.pushNewBranch();
        context.analyzeBlock(block);

        if (closeBlockSourceRef != null && returnType != null && !branch.hasReturnStatement()) {
            analyzerContext.getErrorScope().addError(new FunctionMissingReturnStatError(closeBlockSourceRef));
        }

        if (context.getEvalMode() == EvalModeEnum.ANALYZER_SERVICE) {
            String description = getName() + getDescription();
            documentation = new SymbolDocumentation(moduleScope.getModuleId(), SymbolTypeEnum.FUNCTION, description, docText);
        }

        context.popBranch();
    }

    @Override
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
        context.evalBlock(block);
        return context.getReturnValue();
    }

    @Override
    public SymbolDocumentation getDocumentation() {
        return documentation;
    }
}
