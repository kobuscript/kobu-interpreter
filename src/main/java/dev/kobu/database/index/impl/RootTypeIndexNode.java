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

package dev.kobu.database.index.impl;

import dev.kobu.database.Fact;
import dev.kobu.database.index.RootIndexNode;
import dev.kobu.database.index.Match;
import dev.kobu.interpreter.ast.AnalyzerContext;
import dev.kobu.interpreter.ast.eval.ValueExpr;
import dev.kobu.interpreter.ast.eval.context.EvalContextProvider;
import dev.kobu.interpreter.ast.eval.expr.value.ArrayValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.RecordValueExpr;
import dev.kobu.interpreter.ast.query.QueryTypeClause;
import dev.kobu.interpreter.ast.symbol.array.ArrayType;
import dev.kobu.interpreter.ast.symbol.ModuleScope;

import java.util.ArrayList;
import java.util.List;

public class RootTypeIndexNode extends RootIndexNode {

    private final EvalContextProvider evalContextProvider;

    private final AnalyzerContext analyzerContext;

    private final ModuleScope moduleScope;

    private final QueryTypeClause queryTypeClause;

    private List<Fact> accList;

    private int matchId;

    public RootTypeIndexNode(EvalContextProvider evalContextProvider, AnalyzerContext analyzerContext,
                             ModuleScope moduleScope, QueryTypeClause queryTypeClause) {
        this.evalContextProvider = evalContextProvider;
        this.analyzerContext = analyzerContext;
        this.moduleScope = moduleScope;
        this.queryTypeClause = queryTypeClause;
    }

    @Override
    public void receive(Fact fact) {
        if (queryTypeClause.accumulator()) {
            if (canDispatch(fact)) {
                initializeAccList();
                accList.removeIf(fact::overrides);
                accList.add(fact);
            }
        } else {
            if (canDispatch(fact)) {
                var evalContext = evalContextProvider.newEvalContext(analyzerContext, moduleScope);
                if (fact instanceof RecordValueExpr) {
                    var record = (RecordValueExpr) fact;
                    dispatch(new Match(evalContext, record, record, queryTypeClause.getBind()));
                } else {
                    dispatch(new Match(evalContext, null, (ValueExpr) fact, queryTypeClause.getBind()));
                }
            }
        }
    }

    private boolean canDispatch(Fact fact) {
        var type = queryTypeClause.getType();
        if (queryTypeClause.accumulator()) {
            type = ((ArrayType)type).getElementType();
        }
        if (queryTypeClause.includeSubtypes()) {
            return type.isAssignableFrom(fact.getType());
        } else {
            return type.getName().equals(fact.getType().getName());
        }
    }

    @Override
    public void clear() {
        if (queryTypeClause.accumulator()) {
            accList = null;
        }
        super.clear();
    }

    @Override
    public void beforeRun() {
        if (queryTypeClause.accumulator()) {
            var evalContext = evalContextProvider.newEvalContext(analyzerContext, moduleScope);
            Match match = null;
            if (matchId == 0) {
                match = new Match(evalContext, null,
                        new ArrayValueExpr((ArrayType) queryTypeClause.getType(), copyAccList()),
                        queryTypeClause.getBind());
                matchId = match.getMatchId();
            } else {
                match = new Match(matchId, evalContext, null,
                        new ArrayValueExpr((ArrayType) queryTypeClause.getType(), copyAccList()),
                        queryTypeClause.getBind());
            }
            dispatch(match);
        }
    }

    @Override
    public void afterRun() {
        if (queryTypeClause.accumulator()) {
            super.clear();
        }
    }

    private void initializeAccList() {
        if (accList == null) {
            accList = new ArrayList<>();
        }
    }

    private List<ValueExpr> copyAccList() {
        List<ValueExpr> values = new ArrayList<>();
        if (accList != null) {
            accList.forEach(fact -> values.add((ValueExpr) fact));
        }
        return values;
    }

}
