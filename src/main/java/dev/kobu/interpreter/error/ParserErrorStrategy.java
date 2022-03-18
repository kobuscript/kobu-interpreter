package dev.kobu.interpreter.error;

import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.Token;

public class ParserErrorStrategy extends DefaultErrorStrategy {

    @Override
    protected Token getMissingSymbol(Parser recognizer) {
        return super.getMissingSymbol(recognizer);
    }
}
