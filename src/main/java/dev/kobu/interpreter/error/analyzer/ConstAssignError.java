package dev.kobu.interpreter.error.analyzer;

import dev.kobu.interpreter.ast.symbol.ConstantSymbol;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.error.AnalyzerError;

public class ConstAssignError extends AnalyzerError {

    private final ConstantSymbol constSymbol;

    public ConstAssignError(SourceCodeRef sourceCodeRef, ConstantSymbol constSymbol) {
        super(sourceCodeRef);
        this.constSymbol = constSymbol;
    }

    @Override
    public String getDescription() {
        return "Cannot assign a value to constant '" + constSymbol.getName() + "'";
    }
}
