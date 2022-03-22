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
import dev.kobu.interpreter.ast.eval.function.array.*;
import dev.kobu.interpreter.ast.utils.StringFunctions;

import java.util.*;

public class ArrayType implements Type {

    private final Type elementType;

    private final Map<String, FunctionDefinition> methods = new HashMap<>();

    public ArrayType(Type elementType) {
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
    public List<FunctionDefinition> getMethods() {
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
    public FunctionDefinition resolveMethod(String name) {
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
            return new ArrayType(elementType.getCommonSuperType(((ArrayType)type).getElementType()));
        }
        return BuiltinScope.ANY_TYPE;
    }

    @Override
    public Comparator<ValueExpr> getComparator() {
        return null;
    }

    private void buildMethods() {
        BuiltinTypeSymbol numberType = BuiltinScope.NUMBER_TYPE;

        methods.put("size", new BuiltinFunctionSymbol(this, "size", new ArraySizeMethodImpl()));
        methods.put("length", new BuiltinFunctionSymbol(this, "length", new ArraySizeMethodImpl()));
        methods.put("add", new BuiltinFunctionSymbol(this, "add", new ArrayAddMethodImpl(),
                new FunctionParameter("elem", elementType, false)));
        methods.put("remove", new BuiltinFunctionSymbol(this, "remove", new ArrayRemoveMethodImpl(),
                new FunctionParameter("index", numberType, false)));
        methods.put("addAll", new BuiltinFunctionSymbol(this, "addAll", new ArrayAddAllMethodImpl(),
                new FunctionParameter("arr", this, false)));
        methods.put("distinct", new BuiltinFunctionSymbol(this, "distinct", new ArrayDistinctMethodImpl(), this));

        if (elementType == null) {
            return;
        }

        if (elementType.getComparator() != null) {
            methods.put("sort", new BuiltinFunctionSymbol(this, "sort", new ArraySortMethodImpl()));
        } else if (elementType instanceof RecordTypeSymbol) {
            for (Map.Entry<String, RecordTypeAttribute> attrEntry : ((RecordTypeSymbol) elementType).getAttributes().entrySet()) {
                if (attrEntry.getValue().getType().getComparator() != null) {
                    String name = attrEntry.getKey();
                    String methodName = "sortBy" + name.substring(0, 1).toUpperCase(Locale.ROOT);
                    if (name.length() > 1) {
                        methodName += name.substring(1);
                    }
                    methods.put(methodName, new BuiltinFunctionSymbol(this, methodName,
                            new ArraySortMethodImpl(name)));
                }
            }
        } else if (elementType instanceof TupleType) {
//            if (((TupleType)elementType).getLeftType().getComparator() != null) {
//                methods.put("sortByLeft", new BuiltinFunctionSymbol(this, "sortByLeft",
//                        new ArraySortMethodImpl("left")));
//            }
//            if (((TupleType)elementType).getRightType().getComparator() != null) {
//                methods.put("sortByRight", new BuiltinFunctionSymbol(this, "sortByRight",
//                        new ArraySortMethodImpl("right")));
//            }
        }

        if (elementType instanceof TemplateTypeSymbol) {
            methods.put("trim", new BuiltinFunctionSymbol(this, "trim", new ArrayTemplateTrimMethodImpl(),
                    new ArrayType(BuiltinScope.STRING_TYPE)));
            methods.put("strJoin", new BuiltinFunctionSymbol(this, "strJoin", new ArrayTemplateJoinMethodImpl(),
                    BuiltinScope.STRING_TYPE,
                    new FunctionParameter("delimiter", BuiltinScope.STRING_TYPE, true)));
            methods.put("mkString", new BuiltinFunctionSymbol(this, "mkString", new ArrayStringMkMethodImpl(),
                    BuiltinScope.STRING_TYPE,
                    new FunctionParameter("prefix", BuiltinScope.STRING_TYPE, false),
                    new FunctionParameter("delimiter", BuiltinScope.STRING_TYPE, false),
                    new FunctionParameter("suffix", BuiltinScope.STRING_TYPE, false)));
        } else if (elementType instanceof StringTypeSymbol) {
            methods.put("strJoin", new BuiltinFunctionSymbol(this, "strJoin", new ArrayStringJoinMethodImpl(),
                    BuiltinScope.STRING_TYPE,
                    new FunctionParameter("delimiter", BuiltinScope.STRING_TYPE, true)));
            methods.put("mkString", new BuiltinFunctionSymbol(this, "mkString", new ArrayStringMkMethodImpl(),
                    BuiltinScope.STRING_TYPE,
                    new FunctionParameter("prefix", BuiltinScope.STRING_TYPE, false),
                    new FunctionParameter("delimiter", BuiltinScope.STRING_TYPE, false),
                    new FunctionParameter("suffix", BuiltinScope.STRING_TYPE, false)));
            methods.put("trim", new BuiltinFunctionSymbol(this, "trim", new ArrayStringTrimMethodImpl(), this));

            methods.put("kebabToCamelCase", new BuiltinFunctionSymbol(this, "kebabToCamelCase",
                    new ArrayStringConverterMethodImpl(StringFunctions::kebabToCamelCase),
                    this));
            methods.put("kebabToPascalCase", new BuiltinFunctionSymbol(this, "kebabToPascalCase",
                    new ArrayStringConverterMethodImpl(StringFunctions::kebabToPascalCase),
                    this));
            methods.put("kebabToSnakeCase", new BuiltinFunctionSymbol(this, "kebabToSnakeCase",
                    new ArrayStringConverterMethodImpl(StringFunctions::kebabToSnakeCase),
                    this));
            methods.put("camelToKebabCase", new BuiltinFunctionSymbol(this, "camelToKebabCase",
                    new ArrayStringConverterMethodImpl(StringFunctions::camelToKebabCase),
                    this));
            methods.put("camelToPascalCase", new BuiltinFunctionSymbol(this, "camelToPascalCase",
                    new ArrayStringConverterMethodImpl(StringFunctions::camelToPascalCase),
                    this));
            methods.put("camelToSnakeCase", new BuiltinFunctionSymbol(this, "camelToSnakeCase",
                    new ArrayStringConverterMethodImpl(StringFunctions::camelToSnakeCase),
                    this));
            methods.put("pascalToKebabCase", new BuiltinFunctionSymbol(this, "pascalToKebabCase",
                    new ArrayStringConverterMethodImpl(StringFunctions::pascalToKebabCase),
                    this));
            methods.put("pascalToCamelCase", new BuiltinFunctionSymbol(this, "pascalToCamelCase",
                    new ArrayStringConverterMethodImpl(StringFunctions::pascalToCamelCase),
                    this));
            methods.put("pascalToSnakeCase", new BuiltinFunctionSymbol(this, "pascalToSnakeCase",
                    new ArrayStringConverterMethodImpl(StringFunctions::pascalToSnakeCase),
                    this));
            methods.put("snakeToKebabCase", new BuiltinFunctionSymbol(this, "snakeToKebabCase",
                    new ArrayStringConverterMethodImpl(StringFunctions::snakeToKebabCase),
                    this));
            methods.put("snakeToCamelCase", new BuiltinFunctionSymbol(this, "snakeToCamelCase",
                    new ArrayStringConverterMethodImpl(StringFunctions::snakeToCamelCase),
                    this));
            methods.put("snakeToPascalCase", new BuiltinFunctionSymbol(this, "snakeToPascalCase",
                    new ArrayStringConverterMethodImpl(StringFunctions::snakeToPascalCase),
                    this));
        }

        methods.put("reverse", new BuiltinFunctionSymbol(this, "reverse", new ArrayReverseMethodImpl()));
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
