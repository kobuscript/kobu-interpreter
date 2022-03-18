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
import dev.kobu.interpreter.ast.eval.expr.value.NullValueExpr;
import dev.kobu.interpreter.ast.symbol.*;
import dev.kobu.interpreter.error.analyzer.InvalidAssignExprTypeError;
import dev.kobu.interpreter.error.analyzer.InvalidVariableDeclError;
import dev.kobu.interpreter.ast.eval.Expr;
import dev.kobu.interpreter.ast.eval.HasTargetType;
import dev.kobu.interpreter.ast.eval.Statement;

public class VarDeclExpr implements Statement {

    private final VariableSymbol varSymbol;

    private Expr valueExpr;

    public VarDeclExpr(VariableSymbol varSymbol) {
        this.varSymbol = varSymbol;
    }

    public VariableSymbol getVarSymbol() {
        return varSymbol;
    }

    public Expr getValueExpr() {
        return valueExpr;
    }

    public void setValueExpr(Expr valueExpr) {
        this.valueExpr = valueExpr;
    }

    @Override
    public void analyze(EvalContext context) {
        if (valueExpr != null && !(valueExpr instanceof NullValueExpr)) {
            if (varSymbol.getType() == null) {
                valueExpr.analyze(context);
                if (valueExpr.getType() == null) {
                    varSymbol.setType(BuiltinScope.ANY_TYPE);
                    context.addAnalyzerError(new InvalidVariableDeclError(varSymbol.getSourceCodeRef()));
                } else if (valueExpr.getType() instanceof UnknownType) {
                    varSymbol.setType(BuiltinScope.ANY_TYPE);
                } else {
                    varSymbol.setType(valueExpr.getType());
                }
            } else {
                if (valueExpr instanceof HasTargetType) {
                    ((HasTargetType) valueExpr).setTargetType(varSymbol.getType());
                }
                valueExpr.analyze(context);
            }
        } else if (varSymbol.getType() == null) {
            varSymbol.setType(BuiltinScope.ANY_TYPE);
            context.addAnalyzerError(new InvalidVariableDeclError(varSymbol.getSourceCodeRef()));
        }

        var scope = context.getCurrentScope();

        scope.define(context.getAnalyzerContext(), varSymbol);

        if (valueExpr != null && !(valueExpr instanceof NullValueExpr)) {
            Type valueType = valueExpr.getType();
            if (valueType instanceof UnknownType) {
                return;
            }
            if (!varSymbol.getType().isAssignableFrom(valueType)) {
                context.addAnalyzerError(new InvalidAssignExprTypeError(valueExpr.getSourceCodeRef(),
                        varSymbol.getType(), valueType));
            } else if (varSymbol.getType() instanceof ModuleRefSymbol) {
                context.addAnalyzerError(new InvalidAssignExprTypeError(valueExpr.getSourceCodeRef(),
                        varSymbol.getType(), valueType));
                varSymbol.setType(BuiltinScope.ANY_TYPE);
            }
        }
    }

    @Override
    public void evalStat(EvalContext context) {
        var scope = context.getCurrentScope();
        scope.define(context.getAnalyzerContext(), varSymbol);
        if (valueExpr != null) {
            scope.setValue(varSymbol.getName(), valueExpr.evalExpr(context));
        } else {
            scope.setValue(varSymbol.getName(), new NullValueExpr());
        }
    }

    @Override
    public SourceCodeRef getSourceCodeRef() {
        return varSymbol.getSourceCodeRef();
    }

    public String getName() {
        return varSymbol.getName();
    }

}
