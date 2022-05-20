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
import dev.kobu.interpreter.ast.eval.function.file.*;
import dev.kobu.interpreter.ast.symbol.array.ArrayTypeFactory;
import dev.kobu.interpreter.ast.symbol.function.FunctionParameter;

public class FileTypeSymbol extends BuiltinTypeSymbol implements HasConstructor {

    private static final String TYPE_NAME = "File";

    private final BuiltinFunctionSymbol constructor = new BuiltinFunctionSymbol(TYPE_NAME, new FileConstructorImpl(),
            this, new FunctionParameter("path", BuiltinScope.PATH_TYPE, false));

    public FileTypeSymbol() {
        super(TYPE_NAME);
    }

    @Override
    public boolean isAssignableFrom(Type type) {
        return type instanceof FileTypeSymbol;
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
        addMethod(new BuiltinFunctionSymbol("appendString", new FileAppendStringMethodImpl(),
                new FunctionParameter("text", BuiltinScope.STRING_TYPE, false),
                new FunctionParameter("charset", BuiltinScope.STRING_TYPE, true)));
        addMethod(new BuiltinFunctionSymbol("appendTemplate", new FileAppendTemplateMethodImpl(),
                new FunctionParameter("template", BuiltinScope.ANY_TEMPLATE_TYPE, false),
                new FunctionParameter("charset", BuiltinScope.STRING_TYPE, true)));
        addMethod(new BuiltinFunctionSymbol("getExt", new FileGetExtMethodImpl(),
                BuiltinScope.STRING_TYPE));
        addMethod(new BuiltinFunctionSymbol("getName", new FileGetNameMethodImpl(),
                BuiltinScope.STRING_TYPE));
        addMethod(new BuiltinFunctionSymbol("exists", new FileExistsMethodImpl(),
                BuiltinScope.BOOLEAN_TYPE));
        addMethod(new BuiltinFunctionSymbol("getPath", new FileGetPathMethodImpl(),
                BuiltinScope.PATH_TYPE));
        addMethod(new BuiltinFunctionSymbol("isDirectory", new FileIsDirectoryMethodImpl(),
                BuiltinScope.BOOLEAN_TYPE));
        addMethod(new BuiltinFunctionSymbol("isFile", new FileIsFileMethodImpl(),
                BuiltinScope.BOOLEAN_TYPE));
        addMethod(new BuiltinFunctionSymbol("list", new FileListMethodImpl(),
                ArrayTypeFactory.getArrayTypeFor(this)));
        addMethod(new BuiltinFunctionSymbol("read", new FileReadMethodImpl(),
                BuiltinScope.STRING_TYPE,
                new FunctionParameter("charset", BuiltinScope.STRING_TYPE, true)));
        addMethod(new BuiltinFunctionSymbol("writeString", new FileWriteStringMethodImpl(),
                new FunctionParameter("text", BuiltinScope.STRING_TYPE, false),
                new FunctionParameter("charset", BuiltinScope.STRING_TYPE, true)));
        addMethod(new BuiltinFunctionSymbol("writeTemplate", new FileWriteTemplateMethodImpl(),
                new FunctionParameter("template", BuiltinScope.ANY_TEMPLATE_TYPE, false),
                new FunctionParameter("charset", BuiltinScope.STRING_TYPE, true)));

    }
}
