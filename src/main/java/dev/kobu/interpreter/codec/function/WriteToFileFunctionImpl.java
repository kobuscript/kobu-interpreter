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

package dev.kobu.interpreter.codec.function;

import dev.kobu.interpreter.ast.eval.ValueExpr;
import dev.kobu.interpreter.ast.eval.context.EvalContext;
import dev.kobu.interpreter.ast.eval.expr.value.FileValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.StringValueExpr;
import dev.kobu.interpreter.ast.eval.function.NativeFunction;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.codec.Writer;
import dev.kobu.interpreter.error.eval.IllegalArgumentError;

import java.util.Map;

public class WriteToFileFunctionImpl extends NativeFunction {

    private final Writer writer;

    public WriteToFileFunctionImpl(Writer writer) {
        this.writer = writer;
    }

    @Override
    protected ValueExpr run(EvalContext context, Map<String, ValueExpr> args, SourceCodeRef sourceCodeRef) {
        StringValueExpr sourceExpr = (StringValueExpr) args.get("value");
        if (sourceExpr == null) {
            throw new IllegalArgumentError("'value' cannot be null", sourceCodeRef);
        }
        FileValueExpr fileExpr = (FileValueExpr) args.get("file");
        if (fileExpr == null) {
            throw new IllegalArgumentError("'file' cannot be null", sourceCodeRef);
        }

        context.getOutputWriter().writeToFile(getModuleScope(), context, writer,
                fileExpr.getFile().getAbsolutePath(), sourceExpr, args, sourceCodeRef);

        return null;
    }

}
