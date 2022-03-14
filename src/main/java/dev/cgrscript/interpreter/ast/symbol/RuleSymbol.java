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

import dev.cgrscript.interpreter.ast.AnalyzerContext;
import dev.cgrscript.interpreter.ast.AnalyzerErrorScope;
import dev.cgrscript.interpreter.ast.eval.*;
import dev.cgrscript.interpreter.ast.eval.context.EvalContext;
import dev.cgrscript.interpreter.ast.eval.context.EvalContextProvider;
import dev.cgrscript.interpreter.ast.eval.context.EvalModeEnum;
import dev.cgrscript.interpreter.ast.query.Query;
import dev.cgrscript.interpreter.error.analyzer.CyclicRuleReferenceError;
import dev.cgrscript.interpreter.error.analyzer.IncompatibleRulesError;
import dev.cgrscript.interpreter.error.analyzer.InvalidParentRuleError;

import java.util.ArrayList;
import java.util.List;

public class RuleSymbol extends Symbol implements HasExpr {

    private final SourceCodeRef closeRuleRef;

    private final ModuleScope moduleScope;

    private final RuleTypeEnum ruleType;

    private final String docText;

    private RuleSymbol parentRuleSymbol;

    private Query query;

    private List<Evaluable> block;

    private SymbolDocumentation documentation;

    public RuleSymbol(SourceCodeRef sourceCodeRef, String name,
                      SourceCodeRef closeRuleRef, ModuleScope moduleScope, RuleTypeEnum ruleType,
                      String docText) {
        super(moduleScope, sourceCodeRef, name);
        this.closeRuleRef = closeRuleRef;
        this.moduleScope = moduleScope;
        this.ruleType = ruleType;
        this.docText = docText;
    }

    public ModuleScope getModuleScope() {
        return moduleScope;
    }

    public RuleTypeEnum getRuleType() {
        return ruleType;
    }

    public void setParentRuleSymbol(RuleSymbol parentRuleSymbol) {
        this.parentRuleSymbol = parentRuleSymbol;
    }

    public String getFullName() {
        return moduleScope.getModuleId() + "." + getName();
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

    public RuleSymbol getParentRuleSymbol() {
        return parentRuleSymbol;
    }

    @Override
    public void analyze(AnalyzerContext context, EvalContextProvider evalContextProvider) {
        AnalyzerErrorScope errorScope = context.getErrorScope();
        if (parentRuleSymbol != null) {
            List<String> path = new ArrayList<>();
            path.add(getName());
            analyzePath(context, this, path);

            if (!query.getTypeClause().compatibleWith(parentRuleSymbol.getQuery().getTypeClause())) {
                errorScope.addError(new IncompatibleRulesError(getSourceCodeRef(),
                        this, parentRuleSymbol));
            }
        }
        var evalContext = evalContextProvider.newEvalContext(context, moduleScope);
        int errors = errorScope.getErrors() != null ? errorScope.getErrors().size() : 0;
        if (query != null) {
            query.analyze(evalContext);
            if (errorScope.getErrors() == null || errors == errorScope.getErrors().size()) {
                //analyze block only if query has no errors
                evalContext.analyzeBlock(block);
            }
        }

        if (moduleScope.getEvalMode() == EvalModeEnum.ANALYZER_SERVICE) {
            String description;
            if (ruleType == RuleTypeEnum.RULE) {
                description = "def rule " + getName();
            } else if (ruleType == RuleTypeEnum.TEMPLATE) {
                description = "def template " + getName();
            } else {
                description = "def file " + getName();
            }
            if (parentRuleSymbol != null) {
                description += " extends " + parentRuleSymbol.getName();
            }
            documentation = new SymbolDocumentation(moduleScope.getModuleId(), SymbolTypeEnum.RULE, description, docText);
        }
    }

    @Override
    public SymbolDocumentation getDocumentation() {
        return documentation;
    }

    private void analyzePath(AnalyzerContext context, RuleSymbol rule, List<String> path) {
        if (rule.getParentRuleSymbol() == null) {
            return;
        }

        if (getFullName().equals(rule.getParentRuleSymbol().getFullName())) {
            context.getErrorScope().addError(new CyclicRuleReferenceError(getSourceCodeRef(), path));
            return;
        }
        path.add(rule.getParentRuleSymbol().getFullName());
        RuleSymbol parent = rule.getParentRuleSymbol();
        analyzePath(context, parent, path);
    }

}
