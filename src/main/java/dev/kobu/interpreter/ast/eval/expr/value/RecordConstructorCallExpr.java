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

import dev.kobu.interpreter.ast.eval.context.EvalContext;
import dev.kobu.interpreter.ast.eval.Expr;
import dev.kobu.interpreter.ast.eval.ValueExpr;
import dev.kobu.interpreter.ast.eval.expr.RecordFieldExpr;
import dev.kobu.interpreter.ast.symbol.RecordTypeSymbol;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.ast.symbol.Type;
import dev.kobu.interpreter.ast.symbol.UnknownType;
import dev.kobu.interpreter.error.analyzer.InvalidRecordFieldError;
import dev.kobu.interpreter.error.analyzer.InvalidRecordFieldTypeError;
import dev.kobu.interpreter.error.analyzer.UndefinedTypeError;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RecordConstructorCallExpr implements Expr {

    private final SourceCodeRef sourceCodeRef;

    private final List<RecordFieldExpr> fields = new ArrayList<>();

    private final Type recordType;

    public RecordConstructorCallExpr(SourceCodeRef sourceCodeRef, Type recordType) {
        this.sourceCodeRef = sourceCodeRef;
        this.recordType = recordType;
    }

    public void addField(RecordFieldExpr recordField) {
        fields.add(recordField);
    }

    @Override
    public SourceCodeRef getSourceCodeRef() {
        return sourceCodeRef;
    }

    @Override
    public Type getType() {
        return recordType;
    }

    @Override
    public void analyze(EvalContext context) {
        if (!(recordType instanceof RecordTypeSymbol)) {
            context.addAnalyzerError(new UndefinedTypeError(sourceCodeRef, recordType.getName(),
                    context.getNewGlobalDefinitionOffset()));
            return;
        }
        for (RecordFieldExpr fieldExpr : fields) {
            Type fieldType = recordType.resolveField(fieldExpr.getFieldName());
            if (fieldType != null) {
                fieldExpr.setTargetType(fieldType);
            }
            fieldExpr.analyze(context);
            var exprType = fieldExpr.getType();
            if (fieldType == null) {
                context.addAnalyzerError(new InvalidRecordFieldError(fieldExpr.getSourceCodeRef(),
                        (RecordTypeSymbol) recordType, fieldExpr.getFieldName()));
                continue;
            }
            if (!(exprType instanceof UnknownType) && !fieldType.isAssignableFrom(fieldExpr.getType())) {
                context.addAnalyzerError(new InvalidRecordFieldTypeError(fieldExpr.getSourceCodeRef(),
                        (RecordTypeSymbol) recordType, fieldExpr.getFieldName(), fieldExpr.getType()));
            }
        }
    }

    @Override
    public ValueExpr evalExpr(EvalContext context) {
        int id = context.getDatabase().generateRecordId();

        Map<String, ValueExpr> fieldValues = new LinkedHashMap<>();

        for (RecordFieldExpr fieldExpr : fields) {
            ValueExpr fieldValueExpr = fieldExpr.evalExpr(context);

            fieldValues.put(fieldExpr.getFieldName(), fieldValueExpr);
        }

        return new RecordValueExpr(recordType, fieldValues, id);
    }

}
