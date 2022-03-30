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

package dev.kobu.interpreter.ast.symbol;

import dev.kobu.config.ProjectProperty;
import dev.kobu.interpreter.ast.AnalyzerContext;
import dev.kobu.interpreter.ast.eval.*;
import dev.kobu.interpreter.ast.eval.context.ContextSnapshot;
import dev.kobu.interpreter.ast.eval.context.EvalContextProvider;
import dev.kobu.interpreter.ast.eval.context.EvalModeEnum;
import dev.kobu.interpreter.ast.eval.expr.value.ArrayValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.StringValueExpr;
import dev.kobu.interpreter.ast.eval.function.NativeFunction;
import dev.kobu.interpreter.ast.eval.function.NativeFunctionId;
import dev.kobu.interpreter.ast.symbol.array.ArrayType;
import dev.kobu.interpreter.ast.symbol.array.ArrayTypeFactory;
import dev.kobu.interpreter.ast.symbol.function.FunctionParameter;
import dev.kobu.interpreter.ast.symbol.function.FunctionSymbol;
import dev.kobu.interpreter.ast.utils.SymbolDescriptorUtils;
import dev.kobu.interpreter.error.AnalyzerError;
import dev.kobu.interpreter.error.analyzer.InvalidMainFunctionError;
import dev.kobu.interpreter.error.analyzer.InvalidTypeError;
import dev.kobu.interpreter.error.analyzer.MainFunctionNotFoundError;
import dev.kobu.interpreter.error.analyzer.SymbolConflictError;
import dev.kobu.interpreter.file_system.ScriptRef;
import dev.kobu.interpreter.module.ModuleIndex;

import java.util.*;
import java.util.stream.Collectors;

public class ModuleScope implements Scope {

    private static final BuiltinScope builtinScope = new BuiltinScope();

    private final List<ProjectProperty> properties;

    private final Map<NativeFunctionId, NativeFunction> nativeFunctions;

    private final String projectDir;

    private final String moduleId;

    private final ScriptRef script;

    private final Map<String, ModuleScope> loadedModules = new HashMap<>();

    private final Map<String, Symbol> symbols = new HashMap<>();

    private final Map<String, Symbol> dependenciesSymbols = new HashMap<>();

    private final Map<Integer, DocumentationSource> documentationSourceByOffset = new HashMap<>();

    private final Map<Integer, HasElementRef> refsByOffset = new HashMap<>();

    private final Map<Integer, AutoCompletionSource> autoCompletionSourceByOffset = new HashMap<>();

    private final ModuleIndex moduleIndex;

    private final EvalModeEnum evalMode;

    private boolean hasImports = false;

    private int newImportOffset;

    private int maxRefOffset = 0;

    private int maxAutoCompletionSourceOffset = 0;

    public ModuleScope(String moduleId, ScriptRef script, String projectDir,
                       List<ProjectProperty> properties, Map<NativeFunctionId, NativeFunction> nativeFunctions,
                       ModuleIndex moduleIndex, EvalModeEnum evalMode) {
        this.moduleId = moduleId;
        this.script = script;
        this.properties = properties;
        this.projectDir = projectDir;
        this.nativeFunctions = nativeFunctions;
        this.moduleIndex = moduleIndex;
        this.evalMode = evalMode;
    }

    @Override
    public Scope getEnclosingScope() {
        return builtinScope;
    }

    @Override
    public void define(AnalyzerContext analyzerContext, Symbol symbol) {
        symbol.setScope(this);
        Symbol currentDef = resolve(symbol.getNameInModule());

        if (currentDef != null) {
            analyzerContext.getErrorScope().addError(new SymbolConflictError(currentDef, symbol));
        }

        symbols.put(symbol.getNameInModule(), symbol);
    }

    public void registerDocumentationSource(int offset, DocumentationSource docSource) {
        documentationSourceByOffset.put(offset, docSource);
    }

    @Override
    public Symbol resolve(String name) {

        Symbol symbol = resolveLocal(name);
        if (symbol != null) {
            return symbol;
        }

        symbol = dependenciesSymbols.get(name);
        if (symbol != null) {
            return symbol;
        }
        return builtinScope.resolve(name);

    }

    @Override
    public Collection<Symbol> getSymbols() {
        return getSymbols(true);
    }

    @Override
    public void getSnapshot(ContextSnapshot snapshot) {

    }

    public Collection<Symbol> getSymbols(boolean includeBuiltin) {
        var result = new ArrayList<>(symbols.values());
        if (includeBuiltin) {
            result.addAll(builtinScope.getSymbols());
        }
        return result;
    }

    public Collection<Symbol> getDependenciesSymbols() {
        return new ArrayList<>(dependenciesSymbols.values());
    }

    public void registerRef(int offset, HasElementRef ref) {
        maxRefOffset = Math.max(maxRefOffset, offset);
        refsByOffset.put(offset, ref);
    }

    public void registerAutoCompletionSource(int offset, AutoCompletionSource autoCompletionSource) {
        maxAutoCompletionSourceOffset = Math.max(maxAutoCompletionSourceOffset, offset);
        autoCompletionSourceByOffset.put(offset, autoCompletionSource);
    }

    public HasElementRef getRef(int offset) {
        var elem = refsByOffset.get(offset);
        while (elem == null && offset <= maxRefOffset) {
            elem = refsByOffset.get(++offset);
        }
        return elem;
    }

    public List<SymbolDescriptor> getSuggestions(int offset, List<ModuleScope> externalModules) {
        var elem = autoCompletionSourceByOffset.get(offset);
        if (elem != null) {
            return elem.requestSuggestions(externalModules);
        }
        while (elem == null && offset <= maxAutoCompletionSourceOffset) {
            elem = autoCompletionSourceByOffset.get(++offset);
        }
        if (elem != null && !elem.hasOwnCompletionScope()) {
            return elem.requestSuggestions(externalModules);
        }
        return SymbolDescriptorUtils.getGlobalKeywords();
    }

    public SymbolDocumentation getDocumentation(int offset) {
        var docSource = documentationSourceByOffset.get(offset);
        if (docSource != null) {
            return docSource.getDocumentation();
        }
        return null;
    }

    public Symbol resolveLocal(String name) {
        return symbols.get(name);
    }

    public NativeFunction getNativeFunction(NativeFunctionId nativeFunctionId) {
        return nativeFunctions.get(nativeFunctionId);
    }

    public void analyze(AnalyzerContext context, EvalContextProvider evalContextProvider) {
        HashSet<String> modulesSet = new HashSet<>();
        modulesSet.add(moduleId);
        analyze(modulesSet, context, evalContextProvider);
    }

    private void analyze(Set<String> modulesSet, AnalyzerContext context, EvalContextProvider evalContextProvider) {
        for (ModuleScope module : loadedModules.values()) {
            if (modulesSet.add(module.getModuleId())) {
                context.pushErrorScope();
                try {
                    module.analyze(modulesSet, context, evalContextProvider);
                } finally {
                    context.popErrorScope();
                }
            }
        }
        for (Symbol sym : symbols.values()) {
            if (sym instanceof HasExpr) {
                ((HasExpr) sym).analyze(context, evalContextProvider);
            }
        }
    }

    public ScriptRef getScript() {
        return script;
    }

    public String getModuleId() {
        return moduleId;
    }

    public void runMainFunction(AnalyzerContext analyzerContext, EvalContextProvider evalContextProvider,
                                List<String> args) throws AnalyzerError {
        var symbol = symbols.get("main");
        if (!(symbol instanceof FunctionSymbol)) {
            throw new MainFunctionNotFoundError(script);
        }
        FunctionSymbol function = (FunctionSymbol) symbol;
        if (function.getParameters().size() > 1) {
            throw new InvalidMainFunctionError(function.getSourceCodeRef(), function);
        }

        if (function.getParameters().size() == 1) {

            FunctionParameter param = function.getParameters().get(0);
            if (!(param.getType() instanceof ArrayType) &&
                    !(((ArrayType)param.getType()).getElementType().equals(BuiltinScope.STRING_TYPE))) {
                throw new InvalidTypeError(function.getSourceCodeRef(), ArrayTypeFactory.getArrayTypeFor(BuiltinScope.STRING_TYPE), param.getType());
            }

            List<ValueExpr> values = args.stream().map(StringValueExpr::new).collect(Collectors.toList());
            ArrayValueExpr argsArrayExpr = new ArrayValueExpr(ArrayTypeFactory.getArrayTypeFor(BuiltinScope.STRING_TYPE), values);
            List<ValueExpr> argList = new ArrayList<>();
            argList.add(argsArrayExpr);
            function.eval(analyzerContext, evalContextProvider, argList);
        } else {
            function.eval(analyzerContext, evalContextProvider, new ArrayList<>());
        }

    }

    public Map<String, Symbol> getSymbolsMap() {
        return symbols;
    }

    public boolean addModule(AnalyzerContext analyzerContext, ModuleScope moduleScope,
                             String alias, SourceCodeRef sourceCodeRef) {
        if (moduleId.equals(moduleScope.moduleId)) {
            return false;
        }
        if (loadedModules.containsKey(moduleScope.getModuleId())) {
            return false;
        }
        loadedModules.put(moduleScope.getModuleId(), moduleScope);
        merge(analyzerContext, moduleScope, alias, sourceCodeRef);
        return true;
    }

    public List<String> findCyclicPath() {
        Stack<String> moduleStack = new Stack<>();
        moduleStack.push(moduleId);
        if (findCyclicPath(moduleStack, moduleId)) {
            return new ArrayList<>(moduleStack);
        }
        return null;
    }

    private void merge(AnalyzerContext context, ModuleScope dependency, String alias, SourceCodeRef sourceCodeRef) {
        if (alias != null) {
            ModuleRefSymbol moduleRefSymbol = new ModuleRefSymbol(this, sourceCodeRef, alias, dependency);
            Symbol currentDef = symbols.get(alias);
            if (currentDef != null) {
                context.getErrorScope().addError(new SymbolConflictError(currentDef, moduleRefSymbol));
            } else {
                symbols.put(alias, moduleRefSymbol);
            }
        } else {
            dependency.getSymbolsMap().forEach((name, symbol) -> {
                Symbol currentDef = symbols.get(name);
                if (currentDef != null) {
                    context.getErrorScope().addError(new SymbolConflictError(currentDef, symbol));
                } else {
                    dependenciesSymbols.put(name, symbol);
                }
            });
        }
    }

    private boolean findCyclicPath(Stack<String> moduleStack, String moduleId) {
        for (ModuleScope module : loadedModules.values()) {
            if (!moduleStack.contains(module.getModuleId())) {
                moduleStack.push(module.getModuleId());
                if (module.findCyclicPath(moduleStack, moduleId)) {
                    return true;
                }
                moduleStack.pop();
            } else {
                if (module.getModuleId().equals(moduleId)) {
                    moduleStack.push(module.getModuleId());
                    return true;
                }
            }
        }

        return false;
    }

    public Set<String> getDependenciesIds() {
        return new HashSet<>(loadedModules.keySet());
    }

    public List<ProjectProperty> getProperties() {
        return properties;
    }

    public String getProjectDir() {
        return projectDir;
    }

    public int getNewImportOffset() {
        return newImportOffset;
    }

    public void setNewImportOffset(int newImportOffset) {
        this.newImportOffset = newImportOffset;
    }

    public boolean hasImports() {
        return hasImports;
    }

    public void setHasImports(boolean hasImports) {
        this.hasImports = true;
    }

    public ModuleIndex getModuleIndex() {
        return moduleIndex;
    }

    public EvalModeEnum getEvalMode() {
        return evalMode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ModuleScope that = (ModuleScope) o;
        return Objects.equals(moduleId, that.moduleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(moduleId);
    }
}
