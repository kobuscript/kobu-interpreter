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

import dev.kobu.interpreter.ast.symbol.Type;
import dev.kobu.interpreter.ast.symbol.generics.TypeAlias;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class TupleTypeElement {

    private final Type elementType;

    private TupleTypeElement next;

    public TupleTypeElement(Type elementType) {
        this.elementType = elementType;
    }

    public Type getElementType() {
        return elementType;
    }

    public TupleTypeElement getNext() {
        return next;
    }

    public void setNext(TupleTypeElement next) {
        this.next = next;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TupleTypeElement that = (TupleTypeElement) o;
        return Objects.equals(elementType, that.elementType) && Objects.equals(next, that.next);
    }

    @Override
    public int hashCode() {
        return Objects.hash(elementType, next);
    }

    public boolean isAssignableFrom(TupleTypeElement other) {
        if (!elementType.isAssignableFrom(other.elementType)) {
            return false;
        }
        if (next == null && other.next == null) {
            return true;
        }
        if (next == null || other.next == null) {
            return false;
        }
        return next.isAssignableFrom(other.next);
    }

    public TupleTypeElement getCommonSuperType(TupleTypeElement other) {
        TupleTypeElement superType = new TupleTypeElement(elementType.getCommonSuperType(other.elementType));

        if (next == null && other.next == null) {
            return superType;
        }
        if (next == null || other.next == null) {
            return null;
        }

        TupleTypeElement nextElem = next.getCommonSuperType(other.next);
        if (nextElem == null) {
            return null;
        }
        superType.setNext(nextElem);
        return superType;
    }

    public void getAliases(Set<TypeAlias> aliases) {
        if (elementType instanceof TypeAlias) {
            aliases.add((TypeAlias) elementType);
        }
        if (next != null) {
            next.getAliases(aliases);
        }
    }

    public TupleTypeElement constructFor(Map<String, Type> typeArgs) {
        Type type = null;
        if (elementType instanceof TypeAlias) {
            type = typeArgs.get(elementType.getName());
        }
        if (type == null) {
            type = elementType;
        }
        TupleTypeElement tupleTypeElement = new TupleTypeElement(type);
        if (next != null) {
            tupleTypeElement.setNext(next.constructFor(typeArgs));
        }
        return tupleTypeElement;
    }

    public String getName() {
        StringBuilder str = new StringBuilder("(");
        addElemStr(str);
        str.append(")");
        return str.toString();
    }

    private void addElemStr(StringBuilder str) {
        str.append(elementType.getName());
        if (next != null) {
            str.append(", ");
            next.addElemStr(str);
        }
    }

}
