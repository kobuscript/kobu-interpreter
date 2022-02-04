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

package dev.cgrscript.interpreter.ast.symbol;

import dev.cgrscript.config.ProjectProperty;
import dev.cgrscript.database.Database;
import dev.cgrscript.interpreter.ast.eval.ValueExpr;
import dev.cgrscript.interpreter.ast.eval.expr.value.ArrayValueExpr;
import dev.cgrscript.interpreter.ast.eval.expr.value.StringValueExpr;
import dev.cgrscript.interpreter.ast.eval.function.NativeFunction;
import dev.cgrscript.interpreter.ast.eval.function.NativeFunctionId;
import dev.cgrscript.interpreter.error.AnalyzerError;
import dev.cgrscript.interpreter.error.AnalyzerErrorList;
import dev.cgrscript.interpreter.error.analyzer.InvalidMainFunctionError;
import dev.cgrscript.interpreter.error.analyzer.InvalidTypeError;
import dev.cgrscript.interpreter.error.analyzer.MainFunctionNotFoundError;
import dev.cgrscript.interpreter.error.analyzer.SymbolConflictError;
import dev.cgrscript.interpreter.file_system.CgrScriptFile;
import dev.cgrscript.interpreter.file_system.ScriptRef;
import dev.cgrscript.interpreter.input.InputReader;
import dev.cgrscript.interpreter.module.AnalyzerStepEnum;
import dev.cgrscript.interpreter.writer.OutputWriter;

import java.util.*;
import java.util.stream.Collectors;

public class ModuleScope implements Scope {

    private static final BuiltinScope builtinScope = new BuiltinScope();

    private final List<ProjectProperty> properties;

    private final Map<NativeFunctionId, NativeFunction> nativeFunctions;

    private final String projectDir;

    private final String moduleId;

    private final ScriptRef script;

    private final Set<String> importedModules = new HashSet<>();

    private final Map<String, Symbol> symbols = new HashMap<>();

    private List<AnalyzerError> errors;

    private AnalyzerStepEnum step;

    public ModuleScope(String moduleId, ScriptRef script, String projectDir,
                       List<ProjectProperty> properties, Map<NativeFunctionId, NativeFunction> nativeFunctions) {
        this.moduleId = moduleId;
        this.script = script;
        this.properties = properties;
        this.projectDir = projectDir;
        this.nativeFunctions = nativeFunctions;
    }

    @Override
    public Scope getEnclosingScope() {
        return builtinScope;
    }

    @Override
    public void define(Symbol symbol) {
        symbol.setScope(this);
        Symbol currentDef = resolve(symbol.getName());

        if (currentDef != null) {
            addError(new SymbolConflictError(currentDef, symbol));
        }

        symbols.put(symbol.getName(), symbol);
    }

    @Override
    public Symbol resolve(String name) {
        Symbol symbol = symbols.get(name);
        if (symbol != null) {
            return symbol;
        }
        return builtinScope.resolve(name);
    }

    public NativeFunction getNativeFunction(NativeFunctionId nativeFunctionId) {
        return nativeFunctions.get(nativeFunctionId);
    }

    public void merge(ModuleScope imported) {
        importedModules.addAll(imported.importedModules);
        imported.getSymbols().forEach((name, symbol) -> {
            Symbol currentDef = symbols.get(name);
            if (currentDef != null) {
                addError(new SymbolConflictError(currentDef, symbol));
            } else {
                symbols.put(name, symbol);
            }
        });
        addErrors(imported.getErrors());
    }

    public void analyze(Database database, InputReader inputReader, OutputWriter outputWriter) {
        for (Symbol sym : symbols.values()) {
            if (sym instanceof HasExpr) {
                ((HasExpr) sym).analyze(database, inputReader, outputWriter);
            }
        }
    }

    public ScriptRef getScript() {
        return script;
    }

    public String getModuleId() {
        return moduleId;
    }

    public void runMainFunction(List<String> args, Database database,
                                InputReader inputReader, OutputWriter outputWriter) throws AnalyzerError {
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
                    !(((ArrayType)param.getType()).getElementType().getName().equals(BuiltinScope.STRING_TYPE.getName()))) {
                throw new InvalidTypeError(function.getSourceCodeRef(), new ArrayType(BuiltinScope.STRING_TYPE), param.getType());
            }

            List<ValueExpr> values = args.stream().map(StringValueExpr::new).collect(Collectors.toList());
            ArrayValueExpr argsArrayExpr = new ArrayValueExpr(new ArrayType(BuiltinScope.STRING_TYPE), values);
            List<ValueExpr> argList = new ArrayList<>();
            argList.add(argsArrayExpr);
            function.eval(argList, database, inputReader, outputWriter);
        } else {
            function.eval(new ArrayList<>(), database, inputReader, outputWriter);
        }

    }

    public List<AnalyzerError> getErrors() {
        return errors;
    }

    public void checkErrors() throws AnalyzerError {
        if (errors == null || errors.isEmpty()) {
            return;
        }
        throw new AnalyzerErrorList(errors);
    }

    public Map<String, Symbol> getSymbols() {
        return symbols;
    }

    public boolean addImportedModule(String importedModuleId) {
        return importedModules.add(importedModuleId);
    }

    public AnalyzerStepEnum getStep() {
        return step;
    }

    public void setStep(AnalyzerStepEnum step) {
        this.step = step;
    }

    public void addError(AnalyzerError error) {
        if (errors == null) {
            errors = new ArrayList<>();
        }
        errors.add(error);
    }

    public void addErrors(List<AnalyzerError> errorList) {
        if (errorList == null) {
            return;
        }
        if (errors == null) {
            errors = new ArrayList<>();
        }
        errors.addAll(errorList);
    }

    public List<ProjectProperty> getProperties() {
        return properties;
    }

    public String getProjectDir() {
        return projectDir;
    }

}
