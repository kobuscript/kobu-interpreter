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
import dev.kobu.interpreter.ast.eval.HasTargetType;
import dev.kobu.interpreter.ast.eval.ValueExpr;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.ast.symbol.Type;

import java.util.Map;

public class FunctionArgExpr implements Expr, HasTargetType {

    private final SourceCodeRef sourceCodeRef;

    private final Expr expr;

    private Type targetType;

    private Type type;

    public FunctionArgExpr(SourceCodeRef sourceCodeRef, Expr expr) {
        this.sourceCodeRef = sourceCodeRef;
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
        if (expr instanceof HasTargetType) {
            ((HasTargetType)expr).setTargetType(targetType);
        }
        expr.analyze(context);
        this.type = expr.getType();
    }

    @Override
    public ValueExpr evalExpr(EvalContext context) {
        return expr.evalExpr(context);
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

    public Type getExprType() {
        if (expr == null) {
            return null;
        }
        return expr.getType();
    }
}
