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

package dev.kobu.interpreter.ast.symbol.function;

import dev.kobu.interpreter.ast.symbol.BuiltinScope;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.ast.symbol.Type;

import java.util.Map;
import java.util.Objects;

public class FunctionParameter {

    private final String name;

    private final boolean optional;

    private Type type;

    private SourceCodeRef sourceCodeRef;

    public FunctionParameter(String name, Type type, boolean optional) {
        this.name = name;
        this.type = type;
        this.optional = optional;
    }

    public FunctionParameter(SourceCodeRef sourceCodeRef, String name, Type type, boolean optional) {
        this.sourceCodeRef = sourceCodeRef;
        this.name = name;
        this.type = type;
        this.optional = optional;
    }

    public SourceCodeRef getSourceCodeRef() {
        return sourceCodeRef;
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public boolean isOptional() {
        return optional;
    }

    public String getDescription(Map<String, Type> typeArgs) {
        String str = name;
        if (optional) {
            str += "?";
        }
        str += ": " + (type != null ? type.constructFor(typeArgs).getName() : BuiltinScope.ANY_TYPE.getName());
        return str;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FunctionParameter that = (FunctionParameter) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    public FunctionTypeParameter toFunctionTypeParameter() {
        return new FunctionTypeParameter(type, optional);
    }
}
