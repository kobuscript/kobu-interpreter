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

package dev.cgrscript.interpreter.ast.eval;

import dev.cgrscript.interpreter.ast.symbol.ModuleRefSymbol;
import dev.cgrscript.interpreter.error.analyzer.SymbolConflictError;
import dev.cgrscript.interpreter.ast.symbol.ModuleScope;
import dev.cgrscript.interpreter.ast.symbol.Scope;
import dev.cgrscript.interpreter.ast.symbol.Symbol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        symbols.putAll(scope.symbols);
        memory.putAll(scope.memory);
    }

    @Override
    public Scope getEnclosingScope() {
        return enclosingScope;
    }

    @Override
    public void define(Symbol symbol) {
        symbol.setScope(this);
        var currentSymbol = this.symbols.get(symbol.getName());
        if (currentSymbol != null) {
            moduleScope.addError(new SymbolConflictError(currentSymbol, symbol));
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

    public void setValue(String symbolName, ValueExpr value) {
        memory.put(symbolName, value);
    }

    public List<String> getKeys() {
        return new ArrayList<>(symbols.keySet());
    }

    public ValueExpr getValue(String symbolName) {
        var value = memory.get(symbolName);
        if (value == null) {
            if (enclosingScope instanceof LocalScope) {
                return ((LocalScope) enclosingScope).getValue(symbolName);
            } else if (enclosingScope instanceof ModuleScope){
                var symbol = enclosingScope.resolve(symbolName);
                if (symbol instanceof ModuleRefSymbol) {

                }
            }
        }
        return value;
    }
}
