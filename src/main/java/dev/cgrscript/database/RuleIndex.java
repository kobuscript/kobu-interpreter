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

package dev.cgrscript.database;

import dev.cgrscript.database.index.IndexNode;
import dev.cgrscript.database.index.impl.JoinIndexNode;
import dev.cgrscript.database.index.impl.PipeIndexNode;
import dev.cgrscript.database.index.impl.RootTypeIndexNode;
import dev.cgrscript.database.index.impl.RuleIndexNode;
import dev.cgrscript.database.match.Match;
import dev.cgrscript.database.match.MatchStateEnum;
import dev.cgrscript.interpreter.ast.eval.EvalContext;
import dev.cgrscript.interpreter.ast.eval.ValueExpr;
import dev.cgrscript.interpreter.ast.eval.expr.value.RecordValueExpr;
import dev.cgrscript.interpreter.ast.query.Query;
import dev.cgrscript.interpreter.ast.query.QueryJoin;
import dev.cgrscript.interpreter.ast.query.QueryPipeClause;
import dev.cgrscript.interpreter.ast.query.QueryTypeClause;
import dev.cgrscript.interpreter.ast.symbol.RuleSymbol;

import java.util.*;

public class RuleIndex {

    private final List<RuleSymbol> rules = new ArrayList<>();

    private final List<RootTypeIndexNode> index = new ArrayList<>();

    private final Map<String, RootTypeIndexNode> rootNodeMap = new HashMap<>();

    private final Map<String, RuleIndexNode> ruleNodeMap = new HashMap<>();

    public void clear() {
        for (RootTypeIndexNode rootTypeIndexNode : index) {
            rootTypeIndexNode.clear();
        }
    }

    public void clearMatchGroup(int matchGroupId) {
        for (RootTypeIndexNode rootTypeIndexNode : index) {
            rootTypeIndexNode.clearMatchGroup(matchGroupId);
        }
    }

    public void addRule(RuleSymbol rule) {
        rules.add(rule);

        Query query = rule.getQuery();
        IndexNode node = addQueryTypeClause(query.getTypeClause());

        if (query.getJoins() != null) {
            for (QueryJoin join : query.getJoins()) {
                node = addJoin(node, join);
            }
        }

        RuleIndexNode ruleNode = new RuleIndexNode(rule, query, rule.getBlock());
        node.addChild(ruleNode);
        ruleNodeMap.put(rule.getName(), ruleNode);
    }

    public void linkRules() {
        for (RuleSymbol rule : rules) {
            if (rule.getParentRule() != null) {
                RuleIndexNode parentNode = ruleNodeMap.get(rule.getParentRule());
                RuleIndexNode node = ruleNodeMap.get(rule.getName());
                parentNode.addChild(node);
            }
        }
        for (RuleSymbol rule : rules) {
            int priority = 0;
            String parent = rule.getParentRule();
            RuleIndexNode node = ruleNodeMap.get(rule.getName());
            while (parent != null) {
                priority += 1;
                parent = ruleNodeMap.get(parent).getRuleSymbol().getParentRule();
            }
            node.setPriority(priority);
        }
    }

    public void insertFact(EvalContext context, ValueExpr fact) {
        RecordValueExpr record = null;
        if (fact instanceof RecordValueExpr) {
            record = (RecordValueExpr) fact;
        }
        Match match = new Match(context, record, fact);
        for (RootTypeIndexNode indexNode : index) {
            indexNode.receive(match);
        }
    }

    public void run(MatchStateEnum mode) {
        ruleNodeMap.values().stream()
            .sorted(Comparator.comparing(RuleIndexNode::getPriority).reversed())
            .forEach(node -> node.run(mode));
    }

    private IndexNode addQueryTypeClause(QueryTypeClause queryTypeClause) {

        RootTypeIndexNode rootNode = rootNodeMap.computeIfAbsent(queryTypeClause.getKey(),
                k -> {
                    RootTypeIndexNode node = new RootTypeIndexNode(queryTypeClause);
                    this.index.add(node);
                    return node;
                });

        QueryPipeClause clause = queryTypeClause.getPipeClause();
        IndexNode lastNode = rootNode;
        while (clause != null) {
            PipeIndexNode node = new PipeIndexNode(clause);
            lastNode.addChild(node);
            clause = clause.getNext();
            lastNode = node;
        }

        return lastNode;
    }

    private IndexNode addJoin(IndexNode parent, QueryJoin queryJoin) {
        JoinIndexNode node = new JoinIndexNode(queryJoin);

        parent.addChild(node.getLeftSlot());
        addQueryTypeClause(queryJoin.getTypeClause()).addChild(node.getRightSlot());
        return node;
    }

}
