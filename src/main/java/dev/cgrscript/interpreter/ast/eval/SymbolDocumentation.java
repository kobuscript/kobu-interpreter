package dev.cgrscript.interpreter.ast.eval;

public class SymbolDocumentation {

    private final String moduleId;

    private final SymbolTypeEnum symbolType;

    private final String description;

    private final String documentation;

    private final String containingType;

    public SymbolDocumentation(String moduleId, SymbolTypeEnum symbolType, String description, String documentation) {
        this.moduleId = moduleId;
        this.symbolType = symbolType;
        this.description = description;
        this.documentation = documentation;
        this.containingType = null;
    }

    public SymbolDocumentation(String moduleId, SymbolTypeEnum symbolType, String description,
                               String documentation, String containingType) {
        this.moduleId = moduleId;
        this.symbolType = symbolType;
        this.description = description;
        this.documentation = documentation;
        this.containingType = containingType;
    }

    public SymbolDocumentation(String moduleId, SymbolTypeEnum symbolType, String description) {
        this.moduleId = moduleId;
        this.symbolType = symbolType;
        this.description = description;
        this.documentation = null;
        this.containingType = null;
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

    public SymbolTypeEnum getSymbolType() {
        return symbolType;
    }

    public String getContainingType() {
        return containingType;
    }

}
