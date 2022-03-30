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

import dev.kobu.interpreter.ast.eval.function.any.TestTypeMethodImpl;

public class AnyTypeSymbol extends BuiltinTypeSymbol {

    public static final String ANY_TYPE = "Any";

    public AnyTypeSymbol() {
        super(ANY_TYPE);
    }

    @Override
    public boolean isAssignableFrom(Type type) {
        return true;
    }

    @Override
    public Type getCommonSuperType(Type type) {
        return this;
    }

    public void buildMethods() {
        addMethod(new BuiltinFunctionSymbol(this,"isString", new TestTypeMethodImpl(BuiltinScope.STRING_TYPE),
                BuiltinScope.BOOLEAN_TYPE));
        addMethod(new BuiltinFunctionSymbol(this,"isNumber", new TestTypeMethodImpl(BuiltinScope.NUMBER_TYPE),
                BuiltinScope.BOOLEAN_TYPE));
        addMethod(new BuiltinFunctionSymbol(this,"isBoolean", new TestTypeMethodImpl(BuiltinScope.BOOLEAN_TYPE),
                BuiltinScope.BOOLEAN_TYPE));
        addMethod(new BuiltinFunctionSymbol(this,"isRecord", new TestTypeMethodImpl(BuiltinScope.ANY_RECORD_TYPE),
                BuiltinScope.BOOLEAN_TYPE));
    }
}
