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

package dev.kobu.interpreter.ast.eval;

import dev.kobu.interpreter.ast.AnalyzerContext;
import dev.kobu.interpreter.ast.eval.context.ContextSnapshot;
import dev.kobu.interpreter.ast.symbol.function.FunctionSymbol;
import dev.kobu.interpreter.ast.symbol.ModuleScope;
import dev.kobu.interpreter.ast.symbol.Scope;
import dev.kobu.interpreter.ast.symbol.Symbol;
import dev.kobu.interpreter.ast.utils.SymbolDescriptorUtils;
import dev.kobu.interpreter.error.analyzer.SymbolConflictError;

import java.util.*;

public class LocalScope implements Scope {

    private final Map<String, Symbol> symbols = new HashMap<>();

    private final Map<String, ValueExpr> memory = new HashMap<>();

    private final ModuleScope moduleScope;

    private final Scope enclosingScope;

    public LocalScope(ModuleScope moduleScope, Scope enclosingScope) {
        this.moduleScope = moduleScope;
        this.enclosingScope = enclosingScope != null ? enclosingScope : moduleScope;
    }

    public void addAll(LocalScope scope) {
        if (scope.getEnclosingScope() instanceof LocalScope) {
            addAll((LocalScope) scope.getEnclosingScope());
        }
        symbols.putAll(scope.symbols);
        memory.putAll(scope.memory);
    }

    @Override
    public Scope getEnclosingScope() {
        return enclosingScope;
    }

    @Override
    public void define(AnalyzerContext analyzerContext, Symbol symbol) {
        symbol.setScope(this);
        var currentSymbol = this.symbols.get(symbol.getName());
        if (currentSymbol != null) {
            analyzerContext.getErrorScope().addError(new SymbolConflictError(currentSymbol, symbol));
        }
        this.symbols.put(symbol.getName(), symbol);

    }

    @Override
    public Symbol resolve(String name) {
        var symbol = symbols.get(name);
        if (symbol == null) {
            symbol = enclosingScope.resolve(name);
        }
        return symbol;
    }

    public Symbol resolveLocal(String name) {
        return symbols.get(name);
    }

    @Override
    public Collection<Symbol> getSymbols() {
        return symbols.values();
    }

    @Override
    public void getSnapshot(ContextSnapshot snapshot) {
        if (enclosingScope != null) {
            enclosingScope.getSnapshot(snapshot);
        }
        for (Map.Entry<String, ValueExpr> entry : memory.entrySet()) {
            snapshot.add(entry.getKey(), entry.getValue());
        }
    }

    public void setValue(String symbolName, ValueExpr value) {
        if (symbols.containsKey(symbolName)) {
            memory.put(symbolName, value);
        } else if (enclosingScope instanceof LocalScope) {
            ((LocalScope) enclosingScope).setValue(symbolName, value);
        }
    }

    public List<String> getKeys() {
        return new ArrayList<>(symbols.keySet());
    }

    public ValueExpr getValue(String symbolName) {
        var value = memory.get(symbolName);
        if (value == null && enclosingScope instanceof LocalScope) {
            return ((LocalScope) enclosingScope).getValue(symbolName);
        }
        return value;
    }

    public Collection<SymbolDescriptor> getSymbolDescriptors(SymbolTypeEnum... types) {
        Set<SymbolTypeEnum> typeSet = new HashSet<>(Arrays.asList(types));
        return getSymbolDescriptors(typeSet);
    }

    public Collection<SymbolDescriptor> getSymbolDescriptors(Set<SymbolTypeEnum> typeSet) {
        Collection<SymbolDescriptor> result = new LinkedHashSet<>();
        Scope scope = this;

        while (scope != null) {
            for (Symbol symbol : scope.getSymbols()) {
                SymbolDescriptor symbolDescriptor = new SymbolDescriptor(symbol);
                if (typeSet.contains(symbolDescriptor.getType())) {
                    result.add(symbolDescriptor);
                }
            }
            scope = scope.getEnclosingScope();
        }

        for (Symbol symbol : moduleScope.getDependenciesSymbols()) {
            SymbolDescriptor symbolDescriptor = new SymbolDescriptor(symbol);
            if (typeSet.contains(symbolDescriptor.getType())) {
                result.add(symbolDescriptor);
            }
        }

        if (typeSet.contains(SymbolTypeEnum.KEYWORD)) {
            result.addAll(SymbolDescriptorUtils.getStatKeywords());
        }

        return result;
    }

}
