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
import dev.cgrscript.config.error.ProjectError;
import dev.cgrscript.database.Database;
import dev.cgrscript.interpreter.ast.EvalTreeParserVisitor;
import dev.cgrscript.interpreter.ast.TypeHierarchyParserVisitor;
import dev.cgrscript.interpreter.ast.symbol.ModuleScope;
import dev.cgrscript.interpreter.error.AnalyzerError;
import dev.cgrscript.interpreter.error.CgrScriptError;
import dev.cgrscript.interpreter.error.ParserError;
import dev.cgrscript.interpreter.error.ParserErrorListener;
import dev.cgrscript.interpreter.file_system.CgrFile;
import dev.cgrscript.interpreter.file_system.CgrFileSystem;
import dev.cgrscript.interpreter.input.FileFetcher;
import dev.cgrscript.interpreter.input.InputNativeFunctionRegistry;
import dev.cgrscript.interpreter.input.InputReader;
import dev.cgrscript.interpreter.module.AnalyzerStepEnum;
import dev.cgrscript.interpreter.module.ModuleLoader;
import dev.cgrscript.interpreter.writer.FileSystemWriterHandler;
import dev.cgrscript.interpreter.writer.OutputWriter;
import dev.cgrscript.interpreter.writer.OutputWriterLogTypeEnum;
import dev.cgrscript.interpreter.writer.OutputWriterModeEnum;

import java.util.ArrayList;
import java.util.List;

public class CgrScriptAnalyzerService {

    private final CgrFileSystem fileSystem;

    private final Database database = new Database();

    private final InputReader inputReader = new InputReader(new FileFetcher());

    private final OutputWriter outputWriter = new OutputWriter(
            OutputWriterModeEnum.LOG_ONLY,
            OutputWriterLogTypeEnum.NORMAL,
            new FileSystemWriterHandler());

    public CgrScriptAnalyzerService(CgrFileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }

    public List<CgrScriptError> analyze(CgrFile file) {

        List<CgrScriptError> errors = new ArrayList<>();

        ParserErrorListener parserErrorListener = new ParserErrorListener();
        ProjectReader projectReader;
        try {
            projectReader = new ProjectReader();
        } catch (ProjectError e) {
            //Project definition has errors. We can't validate the current script.
            return errors;
        }
        ModuleLoader moduleLoader = new ModuleLoader(fileSystem, projectReader, parserErrorListener);
        InputNativeFunctionRegistry.register(moduleLoader);
        ModuleScope moduleScope;
        try {
            moduleScope = moduleLoader.load(file);
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

        EvalTreeParserVisitor evalTreeParserVisitor = new EvalTreeParserVisitor(moduleLoader, moduleScope);
        try {
            moduleLoader.visit(moduleScope.getModuleId(), evalTreeParserVisitor, AnalyzerStepEnum.EVAL_TREE);
        } catch (AnalyzerError e) {
            errors.addAll(e.toCgrScriptError(file));
            return errors;
        }

        if (addErrors(file, errors, moduleScope)) {
            return errors;
        }

        TypeHierarchyParserVisitor typeHierarchyParserVisitor = new TypeHierarchyParserVisitor(moduleLoader, moduleScope);
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
