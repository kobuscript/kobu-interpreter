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

package dev.kobu.interpreter.ast.utils;

import dev.kobu.interpreter.ast.eval.ValueExpr;
import dev.kobu.interpreter.ast.eval.context.EvalContext;
import dev.kobu.interpreter.ast.eval.expr.value.BooleanValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.FunctionRefValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.NullValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.NumberValueExpr;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.ast.symbol.function.KobuFunction;
import dev.kobu.interpreter.error.eval.InternalInterpreterError;
import dev.kobu.interpreter.error.eval.NullPointerError;

import java.util.List;

public class FunctionUtils {

    public static KobuFunction toFunction(ValueExpr valueExpr) {
        if (valueExpr instanceof KobuFunction) {
            return (KobuFunction) valueExpr;
        }
        if (valueExpr instanceof FunctionRefValueExpr) {
            return ((FunctionRefValueExpr) valueExpr).getFunction();
        }

        throw new InternalInterpreterError("Not a function: " + valueExpr.getClass().getName(), null);
    }

    public static boolean runPredicate(EvalContext evalContext, ValueExpr pred, ValueExpr elem, SourceCodeRef sourceCodeRef) {
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

    public static ValueExpr runReducer(EvalContext evalContext, ValueExpr reducer,
                                       ValueExpr elem1, ValueExpr elem2,
                                       SourceCodeRef sourceCodeRef) {
        KobuFunction fn = FunctionUtils.toFunction(reducer);
        return evalContext.evalFunction(fn, List.of(elem1, elem2), sourceCodeRef);
    }

    public static NumberValueExpr runComparator(EvalContext evalContext, ValueExpr comparator,
                                                ValueExpr elem1, ValueExpr elem2,
                                                SourceCodeRef sourceCodeRef) {
        KobuFunction fn = FunctionUtils.toFunction(comparator);
        ValueExpr result = evalContext.evalFunction(fn, List.of(elem1, elem2), sourceCodeRef);
        if (result instanceof NullValueExpr) {
            throw new NullPointerError("A comparator function cannot return null", sourceCodeRef);
        }
        if (!(result instanceof NumberValueExpr)) {
            throw new InternalInterpreterError("Invalid comparator response. " +
                    "Expected NumberValueExpr, got: " + result.getClass().getName(), sourceCodeRef);
        }
        return (NumberValueExpr) result;
    }
}
