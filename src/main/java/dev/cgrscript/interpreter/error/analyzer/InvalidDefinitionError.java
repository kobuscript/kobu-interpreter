package dev.cgrscript.interpreter.error.analyzer;

import dev.cgrscript.interpreter.ast.symbol.SourceCodeRef;
import dev.cgrscript.interpreter.error.AnalyzerError;

public class InvalidDefinitionError extends AnalyzerError {

    public InvalidDefinitionError(SourceCodeRef sourceCodeRef) {
        super(sourceCodeRef);
    }

    @Override
    public String getDescription() {
        return "'type', 'template', 'rule', 'file' or 'native' expected";
    }
}
