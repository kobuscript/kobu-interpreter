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
import dev.cgrscript.interpreter.ast.eval.expr.value.NullValueExpr;
import dev.cgrscript.interpreter.ast.eval.expr.value.NumberValueExpr;
import dev.cgrscript.interpreter.ast.symbol.*;
import dev.cgrscript.interpreter.error.analyzer.*;
import dev.cgrscript.interpreter.error.eval.InternalInterpreterError;
import dev.cgrscript.interpreter.error.eval.NullPointerError;

public class PostIncDecExpr implements Statement, Expr, HasTypeScope, Assignment {

    private final SourceCodeRef sourceCodeRef;

    private final Expr refExpr;

    private final IncDecOperatorEnum operator;

    private Type typeScope;

    private ValueExpr valueScope;

    private Type type;

    public PostIncDecExpr(SourceCodeRef sourceCodeRef, Expr refExpr, IncDecOperatorEnum operator) {
        this.sourceCodeRef = sourceCodeRef;
        this.refExpr = refExpr;
        this.operator = operator;
    }

    @Override
    public SourceCodeRef getSourceCodeRef() {
        return sourceCodeRef;
    }

    @Override
    public void analyze(EvalContext context) {
        if (!(refExpr instanceof MemoryReference)) {
            context.addAnalyzerError(new InvalidExpressionError(sourceCodeRef));
            this.type = UnknownType.INSTANCE;
            return;
        }
        if (typeScope != null && refExpr instanceof HasTypeScope) {
            ((HasTypeScope)refExpr).setTypeScope(typeScope);
        }
        refExpr.analyze(context);
        if (refExpr.getType() instanceof UnknownType) {
            this.type = UnknownType.INSTANCE;
            return;
        }
        if (!(refExpr.getType() instanceof NumberTypeSymbol)) {
            var numberType = BuiltinScope.NUMBER_TYPE;
            context.addAnalyzerError(new InvalidTypeError(sourceCodeRef, numberType, refExpr.getType()));
            this.type = UnknownType.INSTANCE;
            return;
        }
        this.type = refExpr.getType();
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public ValueExpr evalExpr(EvalContext context) {
        return eval(context);
    }

    @Override
    public void evalStat(EvalContext context) {
        eval(context);
    }

    private ValueExpr eval(EvalContext context) {

        if (valueScope != null && refExpr instanceof HasTypeScope) {
            ((HasTypeScope)refExpr).setValueScope(valueScope);
        }
        ValueExpr val = refExpr.evalExpr(context);

        if (!(val instanceof NumberValueExpr)) {
            throw new InternalInterpreterError("Expected 'number', but got '" + val.getType() + "'", sourceCodeRef);
        }

        Number newVal;
        if (operator == IncDecOperatorEnum.INC) {
            newVal = ((NumberValueExpr)val).inc();
        } else {
            newVal = ((NumberValueExpr)val).dec();
        }

        NumberValueExpr newValExpr = new NumberValueExpr(newVal);
        ((MemoryReference)refExpr).assign(context, newValExpr);
        return new NumberValueExpr(((NumberValueExpr)val).getValue());
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
