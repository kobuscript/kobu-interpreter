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

import dev.cgrscript.config.ProjectReader;
import dev.cgrscript.database.Database;
import dev.cgrscript.interpreter.ast.EvalTreeParserVisitor;
import dev.cgrscript.interpreter.ast.TypeHierarchyParserVisitor;
import dev.cgrscript.interpreter.ast.eval.HasElementRef;
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
import dev.cgrscript.interpreter.module.AnalyzerStepEnum;
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

    private final ProjectReader projectReader = new ProjectReader();

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

    public synchronized List<CgrScriptError> analyze(CgrFile file) {

        CgrFile projectFile = fileSystem.findProjectDefinition(file);
        ModuleLoader moduleLoader = getModuleLoader(projectFile);

        List<CgrScriptError> errors = new ArrayList<>();

        ParserErrorListener parserErrorListener = new ParserErrorListener();

        ModuleScope moduleScope;
        try {
            moduleScope = moduleLoader.load(parserErrorListener, file, projectFile);
        } catch (AnalyzerError e) {
            errors.addAll(e.toCgrScriptError(file));
            return errors;
        }

        if (addErrors(file, errors, parserErrorListener)) {
            return errors;
        }

        if (addErrors(file, errors, moduleScope)) {
            return errors;
        }

        EvalTreeParserVisitor evalTreeParserVisitor = new EvalTreeParserVisitor(moduleLoader, moduleScope, parserErrorListener);
        try {
            moduleLoader.visit(moduleScope.getModuleId(), evalTreeParserVisitor, AnalyzerStepEnum.EVAL_TREE);
        } catch (AnalyzerError e) {
            errors.addAll(e.toCgrScriptError(file));
            return errors;
        }

        if (addErrors(file, errors, moduleScope)) {
            return errors;
        }

        TypeHierarchyParserVisitor typeHierarchyParserVisitor = new TypeHierarchyParserVisitor(moduleLoader, moduleScope, parserErrorListener);
        try {
            moduleLoader.visit(moduleScope.getModuleId(), typeHierarchyParserVisitor, AnalyzerStepEnum.TYPE_HIERARCHY);
        } catch (AnalyzerError e) {
            errors.addAll(e.toCgrScriptError(file));
            return errors;
        }

        if (addErrors(file, errors, moduleScope)) {
            return errors;
        }

        moduleScope.analyze(database, inputReader, outputWriter);

        if (addErrors(file, errors, moduleScope)) {
            return errors;
        }

        return errors;
    }

    public synchronized CgrFile findModuleFile(CgrFile refFile, String moduleId) throws AnalyzerError {
        CgrFile projectFile = fileSystem.findProjectDefinition(refFile);
        ModuleLoader moduleLoader = getModuleLoader(projectFile);
        return moduleLoader.findModuleFile(projectFile, refFile, moduleId);
    }

    public synchronized CgrElementDescriptor getTypeDescriptor(CgrFile refFile, String typeName) {
        CgrFile projectFile = fileSystem.findProjectDefinition(refFile);
        ModuleLoader moduleLoader = getModuleLoader(projectFile);
        return moduleLoader.getTypeDescriptor(refFile, typeName);
    }

    public synchronized SourceCodeRef getElementRef(CgrFile refFile, int offset) {
        CgrFile projectFile = fileSystem.findProjectDefinition(refFile);
        ModuleLoader moduleLoader = getModuleLoader(projectFile);
        CgrScriptFile script = moduleLoader.loadScript(refFile);
        if (script != null) {
            ModuleScope module = moduleLoader.getScope(script.extractModuleId());
            HasElementRef elementRef = module.getRef(offset);
            if (elementRef != null) {
                return elementRef.getElementRef();
            }
        }
        return null;
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

    private ModuleLoader getModuleLoader(CgrFile projectFile) {
        String projectPath = projectFile != null ? projectFile.getAbsolutePath() : "-default-";
        return modules.computeIfAbsent(projectPath, k -> {
            var loader = new ModuleLoader(fileSystem, projectReader);
            InputNativeFunctionRegistry.register(loader);
            return loader;
        });
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

    private boolean addErrors(CgrFile file, List<CgrScriptError> errors, ModuleScope moduleScope) {
        var analyzerErrors = moduleScope.getErrors();
        if (analyzerErrors == null || analyzerErrors.isEmpty()) {
            return false;
        }
        for (AnalyzerError analyzerError : analyzerErrors) {
            errors.addAll(analyzerError.toCgrScriptError(file));
        }
        return true;
    }

}
