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
import dev.kobu.interpreter.ast.eval.expr.value.BooleanValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.NullValueExpr;
import dev.kobu.interpreter.ast.eval.function.BuiltinMethod;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.ast.symbol.array.ArrayType;
import dev.kobu.interpreter.ast.symbol.function.KobuFunction;
import dev.kobu.interpreter.ast.utils.FunctionUtils;
import dev.kobu.interpreter.error.eval.IllegalArgumentError;
import dev.kobu.interpreter.error.eval.InternalInterpreterError;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ArrayFilterMethodImpl extends BuiltinMethod {

    private final ArrayType arrayType;

    public ArrayFilterMethodImpl(ArrayType arrayType) {
        this.arrayType = arrayType;
    }

    @Override
    protected ValueExpr run(EvalContext context, ValueExpr object, Map<String, ValueExpr> args, SourceCodeRef sourceCodeRef) {
        ArrayValueExpr arrayExpr = (ArrayValueExpr) object;
        ValueExpr pred = args.get("pred");

        if (pred == null || pred instanceof NullValueExpr) {
            throw new IllegalArgumentError("predicate cannot be null", sourceCodeRef);
        }

        List<ValueExpr> filtered = new ArrayList<>();
        for (ValueExpr valueExpr : arrayExpr.getValue()) {
            if (runPredicate(context, pred, valueExpr, sourceCodeRef)) {
                filtered.add(valueExpr);
            }
        }

        return new ArrayValueExpr(arrayType, filtered);
    }

    private boolean runPredicate(EvalContext evalContext, ValueExpr pred, ValueExpr elem, SourceCodeRef sourceCodeRef) {
        KobuFunction fn = FunctionUtils.toFunction(pred);
        ValueExpr result = evalContext.evalFunction(fn, List.of(elem), sourceCodeRef);
        if (result instanceof BooleanValueExpr) {
            return ((BooleanValueExpr) result).getValue();
        }
        if (result instanceof NullValueExpr) {
            return false;
        }
        String name = result != null ? result.getClass().getName() : "'null'";
        throw new InternalInterpreterError("BooleanValueExpr expected, got " + name, sourceCodeRef);
    }

    @Override
    public String getDocumentation() {
        return "Selects all elements of this array which satisfy a predicate";
    }
}
