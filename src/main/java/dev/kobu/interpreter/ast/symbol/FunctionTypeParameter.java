package dev.kobu.interpreter.ast.symbol;

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

}
