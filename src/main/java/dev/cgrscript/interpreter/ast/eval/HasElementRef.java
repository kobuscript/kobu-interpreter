package dev.cgrscript.interpreter.ast.eval;

import dev.cgrscript.interpreter.ast.symbol.SourceCodeRef;

public interface HasElementRef extends AutoCompletionSource {

    SourceCodeRef getElementRef();

}
