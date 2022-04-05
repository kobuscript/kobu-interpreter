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

import dev.kobu.interpreter.ast.eval.HasTargetType;
import dev.kobu.interpreter.ast.eval.Statement;
import dev.kobu.interpreter.ast.eval.context.EvalContext;
import dev.kobu.interpreter.ast.symbol.*;
import dev.kobu.interpreter.error.analyzer.ConstNotInitializedError;
import dev.kobu.interpreter.error.analyzer.InvalidAssignExprTypeError;
import dev.kobu.interpreter.error.analyzer.InvalidVariableDeclError;

import java.util.Map;

public class ConstDeclExpr implements Statement {

    private final ConstantSymbol constSymbol;

    public ConstDeclExpr(ConstantSymbol constSymbol) {
        this.constSymbol = constSymbol;
    }

    public ConstantSymbol getConstSymbol() {
        return constSymbol;
    }

    @Override
    public SourceCodeRef getSourceCodeRef() {
        return constSymbol.getSourceCodeRef();
    }

    @Override
    public void setResolvedTypes(Map<String, Type> resolvedTypes) {
        if (constSymbol.getExpr() != null) {
            constSymbol.getExpr().setResolvedTypes(resolvedTypes);
        }
    }

    @Override
    public void analyze(EvalContext context) {
        if (constSymbol.getExpr() == null) {
            context.addAnalyzerError(new ConstNotInitializedError(constSymbol.getSourceCodeRef(), constSymbol));
            return;
        }

        if (constSymbol.getType() == null) {
            constSymbol.getExpr().analyze(context);
            if (constSymbol.getExpr().getType() == null) {
                constSymbol.setType(BuiltinScope.ANY_TYPE);
                context.addAnalyzerError(new InvalidVariableDeclError(constSymbol.getSourceCodeRef()));
            } else if (constSymbol.getExpr().getType() instanceof UnknownType) {
                constSymbol.setType(BuiltinScope.ANY_TYPE);
            } else {
                constSymbol.setType(constSymbol.getExpr().getType());
            }
        } else {
            if (constSymbol.getExpr() instanceof HasTargetType) {
                ((HasTargetType) constSymbol.getExpr()).setTargetType(constSymbol.getType());
            }
            constSymbol.getExpr().analyze(context);
        }

        var scope = context.getCurrentScope();

        scope.define(context.getAnalyzerContext(), constSymbol);

        Type valueType = constSymbol.getExpr().getType();
        if (valueType instanceof UnknownType) {
            return;
        }
        if (!constSymbol.getType().isAssignableFrom(valueType)) {
            context.addAnalyzerError(new InvalidAssignExprTypeError(constSymbol.getExpr().getSourceCodeRef(),
                    constSymbol.getType(), valueType));
        } else if (constSymbol.getType() instanceof ModuleRefSymbol) {
            context.addAnalyzerError(new InvalidAssignExprTypeError(constSymbol.getExpr().getSourceCodeRef(),
                    constSymbol.getType(), valueType));
            constSymbol.setType(BuiltinScope.ANY_TYPE);
        }

    }

    @Override
    public void evalStat(EvalContext context) {
        var scope = context.getCurrentScope();
        scope.define(context.getAnalyzerContext(), constSymbol);
        scope.setValue(constSymbol.getName(), constSymbol.getExpr().evalExpr(context));
    }

}
