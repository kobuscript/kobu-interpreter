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

import dev.kobu.interpreter.ast.eval.expr.FunctionArgExpr;
import dev.kobu.interpreter.ast.symbol.FunctionType;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.error.AnalyzerError;

import java.util.List;
import java.util.Objects;

public class InvalidFunctionCallError extends AnalyzerError {

    private final FunctionType function;

    private final List<FunctionArgExpr> args;

    public InvalidFunctionCallError(SourceCodeRef sourceCodeRef, FunctionType function, List<FunctionArgExpr> args) {
        super(sourceCodeRef);
        this.function = function;
        this.args = args;
    }

    public FunctionType getFunction() {
        return function;
    }

    public List<FunctionArgExpr> getArgs() {
        return args;
    }

    @Override
    public String getDescription() {
        return "Expected " + getArgsNumber() + ", but got " + args.size() + ".";
    }

    private String getArgsNumber() {
        if (function.getParameters().size() == 1) {
            return "1 argument";
        } else {
            return function.getParameters().size() + " arguments";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        InvalidFunctionCallError that = (InvalidFunctionCallError) o;
        return Objects.equals(function, that.function);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), function);
    }
}
