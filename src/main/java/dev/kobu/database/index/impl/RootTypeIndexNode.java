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

import java.util.*;
import java.util.stream.Collectors;

public class RootTypeIndexNode extends RootIndexNode {

    private final EvalContextProvider evalContextProvider;

    private final AnalyzerContext analyzerContext;

    private final ModuleScope moduleScope;

    private final QueryTypeClause queryTypeClause;

    private Map<Integer, List<Fact>> accMap;

    private final Map<Integer, List<Fact>> factMap = new HashMap<>();

    private Map<Fact, Integer> factMatchMap;

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
                initializeAccMap();
                registerFact(fact);
                var list = accMap.computeIfAbsent(fact.getCreatorId(), k -> new ArrayList<>());
                list.add(fact);
            }
        } else {
            initializeFactMatchMap();
            if (canDispatch(fact)) {
                var evalContext = evalContextProvider.newEvalContext(analyzerContext, moduleScope);

                Fact prevFact = getPreviousFactAndRegister(fact);
                Integer matchId = prevFact != null ? factMatchMap.remove(fact) : null;
                Match match;
                if (fact instanceof RecordValueExpr) {
                    var record = (RecordValueExpr) fact;
                    if (matchId != null) {
                        match = new Match(matchId, evalContext, record, record, queryTypeClause.getBind());
                    } else {
                        match = new Match(evalContext, record, record, queryTypeClause.getBind());
                    }
                } else {
                    if (matchId != null) {
                        match = new Match(matchId, evalContext, null, fact, queryTypeClause.getBind());
                    } else {
                        match = new Match(evalContext, null, fact, queryTypeClause.getBind());
                    }
                }

                factMatchMap.put(fact, match.getMatchId());
                dispatch(match);
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
            return type.equals(fact.getType());
        }
    }

    @Override
    public void clear() {
        if (queryTypeClause.accumulator()) {
            accMap = null;
            factMap.clear();
        }
        super.clear();
    }

    @Override
    public void beforeRun() {
        if (queryTypeClause.accumulator()) {
            filterAccMap();
            var evalContext = evalContextProvider.newEvalContext(analyzerContext, moduleScope);
            Match match;
            if (matchId == 0) {
                match = new Match(evalContext, null,
                        new InternalAccIndexValueExpr(accMap),
                        queryTypeClause.getBind());
                matchId = match.getMatchId();
            } else {
                match = new Match(matchId, evalContext, null,
                        new InternalAccIndexValueExpr(accMap),
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

    private void initializeAccMap() {
        if (accMap == null) {
            accMap = new LinkedHashMap<>();
        }
    }

    private void initializeFactMatchMap() {
        if (factMatchMap == null) {
            factMatchMap = new HashMap<>();
        }
    }

    private void registerFact(Fact fact) {
        List<Fact> facts = factMap.computeIfAbsent(fact.getCreatorId(), k -> new ArrayList<>());
        facts.removeIf(fact::overrides);
        facts.add(fact);
    }

    private Fact getPreviousFactAndRegister(Fact fact) {
        List<Fact> facts = factMap.get(fact.getCreatorId());
        Fact previous = null;
        if (facts != null && !facts.isEmpty()) {
            Iterator<Fact> it = facts.iterator();
            while (it.hasNext()) {
                Fact f = it.next();
                if (fact.overrides(f)) {
                    if (previous == null) {
                        previous = f;
                    }
                    it.remove();
                }
            }
        }
        return previous;
    }

    private boolean validFact(Fact fact) {
        List<Fact> facts = factMap.get(fact.getCreatorId());
        if (facts != null) {
            return facts.contains(fact);
        }
        return false;
    }

    private void filterAccMap() {
        if (accMap != null) {
            accMap.entrySet().removeIf(entry -> {
                entry.getValue().removeIf(f -> !validFact(f));
                return entry.getValue().isEmpty();
            });
        }
    }

}
