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

import dev.kobu.interpreter.ast.eval.context.EvalContext;
import dev.kobu.interpreter.ast.eval.Expr;
import dev.kobu.interpreter.ast.eval.ValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.NullValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.StringValueExpr;
import dev.kobu.interpreter.ast.symbol.Type;
import dev.kobu.interpreter.ast.utils.TemplateIndentation;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;

import java.util.HashSet;
import java.util.Map;

public class TemplateContentStatement extends TemplateStatement {

    private final SourceCodeRef sourceCodeRef;

    private final Expr expr;

    private final boolean shiftInsertionPoint;

    public TemplateContentStatement(SourceCodeRef sourceCodeRef, Expr expr, boolean shiftInsertionPoint) {
        this.sourceCodeRef = sourceCodeRef;
        this.expr = expr;
        this.shiftInsertionPoint = shiftInsertionPoint;
    }

    @Override
    public SourceCodeRef getSourceCodeRef() {
        return sourceCodeRef;
    }

    @Override
    public void setResolvedTypes(Map<String, Type> resolvedTypes) {
        if (expr != null) {
            expr.setResolvedTypes(resolvedTypes);
        }
    }

    @Override
    public void analyze(EvalContext context) {
        if (getNext() != null) {
            getNext().analyze(context);
        }

        analyzeTargetType(context);
        expr.analyze(context);

    }

    @Override
    public void evalTemplate(StringBuilder result, EvalContext context, int insertionIndex) {

        ValueExpr value = expr.evalExpr(context);
        String content = "";
        if (value instanceof StringValueExpr) {
            content = TemplateIndentation.indent(((StringValueExpr) value).getValue(), insertionIndex, false);
        } else if (!(value instanceof NullValueExpr)) {
            content = TemplateIndentation.indent(value.getStringValue(new HashSet<>()), insertionIndex, false);
        }

        result.append(content);
        if (getNext() != null) {
            getNext().evalTemplate(result, context, TemplateIndentation.getInsertionIndex(content, shiftInsertionPoint));
        }

    }

}
