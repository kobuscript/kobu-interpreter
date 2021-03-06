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

package dev.kobu.interpreter.ast.symbol.value;

import dev.kobu.interpreter.ast.eval.function.string.*;
import dev.kobu.interpreter.ast.symbol.*;
import dev.kobu.interpreter.ast.symbol.array.ArrayTypeFactory;
import dev.kobu.interpreter.ast.symbol.function.FunctionParameter;
import dev.kobu.interpreter.ast.utils.StringFunctions;

public class StringTypeSymbol extends BuiltinTypeSymbol implements ValType {

    private static final String TYPE_NAME = "string";

    public StringTypeSymbol() {
        super(TYPE_NAME);
    }

    @Override
    public boolean isAssignableFrom(Type type) {
        return type instanceof StringTypeSymbol;
    }

    @Override
    public Type getCommonSuperType(Type type) {
        if (isAssignableFrom(type)) {
            return this;
        } else if (type instanceof ValType) {
            return BuiltinScope.ANY_VAL_TYPE;
        }
        return BuiltinScope.ANY_TYPE;
    }

    public void buildMethods() {
        var stringArrayType = ArrayTypeFactory.getArrayTypeFor(this);

        addMethod(new BuiltinFunctionSymbol(this,"trim", new TrimMethodImpl(), this));
        addMethod(new BuiltinFunctionSymbol(this,"capitalize", new CapitalizeMethodImpl(), this));
        addMethod(new BuiltinFunctionSymbol(this,"uppercase", new UppercaseMethodImpl(), this));
        addMethod(new BuiltinFunctionSymbol(this,"lowercase", new LowercaseMethodImpl(), this,
                new FunctionParameter("lowercaseTail", BuiltinScope.BOOLEAN_TYPE, true)));
        addMethod(new BuiltinFunctionSymbol(this, "contains", new StringContainsMethodImpl(),
                BuiltinScope.BOOLEAN_TYPE,
                new FunctionParameter("str", this, false)));
        addMethod(new BuiltinFunctionSymbol(this,"substring", new SubstringMethodImpl(), this,
                new FunctionParameter("beginIndex", BuiltinScope.NUMBER_TYPE, false),
                new FunctionParameter("endIndex", BuiltinScope.NUMBER_TYPE, true)));
        addMethod(new BuiltinFunctionSymbol(this,"split", new StringSplitMethodImpl(), stringArrayType,
                new FunctionParameter("regex", this, false),
                new FunctionParameter("limit", BuiltinScope.NUMBER_TYPE, true)));
        addMethod(new BuiltinFunctionSymbol(this,"length", new StringLengthMethodImpl(), BuiltinScope.NUMBER_TYPE));
        addMethod(new BuiltinFunctionSymbol(this,"matches", new StringMatchesMethodImpl(), BuiltinScope.BOOLEAN_TYPE,
                new FunctionParameter("regex", this, false)));
        addMethod(new BuiltinFunctionSymbol(this,"replace", new StringReplaceMethodImpl(), BuiltinScope.STRING_TYPE,
                new FunctionParameter("regex", this, false),
                new FunctionParameter("replacement", this, false)));
        addMethod(new BuiltinFunctionSymbol("compare", new StringCompareMethodImpl(),
                BuiltinScope.NUMBER_TYPE,
                new FunctionParameter("other", this, false)));
        addMethod(new BuiltinFunctionSymbol("escape", new StringEscapeMethodImpl(), BuiltinScope.STRING_TYPE));
        addMethod(new BuiltinFunctionSymbol("repeat", new StringRepeatMethodImpl(), BuiltinScope.STRING_TYPE,
                new FunctionParameter("count", BuiltinScope.NUMBER_TYPE, false)));
        addMethod(new BuiltinFunctionSymbol("toTemplate", new StringToTemplateMethodImpl(), BuiltinScope.ANY_TEMPLATE_TYPE));

        addMethod(new BuiltinFunctionSymbol(this,"kebabToCamelCase",
                new StringConverterMethodImpl(StringFunctions::kebabToCamelCase),
                this));
        addMethod(new BuiltinFunctionSymbol(this,"kebabToPascalCase",
                new StringConverterMethodImpl(StringFunctions::kebabToPascalCase),
                this));
        addMethod(new BuiltinFunctionSymbol(this,"kebabToSnakeCase",
                new StringConverterMethodImpl(StringFunctions::kebabToSnakeCase),
                this));
        addMethod(new BuiltinFunctionSymbol(this,"camelToKebabCase",
                new StringConverterMethodImpl(StringFunctions::camelToKebabCase),
                this));
        addMethod(new BuiltinFunctionSymbol(this,"camelToPascalCase",
                new StringConverterMethodImpl(StringFunctions::camelToPascalCase),
                this));
        addMethod(new BuiltinFunctionSymbol(this,"camelToSnakeCase",
                new StringConverterMethodImpl(StringFunctions::camelToSnakeCase),
                this));
        addMethod(new BuiltinFunctionSymbol(this,"pascalToKebabCase",
                new StringConverterMethodImpl(StringFunctions::pascalToKebabCase),
                this));
        addMethod(new BuiltinFunctionSymbol(this,"pascalToCamelCase",
                new StringConverterMethodImpl(StringFunctions::pascalToCamelCase),
                this));
        addMethod(new BuiltinFunctionSymbol(this,"pascalToSnakeCase",
                new StringConverterMethodImpl(StringFunctions::pascalToSnakeCase),
                this));
        addMethod(new BuiltinFunctionSymbol(this,"snakeToKebabCase",
                new StringConverterMethodImpl(StringFunctions::snakeToKebabCase),
                this));
        addMethod(new BuiltinFunctionSymbol(this,"snakeToCamelCase",
                new StringConverterMethodImpl(StringFunctions::snakeToCamelCase),
                this));
        addMethod(new BuiltinFunctionSymbol(this,"snakeToPascalCase",
                new StringConverterMethodImpl(StringFunctions::snakeToPascalCase),
                this));

    }
}
