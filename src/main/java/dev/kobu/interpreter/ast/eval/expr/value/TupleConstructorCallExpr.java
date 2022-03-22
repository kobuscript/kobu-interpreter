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

import dev.kobu.interpreter.ast.eval.context.EvalContext;
import dev.kobu.interpreter.ast.eval.Expr;
import dev.kobu.interpreter.ast.eval.HasTargetType;
import dev.kobu.interpreter.ast.eval.ValueExpr;
import dev.kobu.interpreter.ast.symbol.TupleType;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.ast.symbol.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TupleConstructorCallExpr implements Expr, HasTargetType {

    private final SourceCodeRef sourceCodeRef;

    private final List<Expr> exprList;

    private Type targetType;

    private TupleType type;

    public TupleConstructorCallExpr(SourceCodeRef sourceCodeRef, List<Expr> exprList) {
        this.sourceCodeRef = sourceCodeRef;
        this.exprList = exprList;
    }

    public List<Expr> getExprList() {
        return exprList;
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
        if (targetType instanceof TupleType) {
            TupleType targetTupleType = (TupleType) targetType;
            if (exprList.size() == targetTupleType.getTypes().size()) {
                for (int i = 0; i < exprList.size(); i++) {
                    Expr expr = exprList.get(i);
                    if (expr instanceof HasTargetType) {
                        ((HasTargetType)expr).setTargetType(targetTupleType.getTypes().get(i));
                    }
                }
            }
        }
        for (Expr expr : exprList) {
            expr.analyze(context);
        }

        this.type = new TupleType(exprList.stream().map(Expr::getType).collect(Collectors.toList()));
    }

    @Override
    public ValueExpr evalExpr(EvalContext context) {
        List<ValueExpr> value = new ArrayList<>();

        for (Expr expr : exprList) {
            value.add(expr.evalExpr(context));
        }

        return new TupleValueExpr(type, value);
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
