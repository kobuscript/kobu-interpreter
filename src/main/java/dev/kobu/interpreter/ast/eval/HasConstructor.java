package dev.kobu.interpreter.ast.eval;

import dev.kobu.interpreter.ast.symbol.BuiltinFunctionSymbol;

public interface HasConstructor {

    BuiltinFunctionSymbol getConstructor();

}
