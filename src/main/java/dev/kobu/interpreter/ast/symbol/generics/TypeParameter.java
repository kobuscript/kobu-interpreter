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

package dev.kobu.interpreter.ast.symbol.generics;

import dev.kobu.interpreter.ast.symbol.SourceCodeRef;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TypeParameter {

    public static List<TypeParameter> typeParameters(String... aliases) {
        List<TypeParameter> parameters = new ArrayList<>();
        for (String alias : aliases) {
            parameters.add(new TypeParameter(null, alias));
        }
        return parameters;
    }

    private final SourceCodeRef sourceCodeRef;

    private final String alias;

    public TypeParameter(SourceCodeRef sourceCodeRef, String alias) {
        this.sourceCodeRef = sourceCodeRef;
        this.alias = alias;
    }

    public String getAlias() {
        return alias;
    }

    public SourceCodeRef getSourceCodeRef() {
        return sourceCodeRef;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TypeParameter that = (TypeParameter) o;
        return Objects.equals(sourceCodeRef, that.sourceCodeRef) && Objects.equals(alias, that.alias);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceCodeRef, alias);
    }

}
