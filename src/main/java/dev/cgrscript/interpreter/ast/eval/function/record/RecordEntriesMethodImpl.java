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

package dev.cgrscript.interpreter.ast.eval.function.record;

import dev.cgrscript.interpreter.ast.eval.context.EvalContext;
import dev.cgrscript.interpreter.ast.eval.ValueExpr;
import dev.cgrscript.interpreter.ast.eval.expr.value.ArrayValueExpr;
import dev.cgrscript.interpreter.ast.eval.expr.value.PairValueExpr;
import dev.cgrscript.interpreter.ast.eval.expr.value.RecordValueExpr;
import dev.cgrscript.interpreter.ast.eval.expr.value.StringValueExpr;
import dev.cgrscript.interpreter.ast.eval.function.BuiltinMethod;
import dev.cgrscript.interpreter.ast.symbol.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RecordEntriesMethodImpl extends BuiltinMethod {

    private final Type type;

    public RecordEntriesMethodImpl(Type type) {
        this.type = type;
    }

    @Override
    protected ValueExpr run(EvalContext context, ValueExpr object, Map<String, ValueExpr> args, SourceCodeRef sourceCodeRef) {
        RecordValueExpr recordExpr = (RecordValueExpr) object;
        List<ValueExpr> result = new ArrayList<>();

        for (String field : recordExpr.getFields()) {
            ValueExpr value = recordExpr.resolveField(field);
            if (type.isAssignableFrom(value.getType())) {
                var pairType = new PairType(BuiltinScope.STRING_TYPE, value.getType());
                result.add(new PairValueExpr(pairType, new StringValueExpr(field), value));
            }
        }

        return new ArrayValueExpr(new ArrayType(type), result);
    }

    @Override
    public String getDocumentation() {
        return "";
    }

}
