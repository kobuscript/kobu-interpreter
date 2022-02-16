package dev.cgrscript.interpreter.ast.eval;

import dev.cgrscript.interpreter.ast.symbol.SourceCodeRef;

import java.util.List;

public interface HasElementRef {

    SourceCodeRef getElementRef();

    List<SymbolDescriptor> requestSuggestions();

}
