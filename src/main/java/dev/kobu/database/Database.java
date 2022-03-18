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

import dev.kobu.interpreter.ast.AnalyzerContext;
import dev.kobu.interpreter.ast.eval.ValueExpr;
import dev.kobu.interpreter.ast.eval.context.EvalContext;
import dev.kobu.interpreter.ast.eval.context.EvalContextProvider;
import dev.kobu.interpreter.ast.eval.expr.value.ArrayValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.PairValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.RecordValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.TemplateValueExpr;
import dev.kobu.interpreter.ast.symbol.RuleSymbol;

import java.util.*;

public class Database {

    private int nextRecordId = 1;

    private int nextMatchId = 1;

    private final RuleIndex ruleIndex = new RuleIndex();

    private final RuleIndex templateIndex = new RuleIndex();

    private final RuleIndex fileIndex = new RuleIndex();

    private final Queue<Fact> buffer = new LinkedList<>();

    private final Map<Integer, Fact> factMap = new LinkedHashMap<>();

    private final List<Fact> facts = new ArrayList<>();

    private int iteration = 0;

    private RuleStepEnum currentStep;

    private boolean running;

    public int generateRecordId() {
        return nextRecordId++;
    }

    public int generateMatchId() {
        return nextMatchId++;
    }

    public void clear() {
        buffer.clear();
        factMap.clear();
        ruleIndex.clear();
        templateIndex.clear();
        fileIndex.clear();
        iteration = 0;
    }

    public void addRule(EvalContextProvider evalContextProvider, AnalyzerContext analyzerContext, RuleSymbol ruleSymbol) {
        switch (ruleSymbol.getRuleType()) {
            case RULE:
                ruleIndex.addRule(evalContextProvider, analyzerContext, ruleSymbol);
                break;
            case TEMPLATE:
                templateIndex.addRule(evalContextProvider, analyzerContext, ruleSymbol);
                break;
            case FILE:
                fileIndex.addRule(evalContextProvider, analyzerContext, ruleSymbol);
                break;
        }
    }

    public void linkRules() {
        ruleIndex.linkRules();
        templateIndex.linkRules();
        fileIndex.linkRules();
    }

    public void insertFact(Fact newFact) {
        buffer.add(newFact);
        newFact.setIteration(iteration);
        var it = facts.iterator();
        while (it.hasNext()) {
            var fact = it.next();
            if (newFact.overrides(fact)) {
                it.remove();
                factMap.remove(fact.getId());
            }
        }
    }

    public void updateRecord(RecordValueExpr record) {
        record.incVersion();
        if (factMap.containsKey(record.getId())) {
            buffer.add(record);
        }
    }

    public void fireRules(EvalContext context) {
        this.running = true;

        currentStep = RuleStepEnum.RULE;

        while (currentStep != null) {
            iteration++;

            Queue<Fact> queue = new LinkedList<>();
            processBuffer(queue, buffer);

            Fact fact;
            RuleIndex currentIndex = getCurrentIndex();
            if (currentIndex == null) return;
            while ((fact = queue.poll()) != null) {
                currentIndex.insertFact(fact);
            }

            currentIndex.run();
            if (!buffer.isEmpty()) {
                continue;
            }

            currentIndex.clear();
            currentStep = currentStep.next();
            buffer.addAll(factMap.values());
        }
        running = false;
    }

    private RuleIndex getCurrentIndex() {
        switch (currentStep) {
            case RULE:
                return ruleIndex;
            case TEMPLATE:
                return templateIndex;
            case FILE:
                return fileIndex;
            default:
                return null;
        }
    }

    public boolean isRunning() {
        return running;
    }

    private void processBuffer(Queue<Fact> queue, Queue<Fact> buffer) {
        Set<Integer> insertedFacts = new HashSet<>();
        Fact fact;
        while ((fact = buffer.poll()) != null) {
            if (fact instanceof RecordValueExpr) {
                RecordValueExpr record = (RecordValueExpr) fact;
                if (!factMap.containsKey(record.getId())) {
                    factMap.put(record.getId(), record);
                    facts.add(record);
                }
                processRecord(queue, record, insertedFacts);
            } else if (fact instanceof TemplateValueExpr) {
                TemplateValueExpr templateValue = (TemplateValueExpr) fact;
                if (!factMap.containsKey(templateValue.getId())) {
                    factMap.put(templateValue.getId(), templateValue);
                    facts.add(templateValue);
                }
                processTemplate(queue, templateValue, insertedFacts);
            }
        }
    }

    private void processRecord(Queue<Fact> queue, RecordValueExpr record, Set<Integer> insertedFacts) {
        if (insertedFacts.add(record.getId())) {
            queue.add(record);

            for (ValueExpr value : record.getValues()) {
                if (value instanceof RecordValueExpr) {
                    processRecord(queue, (RecordValueExpr) value, insertedFacts);
                } else if (value instanceof ArrayValueExpr) {
                    for (ValueExpr arrayItem : ((ArrayValueExpr) value).getValue()) {
                        if (arrayItem instanceof RecordValueExpr) {
                            processRecord(queue, (RecordValueExpr) arrayItem, insertedFacts);
                        }
                    }
                } else if (value instanceof PairValueExpr) {
                    PairValueExpr pairValue = (PairValueExpr) value;
                    if (pairValue.getLeftValue() instanceof RecordValueExpr) {
                        processRecord(queue, (RecordValueExpr) pairValue.getLeftValue(), insertedFacts);
                    }
                    if (pairValue.getRightValue() instanceof RecordValueExpr) {
                        processRecord(queue, (RecordValueExpr) pairValue.getRightValue(), insertedFacts);
                    }
                }
            }
        }
    }

    private void processTemplate(Queue<Fact> queue, TemplateValueExpr template, Set<Integer> insertedFacts) {
        if (insertedFacts.add(template.getId())) {
            queue.add(template);
        }
    }
}
