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
import dev.kobu.interpreter.ast.eval.HasMethods;
import dev.kobu.interpreter.ast.eval.ValueExpr;
import dev.kobu.interpreter.ast.eval.context.SnapshotValue;
import dev.kobu.interpreter.ast.symbol.*;

import java.util.Objects;

public class NumberValueExpr implements ValueExpr, HasMethods {

    private SourceCodeRef sourceCodeRef;

    private final Number value;

    private final NumberTypeSymbol type = BuiltinScope.NUMBER_TYPE;

    public NumberValueExpr(Number value) {
        this.value = value;
    }

    public NumberValueExpr(SourceCodeRef sourceCodeRef, Number value) {
        this.sourceCodeRef = sourceCodeRef;
        this.value = value;
    }

    @Override
    public void analyze(EvalContext context) {

    }

    @Override
    public ValueExpr evalExpr(EvalContext context) {
        return this;
    }

    @Override
    public SourceCodeRef getSourceCodeRef() {
        return sourceCodeRef;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public NamedFunction resolveMethod(String methodName) {
        return type.resolveMethod(methodName);
    }

    public Number getValue() {
        return value;
    }

    public Number inc() {
        if (value instanceof Integer) {
            return ((Integer)value) + 1;
        } else {
            return ((Double)value) + 1;
        }
    }

    public Number dec() {
        if (value instanceof Integer) {
            return ((Integer)value) - 1;
        } else {
            return ((Double)value) - 1;
        }
    }

    public Double toDouble() {
        if (value instanceof Integer) {
            return value.doubleValue();
        }
        return (Double) value;
    }

    @Override
    public String getStringValue() {
        return value.toString();
    }

    @Override
    public SnapshotValue getSnapshotValue() {
        return new NumberSnapshotValue(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NumberValueExpr that = (NumberValueExpr) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    private static class NumberSnapshotValue implements SnapshotValue {

        private final Number value;

        public NumberSnapshotValue(Number value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            NumberSnapshotValue that = (NumberSnapshotValue) o;
            return Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

    }
}
