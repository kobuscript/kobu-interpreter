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

import dev.kobu.interpreter.ast.eval.context.EvalContext;
import dev.kobu.interpreter.ast.eval.ValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.NullValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.RecordValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.StringValueExpr;
import dev.kobu.interpreter.ast.eval.function.BuiltinMethod;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.ast.symbol.Type;
import dev.kobu.interpreter.error.eval.InvalidCallError;

import java.util.Map;

public class RecordPutMethodImpl extends BuiltinMethod {

    @Override
    protected ValueExpr run(EvalContext context, ValueExpr object, Map<String, ValueExpr> args, SourceCodeRef sourceCodeRef) {
        RecordValueExpr recordExpr = (RecordValueExpr) object;
        StringValueExpr attrName = (StringValueExpr) args.get("attr");
        ValueExpr value = args.get("value");
        Type fieldType = recordExpr.getType().resolveField(attrName.getValue());
        if (fieldType == null) {
            throw new InvalidCallError("Type " + recordExpr.getType().getName() +
                    " does not have a " + attrName + "field", sourceCodeRef);
        }
        if (value == null || value instanceof NullValueExpr) {
            recordExpr.updateFieldValue(context, attrName.getValue(), new NullValueExpr());
            return null;
        }
        if (!fieldType.isAssignableFrom(value.getType())) {
            throw new InvalidCallError("Invalid value for " + recordExpr.getType().getName() +
                    "." + attrName + ": " + value.getType().getName(), sourceCodeRef);
        }
        recordExpr.updateFieldValue(context, attrName.getValue(), value);

        return null;
    }

    @Override
    public String getDocumentation() {
        return "Associates the specified value with the specified attribute";
    }

}
