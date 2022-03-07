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

package dev.cgrscript.interpreter.service;

import dev.cgrscript.antlr.cgrscript.CgrScriptLexer;
import dev.cgrscript.antlr.cgrscript.CgrScriptParser;
import dev.cgrscript.config.Project;
import dev.cgrscript.config.ProjectReader;
import dev.cgrscript.config.error.ProjectError;
import dev.cgrscript.interpreter.ast.AnalyzerContext;
import dev.cgrscript.interpreter.file_system.CgrFile;
import dev.cgrscript.interpreter.file_system.CgrFileSystem;
import dev.cgrscript.interpreter.file_system.CgrScriptFile;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.IOException;
import java.io.InputStream;

public class CgrScriptFormatter {

    private final CgrFileSystem fileSystem;

    public CgrScriptFormatter(CgrFileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }

    public String format(AnalyzerContext context, CgrFile cgrFile, int tabSize) throws IOException, ProjectError {

        CgrScriptFile script = loadScript(cgrFile);

        try (InputStream fileStream = cgrFile.newInputStream()) {
            var input = CharStreams.fromStream(fileStream);
            var lexer = new CgrScriptLexer(input);
            var tokens = new CommonTokenStream(lexer);
            var parser = new CgrScriptParser(tokens);
            parser.removeErrorListeners();
            context.getParserErrorListener().setCurrentScript(script);
            parser.addErrorListener(context.getParserErrorListener());
            var tree = parser.prog();

            var visitor = new CgrScriptFormatterVisitor(tokens, tabSize);
            visitor.visit(tree);
            return visitor.getFormattedCode();
        }

    }

    private CgrScriptFile loadScript(CgrFile cgrFile) throws ProjectError {
        Project project;
        CgrFile projectFile = fileSystem.findProjectDefinition(cgrFile);
        ProjectReader projectReader = new ProjectReader(fileSystem);
        if (projectFile != null) {
            project = projectReader.load(projectFile);
        } else {
            project = projectReader.loadDefaultProject(cgrFile);
        }

        return fileSystem.loadScript(project.getSrcDirs(), cgrFile);
    }

}
