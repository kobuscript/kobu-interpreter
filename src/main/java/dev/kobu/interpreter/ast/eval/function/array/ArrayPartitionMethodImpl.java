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
import dev.kobu.interpreter.ast.eval.expr.value.number.NumberValueExpr;
import dev.kobu.interpreter.ast.eval.function.BuiltinMethod;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.ast.symbol.array.ArrayType;
import dev.kobu.interpreter.ast.symbol.array.ArrayTypeFactory;
import dev.kobu.interpreter.error.eval.IllegalArgumentError;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ArrayPartitionMethodImpl extends BuiltinMethod {

    @Override
    protected ValueExpr run(EvalContext context, ValueExpr object, Map<String, ValueExpr> args, SourceCodeRef sourceCodeRef) {
        ArrayValueExpr arrayExpr = (ArrayValueExpr) object;
        ValueExpr sizeExpr = args.get("size");

        if (sizeExpr == null || sizeExpr instanceof NullValueExpr) {
            throw new IllegalArgumentError("'size' cannot be null", sourceCodeRef);
        }
        int size = ((NumberValueExpr) sizeExpr).getValue().intValue();
        if (size <= 0) {
            throw new IllegalArgumentError("'size' must be greater than 0", sourceCodeRef);
        }

        ArrayType arrType = arrayExpr.getType();
        ArrayType resType = ArrayTypeFactory.getArrayTypeFor(arrType);
        List<ValueExpr> result = new ArrayList<>();
        List<ValueExpr> currentSequence = new ArrayList<>();

        int idx = 0;
        for (ValueExpr valueExpr : arrayExpr.getValue()) {
            currentSequence.add(valueExpr);
            idx++;
            if (idx == size) {
                result.add(new ArrayValueExpr(arrType, currentSequence));
                currentSequence = new ArrayList<>();
            }
        }
        if (!currentSequence.isEmpty()) {
            result.add(new ArrayValueExpr(arrType, currentSequence));
        }

        return new ArrayValueExpr(resType, result);
    }

    @Override
    public String getDocumentation() {
        return "Returns consecutive subarrays of this array, each of the same size (the final array may be smaller)";
    }

}
