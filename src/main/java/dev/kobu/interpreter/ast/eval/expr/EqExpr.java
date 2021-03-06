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
import dev.kobu.interpreter.ast.eval.expr.value.BooleanValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.NullValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.number.NumberValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.StringValueExpr;
import dev.kobu.interpreter.ast.symbol.*;
import dev.kobu.interpreter.ast.symbol.value.BooleanTypeSymbol;
import dev.kobu.interpreter.ast.symbol.value.NumberTypeSymbol;
import dev.kobu.interpreter.ast.symbol.value.StringTypeSymbol;
import dev.kobu.interpreter.error.analyzer.InvalidOperatorError;
import dev.kobu.interpreter.error.eval.InternalInterpreterError;
import dev.kobu.interpreter.error.eval.NullPointerError;
import dev.kobu.interpreter.ast.eval.EqOperatorEnum;
import dev.kobu.interpreter.ast.eval.Expr;
import dev.kobu.interpreter.ast.eval.ValueExpr;

import java.util.Map;

public class EqExpr implements Expr {

    private final SourceCodeRef sourceCodeRef;

    private final Expr leftExpr;

    private final EqOperatorEnum operator;

    private final Expr rightExpr;

    private Type type;

    public EqExpr(SourceCodeRef sourceCodeRef, Expr leftExpr, EqOperatorEnum operator, Expr rightExpr) {
        this.sourceCodeRef = sourceCodeRef;
        this.leftExpr = leftExpr;
        this.operator = operator;
        this.rightExpr = rightExpr;
    }

    @Override
    public void analyze(EvalContext context) {
        if (leftExpr == null || rightExpr == null) {
            return;
        }
        leftExpr.analyze(context);
        rightExpr.analyze(context);

        if (leftExpr.getType() instanceof UnknownType || rightExpr.getType() instanceof UnknownType) {
            this.type = UnknownType.INSTANCE;
            return;
        }

        type = UnknownType.INSTANCE;
        if (leftExpr.getType() != null && rightExpr.getType() != null) {
            if (notComparable(leftExpr.getType()) || notComparable(rightExpr.getType())) {
                context.addAnalyzerError(new InvalidOperatorError(sourceCodeRef,
                        leftExpr.getType(), operator.getSymbol(), rightExpr.getType()));
            } else if (!leftExpr.getType().equals(rightExpr.getType())) {
                context.addAnalyzerError(new InvalidOperatorError(sourceCodeRef,
                        leftExpr.getType(), operator.getSymbol(), rightExpr.getType()));
            } else if (leftExpr.getType() instanceof BooleanTypeSymbol &&
                    operator != EqOperatorEnum.EQUALS && operator != EqOperatorEnum.NOT_EQUALS) {
                context.addAnalyzerError(new InvalidOperatorError(sourceCodeRef,
                        leftExpr.getType(), operator.getSymbol(), rightExpr.getType()));
            } else {
                type = BuiltinScope.BOOLEAN_TYPE;
            }
        } else if (operator != EqOperatorEnum.EQUALS && operator != EqOperatorEnum.NOT_EQUALS) {
            context.addAnalyzerError(new InvalidOperatorError(sourceCodeRef,
                    leftExpr.getType(), operator.getSymbol(), rightExpr.getType()));
        } else {
            type = BuiltinScope.BOOLEAN_TYPE;
        }
    }

    @Override
    public ValueExpr evalExpr(EvalContext context) {
        var leftValueExpr = leftExpr.evalExpr(context);
        var rightValueExpr = rightExpr.evalExpr(context);

        if (leftValueExpr instanceof NullValueExpr) {
            if (operator.equals(EqOperatorEnum.EQUALS)) {
                return new BooleanValueExpr(sourceCodeRef, rightValueExpr instanceof NullValueExpr);
            } else if (operator.equals(EqOperatorEnum.NOT_EQUALS)) {
                return new BooleanValueExpr(sourceCodeRef, !(rightValueExpr instanceof NullValueExpr));
            }
            throw new NullPointerError(sourceCodeRef, leftExpr.getSourceCodeRef());
        }
        if (rightValueExpr instanceof NullValueExpr) {
            if (operator.equals(EqOperatorEnum.EQUALS)) {
                return new BooleanValueExpr(sourceCodeRef, false);
            } else if (operator.equals(EqOperatorEnum.NOT_EQUALS)) {
                return new BooleanValueExpr(sourceCodeRef, true);
            }
            throw new NullPointerError(sourceCodeRef, rightExpr.getSourceCodeRef());
        }

        if (leftValueExpr instanceof StringValueExpr && rightValueExpr instanceof StringValueExpr) {
            var leftValue = ((StringValueExpr)leftValueExpr).getValue();
            var rightValue = ((StringValueExpr)rightValueExpr).getValue();

            if (operator.equals(EqOperatorEnum.EQUALS)) {
                return new BooleanValueExpr(sourceCodeRef, leftValue.equals(rightValue));
            } else if (operator.equals(EqOperatorEnum.NOT_EQUALS)) {
                return new BooleanValueExpr(sourceCodeRef, !leftValue.equals(rightValue));
            } else if (operator.equals(EqOperatorEnum.LESS)) {
                return new BooleanValueExpr(sourceCodeRef, leftValue.compareTo(rightValue) < 0);
            } else if (operator.equals(EqOperatorEnum.LESS_OR_EQUALS)) {
                return new BooleanValueExpr(sourceCodeRef, leftValue.compareTo(rightValue) <= 0);
            } else if (operator.equals(EqOperatorEnum.GREATER)) {
                return new BooleanValueExpr(sourceCodeRef, leftValue.compareTo(rightValue) > 0);
            } else if (operator.equals(EqOperatorEnum.GREATER_OR_EQUALS)) {
                return new BooleanValueExpr(sourceCodeRef, leftValue.compareTo(rightValue) >= 0);
            }
        } else if (leftValueExpr instanceof NumberValueExpr && rightValueExpr instanceof NumberValueExpr) {
            var leftValue = ((NumberValueExpr)leftValueExpr).getValue().doubleValue();
            var rightValue = ((NumberValueExpr)rightValueExpr).getValue().doubleValue();

            if (operator.equals(EqOperatorEnum.EQUALS)) {
                return new BooleanValueExpr(sourceCodeRef, leftValue == rightValue);
            } else if (operator.equals(EqOperatorEnum.NOT_EQUALS)) {
                return new BooleanValueExpr(sourceCodeRef, leftValue != rightValue);
            } else if (operator.equals(EqOperatorEnum.LESS)) {
                return new BooleanValueExpr(sourceCodeRef, leftValue < rightValue);
            } else if (operator.equals(EqOperatorEnum.LESS_OR_EQUALS)) {
                return new BooleanValueExpr(sourceCodeRef, leftValue <= rightValue);
            } else if (operator.equals(EqOperatorEnum.GREATER)) {
                return new BooleanValueExpr(sourceCodeRef, leftValue > rightValue);
            } else if (operator.equals(EqOperatorEnum.GREATER_OR_EQUALS)) {
                return new BooleanValueExpr(sourceCodeRef, leftValue >= rightValue);
            }

        } else if (leftValueExpr instanceof BooleanValueExpr && rightValueExpr instanceof BooleanValueExpr) {
            var leftValue = ((BooleanValueExpr)leftValueExpr).getValue();
            var rightValue = ((BooleanValueExpr)rightValueExpr).getValue();

            if (operator.equals(EqOperatorEnum.EQUALS)) {
                return new BooleanValueExpr(sourceCodeRef, leftValue == rightValue);
            } else if (operator.equals(EqOperatorEnum.NOT_EQUALS)) {
                return new BooleanValueExpr(sourceCodeRef, leftValue != rightValue);
            }
        }

        throw new InternalInterpreterError("Invalid operands: " + leftExpr.getType().getName() + " , " +
                rightExpr.getType().getName() + " for operator: '" + operator.getSymbol() + "'", sourceCodeRef);
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

    private boolean notComparable(Type type) {
        return type != null &&
                !(type instanceof StringTypeSymbol) &&
                !(type instanceof BooleanTypeSymbol) &&
                !(type instanceof NumberTypeSymbol);
    }
}
