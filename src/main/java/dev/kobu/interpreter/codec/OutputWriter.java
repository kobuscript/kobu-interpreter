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

package dev.kobu.interpreter.codec;

import dev.kobu.interpreter.ast.eval.ValueExpr;
import dev.kobu.interpreter.ast.eval.context.EvalContext;
import dev.kobu.interpreter.ast.eval.expr.value.RecordValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.StringValueExpr;
import dev.kobu.interpreter.ast.symbol.ModuleScope;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.codec.impl.JsonWriter;
import dev.kobu.interpreter.codec.impl.XmlWriter;
import dev.kobu.interpreter.error.eval.BuiltinFunctionError;
import dev.kobu.interpreter.error.eval.IllegalArgumentError;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Map;

public class OutputWriter {

    private final PrintStream stdOut;

    private final PrintStream stdIn;

    public OutputWriter(PrintStream stdOut, PrintStream stdIn) {
        this.stdOut = stdOut;
        this.stdIn = stdIn;
    }

    public PrintStream getStdOut() {
        return stdOut;
    }

    public PrintStream getStdIn() {
        return stdIn;
    }

    public ValueExpr encode(ModuleScope moduleScope, EvalContext context, Writer writer,
                            ValueExpr source, Map<String, ValueExpr> args, SourceCodeRef sourceCodeRef) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ValueExpr charsetExpr = args.get("charset");
        Charset charset = Charset.defaultCharset();
        if (charsetExpr instanceof StringValueExpr) {
            charset = Charset.forName(((StringValueExpr)charsetExpr).getValue());
        }
        writer.write(moduleScope, context, source, out, charset, args, sourceCodeRef);
        return new StringValueExpr(out.toString());
    }

    public void writeToFile(ModuleScope moduleScope, EvalContext context, Writer writer, String destPath,
                            ValueExpr source, Map<String, ValueExpr> args, SourceCodeRef sourceCodeRef) {
        try (FileOutputStream out = new FileOutputStream(destPath)) {
            ValueExpr charsetExpr = args.get("charset");
            Charset charset = Charset.defaultCharset();
            if (charsetExpr instanceof StringValueExpr) {
                charset = Charset.forName(((StringValueExpr)charsetExpr).getValue());
            }
            writer.write(moduleScope, context, source, out, charset, args, sourceCodeRef);
        } catch (IOException ex) {
            throw new BuiltinFunctionError(ex, sourceCodeRef);
        }
    }

    public static void writeXml(ModuleScope moduleScope, EvalContext evalContext, ValueExpr source, OutputStream out,
                                Charset charset, Map<String, ValueExpr> args, SourceCodeRef sourceCodeRef) {
        RecordValueExpr xmlMappingExpr = (RecordValueExpr) args.get("xmlMapping");
        if (xmlMappingExpr == null) {
            throw new IllegalArgumentError("'xmlMapping' cannot be null", sourceCodeRef);
        }

        XmlWriter xmlWriter = new XmlWriter(xmlMappingExpr, sourceCodeRef, (RecordValueExpr) source);
        xmlWriter.write(out, charset);
    }

    public static void writeJson(ModuleScope moduleScope, EvalContext evalContext, ValueExpr source, OutputStream out,
                                 Charset charset, Map<String, ValueExpr> args, SourceCodeRef sourceCodeRef) {
        JsonWriter jsonWriter = new JsonWriter(sourceCodeRef, source);
        jsonWriter.write(out, charset);
    }

}
