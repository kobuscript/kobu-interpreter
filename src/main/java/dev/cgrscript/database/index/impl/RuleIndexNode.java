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

import dev.cgrscript.database.index.OneInputIndexNode;
import dev.cgrscript.database.match.Match;
import dev.cgrscript.database.match.MatchRef;
import dev.cgrscript.database.match.MatchStateEnum;
import dev.cgrscript.interpreter.ast.eval.Evaluable;
import dev.cgrscript.interpreter.ast.eval.expr.value.RecordValueExpr;
import dev.cgrscript.interpreter.ast.query.Query;
import dev.cgrscript.interpreter.ast.symbol.RuleSymbol;

import java.util.*;

public class RuleIndexNode extends OneInputIndexNode {

    private final RuleSymbol ruleSymbol;

    private final Query query;

    private final List<Evaluable> block;

    private final Map<Integer, RuleInstance> contextMap = new HashMap<>();

    private final Map<Integer, MatchRef> matchMap = new HashMap<>();

    private final Queue<MatchRef> matchQueue = new LinkedList<>();

    private final RuleIndexNodeTypeEnum type;

    private List<RuleIndexNode> children;

    private int priority;

    public RuleIndexNode(RuleSymbol ruleSymbol, Query query, List<Evaluable> block) {
        this.ruleSymbol = ruleSymbol;
        this.query = query;
        this.block = block;
        if (query.hasDependencies()) {
            type = RuleIndexNodeTypeEnum.WITH_DEPS;
        } else {
            type = RuleIndexNodeTypeEnum.NO_DEPS;
        }
    }

    @Override
    public void receive(Match match) {
        if (match.getMatchGroupId() != 0) {
            var ref = matchMap.computeIfAbsent(match.getMatchGroupId(), k -> new MatchRef());
            if (ref.getMatch() == null) {
                matchQueue.add(ref);
            }
            ref.setMatch(match);
        } else {
            matchQueue.add(new MatchRef(match));
        }
    }

    @Override
    public void clear() {
        removeInstances();
        super.clear();
    }

    private void removeInstances() {
        matchMap.clear();
        contextMap.clear();
        matchQueue.clear();
    }

    public void run(MatchStateEnum state) {
        MatchRef matchRef;
        List<MatchRef> matchList = new ArrayList<>();
        while ((matchRef = matchQueue.poll()) != null) {
            var match = matchRef.getMatch();

            if (match.getState().equals(state)) {
                match.getContext().getDatabase().clearMatchGroup(match);
                var record = match.getRootRecord();
                if (children != null) {
                    if (children.stream().noneMatch(child -> child.executed(record))) {
                        runMatch(match);
                    }
                } else {
                    runMatch(match);
                }
                matchMap.remove(match.getMatchGroupId());
            } else {
                matchList.add(matchRef);
            }
        }
        matchQueue.addAll(matchList);
    }

    private void runMatch(Match match) {
        var record = match.getRootRecord();
        if (record != null) {
            var ctx = contextMap.computeIfAbsent(record.getId(),
                    key -> new RuleInstance(query.getWhenExpr(), block));
            ctx.run(match);
        }
    }

    public void addChild(RuleIndexNode child) {
        if (children == null) {
            children = new ArrayList<>();
        }
        children.add(child);
    }

    public boolean executed(RecordValueExpr record) {
        var instance = contextMap.get(record.getId());
        if (instance != null) {
            return instance.executed();
        }
        return false;
    }

    public RuleIndexNodeTypeEnum getType() {
        return type;
    }

    public RuleSymbol getRuleSymbol() {
        return ruleSymbol;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }
}
