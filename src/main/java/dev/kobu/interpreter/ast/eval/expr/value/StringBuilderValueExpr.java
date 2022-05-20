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

import dev.kobu.interpreter.ast.eval.HasMethods;
import dev.kobu.interpreter.ast.eval.ValueExpr;
import dev.kobu.interpreter.ast.eval.context.EvalContext;
import dev.kobu.interpreter.ast.eval.context.SnapshotValue;
import dev.kobu.interpreter.ast.symbol.BuiltinScope;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.ast.symbol.Type;
import dev.kobu.interpreter.ast.symbol.function.NamedFunction;
import dev.kobu.interpreter.ast.symbol.StringBuilderTypeSymbol;
import dev.kobu.interpreter.ast.utils.StringFunctions;

import java.util.Objects;
import java.util.Set;

public class StringBuilderValueExpr implements ValueExpr, HasMethods {

    private SourceCodeRef sourceCodeRef;

    private final StringBuilder value;

    private final StringBuilderTypeSymbol type = BuiltinScope.STRING_BUILDER_TYPE;

    public StringBuilderValueExpr(StringBuilder value) {
        this.value = value;
    }

    public StringBuilderValueExpr(SourceCodeRef sourceCodeRef, StringBuilder value) {
        this.sourceCodeRef = sourceCodeRef;
        this.value = value;
    }

    public StringBuilder getValue() {
        return value;
    }

    @Override
    public SourceCodeRef getSourceCodeRef() {
        return sourceCodeRef;
    }

    @Override
    public void analyze(EvalContext context) {

    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public ValueExpr evalExpr(EvalContext context) {
        return this;
    }

    @Override
    public NamedFunction resolveMethod(String methodName) {
        return type.resolveMethod(methodName);
    }

    @Override
    public String getStringValue(Set<Integer> idSet) {
        return '"' + StringFunctions.encodeString(value.toString()) + '"';
    }

    @Override
    public void prettyPrint(Set<Integer> idSet, StringBuilder out, int level) {
        out.append(getStringValue(idSet));
    }

    @Override
    public SnapshotValue getSnapshotValue() {
        return new StringBuilderSnapshotValue(this.value);
    }

    private static class StringBuilderSnapshotValue implements SnapshotValue {

        private final StringBuilder value;

        public StringBuilderSnapshotValue(StringBuilder value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            StringBuilderSnapshotValue that = (StringBuilderSnapshotValue) o;
            return value == that.value;
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

    }
}
