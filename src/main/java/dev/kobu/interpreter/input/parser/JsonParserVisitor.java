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

import dev.kobu.antlr.json.JSONBaseVisitor;
import dev.kobu.antlr.json.JSONParser;
import dev.kobu.interpreter.ast.eval.ValueExpr;
import dev.kobu.interpreter.ast.eval.context.EvalContext;
import dev.kobu.interpreter.ast.eval.expr.value.*;
import dev.kobu.interpreter.ast.eval.expr.value.number.NumberValueFactory;
import dev.kobu.interpreter.ast.symbol.*;
import dev.kobu.interpreter.ast.symbol.array.ArrayType;
import dev.kobu.interpreter.ast.symbol.array.ArrayTypeFactory;
import dev.kobu.interpreter.ast.symbol.tuple.TupleType;
import dev.kobu.interpreter.ast.symbol.tuple.TupleTypeElement;
import dev.kobu.interpreter.ast.utils.RecordFactory;
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

    private Type currentType;

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
        var record = RecordFactory.create(context, (RecordTypeSymbol) currentType);

        if (ctx.pair() != null) {
            Type objType = currentType;
            for (JSONParser.PairContext pairContext : ctx.pair()) {
                var field = StringFunctions.parseLiteralString(pairContext.STRING().getText())
                        .replaceAll("[^a-zA-Z0-9_]+", "_");

                Type fieldType = currentType.resolveField(field);
                if (fieldType != null) {
                    path.push(field);
                    currentType = fieldType;
                    ValueExpr fieldValue = visit(pairContext.value());
                    if (fieldType.isAssignableFrom(fieldValue.getType())) {
                        record.updateFieldValue(context, field, fieldValue);
                    } else {
                        throwInvalidPathError(fieldType, fieldValue.getType(), sourceCodeRef);
                    }
                    currentType = objType;
                    path.pop();
                }
            }
        }

        return record;
    }

    @Override
    public ValueExpr visitStringExpr(JSONParser.StringExprContext ctx) {
        return new StringValueExpr(StringFunctions.parseLiteralString(ctx.STRING().getText()));
    }

    @Override
    public ValueExpr visitNumberExpr(JSONParser.NumberExprContext ctx) {
        return NumberValueFactory.parse(ctx.NUMBER().getText());
    }

    @Override
    public ValueExpr visitTrueExpr(JSONParser.TrueExprContext ctx) {
        return BooleanValueExpr.TRUE;
    }

    @Override
    public ValueExpr visitFalseExpr(JSONParser.FalseExprContext ctx) {
        return BooleanValueExpr.FALSE;
    }

    @Override
    public ValueExpr visitNullExpr(JSONParser.NullExprContext ctx) {
        return new NullValueExpr();
    }

    @Override
    public ValueExpr visitArr(JSONParser.ArrContext ctx) {
        if (currentType instanceof TupleType) {
            TupleType tupleType = (TupleType) currentType;
            int index = 0;
            TupleTypeElement element = ((TupleType) currentType).getTypeElement();
            List<ValueExpr> valueExprList = new ArrayList<>();
            if (ctx.value() != null) {
                while (element != null && index < ctx.value().size()) {
                    currentType = element.getElementType();
                    valueExprList.add(visit(ctx.value(index)));
                    currentType = tupleType;
                    element = element.getNext();
                    index++;
                }
            }

            return new TupleValueExpr(tupleType, valueExprList);
        } else {
            Type objType = currentType;
            Type elementType = BuiltinScope.ANY_TYPE;
            if (currentType instanceof ArrayType) {
                elementType = ((ArrayType) currentType).getElementType();
            }

            List<ValueExpr> valueExprList = new ArrayList<>();
            if (ctx.value() != null) {
                for (JSONParser.ValueContext valueContext : ctx.value()) {
                    currentType = elementType;
                    valueExprList.add(visit(valueContext));
                    currentType = objType;
                }
            }
            return new ArrayValueExpr(ArrayTypeFactory.getArrayTypeFor(elementType), valueExprList);
        }
    }

    private void throwInvalidPathError(Type expected, Type found, SourceCodeRef sourceCodeRef) {
        String pathStr = String.join(".", path);
        throw new InvalidCallError(pathStr + ": expected '" + expected.getName() +
                "', but got '" + found.getName() + "'", sourceCodeRef);
    }

}
