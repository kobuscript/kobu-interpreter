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

package dev.kobu.interpreter.ast.query;

import dev.kobu.interpreter.ast.eval.context.EvalContext;
import dev.kobu.interpreter.ast.eval.ValueExpr;
import dev.kobu.interpreter.ast.eval.expr.ArrayIndexExpr;
import dev.kobu.interpreter.ast.eval.expr.ArraySliceIndexExpr;
import dev.kobu.interpreter.ast.eval.expr.value.ArrayValueExpr;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.ast.symbol.Type;

import java.util.ArrayList;
import java.util.List;

public class QueryArrayIndexClause implements QueryArrayItemClause {

    private final SourceCodeRef sourceCodeRef;

    private final ArrayIndexExpr index;

    public QueryArrayIndexClause(SourceCodeRef sourceCodeRef, ArrayIndexExpr index) {
        this.sourceCodeRef = sourceCodeRef;
        this.index = index;
    }

    public SourceCodeRef getSourceCodeRef() {
        return sourceCodeRef;
    }

    @Override
    public void analyze(EvalContext context) {
        index.analyze(context);
    }

    public ArrayIndexExpr getIndex() {
        return index;
    }

    @Override
    public void setTypeScope(Type typeScope) {
    }

    @Override
    public void setValueScope(ValueExpr valueScope) {
    }

    @Override
    public List<ValueExpr> eval(EvalContext context, ArrayValueExpr arrayValue) {
        ValueExpr valueExpr = index.eval(context, arrayValue);

        if (valueExpr instanceof ArrayValueExpr && index instanceof ArraySliceIndexExpr) {
            return new ArrayList<>(((ArrayValueExpr) valueExpr).getValue());
        }

        List<ValueExpr> result = new ArrayList<>();
        result.add(valueExpr);
        return result;
    }

}
