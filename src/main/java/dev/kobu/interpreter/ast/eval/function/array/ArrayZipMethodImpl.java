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
import dev.kobu.interpreter.ast.eval.expr.value.TupleValueExpr;
import dev.kobu.interpreter.ast.eval.function.BuiltinMethod;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.ast.symbol.array.ArrayTypeFactory;
import dev.kobu.interpreter.ast.symbol.tuple.TupleType;
import dev.kobu.interpreter.ast.symbol.tuple.TupleTypeElement;
import dev.kobu.interpreter.ast.symbol.tuple.TupleTypeFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ArrayZipMethodImpl extends BuiltinMethod {

    @Override
    protected ValueExpr run(EvalContext context, ValueExpr object, Map<String, ValueExpr> args, SourceCodeRef sourceCodeRef) {
        ArrayValueExpr arrayExpr = (ArrayValueExpr) object;
        ArrayValueExpr thatExpr = (ArrayValueExpr) args.get("that");

        List<ValueExpr> list1 = arrayExpr.getValue();
        List<ValueExpr> list2 = thatExpr.getValue();

        List<ValueExpr> pairs = new ArrayList<>();
        TupleTypeElement tupleTypeElement = new TupleTypeElement(arrayExpr.getType().getElementType());
        tupleTypeElement.setNext(new TupleTypeElement(thatExpr.getType().getElementType()));
        TupleType tupleType = TupleTypeFactory.getTupleTypeFor(tupleTypeElement);
        for (int i = 0; i < list1.size() && i < list2.size(); i++) {
            pairs.add(new TupleValueExpr(tupleType, List.of(list1.get(i), list2.get(i))));
        }

        return new ArrayValueExpr(ArrayTypeFactory.getArrayTypeFor(tupleType), pairs);
    }

    @Override
    public String getDocumentation() {
        return "Returns an array formed from this array and another array by combining corresponding elements in pairs. " +
                "If one of the two arrays is longer than the other, its remaining elements are ignored";
    }

}
