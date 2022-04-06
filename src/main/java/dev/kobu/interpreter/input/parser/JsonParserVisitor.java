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

package dev.kobu.interpreter.input.parser;

import dev.kobu.interpreter.ast.eval.context.EvalContext;
import dev.kobu.interpreter.ast.eval.ValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.*;
import dev.kobu.interpreter.ast.eval.expr.value.number.NumberValueFactory;
import dev.kobu.interpreter.ast.symbol.ModuleScope;
import dev.kobu.interpreter.ast.symbol.RecordTypeSymbol;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.ast.symbol.Type;
import dev.kobu.interpreter.ast.symbol.array.ArrayTypeFactory;
import dev.kobu.interpreter.ast.utils.RecordFactory;
import dev.kobu.antlr.json.JSONBaseVisitor;
import dev.kobu.antlr.json.JSONParser;
import dev.kobu.interpreter.ast.utils.StringFunctions;
import dev.kobu.interpreter.error.eval.InvalidCallError;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class JsonParserVisitor extends JSONBaseVisitor<ValueExpr> {

    public static final String JSON_FILE_TYPE = "JsonFile";

    private final ModuleScope moduleScope;

    private final EvalContext context;

    private final RecordTypeSymbol recordType;

    private final String filePath;

    private final SourceCodeRef sourceCodeRef;

    private RecordTypeSymbol currentType;

    private final Stack<String> path = new Stack<>();

    public JsonParserVisitor(ModuleScope moduleScope, EvalContext context, RecordTypeSymbol recordType,
                             String filePath, SourceCodeRef sourceCodeRef) {
        this.moduleScope = moduleScope;
        this.context = context;
        this.recordType = recordType;
        this.filePath = filePath;
        this.sourceCodeRef = sourceCodeRef;
    }

    @Override
    public ValueExpr visitJson(JSONParser.JsonContext ctx) {
        this.currentType = recordType;
        var record = RecordFactory.create(moduleScope, context, JSON_FILE_TYPE);
        FileValueExpr fileExpr = new FileValueExpr(new File(filePath));
        record.updateFieldValue(context, "file", fileExpr);
        record.updateFieldValue(context, "json", visit(ctx.value()));

        return record;
    }

    @Override
    public ValueExpr visitObj(JSONParser.ObjContext ctx) {
        var record = RecordFactory.create(context, currentType);

        if (ctx.pair() != null) {
            for (JSONParser.PairContext pairContext : ctx.pair()) {
                var field = StringFunctions.parseLiteralString(pairContext.STRING().getText())
                        .replaceAll("[^a-zA-Z0-9_]+", "_");

                Type fieldType = currentType.resolveField(field);
                if (fieldType != null) {
                    path.push(field);
                    ValueExpr fieldValue = visit(pairContext.value());
                    if (fieldType.isAssignableFrom(fieldValue.getType())) {
                        record.updateFieldValue(context, field, fieldValue);
                    } else {
                        throwInvalidPathError(fieldType, fieldValue.getType(), sourceCodeRef);
                    }
                    path.pop();
                }
            }
        }

        return record;
    }

    @Override
    public ValueExpr visitStringExpr(JSONParser.StringExprContext ctx) {
//        var record = RecordFactory.create(moduleScope, context, JSON_STRING_TYPE);
//        record.updateFieldValue(context, "value", new StringValueExpr(ctx.STRING().getText()));
//        return record;
        return null;
    }

    @Override
    public ValueExpr visitNumberExpr(JSONParser.NumberExprContext ctx) {
//        var record = RecordFactory.create(moduleScope, context, JSON_NUMBER_TYPE);
//        record.updateFieldValue(context, "value", NumberValueFactory.parse(ctx.NUMBER().getText()));
//        return record;
        return null;
    }

    @Override
    public ValueExpr visitTrueExpr(JSONParser.TrueExprContext ctx) {
//        var record = RecordFactory.create(moduleScope, context, JSON_BOOLEAN_TYPE);
//        record.updateFieldValue(context, "value", BooleanValueExpr.TRUE);
//        return record;
        return null;
    }

    @Override
    public ValueExpr visitFalseExpr(JSONParser.FalseExprContext ctx) {
//        var record = RecordFactory.create(moduleScope, context, JSON_BOOLEAN_TYPE);
//        record.updateFieldValue(context, "value", BooleanValueExpr.FALSE);
//        return record;
        return null;
    }

    @Override
    public ValueExpr visitNullExpr(JSONParser.NullExprContext ctx) {
        return new NullValueExpr();
    }

    @Override
    public ValueExpr visitArr(JSONParser.ArrContext ctx) {
//        var record = RecordFactory.create(moduleScope, context, JSON_ARRAY_TYPE);
//
//        List<ValueExpr> values = new ArrayList<>();
//        if (ctx.value() != null) {
//            for (JSONParser.ValueContext valueContext : ctx.value()) {
//                values.add(visit(valueContext));
//            }
//        }
//        var jsonValueType = (Type) context.getModuleScope().resolve(JSON_VALUE_TYPE);
//        record.updateFieldValue(context, "value", new ArrayValueExpr(ArrayTypeFactory.getArrayTypeFor(jsonValueType), values));
//
//        return record;
        return null;
    }

    private void throwInvalidPathError(Type expected, Type found, SourceCodeRef sourceCodeRef) {
        String pathStr = String.join(".", path);
        throw new InvalidCallError(pathStr + ": expected '" + expected.getName() +
                "', but got '" + found.getName() + "'", sourceCodeRef);
    }

}
