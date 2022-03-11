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

package dev.cgrscript.database.index.impl;

import dev.cgrscript.database.index.Match;
import dev.cgrscript.interpreter.ast.eval.Evaluable;
import dev.cgrscript.interpreter.ast.eval.Expr;
import dev.cgrscript.interpreter.ast.eval.RuleContext;
import dev.cgrscript.interpreter.ast.eval.ValueExpr;
import dev.cgrscript.interpreter.ast.eval.context.ContextSnapshot;
import dev.cgrscript.interpreter.ast.eval.context.EvalContext;
import dev.cgrscript.interpreter.ast.eval.expr.value.BooleanValueExpr;
import dev.cgrscript.interpreter.ast.symbol.RuleSymbol;
import dev.cgrscript.interpreter.error.eval.InternalInterpreterError;

import java.util.List;

public class RuleInstance implements RuleContext {

    private final RuleSymbol ruleSymbol;

    private final Expr whenExpr;

    private final List<Evaluable> block;

    private boolean executed = false;

    private Match currentMatch;

    private ContextSnapshot lastSnapshot;

    public RuleInstance(RuleSymbol ruleSymbol, Expr whenExpr, List<Evaluable> block) {
        this.ruleSymbol = ruleSymbol;
        this.whenExpr = whenExpr;
        this.block = block;
    }

    public void run(Match match) {
        ContextSnapshot snapshot = match.getSnapshot();
        if (lastSnapshot != null && lastSnapshot.equals(snapshot)) {
            return;
        }
        lastSnapshot = snapshot;
        this.currentMatch = match;
        EvalContext matchCtx = match.getContext();
        EvalContext evalContext = matchCtx.newEvalContext(this);
        evalContext.getCurrentScope().addAll(matchCtx.getCurrentScope());
        if (executeWhenExpression(evalContext)) {
            evalContext.evalBlock(block);
            executed = true;
        }
    }

    public boolean executed() {
        return executed;
    }

    private boolean executeWhenExpression(EvalContext context) {
        if (whenExpr != null) {
            ValueExpr valueExpr = whenExpr.evalExpr(context);
            if (!(valueExpr instanceof BooleanValueExpr)) {
                throw new InternalInterpreterError("Expected: Boolean. Found: " + valueExpr.getStringValue(),
                        valueExpr.getSourceCodeRef());
            }
            return ((BooleanValueExpr) valueExpr).getValue();
        }
        return true;
    }

    @Override
    public RuleSymbol getRuleSymbol() {
        return ruleSymbol;
    }

    @Override
    public Match getMatch() {
        return currentMatch;
    }

}
