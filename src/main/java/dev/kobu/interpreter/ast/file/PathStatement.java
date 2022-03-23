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

package dev.kobu.interpreter.ast.file;

import dev.kobu.interpreter.ast.eval.context.EvalContext;
import dev.kobu.interpreter.ast.eval.Statement;
import dev.kobu.interpreter.ast.eval.expr.value.TemplateValueExpr;

import java.util.ArrayList;
import java.util.List;

public abstract class PathStatement implements Statement {

    private PathStatement next;

    public PathStatement getNext() {
        return next;
    }

    public void setNext(PathStatement next) {
        this.next = next;
    }

    public abstract void evalPath(EvalContext context, List<String> segments);

    @Override
    public void evalStat(EvalContext context) {
        List<String> segments = new ArrayList<>();
        evalPath(context, segments);

        TemplateValueExpr templateValueExpr = (TemplateValueExpr) context.getCurrentScope()
                .getValue("$_templateRef");

        FileOutput file = new FileOutput(segments, templateValueExpr);
        context.getOutputWriter().addFile(file);
    }
}