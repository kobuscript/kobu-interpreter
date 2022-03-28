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
import dev.kobu.interpreter.ast.eval.expr.value.AnonymousFunctionValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.FunctionRefValueExpr;
import dev.kobu.interpreter.ast.symbol.*;
import dev.kobu.interpreter.ast.symbol.function.FunctionType;
import dev.kobu.interpreter.ast.symbol.generics.HasTypeParameters;
import dev.kobu.interpreter.ast.symbol.generics.TypeAlias;
import dev.kobu.interpreter.ast.symbol.generics.TypeArgs;
import dev.kobu.interpreter.error.analyzer.*;
import dev.kobu.interpreter.error.eval.InternalInterpreterError;

import java.util.*;
import java.util.stream.Collectors;

public class FunctionCallExpr implements Expr, UndefinedSymbolListener {

    private final ModuleScope moduleScope;

    private final SourceCodeRef sourceCodeRef;

    private final Expr functionRefExpr;

    private final List<FunctionArgExpr> args;

    private Type type;

    private Map<String, Type> resolvedTypeArgs;

    private TypeArgs typeArgs;

    public FunctionCallExpr(ModuleScope moduleScope, SourceCodeRef sourceCodeRef,
                            Expr functionRefExpr, List<FunctionArgExpr> args) {
        this.moduleScope = moduleScope;
        this.sourceCodeRef = sourceCodeRef;
        this.functionRefExpr = functionRefExpr;
        this.args = args;
    }

    @Override
    public void analyze(EvalContext context) {
        if (functionRefExpr instanceof UndefinedSymbolNotifier) {
            ((UndefinedSymbolNotifier) functionRefExpr).registerUndefinedSymbolListener(this);
        }
        if (functionRefExpr instanceof MemoryReference) {
            ((MemoryReference) functionRefExpr).setFunctionRefMode();
        }
        functionRefExpr.analyze(context);
        if (functionRefExpr.getType() instanceof UnknownType) {
            this.type = UnknownType.INSTANCE;
            return;
        }
        if (!(functionRefExpr.getType() instanceof FunctionType)) {
            context.addAnalyzerError(new InvalidFunctionRefError(functionRefExpr.getSourceCodeRef(), functionRefExpr.getType()));
            this.type = UnknownType.INSTANCE;
            return;
        }

        if (typeArgs != null && !typeArgs.getTypes().isEmpty()) {
            if (functionRefExpr instanceof HasTypeParameters) {
                var typeParameters = ((HasTypeParameters) functionRefExpr).getTypeParameters();
                if (typeArgs.getTypes().size() != typeParameters.size()) {
                    context.addAnalyzerError(new InvalidTypeArgsError(typeArgs.getSourceCodeRef(),
                            typeParameters.size(), typeArgs.getTypes().size()));
                }
            } else {
                context.addAnalyzerError(new InvalidTypeArgsError(typeArgs.getSourceCodeRef(),
                        0, typeArgs.getTypes().size()));
            }
        }

        if (resolvedTypeArgs == null) {
            resolvedTypeArgs = new HashMap<>();
        }
        if (context.getEvalMode() == EvalModeEnum.ANALYZER_SERVICE && functionRefExpr instanceof FunctionRefValueExpr) {
            var function = ((FunctionRefValueExpr) functionRefExpr).getFunction();
            resolvedTypeArgs.putAll(function.providedTypeArguments());
            if (function instanceof BuiltinFunctionSymbol) {
                moduleScope.registerDocumentationSource(sourceCodeRef.getStartOffset(), (Symbol) function);
            }
        }

        this.type = analyzeCall(context, (FunctionType) functionRefExpr.getType());
    }

    public void setTypeArgs(TypeArgs typeArgs) {
        this.typeArgs = typeArgs;
    }

    private Type analyzeCall(EvalContext context, FunctionType functionType) {

        for (int i = 0; i < functionType.getParameters().size(); i++) {
            var parameter = functionType.getParameters().get(i);
            if (i >= args.size()) {
                if (!parameter.isOptional()) {
                    context.addAnalyzerError(new InvalidFunctionCallError(sourceCodeRef, functionType, args));
                    return UnknownType.INSTANCE;
                }
                break;
            }
            var arg = args.get(i);
            Type paramType = parameter.getType();
            Collection<TypeAlias> aliases = paramType.aliases();
            if (!aliases.isEmpty()) {
                paramType = paramType.constructFor(resolvedTypeArgs);
                arg.setResolvedTypes(resolvedTypeArgs);
                arg.setTargetType(paramType);
                arg.analyze(context);
                if (!paramType.aliases().isEmpty()) {
                    paramType = arg.getType();
                }
            } else {
                arg.setResolvedTypes(resolvedTypeArgs);
                arg.setTargetType(parameter.getType());
                arg.analyze(context);
            }

            if (arg.getType() instanceof UnknownType) {
                return UnknownType.INSTANCE;
            }
            if (arg.getType() != null && !paramType.isAssignableFrom(arg.getType())) {
                context.addAnalyzerError(new InvalidTypeError(arg.getSourceCodeRef(),
                        paramType, arg.getType()));
                return UnknownType.INSTANCE;
            }
        }
        if (args.size() > functionType.getParameters().size()) {
            context.addAnalyzerError(new InvalidFunctionCallError(sourceCodeRef, functionType, args));
            return UnknownType.INSTANCE;
        }

        Type returnType = functionType.getReturnType();
        if (returnType != null) {
            returnType = returnType.constructFor(resolvedTypeArgs);
        }

        return returnType;
    }

    @Override
    public ValueExpr evalExpr(EvalContext context) {

        var functionValueExpr = functionRefExpr.evalExpr(context);

        var argValList = args.stream()
                .map(arg -> arg.evalExpr(context)).collect(Collectors.toList());
        if (functionValueExpr instanceof FunctionRefValueExpr) {
            FunctionRefValueExpr functionRef = (FunctionRefValueExpr) functionValueExpr;
            if (functionRef.getValueScope() != null) {
                return context.evalMethod(functionRef.getValueScope(), functionRef.getFunction(), argValList, sourceCodeRef);
            } else {
                return context.evalFunction(functionRef.getFunction(), argValList, sourceCodeRef);
            }
        } else if (functionValueExpr instanceof AnonymousFunctionValueExpr) {
            AnonymousFunctionValueExpr anonymousFunction = (AnonymousFunctionValueExpr) functionValueExpr;
            return context.evalFunction(anonymousFunction, argValList, sourceCodeRef);
        }

        throw new InternalInterpreterError("Unrecognized function type: " + functionValueExpr.getClass(),
                functionRefExpr.getSourceCodeRef());

    }

    @Override
    public SourceCodeRef getSourceCodeRef() {
        return sourceCodeRef;
    }

    @Override
    public void setResolvedTypes(Map<String, Type> resolvedTypes) {
        this.resolvedTypeArgs = resolvedTypes;
    }

    @Override
    public Type getType() {
        return type;
    }

    public List<FunctionArgExpr> getArgs() {
        return args;
    }

    public Expr getFunctionRefExpr() {
        return functionRefExpr;
    }

    @Override
    public void onUndefinedSymbol(EvalContext context, String symbolName) {
        context.addAnalyzerError(new UndefinedFunctionName(this, null, symbolName,
                context.getNewGlobalDefinitionOffset()));
    }

    @Override
    public void onUndefinedSymbol(EvalContext context, Type typeScope, String symbolName) {
        if (typeScope instanceof ModuleRefSymbol) {
            var moduleId = ((ModuleRefSymbol)typeScope).getModuleScopeRef().getModuleId();
            context.addAnalyzerError(new UndefinedFunctionName(this, moduleId, symbolName,
                    context.getNewGlobalDefinitionOffset()));
        } else {
            context.addAnalyzerError(new UndefinedMethodError(sourceCodeRef, typeScope, symbolName));
        }
    }

}
