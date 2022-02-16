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

package dev.cgrscript.interpreter.ast.symbol;

import dev.cgrscript.interpreter.ast.eval.FieldDescriptor;
import dev.cgrscript.interpreter.ast.eval.ValueExpr;
import dev.cgrscript.interpreter.ast.eval.function.array.*;
import dev.cgrscript.interpreter.ast.utils.StringFunctions;

import java.util.*;

public class ArrayType implements Type {

    private final Type elementType;

    private final Map<String, FunctionType> methods = new HashMap<>();

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
    public List<FunctionType> getMethods() {
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
    public FunctionType resolveMethod(String name) {
        return methods.get(name);
    }

    @Override
    public boolean isAssignableFrom(Type type) {
        return type instanceof ArrayType && elementType.isAssignableFrom(((ArrayType) type).elementType);
    }

    @Override
    public Type getCommonSuperType(Type type) {
        return isAssignableFrom(type) ? this : BuiltinScope.ANY_TYPE;
    }

    @Override
    public Comparator<ValueExpr> getComparator() {
        return null;
    }

    private void buildMethods() {
        BuiltinTypeSymbol numberType = BuiltinScope.NUMBER_TYPE;

        methods.put("size", new BuiltinFunctionSymbol("size", new ArraySizeMethodImpl()));
        methods.put("length", new BuiltinFunctionSymbol("length", new ArraySizeMethodImpl()));
        methods.put("add", new BuiltinFunctionSymbol("add", new ArrayAddMethodImpl(),
                new FunctionParameter("elem", elementType, false)));
        methods.put("remove", new BuiltinFunctionSymbol("remove", new ArrayRemoveMethodImpl(),
                new FunctionParameter("index", numberType, false)));
        methods.put("addAll", new BuiltinFunctionSymbol("addAll", new ArrayAddAllMethodImpl(),
                new FunctionParameter("arr", this, false)));
        methods.put("distinct", new BuiltinFunctionSymbol("distinct", new ArrayDistinctMethodImpl(), this));

        if (elementType == null) {
            return;
        }

        if (elementType.getComparator() != null) {
            methods.put("sort", new BuiltinFunctionSymbol("sort", new ArraySortMethodImpl()));
        } else if (elementType instanceof RecordTypeSymbol) {
            for (Map.Entry<String, RecordTypeAttribute> attrEntry : ((RecordTypeSymbol) elementType).getAttributes().entrySet()) {
                if (attrEntry.getValue().getType().getComparator() != null) {
                    String name = attrEntry.getKey();
                    String methodName = "sortBy" + name.substring(0, 1).toUpperCase(Locale.ROOT);
                    if (name.length() > 1) {
                        methodName += name.substring(1);
                    }
                    methods.put(methodName, new BuiltinFunctionSymbol(methodName,
                            new ArraySortMethodImpl(name)));
                }
            }
        } else if (elementType instanceof PairType) {
            if (((PairType)elementType).getLeftType().getComparator() != null) {
                methods.put("sortByLeft", new BuiltinFunctionSymbol("sortByLeft",
                        new ArraySortMethodImpl("left")));
            }
            if (((PairType)elementType).getRightType().getComparator() != null) {
                methods.put("sortByRight", new BuiltinFunctionSymbol("sortByRight",
                        new ArraySortMethodImpl("right")));
            }
        }

        if (elementType instanceof TemplateTypeSymbol) {
            methods.put("trim", new BuiltinFunctionSymbol("trim", new ArrayTemplateTrimMethodImpl(),
                    new ArrayType(BuiltinScope.STRING_TYPE)));
            methods.put("strJoin", new BuiltinFunctionSymbol("strJoin", new ArrayTemplateJoinMethodImpl(),
                    BuiltinScope.STRING_TYPE,
                    new FunctionParameter("delimiter", BuiltinScope.STRING_TYPE, true)));
            methods.put("mkString", new BuiltinFunctionSymbol("mkString", new ArrayStringMkMethodImpl(),
                    BuiltinScope.STRING_TYPE,
                    new FunctionParameter("prefix", BuiltinScope.STRING_TYPE, false),
                    new FunctionParameter("delimiter", BuiltinScope.STRING_TYPE, false),
                    new FunctionParameter("suffix", BuiltinScope.STRING_TYPE, false)));
        } else if (elementType instanceof StringTypeSymbol) {
            methods.put("strJoin", new BuiltinFunctionSymbol("strJoin", new ArrayStringJoinMethodImpl(),
                    BuiltinScope.STRING_TYPE,
                    new FunctionParameter("delimiter", BuiltinScope.STRING_TYPE, true)));
            methods.put("mkString", new BuiltinFunctionSymbol("mkString", new ArrayStringMkMethodImpl(),
                    BuiltinScope.STRING_TYPE,
                    new FunctionParameter("prefix", BuiltinScope.STRING_TYPE, false),
                    new FunctionParameter("delimiter", BuiltinScope.STRING_TYPE, false),
                    new FunctionParameter("suffix", BuiltinScope.STRING_TYPE, false)));
            methods.put("trim", new BuiltinFunctionSymbol("trim", new ArrayStringTrimMethodImpl(), this));

            methods.put("kebabToCamelCase", new BuiltinFunctionSymbol("kebabToCamelCase",
                    new ArrayStringConverterMethodImpl(StringFunctions::kebabToCamelCase),
                    this));
            methods.put("kebabToPascalCase", new BuiltinFunctionSymbol("kebabToPascalCase",
                    new ArrayStringConverterMethodImpl(StringFunctions::kebabToPascalCase),
                    this));
            methods.put("kebabToSnakeCase", new BuiltinFunctionSymbol("kebabToSnakeCase",
                    new ArrayStringConverterMethodImpl(StringFunctions::kebabToSnakeCase),
                    this));
            methods.put("camelToKebabCase", new BuiltinFunctionSymbol("camelToKebabCase",
                    new ArrayStringConverterMethodImpl(StringFunctions::camelToKebabCase),
                    this));
            methods.put("camelToPascalCase", new BuiltinFunctionSymbol("camelToPascalCase",
                    new ArrayStringConverterMethodImpl(StringFunctions::camelToPascalCase),
                    this));
            methods.put("camelToSnakeCase", new BuiltinFunctionSymbol("camelToSnakeCase",
                    new ArrayStringConverterMethodImpl(StringFunctions::camelToSnakeCase),
                    this));
            methods.put("pascalToKebabCase", new BuiltinFunctionSymbol("pascalToKebabCase",
                    new ArrayStringConverterMethodImpl(StringFunctions::pascalToKebabCase),
                    this));
            methods.put("pascalToCamelCase", new BuiltinFunctionSymbol("pascalToCamelCase",
                    new ArrayStringConverterMethodImpl(StringFunctions::pascalToCamelCase),
                    this));
            methods.put("pascalToSnakeCase", new BuiltinFunctionSymbol("pascalToSnakeCase",
                    new ArrayStringConverterMethodImpl(StringFunctions::pascalToSnakeCase),
                    this));
            methods.put("snakeToKebabCase", new BuiltinFunctionSymbol("snakeToKebabCase",
                    new ArrayStringConverterMethodImpl(StringFunctions::snakeToKebabCase),
                    this));
            methods.put("snakeToCamelCase", new BuiltinFunctionSymbol("snakeToCamelCase",
                    new ArrayStringConverterMethodImpl(StringFunctions::snakeToCamelCase),
                    this));
            methods.put("snakeToPascalCase", new BuiltinFunctionSymbol("snakeToPascalCase",
                    new ArrayStringConverterMethodImpl(StringFunctions::snakeToPascalCase),
                    this));
        }

        methods.put("reverse", new BuiltinFunctionSymbol("reverse", new ArrayReverseMethodImpl()));
    }
}
