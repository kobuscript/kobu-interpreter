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

import dev.cgrscript.interpreter.ast.symbol.*;

import java.util.Objects;

public class SymbolDescriptor {

    private final SymbolTypeEnum type;

    private String name;

    private String description = "";

    private String metadata = "";

    private String externalModule;

    public SymbolDescriptor(Symbol symbol) {
        this(symbol, null);
    }

    public SymbolDescriptor(Symbol symbol, String externalModule) {
        this.externalModule = externalModule;
        this.name = symbol.getName();
        if (symbol instanceof FunctionType) {
            this.type = SymbolTypeEnum.FUNCTION;
            this.description = ((FunctionType)symbol).getDescription();
            if (symbol.getSourceCodeRef() != null) {
                this.metadata = symbol.getSourceCodeRef().getModuleId();
            }
        } else if (symbol instanceof VariableSymbol) {
            this.type = SymbolTypeEnum.VARIABLE;
            this.metadata = ((VariableSymbol) symbol).getType().getName();
        } else if (symbol instanceof ModuleRefSymbol) {
            this.name = ((ModuleRefSymbol)symbol).getAlias();
            this.type = SymbolTypeEnum.MODULE;
            this.metadata = ((ModuleRefSymbol)symbol).getModuleScope().getModuleId();
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
        this.type = SymbolTypeEnum.FIELD;
        this.name = fieldDescriptor.getName();
        this.metadata = fieldDescriptor.getTypeName();
    }

    public SymbolDescriptor(FunctionType functionType) {
        this.type = SymbolTypeEnum.FUNCTION;
        this.name = functionType.getName();
        this.description = functionType.getDescription();
    }

    public SymbolDescriptor(SymbolTypeEnum type, String name, String description, String metadata) {
        this.type = type;
        this.name = name;
        this.description = description;
        this.metadata = metadata;
    }

    public SymbolTypeEnum getType() {
        return type;
    }

    public String getName() {
        return name;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SymbolDescriptor that = (SymbolDescriptor) o;
        return type == that.type && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, name);
    }
}
