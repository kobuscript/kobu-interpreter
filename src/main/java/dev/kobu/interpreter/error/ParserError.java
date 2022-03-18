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

import java.util.Objects;

public class ParserError extends Exception implements KobuError {

    private final SourceCodeRef sourceCodeRef;

    public ParserError(SourceCodeRef sourceCodeRef, String message) {
        super(message);
        this.sourceCodeRef = sourceCodeRef;
    }

    @Override
    public SourceCodeRef getSourceCodeRef() {
        return sourceCodeRef;
    }

    @Override
    public KobuActionTypeEnum[] actions() {
        return null;
    }

    @Override
    public AstNode getAstNode() {
        return null;
    }

    @Override
    public SymbolTypeEnum getSymbolType() {
        return null;
    }

    @Override
    public String getTokenText() {
        return null;
    }

    @Override
    public int getNewDefInsertionPoint() {
        return 0;
    }

    @Override
    public String getDescription() {
        return getMessage();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParserError that = (ParserError) o;
        return Objects.equals(sourceCodeRef, that.sourceCodeRef);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceCodeRef);
    }
}
