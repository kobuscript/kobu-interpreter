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

import java.util.List;

public class ArrayItemIndexExpr implements ArrayIndexExpr {

    private final SourceCodeRef sourceCodeRef;

    private final Expr expr;

    public ArrayItemIndexExpr(SourceCodeRef sourceCodeRef, Expr expr) {
        this.sourceCodeRef = sourceCodeRef;
        this.expr = expr;
    }

    @Override
    public SourceCodeRef getSourceCodeRef() {
        return sourceCodeRef;
    }

    @Override
    public void analyze(EvalContext context) {
        expr.analyze(context);
        if (!(expr.getType() instanceof NumberTypeSymbol)) {
            context.getModuleScope().addError(new InvalidTypeError(expr.getSourceCodeRef(),
                    BuiltinScope.NUMBER_TYPE, expr.getType()));
        }
    }


    @Override
    public ValueExpr eval(EvalContext context, ArrayValueExpr arrayValue) {
        var indexValue = getIndexExpr(context, arrayValue);

        List<ValueExpr> array = arrayValue.getValue();
        int index = indexValue.getValue().intValue();

        if (index < -(array.size()) || index >= array.size()) {
            throw new ArrayIndexOutOfBoundsError(sourceCodeRef, arrayValue, index);
        }

        if (index >= 0) {
            return array.get(index);
        }
        return array.get(array.size() + index);
    }

    @Override
    public NumberValueExpr getIndexValue(EvalContext context, ArrayValueExpr arrayExpr) {
        return getIndexExpr(context, arrayExpr);
    }

    private NumberValueExpr getIndexExpr(EvalContext context, ArrayValueExpr arrayExpr) {
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
