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

import dev.cgrscript.config.ProjectReader;
import dev.cgrscript.database.Database;
import dev.cgrscript.interpreter.ast.EvalTreeParserVisitor;
import dev.cgrscript.interpreter.ast.TypeHierarchyParserVisitor;
import dev.cgrscript.interpreter.ast.symbol.ModuleScope;
import dev.cgrscript.interpreter.ast.utils.ErrorMessageFormatter;
import dev.cgrscript.interpreter.error.*;
import dev.cgrscript.interpreter.file_system.CgrFile;
import dev.cgrscript.interpreter.file_system.local.LocalCgrFile;
import dev.cgrscript.interpreter.file_system.local.LocalCgrFileSystem;
import dev.cgrscript.interpreter.input.FileFetcher;
import dev.cgrscript.interpreter.input.InputNativeFunctionRegistry;
import dev.cgrscript.interpreter.input.InputReader;
import dev.cgrscript.interpreter.module.AnalyzerStepEnum;
import dev.cgrscript.interpreter.module.ModuleLoader;
import dev.cgrscript.interpreter.writer.FileSystemWriterHandler;
import dev.cgrscript.interpreter.writer.OutputWriter;
import dev.cgrscript.interpreter.writer.OutputWriterLogTypeEnum;
import dev.cgrscript.interpreter.writer.OutputWriterModeEnum;
import picocli.CommandLine;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "run", description = "Run a CGRScript")
public class RunCommand implements Callable<Integer> {

    @CommandLine.Parameters(index = "0", paramLabel = "FILE", description = "Script to run")
    File file;

    @CommandLine.Parameters(index = "1..*", paramLabel = "ARG", description = "Script arguments")
    List<String> scriptArgs;

    @CommandLine.Option(names = "-preview", description = "Do not write generated files to disk")
    boolean preview;

    @CommandLine.Option(names = "-verbose", description = "Write generated files to the standard output")
    boolean verbose;

    @Override
    public Integer call() throws Exception {
        try {

            if (!file.isFile()) {
                System.err.println("ERROR: File not found: " + file.getAbsolutePath());
                return 1;
            }

            LocalCgrFileSystem fileSystem = new LocalCgrFileSystem();
            ParserErrorListener parserErrorListener = new ParserErrorListener();
            ModuleLoader moduleLoader = new ModuleLoader(fileSystem, new ProjectReader());
            InputNativeFunctionRegistry.register(moduleLoader);
            LocalCgrFile localFile = new LocalCgrFile(this.file);
            CgrFile projectFile = fileSystem.findProjectDefinition(localFile);
            ModuleScope moduleScope = moduleLoader.load(parserErrorListener, localFile, projectFile);

            parserErrorListener.checkErrors();

            moduleScope.checkErrors();

            EvalTreeParserVisitor evalTreeParserVisitor = new EvalTreeParserVisitor(moduleLoader, moduleScope, parserErrorListener);
            moduleLoader.visit(moduleScope.getModuleId(), evalTreeParserVisitor, AnalyzerStepEnum.EVAL_TREE);

            moduleScope.checkErrors();

            TypeHierarchyParserVisitor typeHierarchyParserVisitor = new TypeHierarchyParserVisitor(moduleLoader, moduleScope, parserErrorListener);
            moduleLoader.visit(moduleScope.getModuleId(), typeHierarchyParserVisitor, AnalyzerStepEnum.TYPE_HIERARCHY);

            moduleScope.checkErrors();

            Database database = new Database();
            InputReader inputReader = new InputReader(new FileFetcher());
            OutputWriter outputWriter = new OutputWriter(
                    preview ? OutputWriterModeEnum.LOG_ONLY : OutputWriterModeEnum.WRITE_TO_DISK,
                    verbose ? OutputWriterLogTypeEnum.VERBOSE : OutputWriterLogTypeEnum.NORMAL,
                    new FileSystemWriterHandler(moduleScope.getProjectDir()));

            moduleScope.analyze(database, inputReader, outputWriter);

            moduleScope.checkErrors();

            if (scriptArgs == null) {
                scriptArgs = new ArrayList<>();
            }
            moduleScope.runMainFunction(scriptArgs, database, inputReader, outputWriter);

        } catch (ParserErrorList e) {
            for (ParserError error : e.getErrors()) {
                System.err.println(ErrorMessageFormatter.getMessage(error));
            }
            return 1;
        } catch (AnalyzerErrorList e) {
            for (AnalyzerError error : e.getErrors()) {
                System.err.println(ErrorMessageFormatter.getMessage(error));
            }
            return 1;
        } catch (AnalyzerError e) {
            System.err.println(ErrorMessageFormatter.getMessage(e));
            return 1;
        } catch (EvalError e) {
            System.err.println(ErrorMessageFormatter.getMessage(e));
            return 1;
        }
        return 0;
    }

}
