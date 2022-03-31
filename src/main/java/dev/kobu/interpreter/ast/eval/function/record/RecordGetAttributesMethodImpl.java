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

package dev.kobu.interpreter.ast.eval.function.record;

import dev.kobu.interpreter.ast.eval.FieldDescriptor;
import dev.kobu.interpreter.ast.eval.ValueExpr;
import dev.kobu.interpreter.ast.eval.context.EvalContext;
import dev.kobu.interpreter.ast.eval.expr.value.ArrayValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.RecordValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.StringValueExpr;
import dev.kobu.interpreter.ast.eval.function.BuiltinMethod;
import dev.kobu.interpreter.ast.symbol.BuiltinScope;
import dev.kobu.interpreter.ast.symbol.RecordTypeSymbol;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.ast.symbol.array.ArrayTypeFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RecordGetAttributesMethodImpl extends BuiltinMethod {

    @Override
    protected ValueExpr run(EvalContext context, ValueExpr object, Map<String, ValueExpr> args, SourceCodeRef sourceCodeRef) {
        RecordValueExpr recordValueExpr = (RecordValueExpr) object;
        RecordTypeSymbol recordTypeSymbol = (RecordTypeSymbol) recordValueExpr.getType();

        List<ValueExpr> result = new ArrayList<>();
        for (FieldDescriptor field : recordTypeSymbol.getFields()) {
            result.add(new StringValueExpr(field.getName()));
        }

        return new ArrayValueExpr(ArrayTypeFactory.getArrayTypeFor(BuiltinScope.STRING_TYPE), result);
    }

    @Override
    public String getDocumentation() {
        return "Returns all attributes defined for this record type";
    }

}