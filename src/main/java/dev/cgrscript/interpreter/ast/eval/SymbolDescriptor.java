package dev.cgrscript.interpreter.ast.eval;

import dev.cgrscript.interpreter.ast.symbol.*;

import java.util.Objects;

public class SymbolDescriptor {

    private final SymbolTypeEnum type;

    private final String name;

    public SymbolDescriptor(Symbol symbol) {
        this.name = symbol.getName();
        if (symbol instanceof FunctionType) {
            this.type = SymbolTypeEnum.FUNCTION;
        } else if (symbol instanceof VariableSymbol) {
            this.type = SymbolTypeEnum.VARIABLE;
        } else if (symbol instanceof Type) {
            this.type = SymbolTypeEnum.TYPE;
        } else if (symbol instanceof RuleSymbol) {
            RuleSymbol rule = (RuleSymbol) symbol;
            if (rule.getRuleType() == RuleTypeEnum.RULE) {
                this.type = SymbolTypeEnum.RULE;
            } else if (rule.getRuleType() == RuleTypeEnum.TEMPLATE) {
                this.type = SymbolTypeEnum.TEMPLATE;
            } else {
                this.type = SymbolTypeEnum.FILE;
            }
        } else {
            throw new IllegalArgumentException("invalid symbol type: " + symbol.getClass().getName());
        }
    }

    public SymbolTypeEnum getType() {
        return type;
    }

    public String getName() {
        return name;
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
