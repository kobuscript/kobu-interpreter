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

package dev.kobu.interpreter.ast.eval.function.array;

import dev.kobu.interpreter.ast.eval.ValueExpr;
import dev.kobu.interpreter.ast.eval.context.EvalContext;
import dev.kobu.interpreter.ast.eval.expr.value.ArrayValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.NullValueExpr;
import dev.kobu.interpreter.ast.eval.function.BuiltinMethod;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.ast.utils.FunctionUtils;
import dev.kobu.interpreter.error.eval.IllegalArgumentError;

import java.util.List;
import java.util.Map;

public class ArrayReduceRightMethodImpl extends BuiltinMethod {

    @Override
    protected ValueExpr run(EvalContext context, ValueExpr object, Map<String, ValueExpr> args, SourceCodeRef sourceCodeRef) {
        ArrayValueExpr arrayExpr = (ArrayValueExpr) object;
        List<ValueExpr> valueList = arrayExpr.getValue();
        ValueExpr reducer = args.get("reducer");
        ValueExpr acc = args.get("acc");

        if (reducer == null || reducer instanceof NullValueExpr) {
            throw new IllegalArgumentError("reducer cannot be null", sourceCodeRef);
        }

        ValueExpr result = acc == null || acc instanceof NullValueExpr ? new NullValueExpr() : acc;
        for (int i = valueList.size() - 1; i >= 0; i--) {
            if (i == 0) {
                if (acc == null) {
                    result = valueList.get(0);
                    continue;
                } else {
                    result = acc;
                }
            }
            result = FunctionUtils.runReducer(context, reducer, result, valueList.get(i), sourceCodeRef);
        }

        return result;
    }

    @Override
    public String getDocumentation() {
        return "Reduces the elements of this array (from right to left) using the provided reducer function";
    }

}
