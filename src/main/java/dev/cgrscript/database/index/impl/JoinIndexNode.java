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

package dev.cgrscript.database.index.impl;

import dev.cgrscript.database.Fact;
import dev.cgrscript.database.index.TwoInputsIndexNode;
import dev.cgrscript.database.index.Match;
import dev.cgrscript.interpreter.ast.eval.ValueExpr;
import dev.cgrscript.interpreter.ast.eval.expr.value.ArrayValueExpr;
import dev.cgrscript.interpreter.ast.eval.expr.value.RecordValueExpr;
import dev.cgrscript.interpreter.ast.eval.expr.value.TemplateValueExpr;
import dev.cgrscript.interpreter.ast.query.QueryJoin;
import dev.cgrscript.interpreter.ast.symbol.ArrayType;

import java.util.*;
import java.util.stream.Collectors;

public class JoinIndexNode extends TwoInputsIndexNode {

    private final QueryJoin queryJoin;

    public JoinIndexNode(QueryJoin queryJoin) {
        this.queryJoin = queryJoin;
    }

    @Override
    protected void receive(Match left, Match right) {

        if (queryJoin.getOfExpr() == null) {
            dispatch(left.merge(right));
        } else {

            var ofValueExpr = queryJoin.getOfExpr().evalExpr(left.getContext());

            if (ofValueExpr instanceof RecordValueExpr) {
                RecordValueExpr recordValueExpr = (RecordValueExpr) ofValueExpr;

                if (((Fact)right.getValue()).getCreatorId() == recordValueExpr.getId()) {
                    dispatch(left.merge(right));
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

                dispatch(left.merge(right
                        .setValue(new ArrayValueExpr((ArrayType) queryJoin.getTypeClause().getQueryType(), values),
                                right.getBind())));
            }

        }

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

}
