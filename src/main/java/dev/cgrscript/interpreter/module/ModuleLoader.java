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

import dev.cgrscript.antlr.cgrscript.CgrScriptLexer;
import dev.cgrscript.antlr.cgrscript.CgrScriptParser;
import dev.cgrscript.config.DependencyResolver;
import dev.cgrscript.config.Project;
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
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class ModuleLoader {

    //TODO: implement a better way to register built-in modules
    private static final String[] BUILTIN_MODULES = new String[]{
        "dev.cgrscript.core.types.Csv",
        "dev.cgrscript.core.types.Json"
    };

    private final CgrFileSystem fileSystem;

    private DependencyResolver dependencyResolver;
    private final Project project;

    private final Map<String, ModuleScope> modules = new HashMap<>();

    private final Map<String, ParseTree> modulesParseTree = new HashMap<>();

    private final LinkedHashSet<String> moduleLoaderStack = new LinkedHashSet<>();

    private final Map<NativeFunctionId, NativeFunction> nativeFunctions = new HashMap<>();

    private final ModuleIndex moduleIndex = new ModuleIndex();

    private boolean indexBuilt = false;

    public ModuleLoader(CgrFileSystem fileSystem, Project project) {
        this.fileSystem = fileSystem;
        this.project = project;
    }

    public boolean indexBuilt() {
        return indexBuilt;
    }

    public List<AnalyzerError> buildIndex(ParserErrorListener parserErrorListener) {
        var errors = new ArrayList<AnalyzerError>();
        for (CgrDirectory srcDir : project.getSrcDirs()) {
            fileSystem.walkFileTree(srcDir, entry -> {
                if (entry instanceof CgrScriptFile) {
                    try {
                        CgrScriptFile script = (CgrScriptFile) entry;
                        String moduleId = script.extractModuleId();
                        if (!modules.containsKey(moduleId)) {
                            load(parserErrorListener, script);
                        }
                    } catch (AnalyzerError e) {
                        errors.add(e);
                    }
                }
            });
        }
        for (String builtinModule : BUILTIN_MODULES) {
            if (!modules.containsKey(builtinModule)) {
                try {
                    ModuleScope moduleScope = loadModule(parserErrorListener,
                            getModuleFromClasspath(builtinModule), null, null);
                    modules.put(builtinModule, moduleScope);
                } catch (AnalyzerError e) {
                    errors.add(e);
                }
            }
        }

        for (String moduleId : modules.keySet()) {
            moduleIndex.addModule(moduleId);
        }

        indexBuilt = true;
        return errors;
    }

    public void addNativeFunction(NativeFunctionId nativeFunctionId, NativeFunction nativeFunction) {
        nativeFunctions.put(nativeFunctionId, nativeFunction);
    }

    public ModuleScope load(ParserErrorListener parserErrorListener, CgrFile file) throws AnalyzerError {
        return load(parserErrorListener, fileSystem.loadScript(project.getSrcDirs(), file));
    }

    public ModuleScope load(ParserErrorListener parserErrorListener, CgrScriptFile script) throws AnalyzerError {
        ModuleScope module = loadModule(parserErrorListener, script, null, null);
        modules.put(module.getModuleId(), module);
        moduleIndex.addModule(module.getModuleId());

        return module;
    }

    public CgrFile findModuleFile(String moduleId) {
        return fileSystem.loadScript(project.getSrcDirs(), moduleId);
    }

    public CgrScriptFile loadScript(CgrFile file) {
        return fileSystem.loadScript(project.getSrcDirs(), file);
    }

    public CgrElementDescriptor getTypeDescriptor(CgrFile refFile, String typeName) {
        var scriptFile = fileSystem.loadScript(project.getSrcDirs(), refFile);
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

    public ModuleScope getScope(String moduleId) {
        return modules.get(moduleId);
    }

    public List<ModuleScope> getExternalModules(String moduleId) {
        Set<String> moduleSet = new HashSet<>();
        moduleSet.add(moduleId);
        ModuleScope module = modules.get(moduleId);
        moduleSet.addAll(module.getDependenciesIds());
        List<ModuleScope> externalModules = new ArrayList<>();
        for (Map.Entry<String, ModuleScope> entry : modules.entrySet()) {
            if (!moduleSet.contains(entry.getKey())) {
                externalModules.add(entry.getValue());
            }
        }
        return externalModules;
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

            CgrScriptFile script = fileSystem.loadScript(project.getSrcDirs(), moduleId);
            if (script == null) {
                throw new ModuleNotFoundError(sourceCodeRef, moduleId);
            }
            module = loadModule(parserErrorListener, script, moduleId, sourceCodeRef);
        }
        modules.put(module.getModuleId(), module);
        moduleIndex.addModule(module.getModuleId());
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

    public Project getProject() {
        return project;
    }

    public ModuleIndex getModuleIndex() {
        return moduleIndex;
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
