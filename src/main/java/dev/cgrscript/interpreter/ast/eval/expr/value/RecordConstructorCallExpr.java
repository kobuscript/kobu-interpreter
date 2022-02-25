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

import dev.cgrscript.interpreter.ast.eval.EvalContext;
import dev.cgrscript.interpreter.ast.eval.Expr;
import dev.cgrscript.interpreter.ast.eval.ValueExpr;
import dev.cgrscript.interpreter.ast.eval.expr.RecordFieldExpr;
import dev.cgrscript.interpreter.ast.symbol.*;
import dev.cgrscript.interpreter.error.analyzer.InvalidRecordFieldError;
import dev.cgrscript.interpreter.error.analyzer.InvalidRecordFieldTypeError;
import dev.cgrscript.interpreter.error.analyzer.UndefinedTypeError;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RecordConstructorCallExpr implements Expr {

    private final SourceCodeRef sourceCodeRef;

    private final String moduleAlias;

    private final String recordTypeName;

    private final List<RecordFieldExpr> fields = new ArrayList<>();

    private Type type;

    public RecordConstructorCallExpr(SourceCodeRef sourceCodeRef, String moduleAlias, String recordTypeName) {
        this.sourceCodeRef = sourceCodeRef;
        this.moduleAlias = moduleAlias;
        this.recordTypeName = recordTypeName;
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
        return type;
    }

    @Override
    public void analyze(EvalContext context) {
        Symbol symbolType;

        if (moduleAlias == null) {
            symbolType = context.getCurrentScope().resolve(recordTypeName);
        } else {
            ModuleRefSymbol moduleRefSymbol = (ModuleRefSymbol) context.getModuleScope().resolveLocal(moduleAlias);
            if (moduleRefSymbol == null) {
                context.addAnalyzerError(new UndefinedTypeError(sourceCodeRef, moduleAlias + "." + recordTypeName,
                        context.getNewGlobalDefinitionOffset()));
                type = UnknownType.INSTANCE;
                return;
            }
            symbolType = moduleRefSymbol.getModuleScope().resolve(recordTypeName);
        }

        if (!(symbolType instanceof RecordTypeSymbol)) {
            context.addAnalyzerError(new UndefinedTypeError(sourceCodeRef, recordTypeName,
                    context.getNewGlobalDefinitionOffset()));
            type = UnknownType.INSTANCE;
            return;
        } else {
            type = (RecordTypeSymbol) symbolType;
        }

        RecordTypeSymbol recordType = (RecordTypeSymbol) symbolType;

        for (RecordFieldExpr fieldExpr : fields) {
            Type fieldType = recordType.resolveField(fieldExpr.getFieldName());
            if (fieldType != null) {
                fieldExpr.setTargetType(fieldType);
            }
            fieldExpr.analyze(context);
            var exprType = fieldExpr.getType();
            if (fieldType == null) {
                context.addAnalyzerError(new InvalidRecordFieldError(fieldExpr.getSourceCodeRef(),
                        recordType, fieldExpr.getFieldName()));
                continue;
            }
            if (!(exprType instanceof UnknownType) && !fieldType.isAssignableFrom(fieldExpr.getType())) {
                context.addAnalyzerError(new InvalidRecordFieldTypeError(fieldExpr.getSourceCodeRef(),
                        recordType, fieldExpr.getFieldName(), fieldExpr.getType()));
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

        return new RecordValueExpr(type, fieldValues, id);
    }

}
