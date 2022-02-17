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

import dev.cgrscript.database.match.Match;
import dev.cgrscript.database.match.MatchStateEnum;
import dev.cgrscript.interpreter.ast.eval.EvalContext;
import dev.cgrscript.interpreter.ast.eval.ValueExpr;
import dev.cgrscript.interpreter.ast.eval.expr.value.ArrayValueExpr;
import dev.cgrscript.interpreter.ast.eval.expr.value.PairValueExpr;
import dev.cgrscript.interpreter.ast.eval.expr.value.RecordValueExpr;
import dev.cgrscript.interpreter.ast.eval.expr.value.TemplateValueExpr;
import dev.cgrscript.interpreter.ast.symbol.RuleSymbol;

import java.util.*;

public class Database {

    private int nextRecordId = 1;

    private int nextMatchId = 1;

    private final RuleIndex ruleIndex = new RuleIndex();

    private final RuleIndex templateIndex = new RuleIndex();

    private final RuleIndex fileIndex = new RuleIndex();

    private final Queue<ValueExpr> buffer = new LinkedList<>();

    private final Map<Integer, ValueExpr> facts = new LinkedHashMap<>();

    private final Map<Integer, List<Integer>> matchGroupValuesMap = new HashMap<>();

    private final Map<Integer, Integer> valueMatchGroupMap = new HashMap<>();

    private final Map<Integer, List<Integer>> creatorIdRecordMap = new HashMap<>();

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
        facts.clear();
        ruleIndex.clear();
        templateIndex.clear();
        fileIndex.clear();
        matchGroupValuesMap.clear();
        valueMatchGroupMap.clear();
    }

    public void addRule(RuleSymbol ruleSymbol) {
        switch (ruleSymbol.getRuleType()) {
            case RULE:
                ruleIndex.addRule(ruleSymbol);
                break;
            case TEMPLATE:
                templateIndex.addRule(ruleSymbol);
                break;
            case FILE:
                fileIndex.addRule(ruleSymbol);
                break;
        }
    }

    public void linkRules() {
        ruleIndex.linkRules();
        templateIndex.linkRules();
        fileIndex.linkRules();
    }

    public void addRecord(RecordValueExpr record) {
        buffer.add(record);
        if (record.creatorId() != 0) {
            List<Integer> ids = creatorIdRecordMap.computeIfAbsent(record.creatorId(), k -> new ArrayList<>());
            ids.add(record.creatorId());
        }
    }

    public void addRecord(Match match, RecordValueExpr record) {
        addRecord(record);
        if (match.getMatchGroupId() != 0) {
            List<Integer> values = matchGroupValuesMap.computeIfAbsent(match.getMatchGroupId(), k -> new ArrayList<>());
            values.add(record.getId());
            valueMatchGroupMap.put(record.getId(), match.getMatchGroupId());
        }
    }

    public void addTemplateValue(Match match, TemplateValueExpr templateValue) {
        buffer.add(templateValue);
        if (templateValue.creatorId() != 0) {
            List<Integer> ids = creatorIdRecordMap.computeIfAbsent(templateValue.creatorId(), k -> new ArrayList<>());
            ids.add(templateValue.creatorId());
        }
        if (match.getMatchGroupId() != 0) {
            List<Integer> values = matchGroupValuesMap.computeIfAbsent(match.getMatchGroupId(), k -> new ArrayList<>());
            values.add(templateValue.getId());
            valueMatchGroupMap.put(templateValue.getId(), match.getMatchGroupId());
        }
    }

    public void updateRecord(RecordValueExpr record) {
        if (facts.containsKey(record.getId())) {
            buffer.add(record);
        }
    }

    public void clearMatchGroup(Match match) {
        if (match.getMatchGroupId() != 0) {
            clearMatchGroup(match.getMatchGroupId());
        }
    }

    private void clearMatchGroup(int matchGroupId) {
        List<Integer> values = matchGroupValuesMap.get(matchGroupId);
        if (values != null) {
            for (Integer id : values) {
                RuleIndex index = getCurrentIndex();
                if (index != null) {
                    index.clearMatchGroup(matchGroupId);
                    facts.remove(id);

                    List<Integer> children = creatorIdRecordMap.remove(id);
                    if (children != null) {
                        for (Integer child : children) {
                            Integer valueMatchGroupId = valueMatchGroupMap.remove(child);
                            if (valueMatchGroupId != null) {
                                clearMatchGroup(valueMatchGroupId);
                            }
                        }
                    }
                }
            }
        }
    }

    public void fireRules(EvalContext context) {
        this.running = true;

        currentStep = RuleStepEnum.RULE;

        while (currentStep != null) {

            Queue<ValueExpr> queue = new LinkedList<>();
            processBuffer(queue, buffer);

            ValueExpr fact;
            RuleIndex currentIndex = getCurrentIndex();
            if (currentIndex == null) return;
            while ((fact = queue.poll()) != null) {
                EvalContext factCtx = new EvalContext(context.getEvalMode(),
                        context.getModuleScope(), context.getDatabase(),
                        context.getInputParser(), context.getOutputWriter());
                currentIndex.insertFact(factCtx, fact);
            }

            MatchStateEnum matchState = MatchStateEnum.NO_DEPS;
            while (matchState != null) {
                currentIndex.run(matchState);
                if (!buffer.isEmpty()) {
                    break;
                }
                matchState = matchState.next();
            }

            if (matchState == null) {
                currentIndex.clear();
                currentStep = currentStep.next();
                buffer.addAll(facts.values());
            }
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

    private void processBuffer(Queue<ValueExpr> queue, Queue<ValueExpr> buffer) {
        Set<Integer> insertedFacts = new HashSet<>();
        ValueExpr fact;
        while ((fact = buffer.poll()) != null) {
            if (fact instanceof RecordValueExpr) {
                RecordValueExpr record = (RecordValueExpr) fact;
                if (!facts.containsKey(record.getId())) {
                    facts.put(record.getId(), record);
                }
                processRecord(queue, record, insertedFacts);
            } else if (fact instanceof TemplateValueExpr) {
                TemplateValueExpr templateValue = (TemplateValueExpr) fact;
                if (!facts.containsKey(templateValue.getId())) {
                    facts.put(templateValue.getId(), templateValue);
                }
                processTemplate(queue, templateValue, insertedFacts);
            }
        }
    }

    private void processRecord(Queue<ValueExpr> queue, RecordValueExpr record, Set<Integer> insertedFacts) {
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

    private void processTemplate(Queue<ValueExpr> queue, TemplateValueExpr template, Set<Integer> insertedFacts) {
        if (insertedFacts.add(template.getId())) {
            queue.add(template);
        }
    }
}
