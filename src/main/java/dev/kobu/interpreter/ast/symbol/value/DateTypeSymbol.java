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

import dev.kobu.interpreter.ast.eval.HasConstructor;
import dev.kobu.interpreter.ast.eval.function.date.DateConstructorImpl;
import dev.kobu.interpreter.ast.eval.function.date.DateGetTimeMethodImpl;
import dev.kobu.interpreter.ast.symbol.*;
import dev.kobu.interpreter.ast.symbol.function.FunctionParameter;

public class DateTypeSymbol extends BuiltinTypeSymbol implements ValType, HasConstructor {

    private static final String TYPE_NAME = "Date";

    private final BuiltinFunctionSymbol constructor = new BuiltinFunctionSymbol(TYPE_NAME,
            new DateConstructorImpl(), this,
            new FunctionParameter("date", BuiltinScope.NUMBER_TYPE, true));

    public DateTypeSymbol() {
        super(TYPE_NAME);
    }

    @Override
    public boolean isAssignableFrom(Type type) {
        return type instanceof DateTypeSymbol;
    }

    @Override
    public Type getCommonSuperType(Type type) {
        if (isAssignableFrom(type)) {
            return type;
        } else if (type instanceof ValType) {
            return BuiltinScope.ANY_VAL_TYPE;
        }
        return BuiltinScope.ANY_TYPE;
    }

    public void buildMethods() {
        addMethod(new BuiltinFunctionSymbol(this, "getTime", new DateGetTimeMethodImpl(),
                BuiltinScope.NUMBER_TYPE));
    }

    @Override
    public BuiltinFunctionSymbol getConstructor() {
        return constructor;
    }

}
