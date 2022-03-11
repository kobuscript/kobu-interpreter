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

import dev.cgrscript.interpreter.ast.eval.context.EvalContext;
import dev.cgrscript.interpreter.ast.eval.HasMethods;
import dev.cgrscript.interpreter.ast.eval.ValueExpr;
import dev.cgrscript.interpreter.ast.eval.context.SnapshotValue;
import dev.cgrscript.interpreter.ast.symbol.*;

import java.util.Objects;

public class StringValueExpr implements ValueExpr, HasMethods {

    private SourceCodeRef sourceCodeRef;

    private final String value;

    private final StringTypeSymbol type = BuiltinScope.STRING_TYPE;

    public StringValueExpr(String value) {
        this.value = value;
    }

    public StringValueExpr(SourceCodeRef sourceCodeRef, String value) {
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

    public String getValue() {
        return value;
    }

    @Override
    public FunctionType resolveMethod(String methodName) {
        return type.resolveMethod(methodName);
    }

    @Override
    public String getStringValue() {
        return '"' + value + '"';
    }

    @Override
    public SnapshotValue getSnapshotValue() {
        return new StringSnapshotValue(this.value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StringValueExpr valueExpr = (StringValueExpr) o;
        return value.equals(valueExpr.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    private static class StringSnapshotValue implements SnapshotValue {

        private final String value;

        public StringSnapshotValue(String value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            StringSnapshotValue that = (StringSnapshotValue) o;
            return Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

    }
}
