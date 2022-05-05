package dev.kobu;

import dev.kobu.config.Project;
import dev.kobu.config.ProjectReader;
import dev.kobu.database.Database;
import dev.kobu.interpreter.ast.AnalyzerContext;
import dev.kobu.interpreter.ast.eval.context.EvalContextProvider;
import dev.kobu.interpreter.ast.eval.context.EvalModeEnum;
import dev.kobu.interpreter.ast.symbol.ModuleScope;
import dev.kobu.interpreter.ast.utils.ErrorMessageFormatter;
import dev.kobu.interpreter.codec.CodecNativeFunctionRegistry;
import dev.kobu.interpreter.codec.FileFetcher;
import dev.kobu.interpreter.codec.InputReader;
import dev.kobu.interpreter.codec.OutputWriter;
import dev.kobu.interpreter.error.*;
import dev.kobu.interpreter.file_system.KobuFile;
import dev.kobu.interpreter.file_system.local.LocalKobuFile;
import dev.kobu.interpreter.file_system.local.LocalKobuFileSystem;
import dev.kobu.interpreter.module.ModuleLoader;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class KobuScriptRunner {

    private final File scriptFile;

    private final List<String> arguments;

    private Project project;

    public KobuScriptRunner(File scriptFile, List<String> arguments) {
        this.scriptFile = scriptFile;
        this.arguments = arguments != null ? arguments : new ArrayList<>();
    }

    public KobuScriptRunner(File scriptFile, List<String> arguments, Project project) {
        this(scriptFile, arguments);
        this.project = project;
    }

    public int run(PrintStream out, PrintStream err) {
        try {

            if (!scriptFile.isFile()) {
                err.println("ERROR: File not found: " + scriptFile.getAbsolutePath());
                return 1;
            }

            LocalKobuFileSystem fileSystem = new LocalKobuFileSystem();
            AnalyzerContext analyzerContext = new AnalyzerContext();
            LocalKobuFile localFile = new LocalKobuFile(this.scriptFile);

            Project project = this.project;

            if (project == null) {
                KobuFile projectFile = fileSystem.findProjectDefinition(localFile);
                ProjectReader projectReader = new ProjectReader(fileSystem);

                if (projectFile != null) {
                    project = projectReader.load(projectFile);
                } else {
                    project = projectReader.loadDefaultProject(localFile);
                }
            }

            if (project.getErrors() != null && !project.getErrors().isEmpty()) {
                throw new AnalyzerErrorList(project.getErrors());
            }

            Database database = new Database();
            InputReader inputReader = new InputReader(new FileFetcher());
            OutputWriter outputWriter = new OutputWriter(out, err);
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

            moduleScope.runMainFunction(analyzerContext, evalContextProvider, arguments);

        } catch (ParserErrorList e) {
            for (ParserError error : e.getErrors()) {
                err.println(ErrorMessageFormatter.getMessage(error));
            }
            return 1;
        } catch (AnalyzerErrorList e) {
            for (AnalyzerError error : e.getErrors()) {
                err.println(ErrorMessageFormatter.getMessage(error));
            }
            return 1;
        } catch (AnalyzerError e) {
            err.println(ErrorMessageFormatter.getMessage(e));
            return 1;
        } catch (EvalError e) {
            err.println(ErrorMessageFormatter.getMessage(e));
            return 1;
        }

        return 0;
    }
}
