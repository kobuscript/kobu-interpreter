package dev.cgrscript.interpreter.ast.eval;

public class SymbolDocumentation {

    private final String moduleId;

    private final String description;

    private final String documentation;

    public SymbolDocumentation(String moduleId, String description, String documentation) {
        this.moduleId = moduleId;
        this.description = description;
        this.documentation = documentation;
    }

    public SymbolDocumentation(String moduleId, String description) {
        this.moduleId = moduleId;
        this.description = description;
        this.documentation = null;
    }

    public String getModuleId() {
        return moduleId;
    }

    public String getDescription() {
        return description;
    }

    public String getDocumentation() {
        return documentation;
    }

}
