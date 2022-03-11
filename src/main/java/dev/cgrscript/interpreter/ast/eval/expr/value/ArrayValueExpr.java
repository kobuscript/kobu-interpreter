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

package dev.cgrscript.interpreter.ast.eval.expr.value;

import dev.cgrscript.interpreter.ast.eval.*;
import dev.cgrscript.interpreter.ast.eval.context.EvalContext;
import dev.cgrscript.interpreter.ast.eval.context.SnapshotValue;
import dev.cgrscript.interpreter.ast.symbol.*;
import dev.cgrscript.interpreter.error.eval.ArrayIndexOutOfBoundsError;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ArrayValueExpr implements ValueExpr, HasMethods {

    private final ArrayType type;

    private final List<ValueExpr> value;

    public ArrayValueExpr(ArrayType type, List<ValueExpr> value) {
        this.type = type;
        this.value = value;
    }

    @Override
    public void analyze(EvalContext context) {

    }

    @Override
    public ValueExpr evalExpr(EvalContext context) {
        return this;
    }

    public void assign(SourceCodeRef sourceCodeRef, NumberValueExpr indexExpr, ValueExpr value) {
        int index = indexExpr.getValue().intValue();
        if (index < 0 || index >= this.value.size()) {
            throw new ArrayIndexOutOfBoundsError(sourceCodeRef, this, index);
        }
        this.value.set(index, value);
    }

    @Override
    public SourceCodeRef getSourceCodeRef() {
        return null;
    }

    @Override
    public ArrayType getType() {
        return type;
    }

    public List<ValueExpr> getValue() {
        return value;
    }

    @Override
    public FunctionType resolveMethod(String methodName) {
        return type.resolveMethod(methodName);
    }

    @Override
    public String getStringValue() {
        StringBuilder strBuilder = new StringBuilder("[");

        strBuilder.append(value.stream().map(ValueExpr::getStringValue).collect(Collectors.joining(", ")));

        strBuilder.append("]");
        return strBuilder.toString();
    }

    @Override
    public SnapshotValue getSnapshotValue() {
        return new ArraySnapshotValue(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArrayValueExpr that = (ArrayValueExpr) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    private static class ArraySnapshotValue implements SnapshotValue {

        private List<SnapshotValue> values = new ArrayList<>();

        public ArraySnapshotValue(List<ValueExpr> values) {
            values.forEach(v -> this.values.add(v.getSnapshotValue()));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ArraySnapshotValue that = (ArraySnapshotValue) o;
            return Objects.equals(values, that.values);
        }

        @Override
        public int hashCode() {
            return Objects.hash(values);
        }
    }
}
