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

package dev.cgrscript.database.match;

import dev.cgrscript.interpreter.ast.eval.EvalContext;
import dev.cgrscript.interpreter.ast.eval.ValueExpr;
import dev.cgrscript.interpreter.ast.eval.expr.value.RecordValueExpr;
import dev.cgrscript.interpreter.ast.symbol.VariableSymbol;

public class Match {

    private final int matchGroupId;

    private final int matchId;

    private final EvalContext context;

    private final ValueExpr fact;

    private final MatchStateEnum state;

    private final RecordValueExpr rootRecord;

    public Match(EvalContext context, RecordValueExpr rootRecord, ValueExpr fact) {
        this.context = context;
        this.rootRecord = rootRecord;
        this.matchGroupId = 0;
        this.matchId = context.getDatabase().generateMatchId();
        this.fact = fact;
        this.state = MatchStateEnum.NO_DEPS;
    }

    public Match(int matchGroupId, EvalContext context, RecordValueExpr rootRecord, ValueExpr fact, MatchStateEnum state) {
        this.context = context;
        this.rootRecord = rootRecord;
        this.fact = fact;
        this.matchGroupId = matchGroupId;
        this.matchId = context.getDatabase().generateMatchId();
        this.state = state;
    }

    public Match setFact(ValueExpr fact, String bind) {
        return setFact(this.rootRecord, fact, bind);
    }

    public Match setFact(RecordValueExpr rootRecord, ValueExpr fact, String bind) {
        EvalContext matchCtx = new EvalContext(context.getEvalMode(), context.getModuleScope(), context.getDatabase(),
                context.getInputParser(), context.getOutputWriter());
        matchCtx.getCurrentScope().addAll(context.getCurrentScope());
        if (bind != null) {
            matchCtx.getCurrentScope().define(new VariableSymbol(bind, fact.getType()));
            matchCtx.getCurrentScope().setValue(bind, fact);
        }
        return new Match(matchGroupId, matchCtx, rootRecord, fact, state);
    }

    public Match merge(Match match) {
        EvalContext matchCtx = new EvalContext(context.getEvalMode(), context.getModuleScope(), context.getDatabase(),
                context.getInputParser(), context.getOutputWriter());
        matchCtx.getCurrentScope().addAll(context.getCurrentScope());
        matchCtx.getCurrentScope().addAll(match.context.getCurrentScope());
        return new Match(matchGroupId, matchCtx, rootRecord, fact, state);
    }

    public Match merge(EvalContext context, boolean dependencyResolved) {
        MatchStateEnum state = this.state;
        if (dependencyResolved) {
            if (state == MatchStateEnum.DEPS_NOT_RESOLVED) {
                state = MatchStateEnum.DEPS_PARTIALLY_RESOLVED;
            } else if (state == MatchStateEnum.NO_DEPS) {
                state = MatchStateEnum.DEPS_RESOLVED;
            }
        } else {
            if (state == MatchStateEnum.NO_DEPS) {
                state = MatchStateEnum.DEPS_NOT_RESOLVED;
            } else if (state == MatchStateEnum.DEPS_RESOLVED) {
                state = MatchStateEnum.DEPS_PARTIALLY_RESOLVED;
            }
        }

        return new Match(matchGroupId != 0 ? matchGroupId : matchId, context,
                rootRecord, fact, state);
    }

    public EvalContext getContext() {
        return context;
    }

    public int getMatchId() {
        return matchId;
    }

    public int getMatchGroupId() {
        return matchGroupId;
    }

    public RecordValueExpr getRootRecord() {
        return rootRecord;
    }

    public ValueExpr getFact() {
        return fact;
    }

    public MatchStateEnum getState() {
        return state;
    }

}
