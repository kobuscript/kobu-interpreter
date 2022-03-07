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

package dev.cgrscript.interpreter;

import dev.cgrscript.config.error.ProjectError;
import dev.cgrscript.interpreter.ast.AnalyzerContext;
import dev.cgrscript.interpreter.ast.utils.ErrorMessageFormatter;
import dev.cgrscript.interpreter.file_system.local.LocalCgrFile;
import dev.cgrscript.interpreter.file_system.local.LocalCgrFileSystem;
import dev.cgrscript.interpreter.service.CgrScriptFormatter;
import picocli.CommandLine;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "format", description = "Format a CGRScript")
public class FormatCommand implements Callable<Integer> {

    @CommandLine.Parameters(index = "0", paramLabel = "FILE", description = "Script to format")
    File file;

    @CommandLine.Option(names = "-tabSize", description = "The number of spaces that will be used to indent blocks of code")
    int tabSize;

    @Override
    public Integer call() throws Exception {

        try {

            if (!file.isFile()) {
                System.err.println("ERROR: File not found: " + file.getAbsolutePath());
                return 1;
            }

            LocalCgrFileSystem fileSystem = new LocalCgrFileSystem();
            AnalyzerContext analyzerContext = new AnalyzerContext();
            LocalCgrFile localFile = new LocalCgrFile(this.file);

            CgrScriptFormatter formatter = new CgrScriptFormatter(fileSystem);
            String formattedCode = formatter.format(analyzerContext, localFile, tabSize);

            try (FileWriter writer = new FileWriter(file)) {
                writer.write(formattedCode);
            }

        } catch (IOException ex) {
            System.err.println(ex.getMessage());
            return 1;
        } catch (ProjectError ex) {
            System.err.println(ErrorMessageFormatter.getMessage(ex));
            return 1;
        }

        return 0;
    }

}
