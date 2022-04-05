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

package dev.kobu.interpreter.ast.eval.expr.value;

import dev.kobu.interpreter.ast.eval.HasMethods;
import dev.kobu.interpreter.ast.eval.ValueExpr;
import dev.kobu.interpreter.ast.eval.context.EvalContext;
import dev.kobu.interpreter.ast.eval.context.SnapshotValue;
import dev.kobu.interpreter.ast.symbol.BuiltinScope;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.ast.symbol.Type;
import dev.kobu.interpreter.ast.symbol.function.NamedFunction;

import java.io.File;
import java.util.Objects;

public class FileValueExpr implements ValueExpr, HasMethods {

    private SourceCodeRef sourceCodeRef;

    private final File file;

    private final Type type = BuiltinScope.FILE_TYPE;

    public FileValueExpr(File file) {
        this.file = file;
    }

    public FileValueExpr(SourceCodeRef sourceCodeRef, File file) {
        this.sourceCodeRef = sourceCodeRef;
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    @Override
    public SourceCodeRef getSourceCodeRef() {
        return sourceCodeRef;
    }

    @Override
    public void analyze(EvalContext context) {

    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public ValueExpr evalExpr(EvalContext context) {
        return this;
    }

    @Override
    public NamedFunction resolveMethod(String methodName) {
        return type.resolveMethod(methodName);
    }

    @Override
    public String getStringValue() {
        return file.getAbsolutePath();
    }

    @Override
    public SnapshotValue getSnapshotValue() {
        return new FileSnapshotValue(file.getAbsolutePath());
    }

    private static class FileSnapshotValue implements SnapshotValue {

        private final String filePath;

        public FileSnapshotValue(String filePath) {
            this.filePath = filePath;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FileSnapshotValue that = (FileSnapshotValue) o;
            return Objects.equals(filePath, that.filePath);
        }

        @Override
        public int hashCode() {
            return Objects.hash(filePath);
        }

    }
}
