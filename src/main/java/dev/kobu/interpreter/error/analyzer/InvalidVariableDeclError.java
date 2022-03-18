package dev.kobu.interpreter.error.analyzer;

import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.error.AnalyzerError;

public class InvalidVariableDeclError extends AnalyzerError {

    public InvalidVariableDeclError(SourceCodeRef sourceCodeRef) {
        super(sourceCodeRef);
    }

    @Override
    public String getDescription() {
        return "Missing variable type";
    }
}
