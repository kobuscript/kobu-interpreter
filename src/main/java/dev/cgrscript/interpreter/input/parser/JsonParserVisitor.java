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

import dev.cgrscript.interpreter.ast.eval.context.EvalContext;
import dev.cgrscript.interpreter.ast.eval.ValueExpr;
import dev.cgrscript.interpreter.ast.eval.expr.value.ArrayValueExpr;
import dev.cgrscript.interpreter.ast.eval.expr.value.BooleanValueExpr;
import dev.cgrscript.interpreter.ast.eval.expr.value.NullValueExpr;
import dev.cgrscript.interpreter.ast.eval.expr.value.StringValueExpr;
import dev.cgrscript.interpreter.ast.symbol.ArrayType;
import dev.cgrscript.interpreter.ast.symbol.Type;
import dev.cgrscript.interpreter.ast.utils.NumberParser;
import dev.cgrscript.interpreter.ast.utils.RecordFactory;
import dev.cgrscript.antlr.json.JSONBaseVisitor;
import dev.cgrscript.antlr.json.JSONParser;

import java.util.ArrayList;
import java.util.List;

public class JsonParserVisitor extends JSONBaseVisitor<ValueExpr> {

    public static final String JSON_FILE_TYPE = "JsonFile";

    private static final String JSON_VALUE_TYPE = "JsonValue";

    private static final String JSON_OBJECT_TYPE = "JsonObject";

    private static final String JSON_STRING_TYPE = "JsonString";

    private static final String JSON_NUMBER_TYPE = "JsonNumber";

    private static final String JSON_BOOLEAN_TYPE = "JsonBoolean";

    private static final String JSON_ARRAY_TYPE = "JsonArray";

    private final EvalContext context;

    private final String filePath;

    private final String fileName;

    public JsonParserVisitor(EvalContext context, String filePath, String fileName) {
        this.context = context;
        this.filePath = filePath;
        this.fileName = fileName;
    }

    @Override
    public ValueExpr visitJson(JSONParser.JsonContext ctx) {
        var record = RecordFactory.create(context, JSON_FILE_TYPE);
        if (filePath != null) {
            record.updateFieldValue(context, "filePath", new StringValueExpr(filePath));
        }
        if (fileName != null) {
            record.updateFieldValue(context, "fileName", new StringValueExpr(fileName));
        }

        record.updateFieldValue(context, "json", visit(ctx.value()));

        return record;
    }

    @Override
    public ValueExpr visitObj(JSONParser.ObjContext ctx) {
        var record = RecordFactory.create(context, JSON_OBJECT_TYPE);

        if (ctx.pair() != null) {
            for (JSONParser.PairContext pairContext : ctx.pair()) {
                var field = pairContext.STRING().getText()
                        .replaceAll("[^a-zA-Z0-9_]+", "_");
                record.updateFieldValue(context, field, visit(pairContext.value()));
            }
        }

        return record;
    }

    @Override
    public ValueExpr visitStringExpr(JSONParser.StringExprContext ctx) {
        var record = RecordFactory.create(context, JSON_STRING_TYPE);
        record.updateFieldValue(context, "value", new StringValueExpr(ctx.STRING().getText()));
        return record;
    }

    @Override
    public ValueExpr visitNumberExpr(JSONParser.NumberExprContext ctx) {
        var record = RecordFactory.create(context, JSON_NUMBER_TYPE);
        record.updateFieldValue(context, "value", NumberParser.getNumberValueExpr(ctx.NUMBER().getText()));
        return record;
    }

    @Override
    public ValueExpr visitTrueExpr(JSONParser.TrueExprContext ctx) {
        var record = RecordFactory.create(context, JSON_BOOLEAN_TYPE);
        record.updateFieldValue(context, "value", new BooleanValueExpr(true));
        return record;
    }

    @Override
    public ValueExpr visitFalseExpr(JSONParser.FalseExprContext ctx) {
        var record = RecordFactory.create(context, JSON_BOOLEAN_TYPE);
        record.updateFieldValue(context, "value", new BooleanValueExpr(false));
        return record;
    }

    @Override
    public ValueExpr visitNullExpr(JSONParser.NullExprContext ctx) {
        return new NullValueExpr();
    }

    @Override
    public ValueExpr visitArr(JSONParser.ArrContext ctx) {
        var record = RecordFactory.create(context, JSON_ARRAY_TYPE);

        List<ValueExpr> values = new ArrayList<>();
        if (ctx.value() != null) {
            for (JSONParser.ValueContext valueContext : ctx.value()) {
                values.add(visit(valueContext));
            }
        }
        var jsonValueType = (Type) context.getModuleScope().resolve(JSON_VALUE_TYPE);
        record.updateFieldValue(context, "value", new ArrayValueExpr(new ArrayType(jsonValueType), values));

        return record;
    }

}
