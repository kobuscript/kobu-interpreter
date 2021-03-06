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

import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.ast.eval.context.EvalContext;
import dev.kobu.interpreter.ast.eval.Evaluable;
import dev.kobu.interpreter.ast.eval.Expr;
import dev.kobu.interpreter.ast.symbol.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Query implements Evaluable {

    private final SourceCodeRef sourceCodeRef;

    private final QueryTypeClause typeClause;

    private List<QueryJoin> joins;

    private List<QueryExtractor> extractors;

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

    public void addExtractor(QueryExtractor extractor) {
        if (extractors == null) {
            extractors = new ArrayList<>();
        }
        extractors.add(extractor);
    }

    public QueryTypeClause getTypeClause() {
        return typeClause;
    }

    public List<QueryExtractor> getExtractors() {
        return extractors;
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
    public void setResolvedTypes(Map<String, Type> resolvedTypes) {
        if (typeClause != null) {
            typeClause.setResolvedTypes(resolvedTypes);
        }
        if (extractors != null) {
            for (QueryExtractor extractor : extractors) {
                extractor.setResolvedTypes(resolvedTypes);
            }
        }
        if (joins != null) {
            for (QueryJoin join : joins) {
                join.setResolvedTypes(resolvedTypes);
            }
        }
        if (whenExpr != null) {
            whenExpr.setResolvedTypes(resolvedTypes);
        }
    }

    @Override
    public void analyze(EvalContext context) {
        var typeClause = this.typeClause;
        typeClause.analyze(context);

        if (extractors != null) {
            for (QueryExtractor extractor : extractors) {
                extractor.setTypeScope(typeClause.getQueryType());
                extractor.analyze(context);
            }
        }

        if (joins != null) {
            for (QueryJoin join : joins) {
                join.analyze(context);
            }
        }

        if (whenExpr != null) {
            whenExpr.analyze(context);
        }
    }

}
