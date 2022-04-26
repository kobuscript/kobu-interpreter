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

import dev.kobu.interpreter.ast.eval.FieldDescriptor;
import dev.kobu.interpreter.ast.symbol.function.FunctionSymbol;
import dev.kobu.interpreter.ast.symbol.function.NamedFunction;
import dev.kobu.interpreter.ast.symbol.generics.TypeAlias;

import java.util.*;

public class ModuleRefSymbol extends Symbol implements Type {

    private final String alias;

    private final ModuleScope moduleScopeRef;

    public ModuleRefSymbol(ModuleScope moduleScope, SourceCodeRef sourceCodeRef, String alias, ModuleScope moduleScopeRef) {
        super(moduleScope, sourceCodeRef, "$" + moduleScopeRef.getModuleId(), false);
        this.alias = alias;
        this.moduleScopeRef = moduleScopeRef;
    }

    public String getAlias() {
        return alias;
    }

    public ModuleScope getModuleScopeRef() {
        return moduleScopeRef;
    }

    @Override
    public String getNameInModule() {
        return alias;
    }

    @Override
    public List<FieldDescriptor> getFields() {
        List<FieldDescriptor> fields = new ArrayList<>();
        for (Symbol symbol : moduleScopeRef.getSymbols()) {
            if (symbol instanceof RuleSymbol) {
                fields.add(new FieldDescriptor(symbol.getName(), ((RuleSymbol)symbol).getRuleType().name()));
            }
        }
        return fields;
    }

    @Override
    public List<NamedFunction> getMethods() {
        List<NamedFunction> functions = new ArrayList<>();
        for (Symbol symbol : moduleScopeRef.getSymbols()) {
            if (symbol instanceof FunctionSymbol) {
                functions.add((NamedFunction) symbol);
            }
        }
        return functions;
    }

    @Override
    public Type resolveField(String name) {
        var symbol = moduleScopeRef.resolveLocal(name);
        if (symbol instanceof RuleSymbol) {
            return BuiltinScope.RULE_REF_TYPE;
        }
        return null;
    }

    @Override
    public SourceCodeRef getFieldRef(String name) {
        var symbol = moduleScopeRef.resolveLocal(name);
        if (symbol != null) {
            return symbol.getSourceCodeRef();
        }
        return null;
    }

    @Override
    public NamedFunction resolveMethod(String name) {
        var symbol = moduleScopeRef.resolveLocal(name);
        if (symbol instanceof NamedFunction) {
            return (NamedFunction) symbol;
        }
        return null;
    }

    @Override
    public boolean isAssignableFrom(Type type) {
        return false;
    }

    @Override
    public Type getCommonSuperType(Type type) {
        return null;
    }

    @Override
    public Collection<TypeAlias> aliases() {
        return List.of();
    }

    @Override
    public Type constructFor(Map<String, Type> typeArgs) {
        return this;
    }

    @Override
    public void resolveAliases(Map<String, Type> typeArgs, Type targetType) {

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ModuleRefSymbol that = (ModuleRefSymbol) o;
        return Objects.equals(alias, that.alias) && Objects.equals(moduleScopeRef, that.moduleScopeRef);
    }

    @Override
    public int hashCode() {
        return Objects.hash(alias, moduleScopeRef);
    }

}
