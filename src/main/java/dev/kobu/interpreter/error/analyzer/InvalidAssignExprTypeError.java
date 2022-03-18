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
import dev.kobu.interpreter.ast.symbol.Type;
import dev.kobu.interpreter.error.AnalyzerError;

import java.util.Objects;

public class InvalidAssignExprTypeError extends AnalyzerError {

    private final Type expected;

    private final Type found;

    public InvalidAssignExprTypeError(SourceCodeRef sourceCodeRef, Type expected, Type found) {
        super(sourceCodeRef);
        this.expected = expected;
        this.found = found;
    }

    @Override
    public String getDescription() {
        var foundStr = found != null ? found.getName() : "void";
        var expectedStr = expected != null ? expected.getName() : "void";
        return "Type '" + foundStr + "' is not assignable to type '" + expectedStr + "'";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        InvalidAssignExprTypeError that = (InvalidAssignExprTypeError) o;
        return Objects.equals(expected, that.expected) && Objects.equals(found, that.found);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), expected, found);
    }

}
