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

package dev.kobu.interpreter.error;

import dev.kobu.interpreter.ast.AstNode;
import dev.kobu.interpreter.ast.eval.SymbolTypeEnum;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.file_system.KobuFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public abstract class AnalyzerError extends Exception {

    private final List<SourceCodeRef> sourceCodeRefs = new ArrayList<>();

    public AnalyzerError() {
    }

    public AnalyzerError(SourceCodeRef sourceCodeRef) {
        this.sourceCodeRefs.add(sourceCodeRef);
    }

    public AnalyzerError(List<SourceCodeRef> sourceCodeRefs) {
        this.sourceCodeRefs.addAll(sourceCodeRefs);
    }

    public AnalyzerError(Throwable cause, SourceCodeRef sourceCodeRef) {
        super(cause);
        this.sourceCodeRefs.add(sourceCodeRef);
    }

    public KobuActionTypeEnum[] actions() {
        return null;
    }

    public AstNode getAstNode() {
        return null;
    }

    public SymbolTypeEnum getSymbolType() {
        return null;
    }

    public List<SourceCodeRef> getSourceCodeRefs() {
        return sourceCodeRefs;
    }

    public String getTokenText() {
        return null;
    }

    public int getNewDefInsertionPoint() {
        return 0;
    }

    public String getDescription() {
        return getClass().getSimpleName();
    }

    public List<KobuError> toKobuError(KobuFile file) {
        List<KobuError> errors = new ArrayList<>();

        for (SourceCodeRef sourceCodeRef : sourceCodeRefs) {
            if (sourceCodeRef.getFile().getAbsolutePath().equals(file.getAbsolutePath())) {
                errors.add(new KobuErrorImpl(getDescription(), sourceCodeRef,
                        actions(), getAstNode(), getSymbolType(), getTokenText(), getNewDefInsertionPoint()));
            }
        }

        return errors;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnalyzerError that = (AnalyzerError) o;
        return Objects.equals(getSourceCodeRefs(), that.getSourceCodeRefs());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSourceCodeRefs());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " at " + sourceCodeRefs + ": " + getDescription();
    }

    public static class KobuErrorImpl implements KobuError {

        private final String description;
        private final SourceCodeRef sourceCodeRef;
        private final KobuActionTypeEnum[] actions;
        private final AstNode astNode;
        private final SymbolTypeEnum symbolType;
        private final String tokenText;
        private final int newDefInsertionPoint;

        public KobuErrorImpl(String description, SourceCodeRef sourceCodeRef,
                             KobuActionTypeEnum[] actions, AstNode astNode,
                             SymbolTypeEnum symbolType, String tokenText,
                             int newDefInsertionPoint) {
            this.description = description;
            this.sourceCodeRef = sourceCodeRef;
            this.actions = actions;
            this.astNode = astNode;
            this.symbolType = symbolType;
            this.tokenText = tokenText;
            this.newDefInsertionPoint = newDefInsertionPoint;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public SourceCodeRef getSourceCodeRef() {
            return sourceCodeRef;
        }

        @Override
        public KobuActionTypeEnum[] actions() {
            return actions;
        }

        @Override
        public AstNode getAstNode() {
            return astNode;
        }

        @Override
        public SymbolTypeEnum getSymbolType() {
            return symbolType;
        }

        @Override
        public String getTokenText() {
            return tokenText;
        }

        @Override
        public int getNewDefInsertionPoint() {
            return newDefInsertionPoint;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            KobuErrorImpl that = (KobuErrorImpl) o;
            return Objects.equals(description, that.description) && Objects.equals(sourceCodeRef, that.sourceCodeRef)
                    && Arrays.equals(actions, that.actions) && symbolType == that.symbolType;
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(description, sourceCodeRef, symbolType);
            result = 31 * result + Arrays.hashCode(actions);
            return result;
        }
    }

}
