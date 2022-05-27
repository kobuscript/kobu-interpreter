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

package dev.kobu.interpreter.ast.symbol;

import dev.kobu.interpreter.ast.AnalyzerContext;
import dev.kobu.interpreter.ast.eval.*;
import dev.kobu.interpreter.ast.eval.context.EvalContext;
import dev.kobu.interpreter.ast.eval.context.EvalContextProvider;
import dev.kobu.interpreter.error.analyzer.ConstNotInitializedError;
import dev.kobu.interpreter.error.analyzer.InvalidAssignExprTypeError;
import dev.kobu.interpreter.error.analyzer.InvalidVariableDeclError;

import java.util.HashSet;

public class ConstantSymbol extends Symbol implements HasExpr, AnalyzerListener {

    private Type type;

    private SymbolDocumentation documentation;

    private final Expr expr;

    private ValueExpr valueExpr;

    public ConstantSymbol(ModuleScope moduleScope, String name, ValueExpr expr, Type type, boolean privateAccess) {
        super(moduleScope, null, name, privateAccess);
        this.type = type;
        this.expr = expr;
    }

    public ConstantSymbol(ModuleScope moduleScope, SourceCodeRef sourceCodeRef, String name, Expr expr,
                          Type type, boolean privateAccess) {
        super(moduleScope, sourceCodeRef, name, privateAccess);
        this.type = type;
        this.expr = expr;
    }

    public ValueExpr getValueExpr() {
        return valueExpr;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Expr getExpr() {
        return expr;
    }

    @Override
    public SymbolDocumentation getDocumentation() {
        if (documentation == null) {
            String description = "var " + getName() + ": " + getType().getName();
            if (valueExpr != null) {
                description += " = " + valueExpr.getStringValue(new HashSet<>());
            }
            documentation = new SymbolDocumentation(getModuleScope().getModuleId(),
                    SymbolTypeEnum.VARIABLE, description);
        }
        return documentation;
    }

    @Override
    public void analyze(AnalyzerContext context, EvalContextProvider evalContextProvider) {
        if (expr == null) {
            context.getErrorScope().addError(new ConstNotInitializedError(getSourceCodeRef(), this));
            return;
        }

        var evalContext = evalContextProvider.newEvalContext(context, getModuleScope());

        if (getType() == null) {
            getExpr().analyze(evalContext);
            if (getExpr().getType() == null) {
                setType(BuiltinScope.ANY_TYPE);
                context.getErrorScope().addError(new InvalidVariableDeclError(getSourceCodeRef()));
            } else if (getExpr().getType() instanceof UnknownType) {
                setType(BuiltinScope.ANY_TYPE);
            } else {
                setType(getExpr().getType());
            }
        } else {
            if (getExpr() instanceof HasTargetType) {
                ((HasTargetType) getExpr()).setTargetType(getType());
            }
            getExpr().analyze(evalContext);
        }

        Type valueType = getExpr().getType();
        if (valueType instanceof UnknownType) {
            return;
        }
        if (!getType().isAssignableFrom(valueType)) {
            context.getErrorScope().addError(new InvalidAssignExprTypeError(getExpr().getSourceCodeRef(),
                    getType(), valueType));
        } else if (getType() instanceof ModuleRefSymbol) {
            context.getErrorScope().addError(new InvalidAssignExprTypeError(getExpr().getSourceCodeRef(),
                    getType(), valueType));
            setType(BuiltinScope.ANY_TYPE);
        }

    }

    @Override
    public void afterAnalyzer(AnalyzerContext context, EvalContextProvider evalContextProvider) {
        var evalContext = evalContextProvider.newEvalContext(context, getModuleScope());
        valueExpr = expr.evalExpr(evalContext);
    }
}
