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

package dev.kobu.interpreter.ast.template;

import dev.kobu.database.index.Match;
import dev.kobu.interpreter.ast.eval.context.EvalContext;
import dev.kobu.interpreter.ast.eval.Statement;
import dev.kobu.interpreter.ast.eval.expr.value.RecordValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.TemplateValueExpr;
import dev.kobu.interpreter.ast.symbol.*;
import dev.kobu.interpreter.error.analyzer.InvalidTypeError;

public abstract class TemplateStatement implements Statement {

    private TemplateStatement next;

    private boolean root;

    private Type targetType;

    private SourceCodeRef targetTypeSourceCodeRef;

    public TemplateStatement getNext() {
        return next;
    }

    public void setNext(TemplateStatement next) {
        this.next = next;
    }

    public Type getTargetType() {
        return targetType;
    }

    public void setTargetType(Type targetType) {
        this.targetType = targetType;
    }

    public SourceCodeRef getTargetTypeSourceCodeRef() {
        return targetTypeSourceCodeRef;
    }

    public void setTargetTypeSourceCodeRef(SourceCodeRef targetTypeSourceCodeRef) {
        this.targetTypeSourceCodeRef = targetTypeSourceCodeRef;
    }

    public abstract StringBuilder evalTemplate(EvalContext context, int insertionIndex);

    public boolean isRoot() {
        return root;
    }

    protected void analyzeTargetType(EvalContext context) {
        if (getTargetType() != null) {
            if (!(getTargetType() instanceof TemplateTypeSymbol) &&
                    !(getTargetType() instanceof AnyTemplateTypeSymbol)) {
                context.addAnalyzerError(new InvalidTypeError(targetTypeSourceCodeRef,
                        BuiltinScope.ANY_TEMPLATE_TYPE, targetType));
            }
        }
    }

    @Override
    public void evalStat(EvalContext context) {

        TemplateExecutor executor = () -> {
            this.root = true;
            return evalTemplate(context, 0).toString();
        };

        Match match = context.getRuleContext().getMatch();
        RecordValueExpr rootRecord = match.getRootRecord();
        TemplateValueExpr templateValue = new TemplateValueExpr(context.getDatabase().generateRecordId(),
                executor, rootRecord, rootRecord.getId());
        templateValue.setTargetType((TemplateTypeSymbol) targetType);
        templateValue.setOriginRule(context.getRuleContext().getRuleSymbol().getFullName());
        templateValue.buildType();
        context.getDatabase().insertFact(templateValue);

    }

}
