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

import dev.kobu.database.Fact;
import dev.kobu.interpreter.ast.eval.ValueExpr;
import dev.kobu.interpreter.ast.eval.context.EvalContext;
import dev.kobu.interpreter.ast.eval.context.SnapshotValue;
import dev.kobu.interpreter.ast.symbol.BuiltinScope;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.ast.symbol.TemplateTypeSymbol;
import dev.kobu.interpreter.ast.symbol.Type;
import dev.kobu.interpreter.ast.template.TemplateExecutor;

import java.util.Objects;

public class TemplateValueExpr implements ValueExpr, Fact {

    private final int id;

    private final TemplateExecutor templateExecutor;

    private final RecordValueExpr rootRecord;

    private int creatorId;

    private final TemplateTypeSymbol type = BuiltinScope.TEMPLATE_TYPE;

    private String value;

    private int iteration;

    private String originRule;

    public TemplateValueExpr(int id, TemplateExecutor templateExecutor, RecordValueExpr rootRecord, int creatorId) {
        this.id = id;
        this.templateExecutor = templateExecutor;
        this.rootRecord = rootRecord;
        this.creatorId = creatorId;
    }

    @Override
    public int getId() {
        return id;
    }

    public RecordValueExpr getRootRecord() {
        return rootRecord;
    }

    public String getValue() {
        if (value == null) {
            value = templateExecutor.execute();
        }
        return value;
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
        return type;
    }

    @Override
    public ValueExpr evalExpr(EvalContext context) {
        return this;
    }

    @Override
    public String getStringValue() {
        return getValue();
    }

    @Override
    public SnapshotValue getSnapshotValue() {
        return new TemplateSnapshotValue(this.id);
    }

    @Override
    public int getCreatorId() {
        return creatorId;
    }

    @Override
    public void setCreatorId(int id) {
        this.creatorId = id;
    }

    @Override
    public int getIteration() {
        return iteration;
    }

    @Override
    public void setIteration(int iteration) {
        this.iteration = iteration;
    }

    @Override
    public String getOriginRule() {
        return originRule;
    }

    @Override
    public void setOriginRule(String originRule) {
        this.originRule = originRule;
    }

    public StringValueExpr trim() {
        return new StringValueExpr(getValue().trim());
    }

    private static class TemplateSnapshotValue implements SnapshotValue {

        private final int id;

        public TemplateSnapshotValue(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TemplateSnapshotValue that = (TemplateSnapshotValue) o;
            return id == that.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

    }
}