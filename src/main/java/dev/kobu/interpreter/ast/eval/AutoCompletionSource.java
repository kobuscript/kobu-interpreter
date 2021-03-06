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

import dev.kobu.interpreter.ast.symbol.ModuleScope;
import dev.kobu.interpreter.ast.symbol.RecordTypeSymbol;
import dev.kobu.interpreter.ast.symbol.Symbol;

import java.util.*;

public interface AutoCompletionSource {

    List<SymbolDescriptor> EMPTY_LIST = List.of();

    List<SymbolDescriptor> requestSuggestions(List<ModuleScope> externalModules);

    boolean hasOwnCompletionScope();

    default List<SymbolDescriptor> getExternalSymbols(ModuleScope moduleScope, List<ModuleScope> externalModules, SymbolTypeEnum... types) {
        Set<SymbolTypeEnum> typeSet = new HashSet<>(Arrays.asList(types));
        List<SymbolDescriptor> symbols = new ArrayList<>();
        for (ModuleScope externalModule : externalModules) {
            for (Symbol symbol : externalModule.getSymbols(false)) {
                if (!symbol.isPrivateAccess()) {
                    var descriptor = new SymbolDescriptor(symbol, externalModule.getModuleId(),
                            moduleScope.getNewImportOffset(), moduleScope.hasImports());
                    if (typeSet.contains(descriptor.getType())) {
                        symbols.add(descriptor);
                    }
                }
            }
        }

        return symbols;
    }

    default List<SymbolDescriptor> getGlobalSymbols(ModuleScope moduleScope, SymbolTypeEnum... types) {
        Set<SymbolTypeEnum> typeSet = new HashSet<>(Arrays.asList(types));
        List<SymbolDescriptor> symbols = new ArrayList<>();
        for (Symbol symbol : moduleScope.getSymbols()) {
            var descriptor = new SymbolDescriptor(symbol);
            if (typeSet.contains(descriptor.getType())) {
                symbols.add(descriptor);
            }
        }
        for (Symbol symbol : moduleScope.getDependenciesSymbols()) {
            var descriptor = new SymbolDescriptor(symbol);
            if (typeSet.contains(descriptor.getType())) {
                symbols.add(descriptor);
            }
        }

        return symbols;
    }

    default List<SymbolDescriptor> getTypeSymbols(ModuleScope moduleScope, String prefix) {
        List<SymbolDescriptor> symbols = new ArrayList<>();
        for (Symbol symbol : moduleScope.getSymbols()) {
            if (symbol instanceof RecordTypeSymbol) {
                symbols.add(new SymbolDescriptor(prefix, (RecordTypeSymbol) symbol));
            }
        }
        return symbols;
    }
}
