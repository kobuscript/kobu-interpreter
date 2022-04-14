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
import dev.kobu.interpreter.ast.symbol.value.DateFormatterTypeSymbol;

import java.text.SimpleDateFormat;
import java.util.Objects;

public class DateFormatterValueExpr implements ValueExpr, HasMethods {

    private SourceCodeRef sourceCodeRef;

    private final SimpleDateFormat dateFormatter;

    private final DateFormatterTypeSymbol type = BuiltinScope.DATE_FORMATTER_TYPE;

    public DateFormatterValueExpr(SimpleDateFormat dateFormatter) {
        this.dateFormatter = dateFormatter;
    }

    public DateFormatterValueExpr(SourceCodeRef sourceCodeRef, SimpleDateFormat dateFormatter) {
        this.sourceCodeRef = sourceCodeRef;
        this.dateFormatter = dateFormatter;
    }

    public SimpleDateFormat getDateFormatter() {
        return dateFormatter;
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
    public String getStringValue() {
        return "DateTimeFormatter(" + dateFormatter.toPattern() + ")";
    }

    @Override
    public void prettyPrint(StringBuilder out, int level) {
        out.append(getStringValue());
    }

    @Override
    public SnapshotValue getSnapshotValue() {
        return new DateFormatterSnapshotValue(dateFormatter.toPattern());
    }

    private static class DateFormatterSnapshotValue implements SnapshotValue {

        private final String pattern;

        public DateFormatterSnapshotValue(String pattern) {
            this.pattern = pattern;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DateFormatterSnapshotValue that = (DateFormatterSnapshotValue) o;
            return Objects.equals(pattern, that.pattern);
        }

        @Override
        public int hashCode() {
            return Objects.hash(pattern);
        }

    }
}
