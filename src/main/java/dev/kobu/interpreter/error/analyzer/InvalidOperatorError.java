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

package dev.kobu.interpreter.error.analyzer;

import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.ast.symbol.Type;
import dev.kobu.interpreter.error.AnalyzerError;

public class InvalidOperatorError extends AnalyzerError {

    private final Type operandA;

    private final String operator;

    private final Type operandB;

    private final int operands;

    public InvalidOperatorError(SourceCodeRef sourceCodeRef, Type operandA, String operator, Type operandB) {
        super(sourceCodeRef);
        this.operandA = operandA;
        this.operator = operator;
        this.operandB = operandB;
        this.operands = 2;
    }

    public InvalidOperatorError(SourceCodeRef sourceCodeRef, String operator, Type operandA) {
        super(sourceCodeRef);
        this.operandA = operandA;
        this.operator = operator;
        this.operandB = null;
        this.operands = 1;
    }

    @Override
    public String getDescription() {
        if (operands == 2) {
            String name1 = operandA != null ? operandA.getName() : "null";
            String name2 = operandB != null ? operandB.getName() : "null";
            return "Operator '" + operator + "' cannot be applied to '" + name1 + "' and '" + name2 + "'";
        } else {
            String name1 = operandA != null ? operandA.getName() : "null";
            return "Operator '" + operator + "' cannot be applied to '" + name1 + "'";
        }
    }
}
