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

package dev.cgrscript.interpreter.ast.eval.expr.value;

import dev.cgrscript.interpreter.ast.eval.EvalContext;
import dev.cgrscript.interpreter.ast.eval.Expr;
import dev.cgrscript.interpreter.ast.eval.HasTargetType;
import dev.cgrscript.interpreter.ast.eval.ValueExpr;
import dev.cgrscript.interpreter.ast.symbol.ArrayType;
import dev.cgrscript.interpreter.ast.symbol.SourceCodeRef;
import dev.cgrscript.interpreter.ast.symbol.Type;
import dev.cgrscript.interpreter.ast.symbol.UnknownType;
import dev.cgrscript.interpreter.error.analyzer.InvalidTypeError;

import java.util.ArrayList;
import java.util.List;

public class ArrayConstructorCallExpr implements Expr, HasTargetType {

    private final SourceCodeRef sourceCodeRef;

    private final List<Expr> elements;

    private Type targetType;

    private ArrayType type;

    public ArrayConstructorCallExpr(SourceCodeRef sourceCodeRef, List<Expr> elements) {
        this.sourceCodeRef = sourceCodeRef;
        this.elements = elements;
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
    public void analyze(EvalContext context) {
        Type targetElementType = null;
        if (targetType instanceof ArrayType) {
            targetElementType = ((ArrayType)targetType).getElementType();
        }
        Type elementType = targetElementType;
        if (elements != null) {
            for (Expr element : elements) {
                if (targetElementType != null && element instanceof HasTargetType) {
                    ((HasTargetType) element).setTargetType(targetElementType);
                }
                element.analyze(context);
                var inferType = element.getType();
                if (inferType instanceof UnknownType) {
                    this.type = new ArrayType(UnknownType.INSTANCE);
                    return;
                }
                if (elementType == null) {
                    elementType = inferType;
                } else {
                    var commonType = elementType.getCommonSuperType(inferType);
                    if (commonType == null) {
                        context.getModuleScope().addError(new InvalidTypeError(element.getSourceCodeRef(),
                                inferType, elementType));
                        this.type = new ArrayType(UnknownType.INSTANCE);
                        return;
                    } else {
                        elementType = commonType;
                    }
                }
            }
        }

        this.type = new ArrayType(elementType);
    }

    @Override
    public ValueExpr evalExpr(EvalContext context) {
        List<ValueExpr> value = new ArrayList<>();

        for (Expr elementExpr : elements) {
            value.add(elementExpr.evalExpr(context));
        }

        return new ArrayValueExpr(type, value);
    }

    @Override
    public Type getTargetType() {
        return targetType;
    }

    @Override
    public void setTargetType(Type targetType) {
        this.targetType = targetType;
    }

}
