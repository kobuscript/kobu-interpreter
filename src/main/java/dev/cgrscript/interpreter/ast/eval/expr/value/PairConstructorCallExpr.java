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

package dev.cgrscript.interpreter.ast.eval.expr.value;

import dev.cgrscript.interpreter.ast.eval.context.EvalContext;
import dev.cgrscript.interpreter.ast.eval.Expr;
import dev.cgrscript.interpreter.ast.eval.HasTargetType;
import dev.cgrscript.interpreter.ast.eval.ValueExpr;
import dev.cgrscript.interpreter.ast.symbol.PairType;
import dev.cgrscript.interpreter.ast.symbol.SourceCodeRef;
import dev.cgrscript.interpreter.ast.symbol.Type;

public class PairConstructorCallExpr implements Expr, HasTargetType {

    private final SourceCodeRef sourceCodeRef;

    private final Expr leftExpr;

    private final Expr rightExpr;

    private Type targetType;

    private PairType type;

    public PairConstructorCallExpr(SourceCodeRef sourceCodeRef, Expr leftExpr, Expr rightExpr) {
        this.sourceCodeRef = sourceCodeRef;
        this.leftExpr = leftExpr;
        this.rightExpr = rightExpr;
    }

    @Override
    public SourceCodeRef getSourceCodeRef() {
        return sourceCodeRef;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public void analyze(EvalContext context) {
        if (targetType instanceof PairType) {
            if (leftExpr instanceof HasTargetType) {
                ((HasTargetType)leftExpr).setTargetType(((PairType)targetType).getLeftType());
            }
            if (rightExpr instanceof HasTargetType) {
                ((HasTargetType)rightExpr).setTargetType(((PairType)targetType).getRightType());
            }
        }
        leftExpr.analyze(context);
        rightExpr.analyze(context);

        this.type = new PairType(leftExpr.getType(), rightExpr.getType());
    }

    @Override
    public ValueExpr evalExpr(EvalContext context) {
        return new PairValueExpr(type, leftExpr.evalExpr(context), rightExpr.evalExpr(context));
    }

    @Override
    public Type getTargetType() {
        return targetType;
    }

    @Override
    public void setTargetType(Type targetType) {
        this.targetType = targetType;
    }

}
