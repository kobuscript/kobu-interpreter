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

import dev.kobu.interpreter.ast.eval.FieldDescriptor;
import dev.kobu.interpreter.ast.eval.ValueExpr;
import dev.kobu.interpreter.ast.symbol.BuiltinScope;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.ast.symbol.Type;
import dev.kobu.interpreter.ast.symbol.function.NamedFunction;

import java.util.*;

public class TypeAlias implements Type {

    private final TypeParameter typeParameter;

    public TypeAlias(TypeParameter typeParameter) {
        this.typeParameter = typeParameter;
    }

    public TypeParameter getTypeParameter() {
        return typeParameter;
    }

    @Override
    public SourceCodeRef getSourceCodeRef() {
        return typeParameter.getSourceCodeRef();
    }

    @Override
    public String getName() {
        return typeParameter.getAlias();
    }

    @Override
    public String getIdentifier() {
        return typeParameter.getAlias();
    }

    @Override
    public List<FieldDescriptor> getFields() {
        return BuiltinScope.ANY_TYPE.getFields();
    }

    @Override
    public List<NamedFunction> getMethods() {
        return BuiltinScope.ANY_TYPE.getMethods();
    }

    @Override
    public Type resolveField(String name) {
        return BuiltinScope.ANY_TYPE.resolveField(name);
    }

    @Override
    public SourceCodeRef getFieldRef(String name) {
        return BuiltinScope.ANY_TYPE.getFieldRef(name);
    }

    @Override
    public NamedFunction resolveMethod(String name) {
        return BuiltinScope.ANY_TYPE.resolveMethod(name);
    }

    @Override
    public boolean isAssignableFrom(Type type) {
        return equals(type);
    }

    @Override
    public Type getCommonSuperType(Type type) {
        return isAssignableFrom(type) ? this : BuiltinScope.ANY_TYPE;
    }

    @Override
    public Comparator<ValueExpr> getComparator() {
        return null;
    }

    @Override
    public Collection<TypeAlias> aliases() {
        return List.of(this);
    }

    @Override
    public Type constructFor(Map<String, Type> typeArgs) {
        Type type = typeArgs.get(typeParameter.getAlias());
        if (type != null) {
            return type;
        }
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TypeAlias typeAlias = (TypeAlias) o;
        return Objects.equals(typeParameter, typeAlias.typeParameter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeParameter);
    }

}
