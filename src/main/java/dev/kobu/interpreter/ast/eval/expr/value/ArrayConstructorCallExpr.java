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

package dev.kobu.interpreter.ast.eval.expr.value;

import dev.kobu.interpreter.ast.eval.context.EvalContext;
import dev.kobu.interpreter.ast.eval.Expr;
import dev.kobu.interpreter.ast.eval.HasTargetType;
import dev.kobu.interpreter.ast.eval.ValueExpr;
import dev.kobu.interpreter.ast.symbol.*;
import dev.kobu.interpreter.ast.symbol.array.ArrayType;
import dev.kobu.interpreter.ast.symbol.array.ArrayTypeFactory;
import dev.kobu.interpreter.error.analyzer.InvalidTypeError;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    public void setResolvedTypes(Map<String, Type> resolvedTypes) {
        if (elements != null) {
            elements.forEach(expr -> expr.setResolvedTypes(resolvedTypes));
        }
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
        if (elements != null && !elements.isEmpty()) {
            for (Expr element : elements) {
                if (element instanceof NullValueExpr) {
                    continue;
                }
                if (targetElementType != null && element instanceof HasTargetType) {
                    ((HasTargetType) element).setTargetType(targetElementType);
                }
                element.analyze(context);
                var inferType = element.getType();
                if (inferType instanceof UnknownType) {
                    this.type = ArrayTypeFactory.getArrayTypeFor(UnknownType.INSTANCE);
                    return;
                }
                if (elementType == null) {
                    elementType = inferType;
                } else {
                    var commonType = elementType.getCommonSuperType(inferType);
                    if (commonType == null) {
                        context.addAnalyzerError(new InvalidTypeError(element.getSourceCodeRef(),
                                inferType, elementType));
                        this.type = ArrayTypeFactory.getArrayTypeFor(UnknownType.INSTANCE);
                        return;
                    } else {
                        elementType = commonType;
                    }
                }
            }
        }
        if (elementType == null) {
            elementType = BuiltinScope.ANY_TYPE;
        }

        this.type = ArrayTypeFactory.getArrayTypeFor(elementType);
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
