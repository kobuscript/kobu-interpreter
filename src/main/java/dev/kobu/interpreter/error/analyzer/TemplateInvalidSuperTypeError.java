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

package dev.kobu.interpreter.error.analyzer;

import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.ast.symbol.TemplateTypeSymbol;
import dev.kobu.interpreter.error.AnalyzerError;

public class TemplateInvalidSuperTypeError extends AnalyzerError {

    private final TemplateTypeSymbol templateType;

    private final String superType;

    public TemplateInvalidSuperTypeError(SourceCodeRef sourceCodeRef, TemplateTypeSymbol templateType, String superType) {
        super(sourceCodeRef);
        this.templateType = templateType;
        this.superType = superType;
    }

    public TemplateTypeSymbol getTemplateType() {
        return templateType;
    }

    public String getSuperType() {
        return superType;
    }

    @Override
    public String getDescription() {
        return "'" + superType + "' is not a template type";
    }

}
