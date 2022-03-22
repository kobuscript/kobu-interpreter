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
import dev.kobu.interpreter.ast.eval.ValueExpr;

import java.util.*;
import java.util.stream.Collectors;

public class TupleType implements Type {

    private final List<Type> types;

    private final Map<String, NamedFunction> methods = new HashMap<>();

    public TupleType(List<Type> types) {
        this.types = types;
        buildMethods();
    }

    public List<Type> getTypes() {
        return types;
    }

    @Override
    public SourceCodeRef getSourceCodeRef() {
        return null;
    }

    @Override
    public String getName() {
        return "( " + types.stream().map(Type::getName).collect(Collectors.joining(", ")) + " )";
    }

    @Override
    public String getIdentifier() {
        return null;
    }

    @Override
    public List<FieldDescriptor> getFields() {
        return new ArrayList<>();
    }

    @Override
    public List<NamedFunction> getMethods() {
        return new ArrayList<>(methods.values());
    }

    @Override
    public Type resolveField(String name) {
        return null;
    }

    @Override
    public SourceCodeRef getFieldRef(String name) {
        return null;
    }

    @Override
    public NamedFunction resolveMethod(String name) {
        return methods.get(name);
    }

    @Override
    public boolean isAssignableFrom(Type type) {
        if (type instanceof TupleType) {
            TupleType other = (TupleType) type;
            if (getTypes().size() == other.getTypes().size()) {
                for (int i = 0; i < getTypes().size(); i++) {
                    if (!getTypes().get(i).isAssignableFrom(other.getTypes().get(i))) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public Type getCommonSuperType(Type type) {
        if (isAssignableFrom(type)) {
            return this;
        } else if (type instanceof TupleType) {
            TupleType other = (TupleType) type;
            if (getTypes().size() == other.getTypes().size()) {
                List<Type> commonTypes = new ArrayList<>();
                for (int i = 0; i < getTypes().size(); i++) {
                    commonTypes.add(getTypes().get(i).getCommonSuperType(other.getTypes().get(i)));
                }
                return new TupleType(commonTypes);
            }
        }
        return BuiltinScope.ANY_TYPE;
    }

    @Override
    public Comparator<ValueExpr> getComparator() {
        return null;
    }

    private void buildMethods() {

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TupleType tupleType = (TupleType) o;
        return Objects.equals(types, tupleType.types);
    }

    @Override
    public int hashCode() {
        return Objects.hash(types);
    }

}
