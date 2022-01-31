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

package dev.cgrscript.interpreter.ast.eval.expr;

import dev.cgrscript.interpreter.ast.eval.EvalContext;
import dev.cgrscript.interpreter.ast.eval.Expr;
import dev.cgrscript.interpreter.ast.eval.ValueExpr;
import dev.cgrscript.interpreter.ast.eval.expr.value.NullValueExpr;
import dev.cgrscript.interpreter.ast.eval.expr.value.NumberValueExpr;
import dev.cgrscript.interpreter.ast.symbol.*;
import dev.cgrscript.interpreter.error.analyzer.InvalidOperatorError;
import dev.cgrscript.interpreter.error.eval.ArithmeticError;
import dev.cgrscript.interpreter.error.eval.InternalInterpreterError;
import dev.cgrscript.interpreter.error.eval.NullPointerError;

public class SubExpr implements Expr {

    private final SourceCodeRef sourceCodeRef;

    private final Expr leftExpr;

    private final Expr rightExpr;

    private Type type;

    public SubExpr(SourceCodeRef sourceCodeRef, Expr leftExpr, Expr rightExpr) {
        this.sourceCodeRef = sourceCodeRef;
        this.leftExpr = leftExpr;
        this.rightExpr = rightExpr;
    }

    @Override
    public void analyze(EvalContext context) {
        leftExpr.analyze(context);
        rightExpr.analyze(context);

        if (leftExpr.getType() instanceof UnknownType || rightExpr.getType() instanceof UnknownType) {
            return;
        }

        if (leftExpr.getType() instanceof NumberTypeSymbol && rightExpr.getType() instanceof NumberTypeSymbol) {
            type = BuiltinScope.NUMBER_TYPE;
        } else {
            context.getModuleScope().addError(new InvalidOperatorError(sourceCodeRef,
                    leftExpr.getType(), "-", rightExpr.getType()));
            type = UnknownType.INSTANCE;
        }
    }

    @Override
    public ValueExpr evalExpr(EvalContext context) {
        var leftValueExpr = leftExpr.evalExpr(context);
        var rightValueExpr = rightExpr.evalExpr(context);

        if (leftValueExpr instanceof NullValueExpr) {
            throw new NullPointerError(sourceCodeRef, leftExpr.getSourceCodeRef());
        }
        if (rightValueExpr instanceof NullValueExpr) {
            throw new NullPointerError(sourceCodeRef, rightExpr.getSourceCodeRef());
        }

        if (leftValueExpr instanceof NumberValueExpr && rightValueExpr instanceof NumberValueExpr) {
            Number leftValue = ((NumberValueExpr)leftValueExpr).getValue();
            Number rightValue = ((NumberValueExpr)rightValueExpr).getValue();

            if (leftValue instanceof Integer && rightValue instanceof Integer) {
                return new NumberValueExpr(sourceCodeRef, ((Integer) leftValue) - ((Integer) rightValue));
            }

            try {
                Number result = ((NumberValueExpr) leftValueExpr).toDouble() -
                        ((NumberValueExpr) rightValueExpr).toDouble();
                return new NumberValueExpr(sourceCodeRef, result);
            } catch (Throwable t) {
                throw new ArithmeticError(t, sourceCodeRef);
            }
        }

        throw new InternalInterpreterError("Invalid operands: " + leftExpr.getType().getName() + " , " +
                rightExpr.getType().getName() + " for operator: '-'", sourceCodeRef);
    }

    @Override
    public SourceCodeRef getSourceCodeRef() {
        return sourceCodeRef;
    }

    @Override
    public Type getType() {
        return type;
    }
}
