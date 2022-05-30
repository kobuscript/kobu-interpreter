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

package dev.kobu.interpreter.codec.impl;

import dev.kobu.interpreter.ast.eval.ValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.ArrayValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.RecordValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.TupleValueExpr;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.error.eval.BuiltinFunctionError;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.List;

public class JsonWriter {

    private final static int TAB_SIZE = 4;

    private final SourceCodeRef sourceCodeRef;

    private final ValueExpr sourceExpr;

    public JsonWriter(SourceCodeRef sourceCodeRef, ValueExpr sourceExpr) {
        this.sourceCodeRef = sourceCodeRef;
        this.sourceExpr = sourceExpr;
    }

    public void write(OutputStream out, Charset charset) {

        try {
            OutputStreamWriter writer = new OutputStreamWriter(out, charset);

            appendJson(writer, 0, sourceExpr);
            writer.flush();
        } catch (IOException ex) {
            throw new BuiltinFunctionError(ex, sourceCodeRef);
        }

    }

    private void appendJson(OutputStreamWriter writer, int level, RecordValueExpr recordValueExpr) throws IOException {
        writer.write("{\n");
        int count = 0;
        for (String field : recordValueExpr.getFields()) {
            if (count > 0) {
                writer.write(",\n");
            }
            appendJson(writer, level, field, recordValueExpr.resolveField(field));
            count++;
        }
        writer.write('\n');
        for (int i = 0; i < (level - 1) * TAB_SIZE; i++) {
            writer.write(' ');
        }
        writer.write("}");
    }

    private void appendJson(OutputStreamWriter writer, int level, ArrayValueExpr arrayValueExpr) throws IOException {
        List<ValueExpr> valueExprList = arrayValueExpr.getValue();

        appendJson(writer, valueExprList, level);
    }

    private void appendJson(OutputStreamWriter writer, int level, TupleValueExpr tupleValueExpr) throws IOException {
        List<ValueExpr> valueExprList = tupleValueExpr.getValueExprList();

        appendJson(writer, valueExprList, level);
    }

    private void appendJson(OutputStreamWriter writer, List<ValueExpr> valueExprList, int level) throws IOException {
        writer.write("[\n");
        for (int i = 0; i < level * TAB_SIZE; i++) {
            writer.write(' ');
        }
        int count = 0;
        for (ValueExpr valueExpr : valueExprList) {
            if (count > 0) {
                writer.write(", ");
            }
            appendJson(writer, level, valueExpr);
            count++;
        }
        writer.write('\n');
        for (int i = 0; i < (level - 1) * TAB_SIZE; i++) {
            writer.write(' ');
        }
        writer.write(']');
    }

    private void appendJson(OutputStreamWriter writer, int level, String key, ValueExpr valueExpr) throws IOException {
        for (int i = 0; i < level * TAB_SIZE; i++) {
            writer.write(' ');
        }
        writer.write('"');
        writer.write(key);
        writer.write("\": ");

        appendJson(writer, level, valueExpr);
    }

    private void appendJsonScalarValue(OutputStreamWriter writer, ValueExpr valueExpr) throws IOException {
        writer.write(valueExpr.getStringValue(new HashSet<>()));
    }

    private void appendJson(OutputStreamWriter writer, int level, ValueExpr valueExpr) throws IOException {
        if (valueExpr instanceof RecordValueExpr) {
            appendJson(writer, level + 1, (RecordValueExpr) valueExpr);
        } else if (valueExpr instanceof ArrayValueExpr) {
            appendJson(writer, level + 1, (ArrayValueExpr) valueExpr);
        } else if (valueExpr instanceof TupleValueExpr) {
            appendJson(writer, level + 1, (TupleValueExpr) valueExpr);
        } else {
            appendJsonScalarValue(writer, valueExpr);
        }
    }

}
