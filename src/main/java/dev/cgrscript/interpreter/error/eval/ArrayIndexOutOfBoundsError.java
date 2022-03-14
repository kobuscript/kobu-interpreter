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

package dev.cgrscript.interpreter.error.eval;

import dev.cgrscript.interpreter.ast.eval.expr.value.ArrayValueExpr;
import dev.cgrscript.interpreter.ast.symbol.SourceCodeRef;
import dev.cgrscript.interpreter.error.EvalError;

public class ArrayIndexOutOfBoundsError extends EvalError {

    private final ArrayValueExpr array;

    private final int index;

    public ArrayIndexOutOfBoundsError(SourceCodeRef sourceCodeRef, ArrayValueExpr array, int index) {
        super(sourceCodeRef);
        this.array = array;
        this.index = index;
    }

    public ArrayValueExpr getArray() {
        return array;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public String getDescription() {
        return "Index " + index + " out of bounds for length " + array.getValue().size();
    }
}
