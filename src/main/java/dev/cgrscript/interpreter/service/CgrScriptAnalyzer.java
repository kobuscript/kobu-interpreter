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

import dev.cgrscript.config.Project;
import dev.cgrscript.config.ProjectReader;
import dev.cgrscript.database.Database;
import dev.cgrscript.interpreter.ast.AnalyzerContext;
import dev.cgrscript.interpreter.ast.AnalyzerErrorScope;
import dev.cgrscript.interpreter.ast.EvalTreeParserVisitor;
import dev.cgrscript.interpreter.ast.TypeHierarchyParserVisitor;
import dev.cgrscript.interpreter.ast.eval.EvalModeEnum;
import dev.cgrscript.interpreter.ast.eval.HasElementRef;
import dev.cgrscript.interpreter.ast.eval.SymbolDescriptor;
import dev.cgrscript.interpreter.ast.symbol.ModuleScope;
import dev.cgrscript.interpreter.ast.symbol.SourceCodeRef;
import dev.cgrscript.interpreter.error.AnalyzerError;
import dev.cgrscript.interpreter.error.CgrScriptError;
import dev.cgrscript.interpreter.error.ParserError;
import dev.cgrscript.interpreter.error.ParserErrorListener;
import dev.cgrscript.interpreter.file_system.CgrFile;
import dev.cgrscript.interpreter.file_system.CgrFileSystem;
import dev.cgrscript.interpreter.file_system.CgrScriptFile;
import dev.cgrscript.interpreter.input.FileFetcher;
import dev.cgrscript.interpreter.input.InputNativeFunctionRegistry;
import dev.cgrscript.interpreter.input.InputReader;
import dev.cgrscript.interpreter.module.CgrElementDescriptor;
import dev.cgrscript.interpreter.module.ModuleLoader;
import dev.cgrscript.interpreter.writer.FileSystemWriterHandler;
import dev.cgrscript.interpreter.writer.OutputWriter;
import dev.cgrscript.interpreter.writer.OutputWriterLogTypeEnum;
import dev.cgrscript.interpreter.writer.OutputWriterModeEnum;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CgrScriptAnalyzer {

    private final Database database = new Database();

    private final InputReader inputReader = new InputReader(new FileFetcher());

    private final CgrFileSystem fileSystem;

    private final Map<String, ModuleLoader> modules = new HashMap<>();

    private final OutputWriter outputWriter = new OutputWriter(
            OutputWriterModeEnum.LOG_ONLY,
            OutputWriterLogTypeEnum.NORMAL,
            new FileSystemWriterHandler());

    public CgrScriptAnalyzer(CgrFileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }

    public synchronized void removeModule(CgrFile projectFile) {
        modules.remove(projectFile.getAbsolutePath());
    }

    public synchronized List<CgrScriptError> analyze(ModuleLoader moduleLoader, CgrFile file) {
        List<CgrScriptError> errors = new ArrayList<>();

        AnalyzerContext analyzerContext = new AnalyzerContext();

        ModuleScope moduleScope;
        try {
            moduleScope = moduleLoader.load(analyzerContext, file);
        } catch (AnalyzerError e) {
            errors.addAll(e.toCgrScriptError(file));
            return errors;
        }

        addErrors(file, errors, analyzerContext.getParserErrorListener());
        addErrors(file, errors, analyzerContext.getErrorScope());

        moduleScope.analyze(analyzerContext, database, inputReader, outputWriter);

        addErrors(file, errors, analyzerContext.getErrorScope());

        return errors;
    }

    public synchronized List<CgrScriptError> analyze(CgrFile file) {

        CgrFile projectFile = fileSystem.findProjectDefinition(file);
        ModuleLoader moduleLoader;
        try {
            moduleLoader = getModuleLoader(projectFile, file);
        } catch (AnalyzerError e) {
            return new ArrayList<>(e.toCgrScriptError(file));
        }

        return analyze(moduleLoader, file);
    }

    public synchronized CgrFile findModuleFile(CgrFile refFile, String moduleId) throws AnalyzerError {
        CgrFile projectFile = fileSystem.findProjectDefinition(refFile);
        ModuleLoader moduleLoader = getModuleLoader(projectFile, refFile);
        return moduleLoader.findModuleFile(moduleId);
    }

    public synchronized CgrElementDescriptor getTypeDescriptor(CgrFile refFile, String typeName) throws AnalyzerError {
        CgrFile projectFile = fileSystem.findProjectDefinition(refFile);
        ModuleLoader moduleLoader = getModuleLoader(projectFile, refFile);
        return moduleLoader.getTypeDescriptor(refFile, typeName);
    }

    public synchronized SourceCodeRef getElementRef(CgrFile refFile, int offset) throws AnalyzerError {
        CgrFile projectFile = fileSystem.findProjectDefinition(refFile);
        ModuleLoader moduleLoader = getModuleLoader(projectFile, refFile);
        CgrScriptFile script = moduleLoader.loadScript(refFile);
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

    public synchronized List<SymbolDescriptor> getSuggestions(CgrFile refFile, int offset) throws AnalyzerError {
        CgrFile projectFile = fileSystem.findProjectDefinition(refFile);
        ModuleLoader moduleLoader = getModuleLoader(projectFile, refFile);

        CgrScriptFile script = moduleLoader.loadScript(refFile);
        if (script != null) {
            if (!moduleLoader.indexBuilt()) {
                moduleLoader.buildIndex(new AnalyzerContext());
            }
            analyze(moduleLoader, refFile);

            String moduleId = script.extractModuleId();
            ModuleScope module = moduleLoader.getScope(moduleId);
            List<ModuleScope> externalModules = moduleLoader.getExternalModules(moduleId);
            if (module != null) {
                return module.getSuggestions(offset, externalModules);
            }
        }
        return new ArrayList<>();
    }

    public String loadBuiltinModule(String moduleId) {
        String path = "/" + moduleId.replaceAll("\\.", "/") + CgrFileSystem.SCRIPT_FILE_EXT;
        try (InputStream in = CgrScriptAnalyzer.class.getResourceAsStream(path)) {
            if (in == null) {
                return null;
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            return null;
        }
    }

    private ModuleLoader getModuleLoader(CgrFile projectFile, CgrFile scriptFile) throws AnalyzerError {
        String projectPath = projectFile != null ? projectFile.getAbsolutePath() : "-default-";
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
        moduleLoader = new ModuleLoader(fileSystem, project, EvalModeEnum.ANALYZER_SERVICE);
        InputNativeFunctionRegistry.register(moduleLoader);
        modules.put(projectPath, moduleLoader);
        return moduleLoader;
    }

    private boolean addErrors(CgrFile file, List<CgrScriptError> errors, ParserErrorListener parserErrorListener) {
        var parseErrors = parserErrorListener.getErrors();
        var hasErrors = false;

        for (ParserError parseError : parseErrors) {
            if (parseError.getSourceCodeRef().getFile().getAbsolutePath().equals(file.getAbsolutePath())) {
                errors.add(parseError);
                hasErrors = true;
            }
        }

        return hasErrors;
    }

    private boolean addErrors(CgrFile file, List<CgrScriptError> errors, AnalyzerErrorScope errorScope) {
        var analyzerErrors = errorScope.getErrors();
        if (analyzerErrors == null || analyzerErrors.isEmpty()) {
            return false;
        }
        for (AnalyzerError analyzerError : analyzerErrors) {
            errors.addAll(analyzerError.toCgrScriptError(file));
        }
        return true;
    }

}
