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
import dev.kobu.database.index.Match;
import dev.kobu.database.index.TwoInputsIndexNode;
import dev.kobu.interpreter.ast.eval.ValueExpr;
import dev.kobu.interpreter.ast.eval.context.ContextSnapshot;
import dev.kobu.interpreter.ast.eval.expr.value.ArrayValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.NullValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.RecordValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.TemplateValueExpr;
import dev.kobu.interpreter.ast.query.QueryJoin;
import dev.kobu.interpreter.ast.symbol.array.ArrayType;

import java.util.*;

public class JoinIndexNode extends TwoInputsIndexNode {

    private final QueryJoin queryJoin;

    private Map<Integer, ContextSnapshot> snapshotMap;

    private final Map<MatchPair, Integer> matchPairMap = new HashMap<>();

    private ValueExpr lastOfValueExpr;

    private int lastLeftMatchId;

    public JoinIndexNode(QueryJoin queryJoin) {
        this.queryJoin = queryJoin;

        if (queryJoin.getTypeClause().accumulator()) {
            this.snapshotMap = new HashMap<>();
        }
    }

    @Override
    protected void receive(Match left, Match right) {

        if (queryJoin.getOfExpr() == null) {
            if (right.getValue() instanceof InternalAccIndexValueExpr) {
                if (queryJoin.getTypeClause().getQueryType() instanceof ArrayType) {
                    right = right.setValue(new ArrayValueExpr(
                            (ArrayType) queryJoin.getTypeClause().getQueryType(),
                            ((InternalAccIndexValueExpr) right.getValue()).toList()
                    ), right.getBind());
                    dispatch(merge(left, right));
                } else {
                    InternalAccIndexValueExpr index = (InternalAccIndexValueExpr) right.getValue();
                    for (ValueExpr valueExpr : index.toList()) {
                        right = right.setValue(valueExpr, right.getBind(), true);
                        List<Match> matches = index.eval(right);
                        for (Match rightMatch : matches) {
                            dispatch(merge(left, rightMatch));
                        }
                    }
                }
            } else {
                dispatch(merge(left, right));
            }
        } else {

            ValueExpr ofValueExpr;
            if (lastLeftMatchId == left.getMatchId()) {
                ofValueExpr = lastOfValueExpr;
            } else {
                ofValueExpr = queryJoin.getOfExpr().evalExpr(left.getContext());
                lastOfValueExpr = ofValueExpr;
                lastLeftMatchId = left.getMatchId();
            }

            if (ofValueExpr == null || ofValueExpr instanceof NullValueExpr) {
                if (queryJoin.getTypeClause().accumulator()) {
                    var prevCtx = snapshotMap.get(left.getMatchId());
                    var newMatch = merge(left, right
                            .setValue(new ArrayValueExpr(
                                        (ArrayType) queryJoin.getTypeClause().getQueryType(), new ArrayList<>()),
                                    right.getBind()));
                    var snapshot = newMatch.getSnapshot();
                    if (snapshot.equals(prevCtx)) {
                        return;
                    }
                    snapshotMap.put(left.getMatchId(), snapshot);
                    dispatch(newMatch);
                }
            } else if (ofValueExpr instanceof RecordValueExpr) {
                RecordValueExpr recordValueExpr = (RecordValueExpr) ofValueExpr;

                InternalAccIndexValueExpr index = (InternalAccIndexValueExpr) right.getValue();
                var facts = index.getFacts(recordValueExpr.getId());

                if (facts != null) {
                    for (Fact fact : facts) {
                        Integer matchId = index.getFactMatchMap().remove(fact);
                        Match match;
                        if (matchId == null) {
                            match = new Match(right.getContext(), null, fact, right.getBind());
                            index.getFactMatchMap().put(fact, match.getMatchId());
                        } else {
                            match = new Match(matchId, right.getContext(), null, fact, right.getBind());
                        }

                        List<Match> matches = index.eval(match);
                        for (Match rightMatch : matches) {
                            dispatch(merge(left, rightMatch));
                        }
                    }
                }
            } else if (ofValueExpr instanceof ArrayValueExpr) {

                ArrayValueExpr arrayValueExpr = (ArrayValueExpr) ofValueExpr;
                List<ValueExpr> recordList = arrayValueExpr.getValue();
                InternalAccIndexValueExpr index = (InternalAccIndexValueExpr) right.getValue();
                List<ValueExpr> values = new ArrayList<>();
                for (ValueExpr valueExpr : recordList) {
                    var facts = index.getFacts(((RecordValueExpr)valueExpr).getId());
                    if (facts != null) {
                        values.addAll(facts);
                    }
                }

                Match newMatch = merge(left, right
                        .setValue(new ArrayValueExpr((ArrayType) queryJoin.getTypeClause().getQueryType(), values),
                                right.getBind()));

                if (queryJoin.getTypeClause().accumulator()) {
                    var prevCtx = snapshotMap.get(left.getMatchId());
                    ContextSnapshot snapshot = newMatch.getSnapshot();
                    if (snapshot.equals(prevCtx)) {
                        return;
                    }
                    snapshotMap.put(left.getMatchId(), snapshot);
                }

                dispatch(newMatch);
            }

        }

    }

    private Match merge(Match left, Match right) {
        MatchPair matchPair = new MatchPair(left.getMatchId(), right.getMatchId());
        Integer matchId = matchPairMap.get(matchPair);
        Match newMatch = left.merge(matchId, right);
        if (matchId == null) {
            matchPairMap.put(matchPair, newMatch.getMatchId());
        }
        return newMatch;
    }

    private int getId(ValueExpr valueExpr) {
        if (valueExpr instanceof RecordValueExpr) {
            return ((RecordValueExpr)valueExpr).getId();
        } else if (valueExpr instanceof TemplateValueExpr) {
            return ((TemplateValueExpr)valueExpr).getId();
        }
        return 0;
    }

    private static class MatchPair {

        final int leftId;

        final int rightId;

        public MatchPair(int leftId, int rightId) {
            this.leftId = leftId;
            this.rightId = rightId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MatchPair matchPair = (MatchPair) o;
            return leftId == matchPair.leftId && rightId == matchPair.rightId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(leftId, rightId);
        }

    }
}
