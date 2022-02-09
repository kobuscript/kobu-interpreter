package dev.cgrscript.interpreter.module;

public class CgrTypeDescriptor {

    public static CgrTypeDescriptor builtinType(String name) {
        return new CgrTypeDescriptor(true, null, name);
    }

    public static CgrTypeDescriptor recordType(String moduleId, String name) {
        return new CgrTypeDescriptor(false, moduleId, name);
    }

    private final boolean builtin;

    private final String moduleId;

    private final String name;

    private CgrTypeDescriptor(boolean builtin, String moduleId, String name) {
        this.builtin = builtin;
        this.moduleId = moduleId;
        this.name = name;
    }

    public boolean isBuiltin() {
        return builtin;
    }

    public String getModuleId() {
        return moduleId;
    }

    public String getName() {
        return name;
    }

}
