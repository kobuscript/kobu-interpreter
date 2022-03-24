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
import dev.kobu.interpreter.ast.symbol.array.ArrayType;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.ast.symbol.Type;
import dev.kobu.interpreter.ast.symbol.UnknownType;
import dev.kobu.interpreter.error.analyzer.InvalidArrayIndexAssignmentError;
import dev.kobu.interpreter.error.analyzer.NotArrayTypeError;
import dev.kobu.interpreter.error.eval.InternalInterpreterError;
import dev.kobu.interpreter.error.eval.NullPointerError;
import dev.kobu.interpreter.ast.eval.Expr;
import dev.kobu.interpreter.ast.eval.HasTypeScope;
import dev.kobu.interpreter.ast.eval.MemoryReference;
import dev.kobu.interpreter.ast.eval.ValueExpr;

public class ArrayAccessExpr implements Expr, HasTypeScope, MemoryReference {

    private final SourceCodeRef sourceCodeRef;

    private final Expr arrayExpr;

    private final ArrayIndexExpr indexExpr;

    private Type typeScope;

    private ValueExpr valueScope;

    private boolean assignMode = false;

    private Type type;

    public ArrayAccessExpr(SourceCodeRef sourceCodeRef, Expr arrayExpr, ArrayIndexExpr indexExpr) {
        this.sourceCodeRef = sourceCodeRef;
        this.arrayExpr = arrayExpr;
        this.indexExpr = indexExpr;
    }

    @Override
    public void analyze(EvalContext context) {
        if (arrayExpr instanceof HasTypeScope) {
            ((HasTypeScope)arrayExpr).setTypeScope(typeScope);
        }
        arrayExpr.analyze(context);
        indexExpr.analyze(context);

        if (!(arrayExpr.getType() instanceof ArrayType)) {
            context.addAnalyzerError(new NotArrayTypeError(arrayExpr.getSourceCodeRef(), arrayExpr.getType()));
            this.type = UnknownType.INSTANCE;
            return;
        }

        if (assignMode && !(indexExpr instanceof ArrayItemIndexExpr)) {
            context.addAnalyzerError(new InvalidArrayIndexAssignmentError(indexExpr.getSourceCodeRef()));
            this.type = UnknownType.INSTANCE;
            return;
        }

        if (indexExpr instanceof ArrayItemIndexExpr) {
            this.type = ((ArrayType) arrayExpr.getType()).getElementType();
        } else {
            this.type = arrayExpr.getType();
        }
    }

    @Override
    public ValueExpr evalExpr(EvalContext context) {
        var arrayValue = getArrayExpr(context);
        return indexExpr.eval(context, arrayValue);
    }

    private ArrayValueExpr getArrayExpr(EvalContext context) {
        if (arrayExpr instanceof HasTypeScope) {
            ((HasTypeScope)arrayExpr).setValueScope(valueScope);
        }
        ValueExpr arrayValue = arrayExpr.evalExpr(context);
        if (arrayValue instanceof NullValueExpr) {
            throw new NullPointerError(sourceCodeRef, arrayExpr.getSourceCodeRef());
        }
        if (!(arrayValue instanceof ArrayValueExpr)) {
            throw new InternalInterpreterError("Expected: Array. Found: " + arrayValue.getStringValue(),
                    arrayExpr.getSourceCodeRef());
        }
        return (ArrayValueExpr) arrayValue;
    }

    @Override
    public SourceCodeRef getSourceCodeRef() {
        return sourceCodeRef;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public void setTypeScope(Type typeScope) {
        this.typeScope = typeScope;
    }

    @Override
    public void setValueScope(ValueExpr valueScope) {
        this.valueScope = valueScope;
    }

    @Override
    public void setAssignMode() {
        this.assignMode = true;
    }

    @Override
    public void assign(EvalContext context, ValueExpr value) {
        var arrayValue = getArrayExpr(context);
        var indexValue = indexExpr.getIndexValue(context, arrayValue);

        arrayValue.assign(sourceCodeRef, indexValue, value);
        if (arrayExpr instanceof MemoryReference) {
            ((MemoryReference)arrayExpr).assign(context, arrayValue);
        }
    }
}
