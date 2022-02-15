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

package dev.cgrscript.interpreter.input.parser;

import dev.cgrscript.antlr.csv.CSVBaseVisitor;
import dev.cgrscript.antlr.csv.CSVParser;
import dev.cgrscript.interpreter.ast.eval.EvalContext;
import dev.cgrscript.interpreter.ast.eval.ValueExpr;
import dev.cgrscript.interpreter.ast.eval.expr.value.ArrayValueExpr;
import dev.cgrscript.interpreter.ast.eval.expr.value.NumberValueExpr;
import dev.cgrscript.interpreter.ast.eval.expr.value.RecordValueExpr;
import dev.cgrscript.interpreter.ast.eval.expr.value.StringValueExpr;
import dev.cgrscript.interpreter.ast.symbol.ArrayType;
import dev.cgrscript.interpreter.ast.symbol.Type;
import dev.cgrscript.interpreter.ast.utils.RecordFactory;

import java.util.ArrayList;
import java.util.List;

public class CsvParserVisitor extends CSVBaseVisitor<ValueExpr> {

    public static final String CSV_FILE_TYPE = "CsvFile";

    private static final String CSV_ROW_TYPE = "CsvRow";

    private static final String CSV_COLUMN_TYPE = "CsvColumn";

    private final EvalContext context;

    private final String filePath;

    private final String fileName;

    public CsvParserVisitor(EvalContext context, String filePath, String fileName) {
        this.context = context;
        this.filePath = filePath;
        this.fileName = fileName;
    }

    @Override
    public ValueExpr visitCsvFile(CSVParser.CsvFileContext ctx) {
        var record = RecordFactory.create(context, CSV_FILE_TYPE);
        if (filePath != null) {
            record.updateFieldValue(context, "filePath", new StringValueExpr(filePath));
        }
        if (fileName != null) {
            record.updateFieldValue(context, "fileName", new StringValueExpr(fileName));
        }

        List<ValueExpr> rows = new ArrayList<>();
        if (ctx.hdr() != null) {
            RecordValueExpr rowRec = (RecordValueExpr) visit(ctx.hdr());
            addIndex(0, rowRec);
            rows.add(rowRec);
        }
        var rowType = context.getModuleScope().resolve(CSV_ROW_TYPE);
        ArrayType rowArrayType = new ArrayType((Type) rowType);
        if (ctx.row() != null) {
            int idx = 1;
            for (CSVParser.RowContext rowContext : ctx.row()) {
                RecordValueExpr rowRec = (RecordValueExpr) visit(rowContext);
                addIndex(idx++, rowRec);
                rows.add(rowRec);
            }
            record.updateFieldValue(context, "rows", new ArrayValueExpr(rowArrayType, rows));
        }

        return record;
    }

    private void addIndex(int idx, RecordValueExpr rowRec) {
        NumberValueExpr rowIndex = new NumberValueExpr(idx);
        rowRec.updateFieldValue(context, "index", rowIndex);
        for (ValueExpr colExpr : ((ArrayValueExpr) rowRec.resolveField("columns")).getValue()) {
            ((RecordValueExpr)colExpr).updateFieldValue(context, "rowIndex", rowIndex);
        }
    }

    @Override
    public ValueExpr visitRow(CSVParser.RowContext ctx) {
        var record = RecordFactory.create(context, CSV_ROW_TYPE);
        List<ValueExpr> columns = new ArrayList<>();
        if (ctx.field() != null) {
            int index = 0;
            for (CSVParser.FieldContext fieldContext : ctx.field()) {
                var columnRec = RecordFactory.create(context, CSV_COLUMN_TYPE);
                StringValueExpr fieldValue = (StringValueExpr) visit(fieldContext);
                columnRec.updateFieldValue(context, "index", new NumberValueExpr(index++));
                columnRec.updateFieldValue(context, "value", fieldValue);
                columns.add(columnRec);
            }
        }
        record.updateFieldValue(context, "columns",
                new ArrayValueExpr(new ArrayType((Type) context.getModuleScope().resolve(CSV_COLUMN_TYPE)), columns));
        return record;
    }

    @Override
    public ValueExpr visitField(CSVParser.FieldContext ctx) {
        if (ctx.TEXT() != null) {
            return new StringValueExpr(ctx.TEXT().getText());
        } else if (ctx.STRING() != null) {
            return new StringValueExpr(ctx.STRING().getText()
                    .replaceAll("^\"", "")
                    .replaceAll("\"$", "")
                    .replaceAll("\"\"", "\""));
        }
        return new StringValueExpr("");
    }
}
