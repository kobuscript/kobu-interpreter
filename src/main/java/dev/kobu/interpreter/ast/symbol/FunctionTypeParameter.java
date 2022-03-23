package dev.kobu.interpreter.ast.symbol;

import java.util.Objects;

public class FunctionTypeParameter {

    private final Type type;

    private final boolean optional;

    public FunctionTypeParameter(Type type, boolean optional) {
        this.type = type;
        this.optional = optional;
    }

    public Type getType() {
        return type;
    }

    public boolean isOptional() {
        return optional;
    }

    public String getDescription() {
        String typeName = type.getName();
        if (optional) {
            typeName += "?";
        }
        return typeName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FunctionTypeParameter that = (FunctionTypeParameter) o;
        return optional == that.optional && Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, optional);
    }
}
