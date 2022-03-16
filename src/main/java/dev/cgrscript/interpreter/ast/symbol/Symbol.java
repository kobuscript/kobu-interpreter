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

import dev.cgrscript.interpreter.ast.eval.*;
import dev.cgrscript.interpreter.ast.eval.context.EvalModeEnum;

import java.util.List;
import java.util.Objects;

public abstract class Symbol implements AutoCompletionSource, DocumentationSource {

    private final SourceCodeRef sourceCodeRef;

    private final ModuleScope moduleScope;

    private final String name;

    private Scope scope;

    public Symbol(ModuleScope moduleScope, SourceCodeRef sourceCodeRef, String name) {
        this.moduleScope = moduleScope;
        this.sourceCodeRef = sourceCodeRef;
        this.name = name;
        if (moduleScope != null && moduleScope.getEvalMode() == EvalModeEnum.ANALYZER_SERVICE && sourceCodeRef != null) {
            moduleScope.registerAutoCompletionSource(sourceCodeRef.getStartOffset(), this);
            moduleScope.registerDocumentationSource(sourceCodeRef.getStartOffset(), this);
        }
    }

    public SourceCodeRef getSourceCodeRef() {
        return sourceCodeRef;
    }

    public String getName() {
        return name;
    }

    public String getNameInModule() {
        return name;
    }

    public Scope getScope() {
        return scope;
    }

    public void setScope(Scope scope) {
        this.scope = scope;
    }

    @Override
    public SymbolDocumentation getDocumentation() {
        return null;
    }

    public ModuleScope getModuleScope() {
        return moduleScope;
    }

    @Override
    public List<SymbolDescriptor> requestSuggestions(List<ModuleScope> externalModules) {
        return EMPTY_LIST;
    }

    @Override
    public boolean hasOwnCompletionScope() {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Symbol symbol = (Symbol) o;
        return Objects.equals(moduleScope, symbol.moduleScope) && Objects.equals(name, symbol.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(moduleScope, name);
    }

}
