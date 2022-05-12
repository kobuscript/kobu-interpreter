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

import java.util.Objects;

public class Match {

    private final int matchId;

    private final EvalContext context;

    private final ValueExpr value;

    private final RecordValueExpr rootRecord;

    private final String bind;

    private MatchPath matchPath;

    public Match(int matchId, EvalContext context, RecordValueExpr rootRecord, ValueExpr value, String bind) {
        this.context = context;
        this.rootRecord = rootRecord;
        this.matchId = matchId;
        this.value = value;
        this.bind = bind;
        addValueToCtx(context, value, bind);
    }

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
        return setValue(value, bind, false);
    }

    public Match setValue(ValueExpr value, String bind, boolean newMatchId) {
        return setValue(this.rootRecord, value, bind, newMatchId);
    }

    public Match setValue(RecordValueExpr rootRecord, ValueExpr value, String bind) {
        return setValue(rootRecord, value, bind, false);
    }

    public Match setValue(RecordValueExpr rootRecord, ValueExpr value, String bind, boolean newMatchId) {
        EvalContext matchCtx = context.newEvalContext();
        matchCtx.getCurrentScope().addAll(context.getCurrentScope());
        addValueToCtx(matchCtx, value, bind);
        Match match = new Match(newMatchId ? context.getDatabase().generateMatchId() : matchId,
                matchCtx, rootRecord, value, bind);
        match.matchPath = matchPath;
        return match;
    }

    public Match merge(Match match) {
        return merge(null, match);
    }

    public Match merge(Integer matchId, Match match) {
        EvalContext matchCtx = context.newEvalContext();
        matchCtx.getCurrentScope().addAll(context.getCurrentScope());
        matchCtx.getCurrentScope().addAll(match.context.getCurrentScope());
        Match newMatch;
        if (matchId != null) {
            newMatch = new Match(matchId, matchCtx, rootRecord, value, bind);
        } else {
            newMatch = new Match(matchCtx, rootRecord, value, bind);
        }
        if (matchPath == null) {
            newMatch.matchPath = new MatchPath(this.matchId, match.matchId);
        } else {
            newMatch.matchPath = matchPath.add(this.matchId, match.matchId);
        }
        return newMatch;
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

    public boolean overrides(Match match) {
        return matchPath != null && matchPath.equals(match.matchPath);
    }

    public MatchPath getMatchPath() {
        return matchPath;
    }

    private void addValueToCtx(EvalContext evalContext, ValueExpr value, String bind) {
        if (bind != null) {
            if (evalContext.getCurrentScope().resolveLocal(bind) == null) {
                evalContext.getCurrentScope().define(context.getAnalyzerContext(), new VariableSymbol(context.getModuleScope(),
                        bind, value.getType()));
            }
            evalContext.getCurrentScope().setValue(bind, value);
        }
    }

    public static class MatchPath {

        private final int left;

        private final int right;

        private MatchPath next;

        public MatchPath(int left, int right) {
            this.left = left;
            this.right = right;
        }

        public MatchPath add(int left, int right) {
            MatchPath matchPath = new MatchPath(left, right);
            matchPath.next = this;
            return matchPath;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MatchPath matchPath = (MatchPath) o;
            return left == matchPath.left && right == matchPath.right && Objects.equals(next, matchPath.next);
        }

        @Override
        public int hashCode() {
            return Objects.hash(left, right, next);
        }

    }

}
