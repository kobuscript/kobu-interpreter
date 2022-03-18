package dev.kobu.interpreter.ast.eval;

import dev.kobu.interpreter.ast.symbol.SourceCodeRef;

public interface HasElementRef extends AutoCompletionSource {

    SourceCodeRef getElementRef();

}
