package dev.kobu.interpreter.error.analyzer;

import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.error.AnalyzerError;

public class InvalidStatementError extends AnalyzerError {

    public InvalidStatementError(SourceCodeRef sourceCodeRef) {
        super(sourceCodeRef);
    }

    @Override
    public String getDescription() {
        return "Not a valid statement";
    }
}
