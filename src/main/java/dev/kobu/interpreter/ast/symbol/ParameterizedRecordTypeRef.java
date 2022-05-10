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

import dev.kobu.interpreter.ast.eval.FieldDescriptor;
import dev.kobu.interpreter.ast.symbol.function.NamedFunction;
import dev.kobu.interpreter.ast.symbol.generics.TypeAlias;

import java.util.*;

public class ParameterizedRecordTypeRef implements Type {

    private final SourceCodeRef sourceCodeRef;

    private final Type typeArg;

    public ParameterizedRecordTypeRef(SourceCodeRef sourceCodeRef, Type typeArg) {
        this.sourceCodeRef = sourceCodeRef;
        this.typeArg = typeArg;
    }

    public ParameterizedRecordTypeRef(Type typeArg) {
        this.typeArg = typeArg;
        this.sourceCodeRef = null;
    }

    @Override
    public SourceCodeRef getSourceCodeRef() {
        return sourceCodeRef;
    }

    @Override
    public String getName() {
        return BuiltinScope.RECORD_TYPE_REF_TYPE.getName() + "<" + typeArg.getName() + ">";
    }

    @Override
    public List<FieldDescriptor> getFields() {
        return BuiltinScope.RECORD_TYPE_REF_TYPE.getFields();
    }

    @Override
    public List<NamedFunction> getMethods() {
        return BuiltinScope.RECORD_TYPE_REF_TYPE.getMethods();
    }

    @Override
    public Type resolveField(String name) {
        return BuiltinScope.RECORD_TYPE_REF_TYPE.resolveField(name);
    }

    @Override
    public SourceCodeRef getFieldRef(String name) {
        return BuiltinScope.RECORD_TYPE_REF_TYPE.getFieldRef(name);
    }

    @Override
    public NamedFunction resolveMethod(String name) {
        return BuiltinScope.RECORD_TYPE_REF_TYPE.resolveMethod(name);
    }

    @Override
    public boolean isAssignableFrom(Type type) {
        if (type instanceof ParameterizedRecordTypeRef) {
            return typeArg.isAssignableFrom(((ParameterizedRecordTypeRef) type).typeArg);
        }
        return false;
    }

    @Override
    public Type getCommonSuperType(Type type) {
        if (isAssignableFrom(type)) {
            return this;
        }
        return BuiltinScope.ANY_TYPE;
    }

    @Override
    public Collection<TypeAlias> aliases() {
        return new HashSet<>(typeArg.aliases());
    }

    @Override
    public Type constructFor(Map<String, Type> typeArgs) {
        return new ParameterizedRecordTypeRef(typeArg.constructFor(typeArgs));
    }

    @Override
    public void resolveAliases(Map<String, Type> typeArgs, Type targetType) {
        if (targetType instanceof ParameterizedRecordTypeRef) {
            this.typeArg.resolveAliases(typeArgs, ((ParameterizedRecordTypeRef) targetType).typeArg);
        } else if (targetType == null) {
            this.typeArg.resolveAliases(typeArgs, BuiltinScope.ANY_TYPE);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParameterizedRecordTypeRef that = (ParameterizedRecordTypeRef) o;
        return Objects.equals(typeArg, that.typeArg);
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeArg);
    }

}
