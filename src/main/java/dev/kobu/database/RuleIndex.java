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

package dev.kobu.database;

import dev.kobu.database.index.IndexNode;
import dev.kobu.database.index.impl.FieldIndexNode;
import dev.kobu.database.index.impl.JoinIndexNode;
import dev.kobu.database.index.impl.RootTypeIndexNode;
import dev.kobu.database.index.impl.RuleIndexNode;
import dev.kobu.interpreter.ast.AnalyzerContext;
import dev.kobu.interpreter.ast.eval.context.EvalContextProvider;
import dev.kobu.interpreter.ast.query.*;
import dev.kobu.interpreter.ast.symbol.ModuleScope;
import dev.kobu.interpreter.ast.symbol.RuleSymbol;

import java.util.*;

public class RuleIndex {

    private final List<RuleSymbol> rules = new ArrayList<>();

    private final List<RootTypeIndexNode> index = new ArrayList<>();

    private final Map<String, RuleIndexNode> ruleNodeMap = new HashMap<>();

    public void clear() {
        for (RootTypeIndexNode rootTypeIndexNode : index) {
            rootTypeIndexNode.clear();
        }
    }

    public void addRule(EvalContextProvider evalContextProvider, AnalyzerContext analyzerContext, RuleSymbol rule) {
        rules.add(rule);

        Query query = rule.getQuery();
        IndexNode node = addQueryTypeClause(evalContextProvider, analyzerContext,
                rule.getModuleScope(), query.getTypeClause());

        if (query.getExtractors() != null) {
            for (QueryExtractor extractor : query.getExtractors()) {
                node = addQueryExtractor(evalContextProvider, analyzerContext, node, extractor);
            }
        }

        if (query.getJoins() != null) {
            for (QueryJoin join : query.getJoins()) {
                node = addJoin(evalContextProvider, analyzerContext, node, join);
            }
        }

        RuleIndexNode ruleNode = new RuleIndexNode(rule, query, rule.getBlock());
        node.addChild(ruleNode);
        ruleNodeMap.put(rule.getFullName(), ruleNode);
    }

    public void linkRules() {
        for (RuleSymbol rule : rules) {
            if (rule.getParentRuleSymbol() != null) {
                RuleIndexNode parentNode = ruleNodeMap.get(rule.getParentRuleSymbol().getFullName());
                RuleIndexNode node = ruleNodeMap.get(rule.getFullName());
                parentNode.addChild(node);
            }
        }
        for (RuleSymbol rule : rules) {
            int priority = 0;
            RuleSymbol parent = rule.getParentRuleSymbol();
            RuleIndexNode node = ruleNodeMap.get(rule.getFullName());
            while (parent != null) {
                priority += 1;
                parent = parent.getParentRuleSymbol();
            }
            node.setPriority(priority);
        }
    }

    public void insertFact(Fact fact) {
        for (RootTypeIndexNode indexNode : index) {
            indexNode.receive(fact);
        }
    }

    public void run() {
        for (RootTypeIndexNode indexNode : index) {
            indexNode.beforeRun();
        }
        ruleNodeMap.values().stream()
            .sorted(Comparator.comparing(RuleIndexNode::getPriority).reversed())
            .forEach(RuleIndexNode::run);
        for (RootTypeIndexNode indexNode : index) {
            indexNode.afterRun();
        }
    }

    private IndexNode addQueryTypeClause(EvalContextProvider evalContextProvider, AnalyzerContext analyzerContext,
                                         ModuleScope moduleScope, QueryTypeClause queryTypeClause) {

        RootTypeIndexNode rootNode = new RootTypeIndexNode(evalContextProvider, analyzerContext, moduleScope, queryTypeClause);
        this.index.add(rootNode);

        QueryClause clause = queryTypeClause.getQueryClause();
        IndexNode lastNode = rootNode;
        while (clause != null) {
            FieldIndexNode node = new FieldIndexNode(clause);
            lastNode.addChild(node);
            clause = clause.getNext();
            lastNode = node;
        }

        return lastNode;
    }

    private IndexNode addQueryExtractor(EvalContextProvider evalContextProvider, AnalyzerContext analyzerContext,
                                        IndexNode parent, QueryExtractor queryExtractor) {
        FieldIndexNode lastNode = new FieldIndexNode(queryExtractor);
        parent.addChild(lastNode);
        QueryClause clause = queryExtractor.getQueryClause().getNext();
        while (clause != null) {
            FieldIndexNode node = new FieldIndexNode(clause);
            lastNode.addChild(node);
            clause = clause.getNext();
            lastNode = node;
        }

        return lastNode;
    }

    private IndexNode addJoin(EvalContextProvider evalContextProvider, AnalyzerContext analyzerContext,
                              IndexNode parent, QueryJoin queryJoin) {
        JoinIndexNode node = new JoinIndexNode(queryJoin);

        parent.addChild(node.getLeftSlot());
        addQueryTypeClause(evalContextProvider, analyzerContext, queryJoin.getTypeClause().getModuleScope(),
                queryJoin.getTypeClause()).addChild(node.getRightSlot());
        return node;
    }

}
