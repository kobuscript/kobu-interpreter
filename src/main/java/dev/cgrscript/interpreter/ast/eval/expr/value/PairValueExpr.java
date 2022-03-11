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
import dev.cgrscript.interpreter.ast.symbol.FunctionType;
import dev.cgrscript.interpreter.ast.symbol.PairType;
import dev.cgrscript.interpreter.ast.symbol.SourceCodeRef;
import dev.cgrscript.interpreter.ast.symbol.Type;

import java.util.Objects;

public class PairValueExpr implements ValueExpr, HasMethods {

    private final PairType type;

    private final ValueExpr leftValue;

    private final ValueExpr rightValue;

    public PairValueExpr(PairType type, ValueExpr leftValue, ValueExpr rightValue) {
        this.type = type;
        this.leftValue = leftValue;
        this.rightValue = rightValue;
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
    public FunctionType resolveMethod(String methodName) {
        return type.resolveMethod(methodName);
    }

    public ValueExpr getLeftValue() {
        return leftValue;
    }

    public ValueExpr getRightValue() {
        return rightValue;
    }

    @Override
    public String getStringValue() {
        return "(" + leftValue.getStringValue() + ", " + rightValue.getStringValue() + ")";
    }

    @Override
    public SnapshotValue getSnapshotValue() {
        SnapshotValue left = leftValue != null ? leftValue.getSnapshotValue() : null;
        SnapshotValue right = rightValue != null ? rightValue.getSnapshotValue() : null;
        return new PairSnapshotValue(left, right);
    }

    private static class PairSnapshotValue implements SnapshotValue {

        private final SnapshotValue snapshotLeft;

        private final SnapshotValue snapshotRight;

        private PairSnapshotValue(SnapshotValue snapshotLeft, SnapshotValue snapshotRight) {
            this.snapshotLeft = snapshotLeft;
            this.snapshotRight = snapshotRight;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PairSnapshotValue that = (PairSnapshotValue) o;
            return Objects.equals(snapshotLeft, that.snapshotLeft) && Objects.equals(snapshotRight, that.snapshotRight);
        }

        @Override
        public int hashCode() {
            return Objects.hash(snapshotLeft, snapshotRight);
        }

    }

}
