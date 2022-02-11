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
import dev.cgrscript.interpreter.ast.symbol.*;
import dev.cgrscript.interpreter.error.analyzer.InvalidAssignExprTypeError;
import dev.cgrscript.interpreter.error.analyzer.InvalidVariableDeclError;

public class VarDeclExpr implements Statement {

    private final VariableSymbol varSymbol;

    private Expr valueExpr;

    public VarDeclExpr(VariableSymbol varSymbol) {
        this.varSymbol = varSymbol;
    }

    public Expr getValueExpr() {
        return valueExpr;
    }

    public void setValueExpr(Expr valueExpr) {
        this.valueExpr = valueExpr;
    }

    @Override
    public void analyze(EvalContext context) {
        if (valueExpr != null) {
            if (varSymbol.getType() == null) {
                valueExpr.analyze(context);
                if (valueExpr.getType() == null) {
                    varSymbol.setType(BuiltinScope.ANY_TYPE);
                    context.getModuleScope().addError(new InvalidVariableDeclError(varSymbol.getSourceCodeRef()));
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
            context.getModuleScope().addError(new InvalidVariableDeclError(varSymbol.getSourceCodeRef()));
        }

        var scope = context.getCurrentScope();

        scope.define(varSymbol);

        if (valueExpr != null) {
            if (valueExpr.getType() instanceof UnknownType) {
                return;
            }
            if (!varSymbol.getType().isAssignableFrom(valueExpr.getType())) {
                context.getModuleScope().addError(new InvalidAssignExprTypeError(valueExpr.getSourceCodeRef(),
                        varSymbol.getType(), valueExpr.getType()));
            } else if (varSymbol.getType() instanceof ModuleRefSymbol) {
                context.getModuleScope().addError(new InvalidAssignExprTypeError(valueExpr.getSourceCodeRef(),
                        varSymbol.getType(), valueExpr.getType()));
                varSymbol.setType(BuiltinScope.ANY_TYPE);
            }
        }
    }

    @Override
    public void evalStat(EvalContext context) {
        var scope = context.getCurrentScope();
        scope.define(varSymbol);
        if (valueExpr != null) {
            scope.setValue(varSymbol.getName(), valueExpr.evalExpr(context));
        }
    }

    @Override
    public SourceCodeRef getSourceCodeRef() {
        return varSymbol.getSourceCodeRef();
    }

}
