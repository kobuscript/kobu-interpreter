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
import dev.cgrscript.database.Database;
import dev.cgrscript.interpreter.ast.*;
import dev.cgrscript.interpreter.ast.eval.EvalModeEnum;
import dev.cgrscript.interpreter.ast.eval.SymbolDocumentation;
import dev.cgrscript.interpreter.ast.eval.function.NativeFunction;
import dev.cgrscript.interpreter.ast.eval.function.NativeFunctionId;
import dev.cgrscript.interpreter.ast.symbol.*;
import dev.cgrscript.interpreter.error.AnalyzerError;
import dev.cgrscript.interpreter.error.analyzer.InternalParserError;
import dev.cgrscript.interpreter.error.analyzer.ModuleNotFoundError;
import dev.cgrscript.interpreter.file_system.*;
import dev.cgrscript.interpreter.input.InputReader;
import dev.cgrscript.interpreter.writer.OutputWriter;
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

    private final EvalModeEnum evalMode;

    private final Map<String, ModuleScope> modules = new HashMap<>();

    private final Map<String, ParseTree> modulesParseTree = new HashMap<>();

    private final Map<NativeFunctionId, NativeFunction> nativeFunctions = new HashMap<>();

    private final ModuleIndex moduleIndex = new ModuleIndex();


    private boolean indexBuilt = false;

    public ModuleLoader(CgrFileSystem fileSystem, Project project, EvalModeEnum evalMode) {
        this.fileSystem = fileSystem;
        this.project = project;
        this.evalMode = evalMode;
    }

    public boolean indexBuilt() {
        return indexBuilt;
    }

    public void buildIndex(AnalyzerContext context, Database database, InputReader inputReader, OutputWriter outputWriter) {
        for (CgrDirectory srcDir : project.getSrcDirs()) {
            fileSystem.walkFileTree(srcDir, entry -> {
                if (entry instanceof ScriptRef) {
                    try {
                        ScriptRef script = (ScriptRef) entry;
                        String moduleId = script.extractModuleId();
                        if (!modules.containsKey(moduleId)) {
                            var module = load(context, script);
                            module.analyze(context, database, inputReader, outputWriter);
                        }
                    } catch (AnalyzerError e) {
                        context.getErrorScope().addError(e);
                    }
                }
            });
        }
        for (String builtinModuleId : BUILTIN_MODULES) {
            if (!modules.containsKey(builtinModuleId)) {
                try {
                    ClasspathScriptRef builtinModule = getModuleFromClasspath(builtinModuleId);
                    if (builtinModule == null) {
                        context.getErrorScope().addError(new InternalParserError("Builtin-module not found: " + builtinModuleId));
                    } else {
                        ModuleScope moduleScope = load(context, builtinModule);
                        modules.put(builtinModuleId, moduleScope);
                    }
                } catch (AnalyzerError e) {
                    context.getErrorScope().addError(e);
                }
            }
        }

        for (String moduleId : modules.keySet()) {
            moduleIndex.addModule(moduleId);
        }

        indexBuilt = true;
    }

    public void addNativeFunction(NativeFunctionId nativeFunctionId, NativeFunction nativeFunction) {
        nativeFunctions.put(nativeFunctionId, nativeFunction);
    }

    public ModuleScope load(AnalyzerContext context, CgrFile file) throws AnalyzerError {
        return load(context, (ScriptRef) fileSystem.loadScript(project.getSrcDirs(), file));
    }

    public ModuleScope load(AnalyzerContext context, ScriptRef script) throws AnalyzerError {
        String moduleId = script.extractModuleId();
        context.addModule(null, moduleId);
        try {
            ModuleScope module = loadModule(context, script, moduleId, null);
            if (evalMode == EvalModeEnum.ANALYZER_SERVICE) {
                moduleIndex.addModule(module.getModuleId());
            }

            return module;
        } finally {
            context.removeModule(moduleId);
        }
    }

    public CgrFile findModuleFile(String moduleId) {
        return fileSystem.loadScript(project.getSrcDirs(), moduleId);
    }

    public CgrScriptFile loadScript(CgrFile file, Database database, InputReader inputReader, OutputWriter outputWriter) {
        if (evalMode == EvalModeEnum.ANALYZER_SERVICE && !indexBuilt()) {
            buildIndex(new AnalyzerContext(), database, inputReader, outputWriter);
        }
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
                    symbol = ((ModuleRefSymbol)otherModule).getModuleScopeRef().resolve(canonicalName);
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

    public SymbolDocumentation getSymbolDocumentation(CgrFile refFile, int offset) {
        var scriptFile = fileSystem.loadScript(project.getSrcDirs(), refFile);
        if (scriptFile == null) {
            return null;
        }

        var refModule = modules.get(scriptFile.extractModuleId());
        if (refModule != null) {
            return refModule.getDocumentation(offset);
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

    public ModuleScope loadScope(AnalyzerContext context,
                                 String moduleId,
                                 SourceCodeRef sourceCodeRef) throws AnalyzerError {

        context.addModule(sourceCodeRef, moduleId);
        try {
            ModuleScope module = modules.get(moduleId);
            if (module != null) {
                return module;
            }

            var classpathRef = getModuleFromClasspath(moduleId);
            if (classpathRef != null) {
                module = loadModule(context, classpathRef, moduleId, sourceCodeRef);
            } else {
                CgrScriptFile script = fileSystem.loadScript(project.getSrcDirs(), moduleId);
                if (script == null) {
                    throw new ModuleNotFoundError(sourceCodeRef, moduleId);
                }
                module = loadModule(context, script, moduleId, sourceCodeRef);
            }
            if (evalMode == EvalModeEnum.ANALYZER_SERVICE) {
                moduleIndex.addModule(module.getModuleId());
            }
            return module;
        } finally {
            context.removeModule(moduleId);
        }
    }

    private void visit(String moduleId, CgrScriptParserVisitor<?> visitor) throws AnalyzerError {
        var parserTree = modulesParseTree.get(moduleId);
        if (parserTree == null) {
            throw new InternalParserError("ParserTree not found for " + moduleId);
        }
        visitor.visit(parserTree);
    }

    public Project getProject() {
        return project;
    }

    public ModuleIndex getModuleIndex() {
        return moduleIndex;
    }

    public EvalModeEnum getEvalMode() {
        return evalMode;
    }

    private ClasspathScriptRef getModuleFromClasspath(String moduleId) {
        String path = "/" + moduleId.replaceAll("\\.", "/") + CgrFileSystem.SCRIPT_FILE_EXT;
        if (ModuleLoader.class.getResource(path) != null) {
            return new ClasspathScriptRef(path);
        }
        return null;
    }

    private ModuleScope loadModule(AnalyzerContext context,
                                   ScriptRef script, String moduleId,
                                   SourceCodeRef sourceCodeRef) throws AnalyzerError {

        try (InputStream fileStream = script.newInputStream()) {

            var input = CharStreams.fromStream(fileStream);
            var lexer = new CgrScriptLexer(input);
            var tokens = new CommonTokenStream(lexer);
            var parser = new CgrScriptParser(tokens);
            parser.removeErrorListeners();
            context.getParserErrorListener().setCurrentScript(script);
            parser.addErrorListener(context.getParserErrorListener());
            var tree = parser.prog();

            modulesParseTree.put(moduleId, tree);

            ModuleScope moduleScope = new ModuleScope(moduleId,
                    script,
                    getProject().getProjectDirectory().getAbsolutePath(),
                    getProject().getProperties(),
                    nativeFunctions,
                    getModuleIndex(),
                    getEvalMode());

            modules.put(moduleId, moduleScope);

            var visitor = new ModuleParserVisitor(this, moduleScope, context, script, tokens);
            visitor.visit(tree);

            EvalTreeParserVisitor evalTreeParserVisitor = new EvalTreeParserVisitor(this,
                    moduleScope, context);
            visit(moduleScope.getModuleId(), evalTreeParserVisitor);

            TypeHierarchyParserVisitor typeHierarchyParserVisitor = new TypeHierarchyParserVisitor(this,
                    moduleScope, context);
            visit(moduleScope.getModuleId(), typeHierarchyParserVisitor);

            return moduleScope;

        } catch (IOException ex) {
            throw new ModuleNotFoundError(sourceCodeRef, moduleId);
        }

    }

}
