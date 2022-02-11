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
import dev.cgrscript.config.error.ProjectError;
import dev.cgrscript.interpreter.ast.CgrScriptParserVisitor;
import dev.cgrscript.interpreter.ast.ModuleParserVisitor;
import dev.cgrscript.interpreter.ast.eval.function.NativeFunction;
import dev.cgrscript.interpreter.ast.eval.function.NativeFunctionId;
import dev.cgrscript.interpreter.ast.symbol.*;
import dev.cgrscript.interpreter.error.AnalyzerError;
import dev.cgrscript.interpreter.error.ParserErrorListener;
import dev.cgrscript.interpreter.error.analyzer.CyclicModuleReferenceError;
import dev.cgrscript.interpreter.error.analyzer.InternalParserError;
import dev.cgrscript.interpreter.error.analyzer.ModuleNotFoundError;
import dev.cgrscript.interpreter.file_system.*;
import dev.cgrscript.parser.CgrScriptLexer;
import dev.cgrscript.parser.CgrScriptParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class ModuleLoader {

    private final CgrFileSystem fileSystem;

    private final ProjectReader projectReader;

    private DependencyResolver dependencyResolver;
    private Project project;
    private CgrDirectory projectDir;
    private final List<CgrDirectory> srcDirs = new ArrayList<>();

    private final Map<String, ModuleScope> modules = new HashMap<>();

    private final Map<String, ParseTree> modulesParseTree = new HashMap<>();

    private final LinkedHashSet<String> moduleLoaderStack = new LinkedHashSet<>();

    private final Map<NativeFunctionId, NativeFunction> nativeFunctions = new HashMap<>();

    public ModuleLoader(CgrFileSystem fileSystem, ProjectReader projectReader) {
        this.fileSystem = fileSystem;
        this.projectReader = projectReader;
    }

    public void addNativeFunction(NativeFunctionId nativeFunctionId, NativeFunction nativeFunction) {
        nativeFunctions.put(nativeFunctionId, nativeFunction);
    }

    public ModuleScope load(ParserErrorListener parserErrorListener, CgrFile file, CgrFile projectFile) throws AnalyzerError {
        loadProject(file, projectFile);

        ModuleScope module = loadModule(parserErrorListener, fileSystem.loadScript(srcDirs, file),
                null, null);
        modules.put(module.getModuleId(), module);

        return module;
    }

    public CgrFile findModuleFile(CgrFile projectFile, CgrFile refFile, String moduleId) throws AnalyzerError {
        loadProject(refFile, projectFile);
        return fileSystem.loadScript(srcDirs, moduleId);
    }

    private void loadProject(CgrFile file, CgrFile projectFile) throws ProjectError {
        if (projectFile != null && project == null) {
            loadProjectDefinition(projectFile);
        }
        if (project == null) {
            createDefaultProject(file);
        }
    }

    public CgrElementDescriptor getTypeModule(CgrFile refFile, String typeName) {
        var scriptFile = fileSystem.loadScript(srcDirs, refFile);
        if (scriptFile == null) {
            return null;
        }
        var refModule = modules.get(scriptFile.extractModuleId());
        if (refModule != null) {
            String canonicalName = typeName;
            int idx = typeName.indexOf('.');
            Symbol symbol = null;
            if (idx == -1) {
                symbol = refModule.resolve(typeName);
            } else {
                canonicalName = typeName.substring(idx + 1);
                var otherModule = refModule.resolve(typeName.substring(0, idx));
                if (otherModule instanceof ModuleRefSymbol) {
                    symbol = ((ModuleRefSymbol)otherModule).getModuleScope().resolve(canonicalName);
                }
            }

            if (symbol != null) {
                if (symbol instanceof RecordTypeSymbol) {
                    return CgrElementDescriptor.element(symbol.getSourceCodeRef().getModuleId(), canonicalName);
                } else if (symbol instanceof BuiltinTypeSymbol) {
                    return CgrElementDescriptor.builtinElement(canonicalName);
                }
            }
        }
        return null;
    }

    public CgrElementDescriptor getFunctionModule(CgrFile refFile, String functionName, String scope) {
        var scriptFile = fileSystem.loadScript(srcDirs, refFile);
        if (scriptFile == null) {
            return null;
        }
        var refModule = modules.get(scriptFile.extractModuleId());
        if (refModule != null) {
            Symbol symbol = null;
            if (scope != null) {
                var scopeSymbol = refModule.resolveLocal(scope);
                if (scopeSymbol instanceof ModuleRefSymbol) {
                    symbol = ((ModuleRefSymbol)scopeSymbol).getModuleScope().resolve(functionName);
                }
            } else {
                symbol = refModule.resolve(functionName);
            }

            if (symbol != null) {
                if (symbol instanceof FunctionSymbol) {
                    return CgrElementDescriptor.element(symbol.getSourceCodeRef().getModuleId(), functionName);
                } else if (symbol instanceof BuiltinFunctionSymbol) {
                    return CgrElementDescriptor.builtinElement(functionName);
                }
            }
        }
        return null;
    }

    public ModuleScope getScope(String moduleId) {
        return modules.get(moduleId);
    }

    public ModuleScope loadScope(ParserErrorListener parserErrorListener,
                                 String moduleId, SourceCodeRef sourceCodeRef) throws AnalyzerError {

        ModuleScope module = modules.get(moduleId);
        if (module != null) {
            return module;
        }

        var classpathRef = getModuleFromClasspath(moduleId);
        if (classpathRef != null) {
            module = loadModule(parserErrorListener, classpathRef, moduleId, sourceCodeRef);
        } else {

            CgrScriptFile script = fileSystem.loadScript(srcDirs, moduleId);
            if (script == null) {
                throw new ModuleNotFoundError(sourceCodeRef, moduleId);
            }
            module = loadModule(parserErrorListener, script, moduleId, sourceCodeRef);
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

    public CgrDirectory getProjectDir() {
        return projectDir;
    }

    public Project getProject() {
        return project;
    }

    private void createDefaultProject(CgrFile scriptFile) {
        this.projectDir = fileSystem.getParent(scriptFile);
        this.project = new Project();
        srcDirs.add(this.projectDir);
    }

    private void loadProjectDefinition(CgrFile file) throws ProjectError {
        this.project = projectReader.load(file);
        if (project.getSourcePaths() == null || project.getSourcePaths().isEmpty()) {
            srcDirs.add(fileSystem.getParent(file));
        } else {
            for (ProjectSourcePath sourcePath : project.getSourcePaths()) {
                srcDirs.add((CgrDirectory) fileSystem.loadEntry(fileSystem.getParent(file), sourcePath.getPath()));
            }
        }

        dependencyResolver = new DependencyResolver(project, projectReader);
        this.projectDir = fileSystem.getParent(file);
    }

    private ClasspathScriptRef getModuleFromClasspath(String moduleId) {
        String path = "/" + moduleId.replaceAll("\\.", "/") + CgrFileSystem.SCRIPT_FILE_EXT;
        if (ModuleLoader.class.getResource(path) != null) {
            return new ClasspathScriptRef(path);
        }
        return null;
    }

    private ModuleScope loadModule(ParserErrorListener parserErrorListener,
                                   ScriptRef script, String moduleId,
                                   SourceCodeRef sourceCodeRef) throws AnalyzerError {

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

            var visitor = new ModuleParserVisitor(this, parserErrorListener, script, nativeFunctions);
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
