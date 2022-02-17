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

import dev.cgrscript.database.index.TwoInputsIndexNode;
import dev.cgrscript.database.match.Match;
import dev.cgrscript.interpreter.ast.eval.EvalContext;
import dev.cgrscript.interpreter.ast.eval.ValueExpr;
import dev.cgrscript.interpreter.ast.eval.expr.value.ArrayValueExpr;
import dev.cgrscript.interpreter.ast.eval.expr.value.NullValueExpr;
import dev.cgrscript.interpreter.ast.eval.expr.value.RecordValueExpr;
import dev.cgrscript.interpreter.ast.eval.expr.value.TemplateValueExpr;
import dev.cgrscript.interpreter.ast.query.QueryJoin;
import dev.cgrscript.interpreter.ast.symbol.ArrayType;
import dev.cgrscript.interpreter.ast.symbol.Symbol;
import dev.cgrscript.interpreter.ast.symbol.VariableSymbol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JoinIndexNode extends TwoInputsIndexNode {

    private final QueryJoin queryJoin;

    private Map<Integer, EvalContext> accContextMap;

    public JoinIndexNode(QueryJoin queryJoin) {
        this.queryJoin = queryJoin;
        if (queryJoin.joinArray()) {
            accContextMap = new HashMap<>();
        }
    }

    @Override
    protected void receive(Match left, Match right) {
        if (accContextMap == null) {
            if (right == null) {
                return;
            }
            if (queryJoin.getOfExpr() == null) {
                dispatch(left.merge(right));
            } else {
                RecordValueExpr recordValueExpr = (RecordValueExpr) queryJoin.getOfExpr().evalExpr(left.getContext());

                if (right.getFact().creatorId() == recordValueExpr.getId()) {
                    dispatch(left.merge(right));
                }
            }
        } else {

            ValueExpr fieldValueExpr = queryJoin.getOfExpr().evalExpr(left.getContext());
            if (fieldValueExpr == null || fieldValueExpr instanceof NullValueExpr) {
                fieldValueExpr = new ArrayValueExpr((ArrayType) queryJoin.getOfExpr().getType(), new ArrayList<>());
            }
            ArrayValueExpr arrayValueExpr = (ArrayValueExpr) fieldValueExpr;
            if (right != null && arrayValueExpr.getValue().stream()
                    .noneMatch(item -> right.getFact().creatorId() == ((RecordValueExpr)item).getId())) {
                return;
            }

            EvalContext context = accContextMap.computeIfAbsent(left.getMatchId(), key -> {
                var ctx = new EvalContext(left.getContext().getEvalMode(), left.getContext().getModuleScope(),
                        left.getContext().getDatabase(), left.getContext().getInputParser(),
                        left.getContext().getOutputWriter());
                ctx.getCurrentScope().addAll(left.getContext().getCurrentScope());
                return ctx;
            });

            if (right == null) {
                queryJoin.getTypeClause().createEmptyArray(context);
                dispatch(left.merge(context, false));
                return;
            }

            for (String key : right.getContext().getCurrentScope().getKeys()) {
                Symbol symbol = right.getContext().getCurrentScope().resolve(key);
                if (symbol instanceof VariableSymbol) {
                    VariableSymbol varSymbol = (VariableSymbol) symbol;
                    Symbol currentSymbol = context.getCurrentScope().resolve(key);
                    if (currentSymbol == null) {
                        ArrayType type = new ArrayType(varSymbol.getType());
                        currentSymbol = new VariableSymbol(varSymbol.getSourceCodeRef(), varSymbol.getName(),
                                type);
                        context.getCurrentScope().define(currentSymbol);
                        List<ValueExpr> listValue = new ArrayList<>();
                        listValue.add(right.getContext().getCurrentScope().getValue(key));

                        sort(arrayValueExpr.getValue(), listValue);

                        ArrayValueExpr valueExpr = new ArrayValueExpr(type, listValue);
                        context.getCurrentScope().setValue(key, valueExpr);
                    } else {
                        ArrayValueExpr valueExpr = (ArrayValueExpr) context.getCurrentScope().getValue(key);
                        valueExpr.getValue().add(right.getContext().getCurrentScope().getValue(key));

                        sort(arrayValueExpr.getValue(), valueExpr.getValue());
                    }
                }
            }

            dispatch(left.merge(context, true));
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
            int id1 = v1.creatorId();
            int id2 = v2.creatorId();

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
