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

package dev.kobu.interpreter.ast.eval.statement;

import dev.kobu.interpreter.ast.eval.*;
import dev.kobu.interpreter.ast.eval.context.EvalContext;
import dev.kobu.interpreter.ast.eval.expr.value.BooleanValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.NullValueExpr;
import dev.kobu.interpreter.ast.eval.expr.VarDeclExpr;
import dev.kobu.interpreter.ast.symbol.BuiltinScope;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.ast.symbol.BooleanTypeSymbol;
import dev.kobu.interpreter.error.analyzer.InvalidExpressionError;
import dev.kobu.interpreter.error.analyzer.InvalidTypeError;
import dev.kobu.interpreter.error.eval.InternalInterpreterError;
import dev.kobu.interpreter.error.eval.NullPointerError;

import java.util.List;

public class ForStatement implements Statement {

    private final SourceCodeRef sourceCodeRef;

    private final List<VarDeclExpr> varDeclList;

    private final Expr condExpr;

    private final List<Statement> stepStatList;

    private final List<Evaluable> block;

    public ForStatement(SourceCodeRef sourceCodeRef, List<VarDeclExpr> varDeclList, Expr condExpr,
                        List<Statement> stepStatList, List<Evaluable> block) {
        this.sourceCodeRef = sourceCodeRef;
        this.varDeclList = varDeclList;
        this.condExpr = condExpr;
        this.stepStatList = stepStatList;
        this.block = block;
    }

    @Override
    public void analyze(EvalContext context) {
        context.pushNewScope();
        var branch = context.pushNewBranch();
        branch.setCanInterrupt(true);

        try {
            if (varDeclList != null) {
                for (VarDeclExpr varDeclExpr : varDeclList) {
                    varDeclExpr.analyze(context);
                }
            }

            var booleanType = BuiltinScope.BOOLEAN_TYPE;
            if (condExpr != null) {
                condExpr.analyze(context);
                if (!(condExpr.getType() instanceof BooleanTypeSymbol)) {
                    context.addAnalyzerError(new InvalidTypeError(condExpr.getSourceCodeRef(),
                            booleanType, condExpr.getType()));
                }
            }

            for (Statement stepStat : stepStatList) {
                if (!(stepStat instanceof Assignment)) {
                    context.addAnalyzerError(new InvalidExpressionError(stepStat.getSourceCodeRef()));
                } else {
                    stepStat.analyze(context);
                }
            }

            context.analyzeBlock(block);
        } finally {
            branch = context.popBranch();
            branch.setHasReturnStatement(false);
            context.popScope();
        }
    }

    @Override
    public void evalStat(EvalContext context) {
        context.pushNewScope();
        context.pushNewBranch();

        try {
            if (varDeclList != null) {
                for (VarDeclExpr varDeclExpr : varDeclList) {
                    varDeclExpr.evalStat(context);
                }
            }

            boolean cond = true;
            while (cond) {
                if (condExpr != null) {
                    ValueExpr valueExpr = condExpr.evalExpr(context);
                    if (valueExpr instanceof NullValueExpr) {
                        throw new NullPointerError(sourceCodeRef, condExpr.getSourceCodeRef());
                    }
                    if (!(valueExpr instanceof BooleanValueExpr)) {
                        throw new InternalInterpreterError("Expected: Boolean. Found: " +
                                valueExpr.getStringValue(), getSourceCodeRef());
                    }
                    cond = ((BooleanValueExpr) valueExpr).getValue();
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
        } finally {
            context.popBranch();
            context.popScope();
        }
    }

    @Override
    public SourceCodeRef getSourceCodeRef() {
        return sourceCodeRef;
    }

    public List<VarDeclExpr> getVarDeclList() {
        return varDeclList;
    }

    public Expr getCondExpr() {
        return condExpr;
    }

    public List<Statement> getStepStatList() {
        return stepStatList;
    }

    public List<Evaluable> getBlock() {
        return block;
    }
}
