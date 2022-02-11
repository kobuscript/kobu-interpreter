package dev.cgrscript.interpreter.error.analyzer;

import dev.cgrscript.interpreter.ast.symbol.SourceCodeRef;
import dev.cgrscript.interpreter.error.AnalyzerError;

public class InvalidStatementError extends AnalyzerError {

    public InvalidStatementError(SourceCodeRef sourceCodeRef) {
        super(sourceCodeRef);
    }

    @Override
    public String getDescription() {
        return "not a valid statement";
    }
}
