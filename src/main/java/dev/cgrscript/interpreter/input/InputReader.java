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

package dev.cgrscript.interpreter.input;

import dev.cgrscript.csv.CSVLexer;
import dev.cgrscript.csv.CSVParser;
import dev.cgrscript.interpreter.ast.eval.EvalContext;
import dev.cgrscript.interpreter.ast.eval.ValueExpr;
import dev.cgrscript.interpreter.ast.eval.expr.value.ArrayValueExpr;
import dev.cgrscript.interpreter.ast.symbol.ArrayType;
import dev.cgrscript.interpreter.ast.symbol.Type;
import dev.cgrscript.interpreter.input.parser.CsvParserVisitor;
import dev.cgrscript.interpreter.input.parser.JsonParserVisitor;
import dev.cgrscript.json.JSONLexer;
import dev.cgrscript.json.JSONParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class InputReader {

    private final InputReaderListener listener;

    private final FileFetcher fileFetcher;

    public InputReader(InputReaderListener listener, FileFetcher fileFetcher) {
        this.listener = listener;
        this.fileFetcher = fileFetcher;
    }

    public FileFetcher getFileFetcher() {
        return fileFetcher;
    }

    public ValueExpr readFromFile(EvalContext context, InputParser parser, InputType inputType,
                                  String dir, String pattern, boolean recursive) throws IOException {
        List<File> files = fileFetcher.getFiles(context.getModuleScope().getProjectDir(), dir, pattern, recursive);

        List<ValueExpr> values = new ArrayList<>();
        for (File file : files) {
            try (FileInputStream in = new FileInputStream(file)) {
                values.add(parser.parse(context, file.getAbsolutePath(), file.getName(), in));
            }
        }

        return new ArrayValueExpr(new ArrayType(inputType.getType(context)), values);
    }

    public ValueExpr readFromUrl(EvalContext context, InputParser parser, String url) throws IOException {
        URL urlObj = fileFetcher.getUrl(url);
        try (InputStream in = urlObj.openStream()) {
            return parser.parse(context, urlObj.getPath(), urlObj.getFile(), in);
        }
    }

    public static Type getCsvType(EvalContext context) {
        return (Type) context.getModuleScope().resolve(CsvParserVisitor.CSV_FILE_TYPE);
    }

    public static ValueExpr parseCsv(EvalContext context, String filePath, String fileName, InputStream in) throws IOException {
        var input = CharStreams.fromStream(in);
        var lexer = new CSVLexer(input);
        var tokens = new CommonTokenStream(lexer);
        var parser = new CSVParser(tokens);
        var tree = parser.csvFile();
        var visitor = new CsvParserVisitor(context, filePath, fileName);
        return visitor.visit(tree);
    }

    public static Type getJsonType(EvalContext context) {
        return (Type) context.getModuleScope().resolve(JsonParserVisitor.JSON_FILE_TYPE);
    }

    public static ValueExpr parseJson(EvalContext context, String filePath, String fileName, InputStream in) throws IOException {
        var input = CharStreams.fromStream(in);
        var lexer = new JSONLexer(input);
        var tokens = new CommonTokenStream(lexer);
        var parser = new JSONParser(tokens);
        var tree = parser.json();
        var visitor = new JsonParserVisitor(context, filePath, fileName);
        return visitor.visit(tree);
    }

    public static Type getXmlType(EvalContext context) {
        return null;
    }

    public static ValueExpr parseXml(EvalContext context, String filePath, String fileName, InputStream in) throws IOException {

        return null;
    }

    public static Type getHtmlType(EvalContext context) {
        return null;
    }

    public static ValueExpr parseHtml(EvalContext context, String filePath, String fileName, InputStream in) throws IOException {

        return null;
    }

    public static Type getGraphQlType(EvalContext context) {
        return null;
    }

    public static ValueExpr parseGraphQl(EvalContext context, String filePath, String fileName, InputStream in) throws IOException {

        return null;
    }

    public static Type getDartType(EvalContext context) {
        return null;
    }

    public static ValueExpr parseDart(EvalContext context, String filePath, String fileName, InputStream in) throws IOException {

        return null;
    }

    public static Type getJavaType(EvalContext context) {
        return null;
    }

    public static ValueExpr parseJava(EvalContext context, String filePath, String fileName, InputStream in) throws IOException {

        return null;
    }

    public static Type getTypescriptType(EvalContext context) {
        return null;
    }

    public static ValueExpr parseTypescript(EvalContext context, String filePath, String fileName, InputStream in) throws IOException {

        return null;
    }

}
