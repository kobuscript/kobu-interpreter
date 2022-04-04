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
import dev.kobu.interpreter.ast.eval.function.date.DateFormatMethodImpl;
import dev.kobu.interpreter.ast.eval.function.date.DateFormatterConstructorImpl;
import dev.kobu.interpreter.ast.eval.function.date.DateParserMethodImpl;
import dev.kobu.interpreter.ast.symbol.BuiltinFunctionSymbol;
import dev.kobu.interpreter.ast.symbol.BuiltinScope;
import dev.kobu.interpreter.ast.symbol.BuiltinTypeSymbol;
import dev.kobu.interpreter.ast.symbol.Type;
import dev.kobu.interpreter.ast.symbol.function.FunctionParameter;

public class DateFormatterTypeSymbol extends BuiltinTypeSymbol implements HasConstructor {

    private static final String TYPE_NAME = "DateFormatter";

    private final BuiltinFunctionSymbol constructor = new BuiltinFunctionSymbol(TYPE_NAME,
            new DateFormatterConstructorImpl(), BuiltinScope.DATE_FORMATTER_TYPE,
            new FunctionParameter("pattern", BuiltinScope.STRING_TYPE, false));

    public DateFormatterTypeSymbol() {
        super(TYPE_NAME);
    }

    @Override
    public boolean isAssignableFrom(Type type) {
        return type instanceof DateFormatterTypeSymbol;
    }

    @Override
    public Type getCommonSuperType(Type type) {
        if (isAssignableFrom(type)) {
            return type;
        }
        return BuiltinScope.ANY_TYPE;
    }

    @Override
    public BuiltinFunctionSymbol getConstructor() {
        return constructor;
    }

    public void buildMethods() {
        addMethod(new BuiltinFunctionSymbol(this, "parse", new DateParserMethodImpl(),
                BuiltinScope.DATE_TYPE, new FunctionParameter("source", BuiltinScope.STRING_TYPE, false)));
        addMethod(new BuiltinFunctionSymbol(this, "format", new DateFormatMethodImpl(),
                BuiltinScope.STRING_TYPE, new FunctionParameter("date", BuiltinScope.DATE_TYPE, false)));
    }

}
