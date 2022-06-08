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

import dev.kobu.interpreter.ast.eval.context.EvalContext;
import dev.kobu.interpreter.ast.eval.Expr;
import dev.kobu.interpreter.ast.eval.ValueExpr;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.ast.symbol.Type;
import dev.kobu.interpreter.ast.symbol.UnknownType;
import dev.kobu.interpreter.error.analyzer.CastTypeError;
import dev.kobu.interpreter.error.eval.RuntimeCastTypeError;

import java.util.Map;

public class CastExpr implements Expr {

    private final SourceCodeRef sourceCodeRef;

    private final Type targetType;

    private final Expr expr;

    private Type type;

    public CastExpr(SourceCodeRef sourceCodeRef, Type targetType, Expr expr) {
        this.sourceCodeRef = sourceCodeRef;
        this.targetType = targetType;
        this.expr = expr;
    }


    @Override
    public SourceCodeRef getSourceCodeRef() {
        return sourceCodeRef;
    }

    @Override
    public void setResolvedTypes(Map<String, Type> resolvedTypes) {
        if (expr != null) {
            expr.setResolvedTypes(resolvedTypes);
        }
    }

    @Override
    public void analyze(EvalContext context) {
        if (targetType != null && expr != null) {
            expr.analyze(context);
            if (expr.getType() instanceof UnknownType || targetType instanceof UnknownType) {
                this.type = UnknownType.INSTANCE;
                return;
            }
            if (!targetType.isAssignableFrom(expr.getType()) & !expr.getType().isAssignableFrom(targetType)) {
                context.getAnalyzerContext().getErrorScope().addError(new CastTypeError(sourceCodeRef,
                        targetType, expr.getType()));
                this.type = UnknownType.INSTANCE;
            } else {
                this.type = targetType;
            }
        } else {
            this.type = UnknownType.INSTANCE;
        }
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public ValueExpr evalExpr(EvalContext context) {
        ValueExpr value = expr.evalExpr(context);

        if (!targetType.isAssignableFrom(value.getType())) {
            throw new RuntimeCastTypeError(sourceCodeRef, targetType, value.getType());
        }

        return value;
    }
}
