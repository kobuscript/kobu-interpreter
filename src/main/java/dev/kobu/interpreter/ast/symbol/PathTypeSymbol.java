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

import dev.kobu.interpreter.ast.eval.HasConstructor;
import dev.kobu.interpreter.ast.eval.function.path.*;
import dev.kobu.interpreter.ast.symbol.array.ArrayTypeFactory;
import dev.kobu.interpreter.ast.symbol.function.FunctionParameter;

public class PathTypeSymbol extends BuiltinTypeSymbol implements HasConstructor {

    private static final String TYPE_NAME = "Path";

    private final BuiltinFunctionSymbol constructor = new BuiltinFunctionSymbol(TYPE_NAME, new PathConstructorImpl(),
            this,
            new FunctionParameter("segments", ArrayTypeFactory.getArrayTypeFor(BuiltinScope.STRING_TYPE), false));

    public PathTypeSymbol() {
        super(TYPE_NAME);
    }

    @Override
    public boolean isAssignableFrom(Type type) {
        return type instanceof PathTypeSymbol;
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
        addMethod(new BuiltinFunctionSymbol(this, "getFileName", new PathGetFileNameMethodImpl(), this));
        addMethod(new BuiltinFunctionSymbol(this, "getParent", new PathGetParentMethodImpl(), this));
        addMethod(new BuiltinFunctionSymbol(this, "normalize", new PathNormalizeMethodImpl(), this));
        addMethod(new BuiltinFunctionSymbol(this, "resolve", new PathResolveMethodImpl(), this,
                new FunctionParameter("other", this, false)));
        addMethod(new BuiltinFunctionSymbol(this, "relativize", new PathRelativizeMethodImpl(), this,
                new FunctionParameter("other", this, false)));
        addMethod(new BuiltinFunctionSymbol(this, "toAbsolutePath", new PathToAbsolutePathMethodImpl(), this));
        addMethod(new BuiltinFunctionSymbol(this, "toString", new PathToStringMethodImpl(), BuiltinScope.STRING_TYPE));
    }

}
