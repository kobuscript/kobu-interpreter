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
import dev.cgrscript.interpreter.ast.eval.expr.value.NullValueExpr;
import dev.cgrscript.interpreter.ast.eval.expr.value.BooleanValueExpr;
import dev.cgrscript.interpreter.ast.symbol.BuiltinScope;
import dev.cgrscript.interpreter.error.eval.InternalInterpreterError;
import dev.cgrscript.interpreter.error.analyzer.InvalidTypeError;
import dev.cgrscript.interpreter.error.eval.NullPointerError;
import dev.cgrscript.interpreter.ast.symbol.SourceCodeRef;
import dev.cgrscript.interpreter.ast.symbol.BooleanTypeSymbol;

import java.util.List;

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
    public void analyze(EvalContext context) {
        context.pushNewScope();
        var branch = context.pushNewBranch();
        branch.setCanInterrupt(true);

        condExpr.analyze(context);
        if (!(condExpr.getType() instanceof BooleanTypeSymbol)) {
            BooleanTypeSymbol booleanType = BuiltinScope.BOOLEAN_TYPE;
            context.addAnalyzerError(new InvalidTypeError(condExpr.getSourceCodeRef(),
                    booleanType, condExpr.getType()));
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

        ValueExpr condValue = condExpr.evalExpr(context);
        if (condValue instanceof NullValueExpr) {
            throw new NullPointerError(sourceCodeRef, condExpr.getSourceCodeRef());
        }
        if (!(condValue instanceof BooleanValueExpr)) {
            throw new InternalInterpreterError("Expected: Boolean. Found: " + condValue.getStringValue(),
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

            cond = ((BooleanValueExpr) condValue).getValue();
        }

        context.popBranch();
        context.popScope();
    }
}
