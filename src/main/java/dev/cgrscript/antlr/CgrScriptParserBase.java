package dev.cgrscript.antlr;

import dev.cgrscript.interpreter.error.ParserErrorListener;
import org.antlr.v4.runtime.*;

public abstract class CgrScriptParserBase extends Parser {

    public CgrScriptParserBase(TokenStream input) {
        super(input);
    }

    @Override
    public void notifyErrorListeners(Token offendingToken, String msg, RecognitionException e) {
        super.notifyErrorListeners(offendingToken, msg, e);
    }

    public void notifyErrorListenersPrevToken(String message) {
        notifyErrorListenersToken(-1, message);
    }

    public void notifyErrorListenersToken(int k, String message) {
        notifyErrorListeners(_input.LT(k), message, null);
    }

    public void notifyMissingEndStatement() {
        Token token = _input.LT(-1);
        int charPos = token.getCharPositionInLine() + (token.getStopIndex() - token.getStartIndex()) + 1;
        for (ANTLRErrorListener errorListener : getErrorListeners()) {
            if (errorListener instanceof ParserErrorListener) {
                ((ParserErrorListener) errorListener).missingEndStatement(token.getLine(), charPos);
            }
        }
    }

    public void notifyMissingReturnStatement() {
        Token token = getRuleContext().getStart();
        int charPosStart = token.getCharPositionInLine();
        int charPosStop = token.getCharPositionInLine() + (token.getStopIndex() - token.getStartIndex());
        for (ANTLRErrorListener errorListener : getErrorListeners()) {
            if (errorListener instanceof ParserErrorListener) {
                ((ParserErrorListener) errorListener).missingReturnStatement(token.getLine(), charPosStart, charPosStop);
            }
        }
    }
}
