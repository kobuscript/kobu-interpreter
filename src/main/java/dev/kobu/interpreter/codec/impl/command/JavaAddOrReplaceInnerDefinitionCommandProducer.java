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

package dev.kobu.interpreter.codec.impl.command;

import dev.kobu.antlr.java.JavaLexer;
import dev.kobu.antlr.java.JavaParser;
import dev.kobu.interpreter.ast.eval.expr.value.RecordValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.StringValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.TemplateValueExpr;
import dev.kobu.interpreter.ast.symbol.ModuleScope;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.ast.utils.RecordUtils;
import dev.kobu.interpreter.codec.command.AddContentCommand;
import dev.kobu.interpreter.codec.command.RemoveContentCommand;
import dev.kobu.interpreter.codec.command.TextFileCommand;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class JavaAddOrReplaceInnerDefinitionCommandProducer extends JavaCommandProducer {

    public JavaAddOrReplaceInnerDefinitionCommandProducer(ModuleScope moduleScope) {
        super(moduleScope);
    }

    @Override
    public List<TextFileCommand> produce(InputStream in, String filePath, RecordValueExpr commandRec,
                                         SourceCodeRef sourceCodeRef) throws IOException {

        List<TextFileCommand> commands = new ArrayList<>();
        mainClassName = getMainClassName(filePath);
        StringValueExpr nameExpr = (StringValueExpr) RecordUtils.getRequiredField(commandRec, "name", sourceCodeRef);
        TemplateValueExpr contentExpr = (TemplateValueExpr) RecordUtils.getRequiredField(commandRec, "content", sourceCodeRef);
        String definitionName = nameExpr.getValue();

        var input = CharStreams.fromStream(in);
        var lexer = new JavaLexer(input);
        var tokens = new CommonTokenStream(lexer);
        var parser = new JavaParser(tokens);
        var tree = parser.compilationUnit();
        this.visit(tree);

        Ref ref = innerDefMap.get(definitionName);

        int startIdx;
        if (ref != null) {
            commands.add(new RemoveContentCommand(filePath, ref.startIdx, ref.stopIdx));
            startIdx = ref.startIdx;
        } else {
            startIdx = IntStream.of(bodyStartIdx, lastFieldStopIdx, lastConstructorStopIdx,
                    lastMethodStopIdx, lastInnerDefStopIdx).max().getAsInt();
        }
        commands.add(new AddContentCommand(filePath, startIdx, contentExpr.getValue()));

        return commands;
    }

}
