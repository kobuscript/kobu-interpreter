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
import dev.kobu.interpreter.ast.eval.expr.value.NullValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.BooleanValueExpr;
import dev.kobu.interpreter.ast.symbol.BuiltinScope;
import dev.kobu.interpreter.ast.symbol.Type;
import dev.kobu.interpreter.error.eval.InternalInterpreterError;
import dev.kobu.interpreter.error.analyzer.InvalidTypeError;
import dev.kobu.interpreter.error.eval.NullPointerError;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.ast.symbol.value.BooleanTypeSymbol;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class WhileStatement implements Statement {

    private final SourceCodeRef sourceCodeRef;

    private final Expr condExpr;

    private final List<Evaluable> block;

    public WhileStatement(SourceCodeRef sourceCodeRef, Expr condExpr, List<Evaluable> block) {
        this.sourceCodeRef = sourceCodeRef;
        this.condExpr = condExpr;
        this.block = block;
    }

    @Override
    public SourceCodeRef getSourceCodeRef() {
        return sourceCodeRef;
    }

    @Override
    public void setResolvedTypes(Map<String, Type> resolvedTypes) {
        if (condExpr != null) {
            condExpr.setResolvedTypes(resolvedTypes);
        }
        if (block != null) {
            block.forEach(evaluable -> evaluable.setResolvedTypes(resolvedTypes));
        }
    }

    @Override
    public void analyze(EvalContext context) {
        context.pushNewScope();
        var branch = context.pushNewBranch();
        branch.setCanInterrupt(true);

        try {
            condExpr.analyze(context);
            if (!(condExpr.getType() instanceof BooleanTypeSymbol)) {
                BooleanTypeSymbol booleanType = BuiltinScope.BOOLEAN_TYPE;
                context.addAnalyzerError(new InvalidTypeError(condExpr.getSourceCodeRef(),
                        booleanType, condExpr.getType()));
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
            ValueExpr condValue = condExpr.evalExpr(context);
            if (condValue instanceof NullValueExpr) {
                throw new NullPointerError(sourceCodeRef, condExpr.getSourceCodeRef());
            }
            if (!(condValue instanceof BooleanValueExpr)) {
                throw new InternalInterpreterError("Expected: Boolean. Found: " + condValue.getStringValue(new HashSet<>()),
                        condExpr.getSourceCodeRef());
            }

            boolean cond = ((BooleanValueExpr) condValue).getValue();

            while (cond) {
                var interrupt = context.evalBlock(block);
                if (interrupt != null) {
                    if (interrupt == InterruptTypeEnum.BREAK) {
                        break;
                    }
                    context.getCurrentBranch().setInterrupt(null);
                }

                cond = ((BooleanValueExpr) condExpr.evalExpr(context)).getValue();
            }
        } finally {
            context.popBranch();
            context.popScope();
        }
    }
}
