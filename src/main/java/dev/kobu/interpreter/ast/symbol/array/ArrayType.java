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

package dev.kobu.interpreter.ast.symbol.array;

import dev.kobu.interpreter.ast.eval.FieldDescriptor;
import dev.kobu.interpreter.ast.eval.ValueExpr;
import dev.kobu.interpreter.ast.eval.function.BuiltinMethod;
import dev.kobu.interpreter.ast.eval.function.array.*;
import dev.kobu.interpreter.ast.symbol.*;
import dev.kobu.interpreter.ast.symbol.function.FunctionParameter;
import dev.kobu.interpreter.ast.symbol.function.FunctionType;
import dev.kobu.interpreter.ast.symbol.function.FunctionTypeParameter;
import dev.kobu.interpreter.ast.symbol.function.NamedFunction;
import dev.kobu.interpreter.ast.symbol.generics.TypeAlias;
import dev.kobu.interpreter.ast.symbol.generics.TypeParameter;
import dev.kobu.interpreter.ast.symbol.tuple.TupleType;
import dev.kobu.interpreter.ast.utils.StringFunctions;
import dev.kobu.interpreter.ast.utils.TypeArgsBuilder;

import java.util.*;

public class ArrayType implements Type {

    private final static BuiltinMethod SIZE_METHOD = new ArraySizeMethodImpl();

    private final static BuiltinMethod ADD_METHOD = new ArrayAddMethodImpl();

    private final static BuiltinMethod REMOVE_METHOD = new ArrayRemoveMethodImpl();

    private final static BuiltinMethod ADD_ALL_METHOD = new ArrayAddAllMethodImpl();

    private final static BuiltinMethod DISTINCT_METHOD = new ArrayDistinctMethodImpl();

    private final Type elementType;

    private final Map<String, NamedFunction> methods = new HashMap<>();

    protected ArrayType(Type elementType) {
        this.elementType = elementType;
        buildMethods();
    }

    public Type getElementType() {
        return elementType;
    }

    @Override
    public SourceCodeRef getSourceCodeRef() {
        return null;
    }

    @Override
    public String getName() {
        return elementType.getName() + "[]";
    }

    @Override
    public String getIdentifier() {
        return "ArrayOf" + elementType.getIdentifier();
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
        return type instanceof ArrayType && elementType.isAssignableFrom(((ArrayType) type).elementType);
    }

    @Override
    public Type getCommonSuperType(Type type) {
        if (isAssignableFrom(type)) {
            return this;
        } else if (type instanceof ArrayType) {
            return ArrayTypeFactory.getArrayTypeFor(elementType.getCommonSuperType(((ArrayType)type).getElementType()));
        }
        return BuiltinScope.ANY_TYPE;
    }

    @Override
    public Comparator<ValueExpr> getComparator() {
        return null;
    }

    @Override
    public Collection<TypeAlias> aliases() {
        return elementType.aliases();
    }

    @Override
    public Type constructFor(Map<String, Type> typeArgs) {
        return ArrayTypeFactory.getArrayTypeFor(elementType.constructFor(typeArgs));
    }

    @Override
    public void resolveAliases(Map<String, Type> typeArgs, Type targetType) {
        if (targetType instanceof ArrayType) {
            elementType.resolveAliases(typeArgs, ((ArrayType) targetType).elementType);
        }
    }

    private void buildMethods() {
        BuiltinTypeSymbol numberType = BuiltinScope.NUMBER_TYPE;

        methods.put("size", new BuiltinFunctionSymbol(this, "size", SIZE_METHOD));
        methods.put("length", new BuiltinFunctionSymbol(this, "length", SIZE_METHOD));
        methods.put("add", new BuiltinFunctionSymbol(this, "add", ADD_METHOD,
                new FunctionParameter("elem", elementType, false)));
        methods.put("remove", new BuiltinFunctionSymbol(this, "remove", REMOVE_METHOD,
                new FunctionParameter("index", numberType, false)));
        methods.put("addAll", new BuiltinFunctionSymbol(this, "addAll", ADD_ALL_METHOD,
                new FunctionParameter("arr", this, false)));
        methods.put("distinct", new BuiltinFunctionSymbol(this, "distinct", DISTINCT_METHOD, this));
        methods.put("reverse", new BuiltinFunctionSymbol(this, "reverse", new ArrayReverseMethodImpl()));

        List<TypeParameter> filterTypeParameters = TypeParameter.typeParameters("A", "B");
        methods.put("filter", new BuiltinFunctionSymbol(this, "filter",
                new ArrayFilterMethodImpl(this),
                filterTypeParameters,
                new TypeArgsBuilder().add("A", elementType).getTypeArgs(),
                new TypeAlias(filterTypeParameters.get(1)),
                new FunctionParameter("pred",
                        new FunctionType(BuiltinScope.BOOLEAN_TYPE,
                                new FunctionTypeParameter(new TypeAlias(filterTypeParameters.get(0)), false)),
                        false)));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArrayType arrayType = (ArrayType) o;
        return Objects.equals(elementType, arrayType.elementType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(elementType);
    }

}
