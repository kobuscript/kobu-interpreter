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

package dev.kobu.interpreter.ast.eval.expr;

import dev.kobu.interpreter.ast.eval.context.EvalContext;
import dev.kobu.interpreter.ast.eval.expr.value.ArrayValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.NullValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.number.NumberValueExpr;
import dev.kobu.interpreter.ast.symbol.BuiltinScope;
import dev.kobu.interpreter.ast.symbol.value.NumberTypeSymbol;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.ast.symbol.Type;
import dev.kobu.interpreter.error.eval.ArrayIndexOutOfBoundsError;
import dev.kobu.interpreter.error.eval.InternalInterpreterError;
import dev.kobu.interpreter.error.analyzer.InvalidTypeError;
import dev.kobu.interpreter.error.eval.NullPointerError;
import dev.kobu.interpreter.ast.eval.Expr;
import dev.kobu.interpreter.ast.eval.ValueExpr;

import java.util.ArrayList;
import java.util.Map;

public class ArraySliceIndexExpr implements ArrayIndexExpr {

    private final SourceCodeRef sourceCodeRef;

    private final Expr begin;

    private final Expr end;

    public ArraySliceIndexExpr(SourceCodeRef sourceCodeRef, Expr begin, Expr end) {
        this.sourceCodeRef = sourceCodeRef;
        this.begin = begin;
        this.end = end;
    }

    @Override
    public SourceCodeRef getSourceCodeRef() {
        return sourceCodeRef;
    }

    @Override
    public void setResolvedTypes(Map<String, Type> resolvedTypes) {
        if (begin != null) {
            begin.setResolvedTypes(resolvedTypes);
        }
        if (end != null) {
            end.setResolvedTypes(resolvedTypes);
        }
    }

    @Override
    public void analyze(EvalContext context) {
        if (begin != null) {
            begin.analyze(context);
            if (!(begin.getType() instanceof NumberTypeSymbol)) {
                context.addAnalyzerError(new InvalidTypeError(begin.getSourceCodeRef(),
                        BuiltinScope.NUMBER_TYPE, begin.getType()));
            }
        }
        if (end != null) {
            end.analyze(context);
            if (!(end.getType() instanceof NumberTypeSymbol)) {
                context.addAnalyzerError(new InvalidTypeError(end.getSourceCodeRef(),
                        BuiltinScope.NUMBER_TYPE, end.getType()));
            }
        }
    }

    @Override
    public ValueExpr eval(EvalContext context, ArrayValueExpr arrayExpr) {
        int beginIndex = 0;
        int endIndex = arrayExpr.getValue().size();

        if (begin != null) {
            beginIndex = getIndexExpr(context, arrayExpr, begin).getValue().intValue();
            if (beginIndex < -(arrayExpr.getValue().size()) || beginIndex >= arrayExpr.getValue().size()) {
                throw new ArrayIndexOutOfBoundsError(sourceCodeRef, arrayExpr, beginIndex);
            }
            if (beginIndex < 0) {
                beginIndex = arrayExpr.getValue().size() + beginIndex;
            }
        }

        if (end != null) {
            endIndex = getIndexExpr(context, arrayExpr, end).getValue().intValue();
            if (endIndex < -(arrayExpr.getValue().size()) || endIndex >= arrayExpr.getValue().size()) {
                throw new ArrayIndexOutOfBoundsError(sourceCodeRef, arrayExpr, endIndex);
            }
            if (endIndex < 0) {
                endIndex = arrayExpr.getValue().size() + endIndex;
            }
        }

        return new ArrayValueExpr(arrayExpr.getType(),
                new ArrayList<>(arrayExpr.getValue().subList(beginIndex, endIndex)));
    }

    @Override
    public NumberValueExpr getIndexValue(EvalContext context, ArrayValueExpr arrayExpr) {
        throw new InternalInterpreterError("Operation not supported.", sourceCodeRef);
    }

    private NumberValueExpr getIndexExpr(EvalContext context, ArrayValueExpr arrayExpr, Expr expr) {
        ValueExpr indexValue = expr.evalExpr(context);
        if (indexValue instanceof NullValueExpr) {
            throw new NullPointerError(sourceCodeRef, expr.getSourceCodeRef());
        }
        if (!(indexValue instanceof NumberValueExpr)) {
            throw new InternalInterpreterError("Expected: Number. Found: " + indexValue.getStringValue(),
                    arrayExpr.getSourceCodeRef());
        }
        return (NumberValueExpr) indexValue;
    }

}
