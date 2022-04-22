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

package dev.kobu.interpreter;

import dev.kobu.config.Project;
import dev.kobu.config.ProjectReader;
import dev.kobu.database.Database;
import dev.kobu.interpreter.ast.AnalyzerContext;
import dev.kobu.interpreter.ast.eval.context.EvalContextProvider;
import dev.kobu.interpreter.ast.eval.context.EvalModeEnum;
import dev.kobu.interpreter.ast.symbol.ModuleScope;
import dev.kobu.interpreter.ast.utils.ErrorMessageFormatter;
import dev.kobu.interpreter.codec.OutputWriter;
import dev.kobu.interpreter.error.*;
import dev.kobu.interpreter.file_system.KobuFile;
import dev.kobu.interpreter.file_system.local.LocalKobuFile;
import dev.kobu.interpreter.file_system.local.LocalKobuFileSystem;
import dev.kobu.interpreter.codec.FileFetcher;
import dev.kobu.interpreter.codec.CodecNativeFunctionRegistry;
import dev.kobu.interpreter.codec.InputReader;
import dev.kobu.interpreter.module.ModuleLoader;
import picocli.CommandLine;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "run", description = "Run a KOBU script")
public class RunCliCommand implements Callable<Integer> {

    @CommandLine.Parameters(index = "0", paramLabel = "FILE", description = "Script to run")
    File file;

    @CommandLine.Parameters(index = "1..*", paramLabel = "ARG", description = "Script arguments")
    List<String> scriptArgs;

    @Override
    public Integer call() throws Exception {
        try {

            if (!file.isFile()) {
                System.err.println("ERROR: File not found: " + file.getAbsolutePath());
                return 1;
            }

            LocalKobuFileSystem fileSystem = new LocalKobuFileSystem();
            AnalyzerContext analyzerContext = new AnalyzerContext();
            LocalKobuFile localFile = new LocalKobuFile(this.file);
            KobuFile projectFile = fileSystem.findProjectDefinition(localFile);
            ProjectReader projectReader = new ProjectReader(fileSystem);
            Project project;
            if (projectFile != null) {
                project = projectReader.load(projectFile);
            } else {
                project = projectReader.loadDefaultProject(localFile);
            }

            Database database = new Database();
            InputReader inputReader = new InputReader(new FileFetcher());
            OutputWriter outputWriter = new OutputWriter();
            EvalContextProvider evalContextProvider = new EvalContextProvider(EvalModeEnum.EXECUTION, fileSystem,
                    database, inputReader, outputWriter);

            ModuleLoader moduleLoader = new ModuleLoader(evalContextProvider, fileSystem, project, EvalModeEnum.EXECUTION);
            CodecNativeFunctionRegistry.register(moduleLoader);
            ModuleScope moduleScope = moduleLoader.load(analyzerContext, localFile);

            analyzerContext.getParserErrorListener().checkErrors();
            moduleScope.analyze(analyzerContext, evalContextProvider);

            List<AnalyzerError> errors = analyzerContext.getAllErrors();
            if (!errors.isEmpty()) {
                throw new AnalyzerErrorList(errors);
            }

            if (scriptArgs == null) {
                scriptArgs = new ArrayList<>();
            }

            moduleScope.runMainFunction(analyzerContext, evalContextProvider, scriptArgs);

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
