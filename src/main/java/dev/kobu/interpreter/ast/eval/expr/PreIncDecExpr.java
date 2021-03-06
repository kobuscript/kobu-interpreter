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

import dev.kobu.interpreter.ast.eval.*;
import dev.kobu.interpreter.ast.eval.context.EvalContext;
import dev.kobu.interpreter.ast.eval.expr.value.number.NumberValueExpr;
import dev.kobu.interpreter.ast.symbol.*;
import dev.kobu.interpreter.ast.symbol.value.NumberTypeSymbol;
import dev.kobu.interpreter.error.analyzer.InvalidExpressionError;
import dev.kobu.interpreter.error.analyzer.InvalidTypeError;
import dev.kobu.interpreter.error.eval.InternalInterpreterError;

import java.util.Map;

public class PreIncDecExpr implements Statement, Expr, Assignment {

    private final SourceCodeRef sourceCodeRef;

    private final Expr refExpr;

    private final IncDecOperatorEnum operator;

    private Type type;

    public PreIncDecExpr(SourceCodeRef sourceCodeRef, Expr refExpr, IncDecOperatorEnum operator) {
        this.sourceCodeRef = sourceCodeRef;
        this.refExpr = refExpr;
        this.operator = operator;
    }

    @Override
    public SourceCodeRef getSourceCodeRef() {
        return sourceCodeRef;
    }

    @Override
    public void setResolvedTypes(Map<String, Type> resolvedTypes) {
        if (refExpr != null) {
            refExpr.setResolvedTypes(resolvedTypes);
        }
    }

    @Override
    public void analyze(EvalContext context) {
        if (!(refExpr instanceof MemoryReference)) {
            context.addAnalyzerError(new InvalidExpressionError(sourceCodeRef));
            this.type = UnknownType.INSTANCE;
            return;
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
        ValueExpr val = refExpr.evalExpr(context);

        if (!(val instanceof NumberValueExpr)) {
            throw new InternalInterpreterError("Expected 'number', but got '" + val.getType() + "'", sourceCodeRef);
        }

        NumberValueExpr newVal;
        if (operator == IncDecOperatorEnum.INC) {
            newVal = ((NumberValueExpr)val).inc();
        } else {
            newVal = ((NumberValueExpr)val).dec();
        }

        ((MemoryReference)refExpr).assign(context, newVal);
        return newVal;
    }
}
