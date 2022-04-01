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

package dev.kobu.interpreter.error.eval;

import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.ast.utils.ErrorMessageFormatter;
import dev.kobu.interpreter.error.EvalError;

public class NullPointerError extends EvalError {

    private final String message;

    private final SourceCodeRef nullSubExprSourceCodeRef;

    public NullPointerError(SourceCodeRef sourceCodeRef, SourceCodeRef nullSubExprSourceCodeRef) {
        super(sourceCodeRef);
        this.message = null;
        this.nullSubExprSourceCodeRef = nullSubExprSourceCodeRef;
    }

    public NullPointerError(String message, SourceCodeRef sourceCodeRef) {
        super(sourceCodeRef);
        this.message = message;
        this.nullSubExprSourceCodeRef = null;
    }

    @Override
    public String getDescription() {
        if (message != null) {
            return "NullPointerError: " + message;
        }
        if (nullSubExprSourceCodeRef != null) {
            return "Expression evaluated to null: " + ErrorMessageFormatter.getText(nullSubExprSourceCodeRef);
        }
        return "NullPointerError";
    }
}
