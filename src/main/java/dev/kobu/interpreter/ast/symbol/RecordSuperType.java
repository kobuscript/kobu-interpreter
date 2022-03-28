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

package dev.kobu.interpreter.ast.symbol;

import dev.kobu.interpreter.ast.AnalyzerContext;
import dev.kobu.interpreter.error.analyzer.InvalidTypeArgsError;

import java.util.List;

public class RecordSuperType {

    private final SourceCodeRef sourceCodeRef;

    private RecordTypeSymbol type;

    private final List<Type> typeArgs;

    public RecordSuperType(SourceCodeRef sourceCodeRef, RecordTypeSymbol type, List<Type> typeArgs) {
        this.sourceCodeRef = sourceCodeRef;
        this.type = type;
        this.typeArgs = typeArgs;
    }

    public void applyTypeArgs(AnalyzerContext analyzerContext) {
        if (typeArgs != null && !typeArgs.isEmpty()) {
            if (type.getTypeParameters() == null || type.getTypeParameters().isEmpty()) {
                analyzerContext.getErrorScope().addError(new InvalidTypeArgsError(sourceCodeRef,
                        0, typeArgs.size()));
                return;
            }
            createNewType(analyzerContext);
        } else if (type.getTypeParameters() != null && !type.getTypeParameters().isEmpty()) {
            if (typeArgs == null || typeArgs.isEmpty()) {
                analyzerContext.getErrorScope().addError(new InvalidTypeArgsError(sourceCodeRef,
                        type.getTypeParameters().size(), 0));
                return;
            }
            createNewType(analyzerContext);
        }
    }

    private void createNewType(AnalyzerContext analyzerContext) {
        if (typeArgs.size() != type.getTypeParameters().size()) {
            analyzerContext.getErrorScope().addError(new InvalidTypeArgsError(sourceCodeRef,
                    type.getTypeParameters().size(), typeArgs.size()));
            return;
        }
        this.type = new RecordTypeSymbol(this.type, typeArgs);
    }

    public SourceCodeRef getSourceCodeRef() {
        return sourceCodeRef;
    }

    public RecordTypeSymbol getType() {
        return type;
    }

    public List<Type> getTypeArgs() {
        return typeArgs;
    }

}
