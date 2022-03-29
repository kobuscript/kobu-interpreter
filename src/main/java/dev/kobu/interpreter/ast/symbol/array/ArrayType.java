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
import dev.kobu.interpreter.ast.symbol.tuple.TupleTypeElement;
import dev.kobu.interpreter.ast.symbol.tuple.TupleTypeFactory;
import dev.kobu.interpreter.ast.utils.TypeArgsBuilder;

import java.util.*;

public class ArrayType implements Type {

    private final static BuiltinMethod SIZE_METHOD = new ArraySizeMethodImpl();
    private final static BuiltinMethod ADD_METHOD = new ArrayAddMethodImpl();
    private final static BuiltinMethod ADD_ALL_METHOD = new ArrayAddAllMethodImpl();
    private final static BuiltinMethod REMOVE_METHOD = new ArrayRemoveMethodImpl();
    private final static BuiltinMethod DISTINCT_METHOD = new ArrayDistinctMethodImpl();
    private final static BuiltinMethod REVERSE_METHOD = new ArrayReverseMethodImpl();
    private final static BuiltinMethod FILTER_METHOD = new ArrayFilterMethodImpl();
    private final static BuiltinMethod MAP_METHOD = new ArrayMapMethodImpl();
    private final static BuiltinMethod FLAT_MAP_METHOD = new ArrayFlatMapMethodImpl();
    private final static BuiltinMethod FOR_EACH_METHOD = new ArrayForEachMethodImpl();
    private final static BuiltinMethod ZIP_METHOD = new ArrayZipMethodImpl();

    private final static TypeParameter TYPE_PARAMETER_A = new TypeParameter("A");
    private final static TypeParameter TYPE_PARAMETER_B = new TypeParameter("B");
    private final static TypeAlias TYPE_ALIAS_A = new TypeAlias(TYPE_PARAMETER_A);
    private final static TypeAlias TYPE_ALIAS_B = new TypeAlias(TYPE_PARAMETER_B);
    private final static ArrayType ARRAY_TYPE_ALIAS_B = new ArrayType(TYPE_ALIAS_B);
    private final static ArrayType ARRAY_PAIR_TYPE_ALIAS_A_B = new ArrayType(getPairTypeAlias());

    private static TupleType getPairTypeAlias() {
        TupleTypeElement tupleTypeElement = new TupleTypeElement(TYPE_ALIAS_A);
        tupleTypeElement.setNext(new TupleTypeElement(TYPE_ALIAS_B));
        return TupleTypeFactory.getTupleTypeFor(tupleTypeElement);
    }

    private final Map<String, BuiltinFunctionSymbol> methods = new HashMap<>();

    private final Type elementType;

    protected ArrayType(Type elementType) {
        this.elementType = elementType;
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

    protected void buildMethods() {
        if (!methods.isEmpty()) {
            return;
        }

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
        methods.put("reverse", new BuiltinFunctionSymbol(this, "reverse", REVERSE_METHOD));

        methods.put("filter", new BuiltinFunctionSymbol(this, "filter",
                FILTER_METHOD,
                List.of(TYPE_PARAMETER_A),
                new TypeArgsBuilder().add("A", elementType).getTypeArgs(),
                this,
                new FunctionParameter("pred",
                        new FunctionType(BuiltinScope.BOOLEAN_TYPE,
                                new FunctionTypeParameter(TYPE_ALIAS_A, false)),
                        false)));

        methods.put("map", new BuiltinFunctionSymbol(this, "map",
                MAP_METHOD,
                List.of(TYPE_PARAMETER_A, TYPE_PARAMETER_B),
                new TypeArgsBuilder().add("A", elementType).getTypeArgs(),
                ARRAY_TYPE_ALIAS_B,
                new FunctionParameter("fn",
                        new FunctionType(TYPE_ALIAS_B,
                                new FunctionTypeParameter(TYPE_ALIAS_A, false)),
                        false)));

        methods.put("flatMap", new BuiltinFunctionSymbol(this, "flatMap",
                FLAT_MAP_METHOD,
                List.of(TYPE_PARAMETER_A, TYPE_PARAMETER_B),
                new TypeArgsBuilder().add("A", elementType).getTypeArgs(),
                ARRAY_TYPE_ALIAS_B,
                new FunctionParameter("fn",
                        new FunctionType(ARRAY_TYPE_ALIAS_B,
                                new FunctionTypeParameter(TYPE_ALIAS_A, false)),
                        false)));

        methods.put("forEach", new BuiltinFunctionSymbol(this, "forEach",
                FOR_EACH_METHOD,
                List.of(TYPE_PARAMETER_A),
                new TypeArgsBuilder().add("A", elementType).getTypeArgs(),
                new FunctionParameter("fn",
                        new FunctionType(new FunctionTypeParameter(TYPE_ALIAS_A, false)), false)));

        methods.put("zip", new BuiltinFunctionSymbol(this, "zip",
                ZIP_METHOD,
                List.of(TYPE_PARAMETER_A, TYPE_PARAMETER_B),
                new TypeArgsBuilder().add("A", elementType).getTypeArgs(),
                ARRAY_PAIR_TYPE_ALIAS_A_B,
                new FunctionParameter("that", ARRAY_TYPE_ALIAS_B, false)));
    }

}
