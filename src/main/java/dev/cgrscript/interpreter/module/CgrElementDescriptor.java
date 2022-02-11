package dev.cgrscript.interpreter.module;

public class CgrElementDescriptor {

    public static CgrElementDescriptor builtinElement(String name) {
        return new CgrElementDescriptor(true, null, name);
    }

    public static CgrElementDescriptor element(String moduleId, String name) {
        return new CgrElementDescriptor(false, moduleId, name);
    }

    private final boolean builtin;

    private final String moduleId;

    private final String name;

    private CgrElementDescriptor(boolean builtin, String moduleId, String name) {
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
