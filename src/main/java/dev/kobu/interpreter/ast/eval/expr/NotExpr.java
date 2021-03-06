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
import dev.kobu.interpreter.ast.eval.expr.value.BooleanValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.NullValueExpr;
import dev.kobu.interpreter.ast.symbol.*;
import dev.kobu.interpreter.ast.symbol.value.BooleanTypeSymbol;
import dev.kobu.interpreter.error.analyzer.InvalidOperatorError;
import dev.kobu.interpreter.error.eval.InternalInterpreterError;

import java.util.Map;

public class NotExpr implements Expr {

    private final SourceCodeRef sourceCodeRef;

    private final Expr expr;

    private Type type;

    public NotExpr(SourceCodeRef sourceCodeRef, Expr expr) {
        this.sourceCodeRef = sourceCodeRef;
        this.expr = expr;
    }

    @Override
    public void analyze(EvalContext context) {
        expr.analyze(context);

        if (expr.getType() instanceof UnknownType) {
            type = UnknownType.INSTANCE;
            return;
        }

        var booleanType = BuiltinScope.BOOLEAN_TYPE;
        if (expr.getType() instanceof BooleanTypeSymbol) {
            type = booleanType;
        } else {
            context.addAnalyzerError(new InvalidOperatorError(sourceCodeRef, "not", expr.getType()));
            type = UnknownType.INSTANCE;
        }
    }

    @Override
    public ValueExpr evalExpr(EvalContext context) {
        ValueExpr valueExpr = expr.evalExpr(context);
        if (valueExpr instanceof NullValueExpr) {
            return BooleanValueExpr.TRUE;
        }
        if (!(valueExpr instanceof BooleanValueExpr)) {
            throw new InternalInterpreterError("Expected: boolean. Found: " + expr.getType(),
                    expr.getSourceCodeRef());
        }
        return BooleanValueExpr.fromValue(!((BooleanValueExpr)valueExpr).getValue());
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
    public Type getType() {
        return type;
    }
}
