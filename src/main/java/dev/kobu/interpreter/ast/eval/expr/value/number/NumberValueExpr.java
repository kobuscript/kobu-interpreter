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

package dev.kobu.interpreter.ast.eval.expr.value.number;

import dev.kobu.interpreter.ast.eval.context.EvalContext;
import dev.kobu.interpreter.ast.eval.HasMethods;
import dev.kobu.interpreter.ast.eval.ValueExpr;
import dev.kobu.interpreter.ast.eval.context.SnapshotValue;
import dev.kobu.interpreter.ast.symbol.*;
import dev.kobu.interpreter.ast.symbol.function.NamedFunction;
import dev.kobu.interpreter.ast.symbol.value.NumberTypeSymbol;

import java.util.Objects;

public abstract class NumberValueExpr implements ValueExpr, HasMethods {

    private SourceCodeRef sourceCodeRef;

    protected final Number value;

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

    public NumberValueExpr add(NumberValueExpr other) {
        if (other instanceof IntegerValueExpr) {
            return add((IntegerValueExpr) other);
        } else if (other instanceof LongValueExpr) {
            return add((LongValueExpr) other);
        } else {
            return add((DoubleValueExpr) other);
        }
    }

    public NumberValueExpr sub(NumberValueExpr other) {
        if (other instanceof IntegerValueExpr) {
            return sub((IntegerValueExpr) other);
        } else if (other instanceof LongValueExpr) {
            return sub((LongValueExpr) other);
        } else {
            return sub((DoubleValueExpr) other);
        }
    }

    public NumberValueExpr mult(NumberValueExpr other) {
        if (other instanceof IntegerValueExpr) {
            return mult((IntegerValueExpr) other);
        } else if (other instanceof LongValueExpr) {
            return mult((LongValueExpr) other);
        } else {
            return mult((DoubleValueExpr) other);
        }
    }

    public NumberValueExpr div(NumberValueExpr other) {
        if (other instanceof IntegerValueExpr) {
            return div((IntegerValueExpr) other);
        } else if (other instanceof LongValueExpr) {
            return div((LongValueExpr) other);
        } else {
            return div((DoubleValueExpr) other);
        }
    }

    public NumberValueExpr mod(NumberValueExpr other) {
        return new DoubleValueExpr(value.doubleValue() % other.value.doubleValue());
    }

    public NumberValueExpr ceil() {
        return new DoubleValueExpr(Math.ceil(value.doubleValue()));
    }

    public NumberValueExpr floor() {
        return new DoubleValueExpr(Math.floor(value.doubleValue()));
    }

    public NumberValueExpr pow(NumberValueExpr exp) {
        return new DoubleValueExpr(Math.pow(value.doubleValue(), exp.value.doubleValue()));
    }

    public NumberValueExpr round() {
        return new LongValueExpr(Math.round(value.doubleValue()));
    }

    public abstract NumberValueExpr inc();
    public abstract NumberValueExpr dec();

    public abstract NumberValueExpr abs();

    public abstract NumberValueExpr add(IntegerValueExpr other);
    public abstract NumberValueExpr add(LongValueExpr other);
    public abstract NumberValueExpr add(DoubleValueExpr other);

    public abstract NumberValueExpr sub(IntegerValueExpr other);
    public abstract NumberValueExpr sub(LongValueExpr other);
    public abstract NumberValueExpr sub(DoubleValueExpr other);

    public abstract NumberValueExpr mult(IntegerValueExpr other);
    public abstract NumberValueExpr mult(LongValueExpr other);
    public abstract NumberValueExpr mult(DoubleValueExpr other);

    public abstract NumberValueExpr div(IntegerValueExpr other);
    public abstract NumberValueExpr div(LongValueExpr other);
    public abstract NumberValueExpr div(DoubleValueExpr other);

    @Override
    public String getStringValue() {
        return value.toString();
    }

    @Override
    public void prettyPrint(StringBuilder out, int level) {
        out.append(getStringValue());
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
