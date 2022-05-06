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
import dev.kobu.database.index.TwoInputsIndexNode;
import dev.kobu.database.index.Match;
import dev.kobu.interpreter.ast.eval.ValueExpr;
import dev.kobu.interpreter.ast.eval.context.ContextSnapshot;
import dev.kobu.interpreter.ast.eval.expr.value.ArrayValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.NullValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.RecordValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.TemplateValueExpr;
import dev.kobu.interpreter.ast.query.QueryJoin;
import dev.kobu.interpreter.ast.symbol.array.ArrayType;

import java.util.*;
import java.util.stream.Collectors;

public class JoinIndexNode extends TwoInputsIndexNode {

    private final QueryJoin queryJoin;

    private Map<Integer, ContextSnapshot> snapshotMap;

    private final Map<MatchPair, Integer> matchPairMap = new HashMap<>();

    public JoinIndexNode(QueryJoin queryJoin) {
        this.queryJoin = queryJoin;

        if (queryJoin.getTypeClause().accumulator()) {
            this.snapshotMap = new HashMap<>();
        }
    }

    @Override
    protected void receive(Match left, Match right) {

        if (queryJoin.getOfExpr() == null) {
            dispatch(merge(left, right));
        } else {

            var ofValueExpr = queryJoin.getOfExpr().evalExpr(left.getContext());

            if (ofValueExpr == null || ofValueExpr instanceof NullValueExpr) {
                if (queryJoin.getTypeClause().accumulator()) {
                    merge(left, right
                            .setValue(new ArrayValueExpr(
                                        (ArrayType) queryJoin.getTypeClause().getQueryType(), new ArrayList<>()),
                                    right.getBind()));
                } else {
                    dispatch(merge(left, right));
                }
            } else if (ofValueExpr instanceof RecordValueExpr) {
                RecordValueExpr recordValueExpr = (RecordValueExpr) ofValueExpr;

                if (((Fact)right.getValue()).getCreatorId() == recordValueExpr.getId()) {
                    dispatch(merge(left, right));
                }
            } else if (ofValueExpr instanceof ArrayValueExpr) {

                ArrayValueExpr arrayValueExpr = (ArrayValueExpr) ofValueExpr;
                List<ValueExpr> recordList = arrayValueExpr.getValue();
                Set<Integer> recordSet = recordList
                        .stream()
                        .map(v -> ((RecordValueExpr)v).getId())
                        .collect(Collectors.toSet());

                List<ValueExpr> values = ((ArrayValueExpr)right.getValue()).getValue();
                values = values
                        .stream()
                        .filter(value -> recordSet.contains(((Fact)value).getCreatorId()))
                        .collect(Collectors.toList());

                sort(recordList, values);

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

    private void sort(List<ValueExpr> source, List<ValueExpr> list) {
        Map<Integer, Integer> idxMap = new HashMap<>();
        for (int i = 0; i < source.size(); i++) {
            ValueExpr valueExpr = source.get(i);
            int id = getId(valueExpr);
            idxMap.put(id, i);
        }
        list.sort((v1, v2) -> {
            int id1 = ((Fact)v1).getCreatorId();
            int id2 = ((Fact)v2).getCreatorId();

            return idxMap.get(id1) - idxMap.get(id2);
        });
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
