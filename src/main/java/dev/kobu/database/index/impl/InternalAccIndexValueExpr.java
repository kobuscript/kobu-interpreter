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
import dev.kobu.interpreter.ast.eval.ValueExpr;
import dev.kobu.interpreter.ast.eval.context.EvalContext;
import dev.kobu.interpreter.ast.eval.context.SnapshotValue;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.ast.symbol.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InternalAccIndexValueExpr implements ValueExpr {

    private final Map<Integer, List<Fact>> factMap;

    public InternalAccIndexValueExpr(Map<Integer, List<Fact>> factMap) {
        this.factMap = factMap != null ? factMap : new HashMap<>();
    }

    public Map<Integer, List<Fact>> getFactMap() {
        return factMap;
    }

    public List<ValueExpr> toList() {
        List<ValueExpr> values = new ArrayList<>();
        for (List<Fact> list : factMap.values()) {
            values.addAll(list);
        }
        return values;
    }

    @Override
    public SourceCodeRef getSourceCodeRef() {
        return null;
    }

    @Override
    public void analyze(EvalContext context) {

    }

    @Override
    public Type getType() {
        return null;
    }

    @Override
    public ValueExpr evalExpr(EvalContext context) {
        return this;
    }

    @Override
    public String getStringValue() {
        return null;
    }

    @Override
    public void prettyPrint(StringBuilder out, int level) {

    }

    @Override
    public SnapshotValue getSnapshotValue() {
        return null;
    }

}
