/*
MIT License

Copyright (c) 2022 Luiz Mineo

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */

package dev.cgrscript.interpreter.error;

import dev.cgrscript.interpreter.ast.symbol.ScriptRef;
import dev.cgrscript.interpreter.ast.symbol.SourceCodeRef;
import dev.cgrscript.interpreter.ast.utils.ErrorMessageFormatter;
import dev.cgrscript.interpreter.error.eval.InternalInterpreterError;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;

import java.io.IOException;

public class ParserErrorListener extends BaseErrorListener {

    private boolean hasErrors = false;

    private ScriptRef currentScript;

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer,
                            Object offendingSymbol,
                            int line, int charPositionInLine,
                            String msg,
                            RecognitionException e) {
        Token offendingToken = (Token) offendingSymbol;
        SourceCodeRef sourceCodeRef = new SourceCodeRef(currentScript, currentScript.extractModuleId(),
                line, charPositionInLine,
                line, charPositionInLine + (offendingToken.getStopIndex() - offendingToken.getStartIndex()));

        try {
            System.err.println(ErrorMessageFormatter.getMessage(new ParserError(sourceCodeRef, msg)));
        } catch (IOException ex) {
            throw new InternalInterpreterError(ex, sourceCodeRef);
        }

        this.hasErrors = true;
    }

    public void missingEndStatement(int line, int charPos) {
        SourceCodeRef sourceCodeRef = new SourceCodeRef(currentScript, currentScript.extractModuleId(),
                line, charPos,
                line, charPos);

        try {
            System.err.println(ErrorMessageFormatter.getMessage(new ParserError(sourceCodeRef, "';' expected")));
        } catch (IOException ex) {
            throw new InternalInterpreterError(ex, sourceCodeRef);
        }

        this.hasErrors = true;
    }

    public void missingReturnStatement(int line, int charPosStart, int charPosStop) {
        SourceCodeRef sourceCodeRef = new SourceCodeRef(currentScript, currentScript.extractModuleId(),
                line, charPosStart,
                line, charPosStop);

        try {
            System.err.println(ErrorMessageFormatter.getMessage(new ParserError(sourceCodeRef, "Missing return type on function declaration")));
        } catch (IOException ex) {
            throw new InternalInterpreterError(ex, sourceCodeRef);
        }

        this.hasErrors = true;
    }

    public void setCurrentScript(ScriptRef currentScript) {
        this.currentScript = currentScript;
    }

    public boolean hasErrors() {
        return hasErrors;
    }

}
