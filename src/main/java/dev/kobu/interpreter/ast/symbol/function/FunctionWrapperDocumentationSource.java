package dev.kobu.interpreter.ast.symbol.function;

import dev.kobu.interpreter.ast.eval.DocumentationSource;
import dev.kobu.interpreter.ast.eval.SymbolDocumentation;
import dev.kobu.interpreter.ast.symbol.Type;

import java.util.Map;

public class FunctionWrapperDocumentationSource implements DocumentationSource {

    private final NamedFunction function;

    private final Map<String, Type> typeArgs;

    public FunctionWrapperDocumentationSource(NamedFunction function, Map<String, Type> typeArgs) {
        this.function = function;
        this.typeArgs = typeArgs;
    }

    @Override
    public SymbolDocumentation getDocumentation() {
        return function.getDocumentation(typeArgs);
    }
}
