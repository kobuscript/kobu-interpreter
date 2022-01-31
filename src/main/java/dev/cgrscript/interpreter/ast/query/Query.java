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

package dev.cgrscript.interpreter.ast.query;

import dev.cgrscript.interpreter.ast.symbol.SourceCodeRef;
import dev.cgrscript.interpreter.ast.eval.EvalContext;
import dev.cgrscript.interpreter.ast.eval.Evaluable;
import dev.cgrscript.interpreter.ast.eval.Expr;

import java.util.ArrayList;
import java.util.List;

public class Query implements Evaluable {

    private final SourceCodeRef sourceCodeRef;

    private final QueryTypeClause typeClause;

    private List<QueryJoin> joins;

    private Expr whenExpr;

    public Query(SourceCodeRef sourceCodeRef, QueryTypeClause typeClause) {
        this.sourceCodeRef = sourceCodeRef;
        this.typeClause = typeClause;
    }

    public void addJoin(QueryJoin join) {
        if (joins == null) {
            joins = new ArrayList<>();
        }
        joins.add(join);
    }

    public QueryTypeClause getTypeClause() {
        return typeClause;
    }

    public List<QueryJoin> getJoins() {
        return joins;
    }

    public Expr getWhenExpr() {
        return whenExpr;
    }

    public void setWhenExpr(Expr whenExpr) {
        this.whenExpr = whenExpr;
    }

    @Override
    public SourceCodeRef getSourceCodeRef() {
        return sourceCodeRef;
    }

    @Override
    public void analyze(EvalContext context) {
        var typeClause = this.typeClause;
        typeClause.analyze(context);

        if (joins != null) {
            for (QueryJoin join : joins) {
                join.analyze(context);
            }
        }

        if (whenExpr != null) {
            whenExpr.analyze(context);
        }
    }

    public boolean hasDependencies() {
        if (joins != null) {
            return joins.stream().anyMatch(join -> join.getOfExpr() != null);
        }
        return false;
    }

}
