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
import dev.cgrscript.database.index.Match;
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

    private final Queue<Match> matchQueue = new LinkedList<>();

    private List<RuleIndexNode> children;

    private int priority;

    public RuleIndexNode(RuleSymbol ruleSymbol, Query query, List<Evaluable> block) {
        this.ruleSymbol = ruleSymbol;
        this.query = query;
        this.block = block;
    }

    @Override
    public void receive(Match match) {
        matchQueue.add(match);
    }

    @Override
    public void clear() {
        removeInstances();
        super.clear();
    }

    private void removeInstances() {
        contextMap.clear();
        matchQueue.clear();
    }

    public void run() {
        Match match;
        while ((match = matchQueue.poll()) != null) {
            var record = match.getRootRecord();
            if (children != null) {
                if (children.stream().noneMatch(child -> child.executed(record))) {
                    runMatch(match);
                }
            } else {
                runMatch(match);
            }
        }
    }

    private void runMatch(Match match) {
        var record = match.getRootRecord();
        if (record != null) {
            var ctx = contextMap.computeIfAbsent(record.getId(),
                    key -> new RuleInstance(ruleSymbol, query.getWhenExpr(), block));
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
