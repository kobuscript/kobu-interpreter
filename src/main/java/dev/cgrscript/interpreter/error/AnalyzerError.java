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

package dev.cgrscript.interpreter.error;

import dev.cgrscript.interpreter.ast.symbol.SourceCodeRef;
import dev.cgrscript.interpreter.file_system.CgrFile;

import java.util.ArrayList;
import java.util.List;

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

    public List<SourceCodeRef> getSourceCodeRefs() {
        return sourceCodeRefs;
    }

    public String getDescription() {
        return getClass().getSimpleName();
    }

    public List<CgrScriptError> toCgrScriptError(CgrFile file) {
        List<CgrScriptError> errors = new ArrayList<>();

        for (SourceCodeRef sourceCodeRef : sourceCodeRefs) {
            if (sourceCodeRef.getFile().getAbsolutePath().equals(file.getAbsolutePath())) {
                errors.add(new CgrScriptErrorImpl(getDescription(), sourceCodeRef));
            }
        }

        return errors;
    }

    public static class CgrScriptErrorImpl implements CgrScriptError {

        private final String description;
        private final SourceCodeRef sourceCodeRef;

        public CgrScriptErrorImpl(String description, SourceCodeRef sourceCodeRef) {
            this.description = description;
            this.sourceCodeRef = sourceCodeRef;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public SourceCodeRef getSourceCodeRef() {
            return sourceCodeRef;
        }
    }
}
