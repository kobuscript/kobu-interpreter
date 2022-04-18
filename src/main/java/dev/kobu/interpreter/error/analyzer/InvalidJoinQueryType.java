package dev.kobu.interpreter.error.analyzer;

import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.ast.symbol.Type;
import dev.kobu.interpreter.error.AnalyzerError;

public class InvalidJoinQueryType extends AnalyzerError {

    private final Type type;

    public InvalidJoinQueryType(SourceCodeRef sourceCodeRef, Type type) {
        super(sourceCodeRef);
        this.type = type;
    }

    @Override
    public String getDescription() {
        return "Expected 'AnyRecord', 'AnyTemplate', 'AnyRecord[]' or 'AnyTemplate[]', but got '" + type.getName() + "'";
    }

}
