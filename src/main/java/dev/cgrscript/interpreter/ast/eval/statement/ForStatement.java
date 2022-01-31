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

package dev.cgrscript.interpreter.ast.eval.statement;

import dev.cgrscript.interpreter.ast.eval.*;
import dev.cgrscript.interpreter.ast.eval.expr.value.BooleanValueExpr;
import dev.cgrscript.interpreter.ast.eval.expr.value.NullValueExpr;
import dev.cgrscript.interpreter.ast.eval.expr.VarDeclExpr;
import dev.cgrscript.interpreter.ast.symbol.BuiltinScope;
import dev.cgrscript.interpreter.ast.symbol.SourceCodeRef;
import dev.cgrscript.interpreter.ast.symbol.BooleanTypeSymbol;
import dev.cgrscript.interpreter.error.analyzer.InvalidExpressionError;
import dev.cgrscript.interpreter.error.analyzer.InvalidTypeError;
import dev.cgrscript.interpreter.error.eval.InternalInterpreterError;
import dev.cgrscript.interpreter.error.eval.NullPointerError;

import java.util.List;

public class ForStatement implements Statement {

    private final SourceCodeRef sourceCodeRef;

    private final List<VarDeclExpr> varDeclList;

    private final List<Expr> condExprList;

    private final List<Statement> stepStatList;

    private final List<Evaluable> block;

    public ForStatement(SourceCodeRef sourceCodeRef, List<VarDeclExpr> varDeclList, List<Expr> condExprList,
                        List<Statement> stepStatList, List<Evaluable> block) {
        this.sourceCodeRef = sourceCodeRef;
        this.varDeclList = varDeclList;
        this.condExprList = condExprList;
        this.stepStatList = stepStatList;
        this.block = block;
    }

    @Override
    public void analyze(EvalContext context) {
        context.pushNewScope();
        var branch = context.pushNewBranch();
        branch.setCanInterrupt(true);

        if (varDeclList != null) {
            for (VarDeclExpr varDeclExpr : varDeclList) {
                varDeclExpr.analyze(context);
            }
        }

        var booleanType = BuiltinScope.BOOLEAN_TYPE;
        for (Expr expr : condExprList) {
            expr.analyze(context);
            if (!(expr.getType() instanceof BooleanTypeSymbol)) {
                context.getModuleScope().addError(new InvalidTypeError(expr.getSourceCodeRef(),
                        booleanType, expr.getType()));
            }
        }

        for (Statement stepStat : stepStatList) {
            if (!(stepStat instanceof Assignment)) {
                context.getModuleScope().addError(new InvalidExpressionError(stepStat.getSourceCodeRef()));
            } else {
                stepStat.analyze(context);
            }
        }

        context.analyzeBlock(block);

        branch = context.popBranch();
        branch.setHasReturnStatement(false);
        context.popScope();
    }

    @Override
    public void evalStat(EvalContext context) {
        context.pushNewScope();
        context.pushNewBranch();

        if (varDeclList != null) {
            for (VarDeclExpr varDeclExpr : varDeclList) {
                varDeclExpr.evalStat(context);
            }
        }

        boolean cond = true;
        while(cond) {
            for (Expr expr : condExprList) {
                ValueExpr valueExpr = expr.evalExpr(context);
                if (valueExpr instanceof NullValueExpr) {
                    throw new NullPointerError(sourceCodeRef, expr.getSourceCodeRef());
                }
                if (!(valueExpr instanceof BooleanValueExpr)) {
                    throw new InternalInterpreterError("Expected: Boolean. Found: " +
                            valueExpr.getStringValue(), getSourceCodeRef());
                }
                boolean exprResult = ((BooleanValueExpr) valueExpr).getValue();
                cond = cond && exprResult;
            }

            if (cond) {
                var interrupt = context.evalBlock(block);
                if (interrupt != null) {
                    if (interrupt == InterruptTypeEnum.BREAK) {
                        break;
                    }
                    context.getCurrentBranch().setInterrupt(null);
                }

                for (Statement stepStat : stepStatList) {
                    stepStat.evalStat(context);
                }
            }
        }

        context.popBranch();
        context.popScope();
    }

    @Override
    public SourceCodeRef getSourceCodeRef() {
        return sourceCodeRef;
    }

    public List<VarDeclExpr> getVarDeclList() {
        return varDeclList;
    }

    public List<Expr> getCondExprList() {
        return condExprList;
    }

    public List<Statement> getStepStatList() {
        return stepStatList;
    }

    public List<Evaluable> getBlock() {
        return block;
    }
}
