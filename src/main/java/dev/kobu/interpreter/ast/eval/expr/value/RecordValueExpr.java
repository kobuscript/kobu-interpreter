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
import dev.kobu.interpreter.ast.eval.context.EvalContext;
import dev.kobu.interpreter.ast.eval.context.SnapshotValue;
import dev.kobu.interpreter.ast.eval.HasFields;
import dev.kobu.interpreter.ast.eval.HasMethods;
import dev.kobu.interpreter.ast.eval.ValueExpr;
import dev.kobu.interpreter.ast.symbol.RuleSymbol;
import dev.kobu.interpreter.ast.symbol.function.NamedFunction;
import dev.kobu.interpreter.ast.symbol.RecordTypeSymbol;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.ast.symbol.Type;

import java.util.*;
import java.util.stream.Collectors;

public class RecordValueExpr implements ValueExpr, HasFields, HasMethods, Fact {

    private final Type type;

    private final Map<String, ValueExpr> fieldValues;

    private final int id;

    private final boolean printId;

    private int version;

    private int creatorId;

    private boolean initialValue;

    private int iteration;

    private RuleSymbol originRule;

    public RecordValueExpr(Type type, Map<String, ValueExpr> fieldValues, int id, boolean printId) {
        this.type = type;
        this.fieldValues = new LinkedHashMap<>(fieldValues);
        this.id = id;
        this.printId = printId;
    }

    public RecordValueExpr(Type type, Map<String, ValueExpr> fieldValues, int id) {
        this(type, fieldValues, id, true);
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
        return null;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public ValueExpr resolveField(String fieldName) {
        ValueExpr value = fieldValues.get(fieldName);
        if (value instanceof NullValueExpr) {
            return null;
        }
        return value;
    }

    @Override
    public void updateFieldValue(EvalContext context, String fieldName, ValueExpr value) {
        fieldValues.put(fieldName, value);
    }

    @Override
    public NamedFunction resolveMethod(String methodName) {
        return null;
    }

    @Override
    public String getStringValue(Set<Integer> idSet) {
        String strValue = ((RecordTypeSymbol) type).getNameInModule();
        if (idSet.add(this.id)) {
            if (fieldValues.isEmpty()) {
                if (!printId) {
                    return strValue + "{}";
                }
                return strValue + "{@id: " + this.id + "}";
            }
            if (!printId) {
                return strValue + "{" +
                        fieldValues.entrySet().stream()
                                .map(e -> e.getKey() + ": " + e.getValue().getStringValue(idSet))
                                .collect(Collectors.joining(", ")) +
                        "}";
            }
            return strValue + "{@id: " +
                    this.id + ",  " +
                    fieldValues.entrySet().stream()
                            .map(e -> e.getKey() + ": " + e.getValue().getStringValue(idSet))
                            .collect(Collectors.joining(", ")) +
                    "}";
        }
        return strValue + "{@id: " + this.id + "}";
    }

    @Override
    public void prettyPrint(Set<Integer> idSet, StringBuilder out, int level) {
        out.append(((RecordTypeSymbol) type).getNameInModule());
        out.append("{\n");
        if (idSet.add(this.id)) {
            int count = 0;
            if (printId) {
                out.append(" ".repeat((level + 1) * PRETTY_PRINT_TAB_SIZE));
                out.append("@id: ");
                out.append(this.id);
                count = 1;
            }
            for (Map.Entry<String, ValueExpr> entry : fieldValues.entrySet()) {
                if (count > 0) {
                    out.append(",\n");
                }
                out.append(" ".repeat((level + 1) * PRETTY_PRINT_TAB_SIZE));
                out.append(entry.getKey());
                out.append(": ");
                entry.getValue().prettyPrint(idSet, out, level + 1);
                count++;
            }
        } else if (printId) {
            out.append(" ".repeat((level + 1) * PRETTY_PRINT_TAB_SIZE));
            out.append("@id: ");
            out.append(this.id);
        }
        out.append('\n');
        out.append(" ".repeat(level * PRETTY_PRINT_TAB_SIZE));
        out.append('}');
    }

    @Override
    public SnapshotValue getSnapshotValue() {
        return new RecordSnapshotValue(this.id, this.version);
    }

    @Override
    public int getId() {
        return id;
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
    public RuleSymbol getOriginRule() {
        return originRule;
    }

    @Override
    public void setOriginRule(RuleSymbol originRule) {
        this.originRule = originRule;
    }

    public int getVersion() {
        return version;
    }

    public void incVersion() {
        this.version++;
    }

    public void setInitialValue() {
        initialValue = true;
    }

    public boolean initialValue() {
        return initialValue;
    }

    public List<ValueExpr> getValues() {
        return new ArrayList<>(fieldValues.values());
    }

    public List<String> getFields() {
        return new ArrayList<>(fieldValues.keySet());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecordValueExpr that = (RecordValueExpr) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    private static class RecordSnapshotValue implements SnapshotValue {

        private final int id;

        private final int version;

        public RecordSnapshotValue(int id, int version) {
            this.id = id;
            this.version = version;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RecordSnapshotValue that = (RecordSnapshotValue) o;
            return id == that.id && version == that.version;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, version);
        }

    }
}
