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

import dev.kobu.antlr.json.JSONLexer;
import dev.kobu.antlr.json.JSONParser;
import dev.kobu.interpreter.ast.eval.ValueExpr;
import dev.kobu.interpreter.ast.eval.context.EvalContext;
import dev.kobu.interpreter.ast.eval.expr.value.ArrayValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.RecordTypeRefValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.RecordValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.StringValueExpr;
import dev.kobu.interpreter.ast.symbol.ModuleScope;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.ast.symbol.Type;
import dev.kobu.interpreter.ast.symbol.array.ArrayTypeFactory;
import dev.kobu.interpreter.error.eval.IllegalArgumentError;
import dev.kobu.interpreter.codec.impl.CsvFileParser;
import dev.kobu.interpreter.codec.impl.JsonParserVisitor;
import dev.kobu.interpreter.codec.impl.XmlFileParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InputReader {

    private final FileFetcher fileFetcher;

    public InputReader(FileFetcher fileFetcher) {
        this.fileFetcher = fileFetcher;
    }

    public ValueExpr readFromFile(ModuleScope moduleScope, EvalContext context, Parser parser, CodecType codecType,
                                  String dir, String pattern, boolean recursive, Map<String, ValueExpr> args,
                                  SourceCodeRef sourceCodeRef) throws IOException {
        List<File> files = fileFetcher.getFiles(context.getModuleScope().getProjectDir(), dir, pattern, recursive);

        List<ValueExpr> values = new ArrayList<>();
        for (File file : files) {
            try (InputStream in = context.getFileSystem().getInputStream(file.toPath())) {
                values.add(parser.parse(moduleScope, context, file.getAbsolutePath(), in, args, sourceCodeRef));
            }
        }

        return new ArrayValueExpr(ArrayTypeFactory.getArrayTypeFor(codecType.getType(moduleScope)), values);
    }

    public static Type getCsvType(ModuleScope moduleScope) {
        return (Type) moduleScope.resolve(CsvFileParser.CSV_FILE_TYPE);
    }

    public static ValueExpr parseCsv(ModuleScope moduleScope, EvalContext context, String filePath,
                                     InputStream in, Map<String, ValueExpr> args,
                                     SourceCodeRef sourceCodeRef) throws IOException {
        StringValueExpr formatExpr = (StringValueExpr) args.get("format");
        StringValueExpr charsetExpr = (StringValueExpr) args.get("charset");

        return CsvFileParser.parse(moduleScope, context, filePath, in, formatExpr, charsetExpr, sourceCodeRef);
    }

    public static Type getJsonType(ModuleScope moduleScope) {
        return (Type) moduleScope.resolve(JsonParserVisitor.JSON_FILE_TYPE);
    }

    public static ValueExpr parseJson(ModuleScope moduleScope, EvalContext context, String filePath, InputStream in,
                                      Map<String, ValueExpr> args, SourceCodeRef sourceCodeRef) throws IOException {
        RecordTypeRefValueExpr recordTypeExpr = (RecordTypeRefValueExpr) args.get("recordType");
        StringValueExpr charsetExpr = (StringValueExpr) args.get("charset");

        if (recordTypeExpr == null) {
            throw new IllegalArgumentError("'recordType' cannot be null", sourceCodeRef);
        }

        Charset charset = Charset.defaultCharset();
        if (charsetExpr != null) {
            charset = Charset.forName(charsetExpr.getValue());
        }

        var input = CharStreams.fromStream(in, charset);
        var lexer = new JSONLexer(input);
        var tokens = new CommonTokenStream(lexer);
        var parser = new JSONParser(tokens);
        var tree = parser.json();
        var visitor = new JsonParserVisitor(moduleScope, context, recordTypeExpr.getValue(), filePath, sourceCodeRef);
        return visitor.visit(tree);
    }

    public static Type getXmlType(ModuleScope moduleScope) {
        return (Type) moduleScope.resolve(XmlFileParser.XML_FILE_TYPE);
    }

    public static ValueExpr parseXml(ModuleScope moduleScope, EvalContext context, String filePath, InputStream in,
                                     Map<String, ValueExpr> args, SourceCodeRef sourceCodeRef) throws IOException {
        RecordValueExpr xmlMappingExpr = (RecordValueExpr) args.get("xmlMapping");
        StringValueExpr charsetExpr = (StringValueExpr) args.get("charset");

        if (xmlMappingExpr == null) {
            throw new IllegalArgumentError("'xmlMapping' cannot be null", sourceCodeRef);
        }

        Charset charset = Charset.defaultCharset();
        if (charsetExpr != null) {
            charset = Charset.forName(charsetExpr.getValue());
        }

        var parser = new XmlFileParser(moduleScope, context, xmlMappingExpr, filePath, charset, sourceCodeRef);
        return parser.parse(in);
    }

    public static Type getDartType(ModuleScope moduleScope) {
        return null;
    }

    public static ValueExpr parseDart(ModuleScope moduleScope, EvalContext context, String filePath, InputStream in,
                                      Map<String, ValueExpr> args, SourceCodeRef sourceCodeRef) throws IOException {

        return null;
    }

    public static Type getJavaType(ModuleScope moduleScope) {
        return null;
    }

    public static ValueExpr parseJava(ModuleScope moduleScope, EvalContext context, String filePath, InputStream in,
                                      Map<String, ValueExpr> args, SourceCodeRef sourceCodeRef) throws IOException {

        return null;
    }

    public static Type getTypescriptType(ModuleScope moduleScope) {
        return null;
    }

    public static ValueExpr parseTypescript(ModuleScope moduleScope, EvalContext context, String filePath, InputStream in,
                                            Map<String, ValueExpr> args, SourceCodeRef sourceCodeRef) throws IOException {

        return null;
    }

}
