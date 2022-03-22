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

package dev.kobu.interpreter.ast.eval.expr.value;

import dev.kobu.interpreter.ast.AnalyzerContext;
import dev.kobu.interpreter.ast.eval.Evaluable;
import dev.kobu.interpreter.ast.eval.Expr;
import dev.kobu.interpreter.ast.eval.HasTargetType;
import dev.kobu.interpreter.ast.eval.ValueExpr;
import dev.kobu.interpreter.ast.eval.context.EvalContext;
import dev.kobu.interpreter.ast.eval.context.EvalContextProvider;
import dev.kobu.interpreter.ast.symbol.*;
import dev.kobu.interpreter.error.analyzer.DuplicatedFunctionParamError;
import dev.kobu.interpreter.error.analyzer.FunctionMissingReturnStatError;
import dev.kobu.interpreter.error.analyzer.InvalidRequiredFunctionParamError;
import dev.kobu.interpreter.error.eval.InternalInterpreterError;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AnonymousFunctionDefinitionExpr implements Expr, HasTargetType, UserDefinedFunction {

    private final SourceCodeRef sourceCodeRef;

    private final SourceCodeRef closeBlockSourceCodeRef;

    private final ModuleScope moduleScope;

    private final List<FunctionParameter> parameters;

    private final List<Evaluable> block;

    private Type returnType;

    private FunctionType type;

    private Type targetType;

    public AnonymousFunctionDefinitionExpr(SourceCodeRef sourceCodeRef, SourceCodeRef closeBlockSourceCodeRef,
                                           ModuleScope moduleScope, List<FunctionParameter> parameters,
                                           List<Evaluable> block, Type returnType) {
        this.sourceCodeRef = sourceCodeRef;
        this.closeBlockSourceCodeRef = closeBlockSourceCodeRef;
        this.moduleScope = moduleScope;
        this.parameters = parameters;
        this.block = block;
        this.returnType = returnType;
    }

    @Override
    public SourceCodeRef getSourceCodeRef() {
        return sourceCodeRef;
    }

    @Override
    public List<FunctionParameter> getParameters() {
        return parameters;
    }

    @Override
    public void analyze(EvalContext context) {
        var scope = context.getCurrentScope();
        var fnCtx = context.getProvider().newEvalContext(context.getAnalyzerContext(),
                moduleScope, this);
        fnCtx.getCurrentScope().addAll(scope);

        if (targetType instanceof FunctionType) {
            FunctionType targetFunctionType = (FunctionType) targetType;
            for (int i = 0; i < targetFunctionType.getParameters().size() && i < parameters.size(); i++) {
                FunctionParameter param = parameters.get(i);
                if (param.getType() == null) {
                    param.setType(targetFunctionType.getParameters().get(i).getType());
                }
            }
            returnType = targetFunctionType.getReturnType();
        }
        for (FunctionParameter parameter : parameters) {
            if (parameter.getType() == null) {
                parameter.setType(BuiltinScope.ANY_TYPE);
            }
        }

        Map<String, FunctionParameter> paramsMap = new HashMap<>();
        FunctionParameter lastOptionalParam = null;
        for (FunctionParameter parameter : parameters) {
            FunctionParameter currentParam = paramsMap.get(parameter.getName());
            if (currentParam != null) {
                context.getAnalyzerContext().getErrorScope().addError(new DuplicatedFunctionParamError(currentParam, parameter));
                continue;
            }
            paramsMap.put(parameter.getName(), parameter);

            if (parameter.isOptional()) {
                lastOptionalParam = parameter;
            } else {
                if (lastOptionalParam != null) {
                    context.getAnalyzerContext().getErrorScope().addError(new InvalidRequiredFunctionParamError(parameter));
                }
            }

            VariableSymbol variableSymbol = new VariableSymbol(moduleScope,
                    parameter.getSourceCodeRef(), parameter.getName(), parameter.getType());
            fnCtx.getCurrentScope().define(fnCtx.getAnalyzerContext(), variableSymbol);
        }

        var branch = fnCtx.pushNewBranch();
        fnCtx.analyzeBlock(block);

        if (inferReturnType()) {
            returnType = fnCtx.getReturnType();
        }

        type = new FunctionType(
                parameters.stream().map(FunctionParameter::toFunctionTypeParameter).collect(Collectors.toList()),
                returnType);

        if (closeBlockSourceCodeRef != null && returnType != null && !branch.hasReturnStatement()) {
            fnCtx.getAnalyzerContext().getErrorScope().addError(new FunctionMissingReturnStatError(closeBlockSourceCodeRef));
        }

    }

    @Override
    public ValueExpr eval(AnalyzerContext analyzerContext, EvalContextProvider evalContextProvider, List<ValueExpr> args) {
        throw new InternalInterpreterError("Can't directly evaluate an anonymous function definition. " +
                "Use evalExpr(EvalContext) to capture an enclosing scope.", sourceCodeRef);
    }

    @Override
    public ValueExpr evalExpr(EvalContext context) {
        return new AnonymousFunctionValueExpr(this, moduleScope, context.getCurrentScope());
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public Type getTargetType() {
        return targetType;
    }

    @Override
    public void setTargetType(Type targetType) {
        this.targetType = targetType;
    }

    @Override
    public Type getReturnType() {
        return returnType;
    }

    @Override
    public SourceCodeRef getCloseBlockSourceRef() {
        return closeBlockSourceCodeRef;
    }

    @Override
    public List<Evaluable> getBlock() {
        return block;
    }

    @Override
    public boolean inferReturnType() {
        return targetType == null;
    }

}
