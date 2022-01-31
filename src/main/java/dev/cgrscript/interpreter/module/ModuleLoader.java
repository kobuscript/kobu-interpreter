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

package dev.cgrscript.interpreter.module;

import dev.cgrscript.config.DependencyResolver;
import dev.cgrscript.config.Project;
import dev.cgrscript.config.ProjectReader;
import dev.cgrscript.config.ProjectSourcePath;
import dev.cgrscript.database.Database;
import dev.cgrscript.interpreter.ast.CgrScriptParserVisitor;
import dev.cgrscript.interpreter.ast.ModuleParserVisitor;
import dev.cgrscript.interpreter.ast.eval.function.NativeFunction;
import dev.cgrscript.interpreter.ast.eval.function.NativeFunctionId;
import dev.cgrscript.interpreter.ast.symbol.*;
import dev.cgrscript.interpreter.error.AnalyzerError;
import dev.cgrscript.interpreter.error.ParserErrorListener;
import dev.cgrscript.interpreter.error.analyzer.CyclicModuleReferenceError;
import dev.cgrscript.interpreter.error.analyzer.InternalParserError;
import dev.cgrscript.interpreter.error.analyzer.InvalidFileError;
import dev.cgrscript.interpreter.error.analyzer.ModuleNotFoundError;
import dev.cgrscript.interpreter.input.InputReader;
import dev.cgrscript.interpreter.writer.OutputWriter;
import dev.cgrscript.parser.CgrScriptLexer;
import dev.cgrscript.parser.CgrScriptParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;

public class ModuleLoader {

    public static final String SCRIPT_FILE_EXT = ".cgr";

    private final ProjectReader projectReader;

    private final ParserErrorListener parserErrorListener;

    private Project project;
    private File projectDir;
    private final List<File> srcDirs = new ArrayList<>();

    private final Map<String, ModuleScope> modules = new HashMap<>();

    private final Map<String, ParseTree> modulesParseTree = new HashMap<>();

    private final LinkedHashSet<String> moduleLoaderStack = new LinkedHashSet<>();

    private final Map<NativeFunctionId, NativeFunction> nativeFunctions = new HashMap<>();

    public ModuleLoader(ProjectReader projectReader, ParserErrorListener parserErrorListener) {
        this.projectReader = projectReader;
        this.parserErrorListener = parserErrorListener;
    }

    public void addNativeFunction(NativeFunctionId nativeFunctionId, NativeFunction nativeFunction) {
        nativeFunctions.put(nativeFunctionId, nativeFunction);
    }

    public ModuleScope load(File file) throws AnalyzerError {
        loadRoot(file);
        if (project == null) {
            createDefaultProject(file);
        }
        var srcDir = getSrcDir(file);

        ModuleScope module = loadModule(new FileScriptRef(srcDir, file), null, null);
        modules.put(module.getModuleId(), module);

        return module;
    }

    public ModuleScope getScope(String moduleId, SourceCodeRef sourceCodeRef) throws AnalyzerError {

        ModuleScope module = modules.get(moduleId);
        if (module != null) {
            return module;
        }

        var classpathRef = getModuleFromClasspath(moduleId);
        if (classpathRef != null) {
            module = loadModule(classpathRef, moduleId, sourceCodeRef);
        } else {

            File moduleFile = getRelativeFile(moduleId);
            File srcDir = getSrcDirRelativeFile(moduleFile);
            if (srcDir == null) {
                throw new ModuleNotFoundError(sourceCodeRef, moduleId);
            }
            moduleFile = srcDir.toPath().resolve(moduleFile.toPath()).toFile();

            module = loadModule(new FileScriptRef(srcDir, moduleFile), moduleId, sourceCodeRef);
        }
        modules.put(module.getModuleId(), module);
        return module;
    }

    public void visit(String moduleId, CgrScriptParserVisitor<?> visitor, AnalyzerStepEnum step) throws AnalyzerError {
        var moduleScope = modules.get(moduleId);
        var parserTree = modulesParseTree.get(moduleId);
        if (parserTree == null) {
            throw new InternalParserError("ParserTree not found for " + moduleId);
        }
        if (step.equals(moduleScope.getStep())) {
            return;
        }
        visitor.visit(parserTree);
        moduleScope.setStep(step);
    }

    public void analyze(Database database, InputReader inputReader, OutputWriter outputWriter) {
        for (ModuleScope module : modules.values()) {
            module.analyze(database, inputReader, outputWriter);
        }
    }

    public List<AnalyzerError> getErrors() {
        List<AnalyzerError> errors = new ArrayList<>();
        for (ModuleScope module : modules.values()) {
            if (module.getErrors() != null) {
                errors.addAll(module.getErrors());
            }
        }
        return errors;
    }

    public File getProjectDir() {
        return projectDir;
    }

    public Project getProject() {
        return project;
    }

    private void loadRoot(File file) throws AnalyzerError {
        if (project == null && file != null) {
            if (file.isFile()) {
                if (file.getName().equals(ProjectReader.PROJECT_CFG)) {
                    this.loadProjectDefinition(file);
                    return;
                }
                loadRoot(file.getParentFile());
            } else if (file.isDirectory()) {
                File[] items = file.listFiles();
                if (items != null) {
                    for (File dirItem : items) {
                        if (dirItem.isFile() && dirItem.getName().equals(ProjectReader.PROJECT_CFG)) {
                            this.loadProjectDefinition(dirItem);
                            return;
                        }
                    }
                    loadRoot(file.getParentFile());
                }
            }
        }
    }

    private void createDefaultProject(File scriptFile) {
        this.projectDir = scriptFile.getParentFile();
        this.project = new Project();
        srcDirs.add(scriptFile.getParentFile());
    }

    private void loadProjectDefinition(File file) throws AnalyzerError {
        this.project = projectReader.load(file);
        if (project.getSourcePaths() == null || project.getSourcePaths().isEmpty()) {
            srcDirs.add(file.getParentFile());
        } else {
            for (ProjectSourcePath sourcePath : project.getSourcePaths()) {
                srcDirs.add(new File(file.getParentFile(), sourcePath.getPath()));
            }
        }

        DependencyResolver dependencyResolver = new DependencyResolver(project, projectReader);
        srcDirs.addAll(dependencyResolver.getDependenciesSrcDirs());

        this.projectDir = file.getParentFile();
    }

    private File getSrcDir(File file) throws AnalyzerError {
        Path filePath = file.toPath();
        for (File srcDir : srcDirs) {
            if (filePath.startsWith(srcDir.toPath())) {
                return srcDir;
            }
        }
        throw new InvalidFileError(new SourceCodeRef(new FileScriptRef(file)), srcDirs);
    }

    private File getRelativeFile(String moduleId) {
        return new File(moduleId.replace('.', '/') + SCRIPT_FILE_EXT);
    }

    private ClasspathScriptRef getModuleFromClasspath(String moduleId) {
        String path = "/" + moduleId.replaceAll("\\.", "/") + SCRIPT_FILE_EXT;
        if (ModuleLoader.class.getResource(path) != null) {
            return new ClasspathScriptRef(path);
        }
        return null;
    }

    private File getSrcDirRelativeFile(File file) {
        Path filePath = file.toPath();
        for (File srcDir : srcDirs) {
            File fullFilePath = srcDir.toPath().resolve(filePath).toFile();
            if (fullFilePath.isFile()) {
                return srcDir;
            }
        }
        return null;
    }

    private ModuleScope loadModule(ScriptRef script, String moduleId, SourceCodeRef sourceCodeRef) throws AnalyzerError {

        String scriptModule = moduleId != null ? moduleId : script.extractModuleId();

        if (!moduleLoaderStack.add(scriptModule)) {
            ArrayList<String> dependencyPath = new ArrayList<>(moduleLoaderStack);
            dependencyPath.add(scriptModule);
            throw new CyclicModuleReferenceError(sourceCodeRef, dependencyPath);
        }

        try (InputStream fileStream = script.newInputStream()) {

            var input = CharStreams.fromStream(fileStream);
            var lexer = new CgrScriptLexer(input);
            var tokens = new CommonTokenStream(lexer);
            var parser = new CgrScriptParser(tokens);
            parser.removeErrorListeners();
            parserErrorListener.setCurrentScript(script);
            parser.addErrorListener(parserErrorListener);
            var tree = parser.prog();

            var visitor = new ModuleParserVisitor(this, script, nativeFunctions);
            visitor.visit(tree);

            var moduleScope = visitor.getModuleScope();
            moduleScope.setStep(AnalyzerStepEnum.MODULE);

            modulesParseTree.put(moduleScope.getModuleId(), tree);

            return moduleScope;

        } catch (IOException ex) {
            throw new ModuleNotFoundError(sourceCodeRef, scriptModule);
        } finally {
            moduleLoaderStack.remove(scriptModule);
        }

    }

}
