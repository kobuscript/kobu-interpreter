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

package dev.cgrscript.interpreter.ast.symbol;

import dev.cgrscript.database.Database;
import dev.cgrscript.interpreter.ast.AnalyzerContext;
import dev.cgrscript.interpreter.ast.AnalyzerErrorScope;
import dev.cgrscript.interpreter.ast.eval.EvalContext;
import dev.cgrscript.interpreter.ast.eval.Evaluable;
import dev.cgrscript.interpreter.ast.query.Query;
import dev.cgrscript.interpreter.error.analyzer.CyclicRuleReferenceError;
import dev.cgrscript.interpreter.error.analyzer.IncompatibleRulesError;
import dev.cgrscript.interpreter.error.analyzer.InvalidParentRuleError;
import dev.cgrscript.interpreter.input.InputReader;
import dev.cgrscript.interpreter.writer.OutputWriter;

import java.util.ArrayList;
import java.util.List;

public class RuleSymbol extends Symbol implements HasExpr {

    private final SourceCodeRef closeRuleRef;

    private final ModuleScope moduleScope;

    private final RuleTypeEnum ruleType;

    private final String parentRule;

    private Query query;

    private List<Evaluable> block;

    public RuleSymbol(SourceCodeRef sourceCodeRef, String name, SourceCodeRef closeRuleRef,
                      ModuleScope moduleScope, RuleTypeEnum ruleType, String parentRule) {
        super(moduleScope, sourceCodeRef, name);
        this.closeRuleRef = closeRuleRef;
        this.moduleScope = moduleScope;
        this.ruleType = ruleType;
        this.parentRule = parentRule;
    }

    public ModuleScope getModuleScope() {
        return moduleScope;
    }

    public RuleTypeEnum getRuleType() {
        return ruleType;
    }

    public String getParentRule() {
        return parentRule;
    }

    public Query getQuery() {
        return query;
    }

    public void setQuery(Query query) {
        this.query = query;
    }

    public List<Evaluable> getBlock() {
        return block;
    }

    public void setBlock(List<Evaluable> block) {
        this.block = block;
    }

    public SourceCodeRef getCloseRuleRef() {
        return closeRuleRef;
    }

    @Override
    public void analyze(AnalyzerContext context, Database database, InputReader inputReader, OutputWriter outputWriter) {
        AnalyzerErrorScope errorScope = context.getErrorScope();
        if (parentRule != null) {
            Symbol parentRuleSym = moduleScope.resolve(parentRule);
            if (!(parentRuleSym instanceof RuleSymbol)) {
                errorScope.addError(new InvalidParentRuleError(getSourceCodeRef(), parentRule));
            } else {
                List<String> path = new ArrayList<>();
                path.add(getName());
                analyzePath(context, this, path);

                if (!query.getTypeClause().compatibleWith(((RuleSymbol)parentRuleSym).getQuery().getTypeClause())) {
                    errorScope.addError(new IncompatibleRulesError(getSourceCodeRef(),
                            this, (RuleSymbol) parentRuleSym));
                }
            }
        }
        var evalContext = new EvalContext(context, moduleScope.getEvalMode(), moduleScope, database, inputReader, outputWriter);
        int errors = errorScope.getErrors() != null ? errorScope.getErrors().size() : 0;
        if (query != null) {
            query.analyze(evalContext);
            if (errorScope.getErrors() == null || errors == errorScope.getErrors().size()) {
                //analyze block if the query has no errors
                evalContext.analyzeBlock(block);
            }
        }
    }

    private void analyzePath(AnalyzerContext context, RuleSymbol rule, List<String> path) {
        if (rule.getParentRule() == null) {
            return;
        }

        if (getName().equals(rule.getParentRule())) {
            context.getErrorScope().addError(new CyclicRuleReferenceError(getSourceCodeRef(), path));
            return;
        }
        path.add(rule.getParentRule());
        RuleSymbol parent = (RuleSymbol) moduleScope.resolve(rule.getParentRule());
        analyzePath(context, parent, path);
    }
}
