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

import dev.cgrscript.interpreter.ast.eval.expr.value.StringValueExpr;
import dev.cgrscript.interpreter.ast.eval.ValueExpr;
import dev.cgrscript.interpreter.ast.eval.function.string.*;
import dev.cgrscript.interpreter.ast.utils.StringFunctions;

import java.util.Comparator;

public class StringTypeSymbol extends BuiltinTypeSymbol {

    private static final String TYPE_NAME = "string";

    public StringTypeSymbol() {
        super(TYPE_NAME);
        buildMethods();
    }

    @Override
    public boolean isAssignableFrom(Type type) {
        return type instanceof StringTypeSymbol;
    }

    @Override
    public Type getCommonSuperType(Type type) {
        return isAssignableFrom(type) ? this : BuiltinScope.ANY_TYPE;
    }

    @Override
    public Comparator<ValueExpr> getComparator() {
        return Comparator.comparing(o -> ((StringValueExpr)o).getValue());
    }

    @Override
    public String getIdentifier() {
        return "String";
    }

    private void buildMethods() {
        var stringArrayType = new ArrayType(this);

        addMethod(new BuiltinFunctionSymbol(this,"trim", new TrimMethodImpl(), this));
        addMethod(new BuiltinFunctionSymbol(this,"capitalize", new CapitalizeMethodImpl(), this));
        addMethod(new BuiltinFunctionSymbol(this,"uppercase", new UppercaseMethodImpl(), this));
        addMethod(new BuiltinFunctionSymbol(this,"lowercase", new LowercaseMethodImpl(), this));
        addMethod(new BuiltinFunctionSymbol(this,"substring", new SubstringMethodImpl(), stringArrayType,
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
