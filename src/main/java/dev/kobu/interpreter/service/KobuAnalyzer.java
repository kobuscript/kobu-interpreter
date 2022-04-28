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

import dev.kobu.config.Project;
import dev.kobu.config.ProjectReader;
import dev.kobu.config.error.ProjectError;
import dev.kobu.database.Database;
import dev.kobu.interpreter.ast.AnalyzerContext;
import dev.kobu.interpreter.ast.AnalyzerErrorScope;
import dev.kobu.interpreter.ast.eval.HasElementRef;
import dev.kobu.interpreter.ast.eval.SymbolDescriptor;
import dev.kobu.interpreter.ast.eval.SymbolDocumentation;
import dev.kobu.interpreter.ast.eval.context.EvalContextProvider;
import dev.kobu.interpreter.ast.eval.context.EvalModeEnum;
import dev.kobu.interpreter.ast.symbol.ModuleScope;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.codec.OutputWriter;
import dev.kobu.interpreter.error.AnalyzerError;
import dev.kobu.interpreter.error.KobuError;
import dev.kobu.interpreter.error.ParserError;
import dev.kobu.interpreter.error.ParserErrorListener;
import dev.kobu.interpreter.file_system.*;
import dev.kobu.interpreter.codec.FileFetcher;
import dev.kobu.interpreter.codec.CodecNativeFunctionRegistry;
import dev.kobu.interpreter.codec.InputReader;
import dev.kobu.interpreter.file_system.local.LocalKobuFile;
import dev.kobu.interpreter.file_system.local.LocalKobuFileSystem;
import dev.kobu.interpreter.module.KobuElementDescriptor;
import dev.kobu.interpreter.module.ModuleLoader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KobuAnalyzer {

    private final KobuFileSystem fileSystem;

    private final Map<String, ModuleLoader> modules = new HashMap<>();

    private final EvalContextProvider evalContextProvider;

    public KobuAnalyzer(KobuFileSystem fileSystem) {
        this.fileSystem = fileSystem;
        Database database = new Database();
        InputReader inputReader = new InputReader(new FileFetcher());
        OutputWriter outputWriter = new OutputWriter();
        evalContextProvider = new EvalContextProvider(EvalModeEnum.ANALYZER_SERVICE, fileSystem, database,
                inputReader, outputWriter);
    }

    public synchronized void removeModule(KobuFile projectFile) {
        modules.remove(projectFile.getAbsolutePath());
    }

    public synchronized List<KobuError> analyze(ModuleLoader moduleLoader, KobuFile file) {
        List<KobuError> errors = new ArrayList<>();

        AnalyzerContext analyzerContext = new AnalyzerContext();

        ModuleScope moduleScope;
        try {
            moduleScope = moduleLoader.load(analyzerContext, file);
        } catch (AnalyzerError e) {
            errors.addAll(e.toKobuError(file));
            return errors;
        }

        addErrors(file, errors, analyzerContext.getParserErrorListener());

        moduleScope.analyze(analyzerContext, evalContextProvider);

        addErrors(file, errors, analyzerContext.getErrorScope());

        return errors;
    }

    public synchronized List<KobuError> analyze(KobuFile file) {

        KobuFile projectFile = fileSystem.findProjectDefinition(file);
        ModuleLoader moduleLoader;
        try {
            moduleLoader = getModuleLoader(projectFile, file);
        } catch (AnalyzerError e) {
            return new ArrayList<>(e.toKobuError(file));
        }

        return analyze(moduleLoader, file);
    }

    public synchronized KobuFile findModuleFile(KobuFile refFile, String moduleId) throws AnalyzerError {
        KobuFile projectFile = fileSystem.findProjectDefinition(refFile);
        ModuleLoader moduleLoader = getModuleLoader(projectFile, refFile);
        return moduleLoader.findModuleFile(moduleId);
    }

    public synchronized KobuElementDescriptor getTypeDescriptor(KobuFile refFile, String typeName) throws AnalyzerError {
        KobuFile projectFile = fileSystem.findProjectDefinition(refFile);
        ModuleLoader moduleLoader = getModuleLoader(projectFile, refFile);
        return moduleLoader.getTypeDescriptor(refFile, typeName);
    }

    public synchronized SourceCodeRef getElementRef(KobuFile refFile, int offset) throws AnalyzerError {
        KobuFile projectFile = fileSystem.findProjectDefinition(refFile);
        ModuleLoader moduleLoader = getModuleLoader(projectFile, refFile);
        KobuScriptFile script = moduleLoader.loadScript(refFile);
        if (script != null) {
            ModuleScope module = moduleLoader.getScope(script.extractModuleId());
            if (module != null) {
                HasElementRef elementRef = module.getRef(offset);
                if (elementRef != null) {
                    return elementRef.getElementRef();
                }
            }
        }
        return null;
    }

    public synchronized List<SymbolDescriptor> getSuggestions(KobuFile refFile, int offset) throws AnalyzerError {
        KobuFile projectFile = fileSystem.findProjectDefinition(refFile);
        ModuleLoader moduleLoader = getModuleLoader(projectFile, refFile);

        KobuScriptFile script = moduleLoader.loadScript(refFile);
        if (script != null) {
            analyze(moduleLoader, refFile);

            String moduleId = script.extractModuleId();
            ModuleScope module = moduleLoader.getScope(moduleId);
            List<ModuleScope> externalModules = moduleLoader.getExternalModules(moduleId);
            if (module != null) {
                return module.getSuggestions(offset, externalModules);
            }
        }
        return null;
    }

    public synchronized SymbolDocumentation getDocumentation(KobuFile refFile, int offset) throws AnalyzerError {
        KobuFile projectFile = fileSystem.findProjectDefinition(refFile);
        ModuleLoader moduleLoader = getModuleLoader(projectFile, refFile);
        return moduleLoader.getSymbolDocumentation(refFile, offset);
    }

    public String getPathModule(KobuFileSystemEntry entry) throws AnalyzerError {
        KobuFile projectFile = fileSystem.findProjectDefinition(entry);
        if (projectFile != null) {
            var projectReader = new ProjectReader(fileSystem);
            Project project = projectReader.load(projectFile);
            var relPath = Path.of(project.getSrcDirs().get(0).getAbsolutePath())
                    .relativize(Path.of(entry.getAbsolutePath()));
            return relPath.toString().replace('/', '.');
        }
        return "";
    }

    public String formatFile(File ioFile, int tabSize) throws ProjectError, IOException {
        KobuFormatter formatter = new KobuFormatter(new LocalKobuFileSystem());
        AnalyzerContext context = new AnalyzerContext();
        return formatter.format(context, new LocalKobuFile(ioFile), tabSize);
    }

    public String loadBuiltinModule(String moduleId) {
        String path = "/" + moduleId.replaceAll("\\.", "/") + KobuFileSystem.SCRIPT_FILE_EXT;
        try (InputStream in = KobuAnalyzer.class.getResourceAsStream(path)) {
            if (in == null) {
                return null;
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            return null;
        }
    }

    private ModuleLoader getModuleLoader(KobuFile projectFile, KobuFile scriptFile) throws AnalyzerError {
        String projectPath;
        if (projectFile != null) {
            projectPath = projectFile.getAbsolutePath();
        } else {
            if (fileSystem.isBuiltinFile(scriptFile)) {
                return modules.values().stream()
                        .filter(m -> m.getProject().getName() != null)
                        .findFirst()
                        .orElse(null);
            }
            projectPath = "-default-";
        }
        var moduleLoader = modules.get(projectPath);
        if (moduleLoader != null) {
            return moduleLoader;
        }

        var projectReader = new ProjectReader(fileSystem);
        Project project;
        if (projectFile != null) {
            project = projectReader.load(projectFile);
        } else {
            project = projectReader.loadDefaultProject(scriptFile);
        }
        moduleLoader = new ModuleLoader(evalContextProvider, fileSystem, project, EvalModeEnum.ANALYZER_SERVICE);
        CodecNativeFunctionRegistry.register(moduleLoader);
        modules.put(projectPath, moduleLoader);
        return moduleLoader;
    }

    private void addErrors(KobuFile file, List<KobuError> errors, ParserErrorListener parserErrorListener) {
        var parseErrors = parserErrorListener.getErrors();

        for (ParserError parseError : parseErrors) {
            if (parseError.getSourceCodeRef().getFile().getAbsolutePath().equals(file.getAbsolutePath())) {
                errors.add(parseError);
            }
        }

    }

    private void addErrors(KobuFile file, List<KobuError> errors, AnalyzerErrorScope errorScope) {
        var analyzerErrors = errorScope.getErrors();
        if (analyzerErrors == null || analyzerErrors.isEmpty()) {
            return;
        }
        for (AnalyzerError analyzerError : analyzerErrors) {
            errors.addAll(analyzerError.toKobuError(file));
        }
    }

}
