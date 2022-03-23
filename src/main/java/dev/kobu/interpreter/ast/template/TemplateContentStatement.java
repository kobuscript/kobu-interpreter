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
import dev.kobu.interpreter.ast.eval.expr.value.StringValueExpr;
import dev.kobu.interpreter.ast.utils.TemplateIndentation;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;

public class TemplateContentStatement extends TemplateStatement {

    private final SourceCodeRef sourceCodeRef;

    private final Expr expr;

    public TemplateContentStatement(SourceCodeRef sourceCodeRef, Expr expr) {
        this.sourceCodeRef = sourceCodeRef;
        this.expr = expr;
    }

    @Override
    public SourceCodeRef getSourceCodeRef() {
        return sourceCodeRef;
    }

    @Override
    public void analyze(EvalContext context) {
        if (getNext() != null) {
            getNext().analyze(context);
        }

        expr.analyze(context);

    }

    @Override
    public StringBuilder evalTemplate(EvalContext context, int insertionIndex) {

        var str = new StringBuilder();

        ValueExpr value = expr.evalExpr(context);

        if (value instanceof StringValueExpr) {
            str.append(TemplateIndentation.indent(((StringValueExpr) value).getValue(), insertionIndex));
        } else {
            str.append(TemplateIndentation.indent(value.getStringValue(), insertionIndex));
        }

        if (getNext() != null) {
            str.append(getNext().evalTemplate(context, TemplateIndentation.getInsertionIndex(str.toString())));
        }

        return str;
    }

}