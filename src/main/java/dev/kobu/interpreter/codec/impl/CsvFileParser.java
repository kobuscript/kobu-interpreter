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
import dev.kobu.interpreter.ast.eval.context.EvalContext;
import dev.kobu.interpreter.ast.eval.expr.value.ArrayValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.FileValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.StringValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.number.IntegerValueExpr;
import dev.kobu.interpreter.ast.symbol.ModuleScope;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.ast.symbol.Type;
import dev.kobu.interpreter.ast.symbol.array.ArrayTypeFactory;
import dev.kobu.interpreter.ast.utils.RecordFactory;
import dev.kobu.interpreter.error.eval.BuiltinFunctionError;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class CsvFileParser {

    public static final String CSV_FILE_TYPE = "CsvFile";

    private static final String CSV_ROW_TYPE = "CsvRow";

    private static final String CSV_COLUMN_TYPE = "CsvColumn";

    public static ValueExpr parse(ModuleScope moduleScope, EvalContext evalContext, String filePath, InputStream in,
                                  StringValueExpr formatExpr, StringValueExpr charsetExpr,
                                  SourceCodeRef sourceCodeRef) {
        var record = RecordFactory.create(moduleScope, evalContext, CSV_FILE_TYPE);
        FileValueExpr fileExpr = new FileValueExpr(new File(filePath));
        record.updateFieldValue(evalContext, "file", fileExpr);

        CSVFormat csvFormat;
        if (formatExpr != null) {
            csvFormat = CSVFormat.valueOf(formatExpr.getValue());
        } else {
            csvFormat = CSVFormat.DEFAULT;
        }

        Charset charset = Charset.defaultCharset();
        if (charsetExpr != null) {
            charset = Charset.forName(charsetExpr.getValue());
        }

        InputStreamReader reader = new InputStreamReader(in, charset);

        try {
            Iterable<CSVRecord> csvRows = csvFormat.parse(reader);
            List<ValueExpr> rows = new ArrayList<>();
            int rowIndex = 0;
            for (CSVRecord csvRow : csvRows) {
                var rowRecord = RecordFactory.create(moduleScope, evalContext, CSV_ROW_TYPE);
                rowRecord.updateFieldValue(evalContext, "index", new IntegerValueExpr(rowIndex));
                List<ValueExpr> cols = new ArrayList<>();
                int colIndex = 0;
                for (String column : csvRow) {
                    var colRecord = RecordFactory.create(moduleScope, evalContext, CSV_COLUMN_TYPE);
                    colRecord.updateFieldValue(evalContext, "rowIndex", new IntegerValueExpr(rowIndex));
                    colRecord.updateFieldValue(evalContext, "index", new IntegerValueExpr(colIndex));
                    colRecord.updateFieldValue(evalContext, "value", new StringValueExpr(column));
                    cols.add(colRecord);

                    colIndex++;
                }
                rowRecord.updateFieldValue(evalContext, "columns",
                        new ArrayValueExpr(ArrayTypeFactory.getArrayTypeFor((Type) moduleScope.resolve(CSV_COLUMN_TYPE)), cols));
                rows.add(rowRecord);
                rowIndex++;
            }
            record.updateFieldValue(evalContext, "rows",
                    new ArrayValueExpr(ArrayTypeFactory.getArrayTypeFor((Type) moduleScope.resolve(CSV_ROW_TYPE)), rows));

        } catch (IOException ex) {
            throw new BuiltinFunctionError(ex, sourceCodeRef);
        }

        return record;
    }

}
