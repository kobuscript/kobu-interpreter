package dev.cgrscript.interpreter.error.analyzer;

import dev.cgrscript.interpreter.ast.symbol.SourceCodeRef;
import dev.cgrscript.interpreter.error.AnalyzerError;

public class InvalidVariableDeclError extends AnalyzerError {

    public InvalidVariableDeclError(SourceCodeRef sourceCodeRef) {
        super(sourceCodeRef);
    }

    @Override
    public String getDescription() {
        return "Missing variable type";
    }
}
