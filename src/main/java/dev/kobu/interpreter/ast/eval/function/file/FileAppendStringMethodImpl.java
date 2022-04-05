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

package dev.kobu.interpreter.ast.eval.function.file;

import dev.kobu.interpreter.ast.eval.ValueExpr;
import dev.kobu.interpreter.ast.eval.context.EvalContext;
import dev.kobu.interpreter.ast.eval.expr.value.FileValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.NullValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.StringValueExpr;
import dev.kobu.interpreter.ast.eval.function.BuiltinMethod;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.error.eval.BuiltinFunctionError;
import dev.kobu.interpreter.error.eval.IllegalArgumentError;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

public class FileAppendStringMethodImpl extends BuiltinMethod {

    @Override
    protected ValueExpr run(EvalContext context, ValueExpr object, Map<String, ValueExpr> args, SourceCodeRef sourceCodeRef) {
        FileValueExpr fileExpr = (FileValueExpr) object;
        File file = fileExpr.getFile();
        ValueExpr textExpr = args.get("text");
        ValueExpr charsetExpr = args.get("charset");

        if (textExpr == null || textExpr instanceof NullValueExpr) {
            throw new IllegalArgumentError("'text' cannot be null", sourceCodeRef);
        }
        String text = ((StringValueExpr)textExpr).getValue();

        Charset charset = Charset.defaultCharset();
        if (charsetExpr instanceof StringValueExpr) {
            charset = Charset.forName(((StringValueExpr)charsetExpr).getValue());
        }

        try (FileWriter writer = new FileWriter(file, charset)) {
            writer.append(text);
        } catch (IOException e) {
            throw new BuiltinFunctionError(e, sourceCodeRef);
        }

        return null;
    }

    @Override
    public String getDocumentation() {
        return "Appends the given text to this file";
    }

}
