package dev.cgrscript.interpreter.error.analyzer;

import dev.cgrscript.interpreter.ast.symbol.SourceCodeRef;
import dev.cgrscript.interpreter.ast.symbol.Type;
import dev.cgrscript.interpreter.error.AnalyzerError;

public class InvalidJoinQueryType extends AnalyzerError {

    private final Type type;

    public InvalidJoinQueryType(SourceCodeRef sourceCodeRef, Type type) {
        super(sourceCodeRef);
        this.type = type;
    }

    @Override
    public String getDescription() {
        return "Expected 'AnyRecord', 'Template', 'AnyRecord[]' or 'Template[]', but got '" + type.getName() + "'";
    }

}
