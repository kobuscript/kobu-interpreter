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
import dev.kobu.interpreter.ast.eval.expr.value.NullValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.number.NumberValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.StringValueExpr;
import dev.kobu.interpreter.ast.symbol.*;
import dev.kobu.interpreter.ast.symbol.value.NumberTypeSymbol;
import dev.kobu.interpreter.ast.symbol.value.StringTypeSymbol;
import dev.kobu.interpreter.error.analyzer.InvalidOperatorError;
import dev.kobu.interpreter.error.eval.ArithmeticError;
import dev.kobu.interpreter.error.eval.InternalInterpreterError;
import dev.kobu.interpreter.error.eval.NullPointerError;
import dev.kobu.interpreter.ast.eval.Expr;
import dev.kobu.interpreter.ast.eval.ValueExpr;

import java.util.HashSet;
import java.util.Map;

public class AddExpr implements Expr {

    private final SourceCodeRef sourceCodeRef;

    private final Expr leftExpr;

    private final Expr rightExpr;

    private Type type;

    public AddExpr(SourceCodeRef sourceCodeRef, Expr leftExpr, Expr rightExpr) {
        this.sourceCodeRef = sourceCodeRef;
        this.leftExpr = leftExpr;
        this.rightExpr = rightExpr;
    }

    @Override
    public void analyze(EvalContext context) {
        leftExpr.analyze(context);
        rightExpr.analyze(context);

        if (leftExpr.getType() instanceof UnknownType || rightExpr.getType() instanceof UnknownType) {
            this.type = UnknownType.INSTANCE;
            return;
        }

        if (leftExpr.getType() instanceof NumberTypeSymbol && rightExpr.getType() instanceof NumberTypeSymbol) {
            type = BuiltinScope.NUMBER_TYPE;
        } else if (leftExpr.getType() instanceof StringTypeSymbol || rightExpr.getType() instanceof StringTypeSymbol) {
            type = BuiltinScope.STRING_TYPE;
        } else {
            context.addAnalyzerError(new InvalidOperatorError(sourceCodeRef,
                    leftExpr.getType(), "+", rightExpr.getType()));
            type = UnknownType.INSTANCE;
        }

    }

    @Override
    public ValueExpr evalExpr(EvalContext context) {

        var leftValueExpr = leftExpr.evalExpr(context);
        var rightValueExpr = rightExpr.evalExpr(context);

        if (leftValueExpr instanceof StringValueExpr || rightValueExpr instanceof StringValueExpr) {
            String str = getString(leftValueExpr) + getString(rightValueExpr);
            return new StringValueExpr(sourceCodeRef, str);
        }

        if (leftValueExpr instanceof NullValueExpr) {
            throw new NullPointerError(sourceCodeRef, leftExpr.getSourceCodeRef());
        }
        if (rightValueExpr instanceof NullValueExpr) {
            throw new NullPointerError(sourceCodeRef, rightExpr.getSourceCodeRef());
        }

        if (leftValueExpr instanceof NumberValueExpr && rightValueExpr instanceof NumberValueExpr) {

            try {
                return ((NumberValueExpr) leftValueExpr).add((NumberValueExpr) rightValueExpr);
            } catch (Throwable t) {
                throw new ArithmeticError(t, sourceCodeRef);
            }
        }

        throw new InternalInterpreterError("Invalid operands: " + leftExpr.getType().getName() + " , " +
                rightExpr.getType().getName() + " for operator: '+'", sourceCodeRef);
    }

    @Override
    public SourceCodeRef getSourceCodeRef() {
        return sourceCodeRef;
    }

    @Override
    public void setResolvedTypes(Map<String, Type> resolvedTypes) {
        if (leftExpr != null) {
            leftExpr.setResolvedTypes(resolvedTypes);
        }
        if (rightExpr != null) {
            rightExpr.setResolvedTypes(resolvedTypes);
        }
    }

    @Override
    public Type getType() {
        return type;
    }

    private String getString(ValueExpr valueExpr) {
        if (valueExpr instanceof StringValueExpr) {
            return ((StringValueExpr)valueExpr).getValue();
        }
        return valueExpr.getStringValue(new HashSet<>());
    }

}
