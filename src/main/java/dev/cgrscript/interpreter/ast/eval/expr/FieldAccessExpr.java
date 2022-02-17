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
import dev.cgrscript.interpreter.error.eval.InternalInterpreterError;
import dev.cgrscript.interpreter.error.analyzer.InvalidAssignmentError;
import dev.cgrscript.interpreter.error.analyzer.InvalidExpressionError;
import dev.cgrscript.interpreter.ast.symbol.SourceCodeRef;
import dev.cgrscript.interpreter.ast.symbol.Type;
import dev.cgrscript.interpreter.ast.symbol.UnknownType;

public class FieldAccessExpr implements Expr, MemoryReference, HasTypeScope {

    private final SourceCodeRef sourceCodeRef;

    private final Expr leftExpr;

    private final Expr rightExpr;

    private boolean assignMode = false;

    private Type type;

    private Type typeScope;

    private ValueExpr valueScope;

    public FieldAccessExpr(SourceCodeRef sourceCodeRef, Expr leftExpr, Expr rightExpr) {
        this.sourceCodeRef = sourceCodeRef;
        this.leftExpr = leftExpr;
        this.rightExpr = rightExpr;
    }

    @Override
    public void analyze(EvalContext context) {

        if (typeScope != null && leftExpr instanceof HasTypeScope) {
            ((HasTypeScope)leftExpr).setTypeScope(typeScope);
        }
        leftExpr.analyze(context);
        Type typeRef = leftExpr.getType();

        if (typeRef instanceof UnknownType) {
            this.type = UnknownType.INSTANCE;
            return;
        }

        if (rightExpr instanceof HasTypeScope) {
            ((HasTypeScope)rightExpr).setTypeScope(typeRef);
        } else {
            context.getModuleScope().addError(new InvalidExpressionError(rightExpr.getSourceCodeRef()));
            this.type = UnknownType.INSTANCE;
            return;
        }
        if (assignMode) {
            if (rightExpr instanceof MemoryReference) {
                ((MemoryReference)rightExpr).setAssignMode();
            } else {
                context.getModuleScope().addError(new InvalidAssignmentError(sourceCodeRef, rightExpr));
                this.type = UnknownType.INSTANCE;
                return;
            }
        }
        rightExpr.analyze(context);

        this.type = rightExpr.getType();
    }

    @Override
    public ValueExpr evalExpr(EvalContext context) {

        if (valueScope != null && leftExpr instanceof HasTypeScope) {
            ((HasTypeScope)leftExpr).setValueScope(valueScope);
        }
        var value = leftExpr.evalExpr(context);

        if (rightExpr instanceof HasTypeScope) {
            ((HasTypeScope)rightExpr).setValueScope(value);
        } else {
            throw new InternalInterpreterError("Invalid Expression", getSourceCodeRef());
        }
        value = rightExpr.evalExpr(context);
        return value;
    }

    @Override
    public SourceCodeRef getSourceCodeRef() {
        return sourceCodeRef;
    }

    @Override
    public Type getType() {
        return type;
    }

    public Expr getLeftExpr() {
        return leftExpr;
    }

    public Expr getRightExpr() {
        return rightExpr;
    }

    @Override
    public void setAssignMode() {
        this.assignMode = true;
    }

    @Override
    public void assign(EvalContext context, ValueExpr value) {

        var refValue = leftExpr.evalExpr(context);
        if (rightExpr instanceof HasTypeScope) {
            ((HasTypeScope)rightExpr).setValueScope(refValue);
        }
        if (rightExpr instanceof MemoryReference) {
            ((MemoryReference)rightExpr).assign(context, value);
            return;
        }

        throw new InternalInterpreterError("Can't assign a value to: " + rightExpr.getClass().getName(),
                rightExpr.getSourceCodeRef());

    }

    @Override
    public void setTypeScope(Type typeScope) {
        this.typeScope = typeScope;
    }

    @Override
    public void setValueScope(ValueExpr valueScope) {
        this.valueScope = valueScope;
    }

}
