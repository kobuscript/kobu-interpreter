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

package dev.kobu.interpreter.ast.eval.expr;

import dev.kobu.interpreter.ast.eval.*;
import dev.kobu.interpreter.ast.eval.context.EvalContext;
import dev.kobu.interpreter.ast.eval.context.EvalModeEnum;
import dev.kobu.interpreter.ast.eval.expr.value.ModuleRefValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.NullValueExpr;
import dev.kobu.interpreter.ast.symbol.*;
import dev.kobu.interpreter.error.analyzer.InvalidFunctionCallError;
import dev.kobu.interpreter.error.analyzer.InvalidTypeError;
import dev.kobu.interpreter.error.analyzer.UndefinedFunctionName;
import dev.kobu.interpreter.error.analyzer.UndefinedMethodError;
import dev.kobu.interpreter.error.eval.NullPointerError;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class FunctionCallExpr implements Expr, HasTypeScope, HasElementRef {

    private final ModuleScope moduleScope;

    private final SourceCodeRef sourceCodeRef;

    private final String functionName;

    private final List<FunctionArgExpr> args;

    private FunctionDefinition functionDefinition;

    private Type typeScope;

    private ValueExpr valueScope;

    private Type type;

    private Collection<SymbolDescriptor> symbolsInScope;

    public FunctionCallExpr(ModuleScope moduleScope, SourceCodeRef sourceCodeRef,
                            String functionName, List<FunctionArgExpr> args) {
        this.moduleScope = moduleScope;
        this.sourceCodeRef = sourceCodeRef;
        this.functionName = functionName;
        this.args = args;

        if (moduleScope.getEvalMode() == EvalModeEnum.ANALYZER_SERVICE) {
            moduleScope.registerRef(sourceCodeRef.getStartOffset(), this);
            moduleScope.registerAutoCompletionSource(sourceCodeRef.getStartOffset(), this);
        }
    }

    @Override
    public void analyze(EvalContext context) {

        if (typeScope == null) {
            if (context.getEvalMode() != EvalModeEnum.EXECUTION) {
                this.symbolsInScope = context.getCurrentScope()
                        .getSymbolDescriptors(
                                SymbolTypeEnum.VARIABLE,
                                SymbolTypeEnum.FUNCTION,
                                SymbolTypeEnum.RULE,
                                SymbolTypeEnum.TEMPLATE,
                                SymbolTypeEnum.FILE,
                                SymbolTypeEnum.KEYWORD,
                                SymbolTypeEnum.TYPE);
            }

            var functionSymbol = context.getModuleScope().resolve(functionName);
            if (!(functionSymbol instanceof FunctionDefinition)) {
                context.addAnalyzerError(new UndefinedFunctionName(this, null, functionName,
                        context.getNewGlobalDefinitionOffset()));
                this.type = UnknownType.INSTANCE;
                return;
            }
            var functionType = (FunctionDefinition)functionSymbol;
            this.type = analyzeCall(context, functionType);
            this.functionDefinition = functionType;

        } else {
            if (context.getEvalMode() != EvalModeEnum.EXECUTION) {
                List<SymbolDescriptor> symbols = new ArrayList<>();
                for (FieldDescriptor field : typeScope.getFields()) {
                    symbols.add(new SymbolDescriptor(field));
                }
                for (FunctionDefinition method : typeScope.getMethods()) {
                    symbols.add(new SymbolDescriptor(method));
                }
                this.symbolsInScope = symbols;
            }

            var methodType = typeScope.resolveMethod(functionName);
            if (methodType == null) {
                if (typeScope instanceof ModuleRefSymbol) {
                    var moduleId = ((ModuleRefSymbol)typeScope).getModuleScopeRef().getModuleId();
                    context.addAnalyzerError(new UndefinedFunctionName(this, moduleId, functionName,
                            context.getNewGlobalDefinitionOffset()));
                } else {
                    context.addAnalyzerError(new UndefinedMethodError(sourceCodeRef, typeScope, functionName));
                }
                this.type = UnknownType.INSTANCE;
                return;
            }
            this.type = analyzeCall(context, methodType);
            this.functionDefinition = methodType;

        }

        if (context.getEvalMode() == EvalModeEnum.ANALYZER_SERVICE && functionDefinition instanceof BuiltinFunctionSymbol) {
            moduleScope.registerDocumentationSource(sourceCodeRef.getStartOffset(), (Symbol) functionDefinition);
        }
    }

    public FunctionDefinition getFunctionType() {
        return functionDefinition;
    }

    private Type analyzeCall(EvalContext context, FunctionDefinition functionDefinition) {
        for (int i = 0; i < functionDefinition.getParameters().size(); i++) {
            var parameter = functionDefinition.getParameters().get(i);
            if (i >= args.size()) {
                if (!parameter.isOptional()) {
                    context.addAnalyzerError(new InvalidFunctionCallError(sourceCodeRef, functionDefinition, args));
                    return UnknownType.INSTANCE;
                }
                break;
            }
            var arg = args.get(i);
            arg.setTargetType(parameter.getType());
            arg.analyze(context);
            if (arg.getType() instanceof UnknownType) {
                return UnknownType.INSTANCE;
            }
            if (arg.getType() != null && !parameter.getType().isAssignableFrom(arg.getType())) {
                context.addAnalyzerError(new InvalidTypeError(arg.getSourceCodeRef(),
                        parameter.getType(), arg.getType()));
                return UnknownType.INSTANCE;
            }
        }
        if (args.size() > functionDefinition.getParameters().size()) {
            context.addAnalyzerError(new InvalidFunctionCallError(sourceCodeRef, functionDefinition, args));
            return UnknownType.INSTANCE;
        }
        return functionDefinition.getReturnType();
    }

    @Override
    public ValueExpr evalExpr(EvalContext context) {
        if (valueScope == null) {
            return context.evalFunction(functionDefinition, args.stream()
                    .map(arg -> arg.evalExpr(context)).collect(Collectors.toList()), sourceCodeRef);
        } else {
            if (valueScope instanceof NullValueExpr) {
                throw new NullPointerError(valueScope.getSourceCodeRef(), valueScope.getSourceCodeRef());
            }
            if (valueScope instanceof ModuleRefValueExpr) {
                return context.evalFunction(functionDefinition, args.stream()
                        .map(arg -> arg.evalExpr(context)).collect(Collectors.toList()), sourceCodeRef);
            }
            return context.evalMethod(valueScope, functionDefinition, args.stream()
                    .map(arg -> arg.evalExpr(context)).collect(Collectors.toList()), sourceCodeRef);
        }
    }

    @Override
    public SourceCodeRef getSourceCodeRef() {
        return sourceCodeRef;
    }

    @Override
    public Type getType() {
        return type;
    }

    public String getFunctionName() {
        return functionName;
    }

    public List<FunctionArgExpr> getArgs() {
        return args;
    }

    @Override
    public void setTypeScope(Type typeScope) {
        this.typeScope = typeScope;
    }

    @Override
    public void setValueScope(ValueExpr valueScope) {
        this.valueScope = valueScope;
    }

    @Override
    public SourceCodeRef getElementRef() {
        return functionDefinition != null ? functionDefinition.getSourceCodeRef() : null;
    }

    @Override
    public List<SymbolDescriptor> requestSuggestions(List<ModuleScope> externalModules) {
        var symbols = new ArrayList<>(symbolsInScope);
        symbols.addAll(getExternalSymbols(moduleScope, externalModules,
                SymbolTypeEnum.FUNCTION, SymbolTypeEnum.RULE,
                SymbolTypeEnum.TEMPLATE, SymbolTypeEnum.FILE,
                SymbolTypeEnum.TYPE));
        return symbols;
    }

    @Override
    public boolean hasOwnCompletionScope() {
        return false;
    }

}
