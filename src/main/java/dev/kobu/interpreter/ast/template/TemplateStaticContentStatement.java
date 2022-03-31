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
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.ast.symbol.Type;
import dev.kobu.interpreter.ast.utils.TemplateIndentation;

import java.util.Map;

public class TemplateStaticContentStatement extends TemplateStatement {

    private final SourceCodeRef sourceCodeRef;

    private final String content;

    public TemplateStaticContentStatement(SourceCodeRef sourceCodeRef, String content) {
        this.sourceCodeRef = sourceCodeRef;
        this.content = content;
    }

    @Override
    public SourceCodeRef getSourceCodeRef() {
        return sourceCodeRef;
    }

    @Override
    public void setResolvedTypes(Map<String, Type> resolvedTypes) {

    }

    @Override
    public void analyze(EvalContext context) {
        if (getNext() != null) {
            getNext().analyze(context);
        }

        analyzeTargetType(context);
    }

    @Override
    public StringBuilder evalTemplate(EvalContext context, int insertionIndex) {
        var str = new StringBuilder(content);
        if (getNext() != null) {
            str.append(getNext().evalTemplate(context, TemplateIndentation.getInsertionIndex(content)));
        }
        if (isRoot()) {
            return new StringBuilder(TemplateIndentation.indent(str.toString(), insertionIndex));
        }

        return str;
    }

}
