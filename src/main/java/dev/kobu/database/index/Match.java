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

package dev.kobu.database.index;

import dev.kobu.interpreter.ast.eval.ValueExpr;
import dev.kobu.interpreter.ast.eval.context.ContextSnapshot;
import dev.kobu.interpreter.ast.eval.context.EvalContext;
import dev.kobu.interpreter.ast.eval.expr.value.RecordValueExpr;
import dev.kobu.interpreter.ast.symbol.VariableSymbol;

public class Match {

    private final int matchId;

    private final EvalContext context;

    private final ValueExpr value;

    private final RecordValueExpr rootRecord;

    private final String bind;

    public Match(EvalContext context, RecordValueExpr rootRecord, ValueExpr value, String bind) {
        this.context = context;
        this.rootRecord = rootRecord;
        this.matchId = context.getDatabase().generateMatchId();
        this.value = value;
        this.bind = bind;
        addValueToCtx(context, value, bind);
    }

    public Match(EvalContext context, RecordValueExpr rootRecord, ValueExpr value) {
        this(context, rootRecord, value, null);
    }

    public Match setValue(ValueExpr value, String bind) {
        return setValue(this.rootRecord, value, bind);
    }

    public Match setValue(RecordValueExpr rootRecord, ValueExpr value, String bind) {
        EvalContext matchCtx = context.newEvalContext();
        matchCtx.getCurrentScope().addAll(context.getCurrentScope());
        addValueToCtx(matchCtx, value, bind);
        return new Match(matchCtx, rootRecord, value, bind);
    }

    public Match merge(Match match) {
        EvalContext matchCtx = context.newEvalContext();
        matchCtx.getCurrentScope().addAll(context.getCurrentScope());
        matchCtx.getCurrentScope().addAll(match.context.getCurrentScope());
        return new Match(matchCtx, rootRecord, value, bind);
    }

    public EvalContext getContext() {
        return context;
    }

    public String getBind() {
        return bind;
    }

    public int getMatchId() {
        return matchId;
    }

    public RecordValueExpr getRootRecord() {
        return rootRecord;
    }

    public ValueExpr getValue() {
        return value;
    }

    public ContextSnapshot getSnapshot() {
        ContextSnapshot snapshot = new ContextSnapshot();
        if (context != null) {
            context.getCurrentScope().getSnapshot(snapshot);
        }
        return snapshot;
    }

    private void addValueToCtx(EvalContext evalContext, ValueExpr value, String bind) {
        if (bind != null) {
            evalContext.getCurrentScope().define(context.getAnalyzerContext(), new VariableSymbol(context.getModuleScope(),
                    bind, value.getType()));
            evalContext.getCurrentScope().setValue(bind, value);
        }
    }

}
