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

package dev.kobu.interpreter.service;

import dev.kobu.antlr.kobulang.KobuLexer;
import dev.kobu.antlr.kobulang.KobuParser;
import dev.kobu.config.Project;
import dev.kobu.config.ProjectReader;
import dev.kobu.config.error.ProjectError;
import dev.kobu.interpreter.ast.AnalyzerContext;
import dev.kobu.interpreter.file_system.KobuFile;
import dev.kobu.interpreter.file_system.KobuFileSystem;
import dev.kobu.interpreter.file_system.KobuScriptFile;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.IOException;
import java.io.InputStream;

public class KobuFormatter {

    private final KobuFileSystem fileSystem;

    public KobuFormatter(KobuFileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }

    public String format(AnalyzerContext context, KobuFile kobuFile, int tabSize) throws IOException, ProjectError {

        KobuScriptFile script = loadScript(kobuFile);

        try (InputStream fileStream = kobuFile.newInputStream()) {
            var input = CharStreams.fromStream(fileStream);
            var lexer = new KobuLexer(input);
            var tokens = new CommonTokenStream(lexer);
            var parser = new KobuParser(tokens);
            parser.removeErrorListeners();
            context.getParserErrorListener().setCurrentScript(script);
            parser.addErrorListener(context.getParserErrorListener());
            var tree = parser.prog();

            var visitor = new KobuFormatterVisitor(tokens, tabSize);
            visitor.visit(tree);
            return visitor.getFormattedCode();
        }

    }

    private KobuScriptFile loadScript(KobuFile kobuFile) throws ProjectError {
        Project project;
        KobuFile projectFile = fileSystem.findProjectDefinition(kobuFile);
        ProjectReader projectReader = new ProjectReader(fileSystem);
        if (projectFile != null) {
            project = projectReader.load(projectFile);
        } else {
            project = projectReader.loadDefaultProject(kobuFile);
        }

        return fileSystem.loadScript(project.getSrcDirs(), kobuFile);
    }

}
