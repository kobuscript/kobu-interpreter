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

package dev.kobu.interpreter.ast.symbol.tuple;

import dev.kobu.interpreter.ast.eval.FieldDescriptor;
import dev.kobu.interpreter.ast.eval.ValueExpr;
import dev.kobu.interpreter.ast.eval.function.tuple.TupleGetMethod;
import dev.kobu.interpreter.ast.eval.function.tuple.TupleSetMethod;
import dev.kobu.interpreter.ast.symbol.BuiltinFunctionSymbol;
import dev.kobu.interpreter.ast.symbol.BuiltinScope;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.ast.symbol.Type;
import dev.kobu.interpreter.ast.symbol.function.FunctionParameter;
import dev.kobu.interpreter.ast.symbol.function.NamedFunction;
import dev.kobu.interpreter.ast.symbol.generics.TypeAlias;

import java.util.*;

public class TupleType implements Type {

    private final TupleTypeElement typeElement;

    private final Map<String, NamedFunction> methods = new HashMap<>();

    protected TupleType(TupleTypeElement typeElement) {
        this.typeElement = typeElement;
        buildMethods();
    }

    public TupleTypeElement getTypeElement() {
        return typeElement;
    }

    @Override
    public SourceCodeRef getSourceCodeRef() {
        return null;
    }

    @Override
    public String getName() {
        return typeElement.getName();
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
            return typeElement.isAssignableFrom(((TupleType) type).typeElement);
        }
        return false;
    }

    @Override
    public Type getCommonSuperType(Type type) {
        if (isAssignableFrom(type)) {
            return this;
        } else if (type instanceof TupleType) {
            TupleType other = (TupleType) type;
            TupleTypeElement elemSuperType = typeElement.getCommonSuperType(other.typeElement);
            if (elemSuperType != null) {
                return TupleTypeFactory.getTupleTypeFor(elemSuperType);
            }
        }
        return BuiltinScope.ANY_TYPE;
    }

    @Override
    public Comparator<ValueExpr> getComparator() {
        return null;
    }

    @Override
    public Collection<TypeAlias> aliases() {
        Set<TypeAlias> aliases = new HashSet<>();
        typeElement.getAliases(aliases);
        return aliases;
    }

    @Override
    public Type constructFor(Map<String, Type> typeArgs) {
        return TupleTypeFactory.getTupleTypeFor(typeElement.constructFor(typeArgs));
    }

    @Override
    public void resolveAliases(Map<String, Type> typeArgs, Type targetType) {
        if (targetType instanceof TupleType) {
            TupleType targetTuple = (TupleType) targetType;
            typeElement.resolveAliases(typeArgs, targetTuple.typeElement);
        }
    }

    private void buildMethods() {
        TupleTypeElement elem = typeElement;
        int index = 0;

        while (elem != null) {
            String getName = "get" + (index + 1);
            String setName = "set" + (index + 1);

            methods.put(getName, new BuiltinFunctionSymbol(this, getName,
                    new TupleGetMethod(index),
                    elem.getElementType()));

            methods.put(setName, new BuiltinFunctionSymbol(this, setName,
                    new TupleSetMethod(index),
                    new FunctionParameter("value", elem.getElementType(), false)));

            elem = elem.getNext();
            index++;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TupleType tupleType = (TupleType) o;
        return Objects.equals(typeElement, tupleType.typeElement);
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeElement);
    }

}
