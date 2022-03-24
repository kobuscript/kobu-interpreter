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

import dev.kobu.interpreter.ast.symbol.*;
import dev.kobu.interpreter.ast.symbol.function.NamedFunction;

import java.util.Objects;

public class SymbolDescriptor {

    private final SymbolTypeEnum type;

    private String name;

    private String description = "";

    private String metadata = "";

    private String label;

    private String externalModule;

    private int newImportOffset;

    private boolean hasImports;

    public SymbolDescriptor(Symbol symbol) {
        this(symbol, null, 0, false);
    }

    public SymbolDescriptor(Symbol symbol, String externalModule, int newImportOffset, boolean hasImports) {
        this.externalModule = externalModule;
        this.newImportOffset = newImportOffset;
        this.hasImports = hasImports;
        this.name = symbol.getNameInModule();
        this.label = name;
        if (symbol instanceof NamedFunction) {
            this.type = SymbolTypeEnum.FUNCTION;
            this.description = ((NamedFunction)symbol).getDescription();
            if (symbol.getSourceCodeRef() != null) {
                this.metadata = symbol.getSourceCodeRef().getModuleId();
            }
        } else if (symbol instanceof VariableSymbol) {
            this.type = SymbolTypeEnum.VARIABLE;
            this.metadata = ((VariableSymbol) symbol).getType().getName();
        } else if (symbol instanceof ModuleRefSymbol) {
            this.name = ((ModuleRefSymbol)symbol).getAlias();
            this.type = SymbolTypeEnum.MODULE_REF;
            this.metadata = ((ModuleRefSymbol)symbol).getModuleScopeRef().getModuleId();
        } else if (symbol instanceof Type) {
            this.type = SymbolTypeEnum.TYPE;
            if (symbol.getSourceCodeRef() != null) {
                this.metadata = symbol.getSourceCodeRef().getModuleId();
            }
        } else if (symbol instanceof RuleSymbol) {
            RuleSymbol rule = (RuleSymbol) symbol;
            if (rule.getRuleType() == RuleTypeEnum.RULE) {
                this.type = SymbolTypeEnum.RULE;
            } else if (rule.getRuleType() == RuleTypeEnum.TEMPLATE) {
                this.type = SymbolTypeEnum.TEMPLATE;
            } else {
                this.type = SymbolTypeEnum.FILE;
            }
            if (symbol.getSourceCodeRef() != null) {
                this.metadata = symbol.getSourceCodeRef().getModuleId();
            }
        } else {
            throw new IllegalArgumentException("invalid symbol type: " + symbol.getClass().getName());
        }
    }

    public SymbolDescriptor(FieldDescriptor fieldDescriptor) {
        this.type = SymbolTypeEnum.ATTRIBUTE;
        this.name = fieldDescriptor.getName();
        this.label = name;
        this.metadata = fieldDescriptor.getTypeName();
    }

    public SymbolDescriptor(NamedFunction namedFunction) {
        this.type = SymbolTypeEnum.FUNCTION;
        this.name = namedFunction.getName();
        this.label = name;
        this.description = namedFunction.getDescription();
    }

    public SymbolDescriptor(SymbolTypeEnum type, String name, String description, String metadata) {
        this(type, name, name, description, metadata);
    }

    public SymbolDescriptor(SymbolTypeEnum type, String name, String label, String description, String metadata) {
        this.type = type;
        this.name = name;
        this.label = label;
        this.description = description;
        this.metadata = metadata;
    }

    public SymbolDescriptor(String prefix, RecordTypeSymbol symbol) {
        this.type = SymbolTypeEnum.TYPE;
        this.name = prefix + "." + symbol.getNameInModule();
        this.label = symbol.getNameInModule();
        if (symbol.getSourceCodeRef() != null) {
            this.metadata = symbol.getSourceCodeRef().getModuleId();
        }
    }

    public SymbolTypeEnum getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public String getMetadata() {
        return metadata;
    }

    public String getExternalModule() {
        return externalModule;
    }

    public int getNewImportOffset() {
        return newImportOffset;
    }

    public boolean hasImports() {
        return hasImports;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SymbolDescriptor that = (SymbolDescriptor) o;
        return type == that.type && Objects.equals(name, that.name) && Objects.equals(description, that.description)
                && Objects.equals(metadata, that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, name, description, metadata);
    }
}
