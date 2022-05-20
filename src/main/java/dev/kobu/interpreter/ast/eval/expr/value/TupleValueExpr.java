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

package dev.kobu.interpreter.ast.eval.expr.value;

import dev.kobu.interpreter.ast.eval.context.EvalContext;
import dev.kobu.interpreter.ast.eval.context.SnapshotValue;
import dev.kobu.interpreter.ast.symbol.ValType;
import dev.kobu.interpreter.ast.symbol.function.NamedFunction;
import dev.kobu.interpreter.ast.symbol.tuple.TupleType;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.ast.symbol.Type;
import dev.kobu.interpreter.ast.eval.HasMethods;
import dev.kobu.interpreter.ast.eval.ValueExpr;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class TupleValueExpr implements ValueExpr, HasMethods {

    private final TupleType type;

    private final List<ValueExpr> valueExprList;

    public TupleValueExpr(TupleType type, List<ValueExpr> valueExprList) {
        this.type = type;
        this.valueExprList = valueExprList;
    }

    @Override
    public SourceCodeRef getSourceCodeRef() {
        return null;
    }

    @Override
    public void analyze(EvalContext context) {

    }

    @Override
    public ValueExpr evalExpr(EvalContext context) {
        return this;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public NamedFunction resolveMethod(String methodName) {
        return type.resolveMethod(methodName);
    }

    public List<ValueExpr> getValueExprList() {
        return valueExprList;
    }

    @Override
    public String getStringValue(Set<Integer> idSet) {
        return "(" + valueExprList.stream().map(v -> v.getStringValue(idSet)).collect(Collectors.joining(", ")) + ")";
    }

    @Override
    public void prettyPrint(Set<Integer> idSet, StringBuilder out, int level) {
        if (valueExprList.stream().allMatch(v -> v.getType() == null || v.getType() instanceof ValType)) {
            out.append('(');
            out.append(valueExprList.stream().map(v -> v.getStringValue(idSet)).collect(Collectors.joining(", ")));
            out.append(')');
        } else {
            out.append("(\n");
            int count = 0;
            for (ValueExpr valueExpr : valueExprList) {
                if (count > 0) {
                    out.append(",\n");
                }
                out.append(" ".repeat(level * PRETTY_PRINT_TAB_SIZE));
                valueExpr.prettyPrint(idSet, out, level + 1);
                count++;
            }
            out.append('\n');
            out.append(" ".repeat(level * PRETTY_PRINT_TAB_SIZE));
            out.append(')');
        }
    }

    @Override
    public SnapshotValue getSnapshotValue() {
        return new TupleSnapshotValue(valueExprList);
    }

    private static class TupleSnapshotValue implements SnapshotValue {

        private final List<SnapshotValue> values = new ArrayList<>();

        public TupleSnapshotValue(List<ValueExpr> values) {
            values.forEach(v -> this.values.add(v.getSnapshotValue()));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TupleSnapshotValue that = (TupleSnapshotValue) o;
            return Objects.equals(values, that.values);
        }

        @Override
        public int hashCode() {
            return Objects.hash(values);
        }

    }

}
