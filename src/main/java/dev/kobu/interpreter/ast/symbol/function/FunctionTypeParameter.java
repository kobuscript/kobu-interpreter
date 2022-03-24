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

import dev.kobu.interpreter.ast.symbol.Type;

import java.util.Map;
import java.util.Objects;

public class FunctionTypeParameter {

    private final Type type;

    private final boolean optional;

    public FunctionTypeParameter(Type type, boolean optional) {
        this.type = type;
        this.optional = optional;
    }

    public Type getType() {
        return type;
    }

    public boolean isOptional() {
        return optional;
    }

    public String getDescription() {
        String typeName = type.getName();
        if (optional) {
            typeName += "?";
        }
        return typeName;
    }

    public FunctionTypeParameter constructFor(Map<String, Type> typeArgs) {
        Type resolvedType = type.constructFor(typeArgs);
        if (resolvedType == type) {
            return this;
        }
        return new FunctionTypeParameter(resolvedType, optional);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FunctionTypeParameter that = (FunctionTypeParameter) o;
        return optional == that.optional && Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, optional);
    }
}
