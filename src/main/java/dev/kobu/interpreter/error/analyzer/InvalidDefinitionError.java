package dev.kobu.interpreter.error.analyzer;

import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.error.AnalyzerError;

public class InvalidDefinitionError extends AnalyzerError {

    public InvalidDefinitionError(SourceCodeRef sourceCodeRef) {
        super(sourceCodeRef);
    }

    @Override
    public String getDescription() {
        return "'type', 'template', 'rule', 'file' or 'native' expected";
    }
}
