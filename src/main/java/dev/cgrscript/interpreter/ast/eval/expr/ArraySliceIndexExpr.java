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

package dev.cgrscript.interpreter.ast.eval.expr;

import dev.cgrscript.interpreter.ast.eval.*;
import dev.cgrscript.interpreter.ast.eval.expr.value.ArrayValueExpr;
import dev.cgrscript.interpreter.ast.eval.expr.value.NullValueExpr;
import dev.cgrscript.interpreter.ast.eval.expr.value.NumberValueExpr;
import dev.cgrscript.interpreter.ast.symbol.BuiltinScope;
import dev.cgrscript.interpreter.ast.symbol.NumberTypeSymbol;
import dev.cgrscript.interpreter.ast.symbol.SourceCodeRef;
import dev.cgrscript.interpreter.error.eval.ArrayIndexOutOfBoundsError;
import dev.cgrscript.interpreter.error.eval.InternalInterpreterError;
import dev.cgrscript.interpreter.error.analyzer.InvalidTypeError;
import dev.cgrscript.interpreter.error.eval.NullPointerError;

import java.util.ArrayList;

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
    public void analyze(EvalContext context) {
        if (begin != null) {
            begin.analyze(context);
            if (!(begin.getType() instanceof NumberTypeSymbol)) {
                context.getModuleScope().addError(new InvalidTypeError(begin.getSourceCodeRef(),
                        BuiltinScope.NUMBER_TYPE, begin.getType()));
            }
        }
        if (end != null) {
            end.analyze(context);
            if (!(end.getType() instanceof NumberTypeSymbol)) {
                context.getModuleScope().addError(new InvalidTypeError(end.getSourceCodeRef(),
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
