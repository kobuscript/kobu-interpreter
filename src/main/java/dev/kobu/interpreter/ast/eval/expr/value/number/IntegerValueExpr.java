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

package dev.kobu.interpreter.ast.eval.expr.value.number;

import dev.kobu.interpreter.ast.symbol.SourceCodeRef;

public class IntegerValueExpr extends NumberValueExpr {

    public IntegerValueExpr(Integer value) {
        super(value);
    }

    public IntegerValueExpr(SourceCodeRef sourceCodeRef, Integer value) {
        super(sourceCodeRef, value);
    }

    @Override
    public NumberValueExpr inc() {
        return new IntegerValueExpr(value.intValue() + 1);
    }

    @Override
    public NumberValueExpr dec() {
        return new IntegerValueExpr(value.intValue() - 1);
    }

    @Override
    public NumberValueExpr abs() {
        return new IntegerValueExpr(value.intValue());
    }

    @Override
    public NumberValueExpr add(IntegerValueExpr other) {
        return new IntegerValueExpr(value.intValue() + other.value.intValue());
    }

    @Override
    public NumberValueExpr add(LongValueExpr other) {
        return new LongValueExpr(value.intValue() + other.value.longValue());
    }

    @Override
    public NumberValueExpr add(DoubleValueExpr other) {
        return new DoubleValueExpr(value.intValue() + other.value.doubleValue());
    }

    @Override
    public NumberValueExpr sub(IntegerValueExpr other) {
        return new IntegerValueExpr(value.intValue() - other.value.intValue());
    }

    @Override
    public NumberValueExpr sub(LongValueExpr other) {
        return new LongValueExpr(value.intValue() - other.value.longValue());
    }

    @Override
    public NumberValueExpr sub(DoubleValueExpr other) {
        return new DoubleValueExpr(value.intValue() - other.value.doubleValue());
    }

    @Override
    public NumberValueExpr mult(IntegerValueExpr other) {
        return new IntegerValueExpr(value.intValue() * other.value.intValue());
    }

    @Override
    public NumberValueExpr mult(LongValueExpr other) {
        return new LongValueExpr(value.intValue() * other.value.longValue());
    }

    @Override
    public NumberValueExpr mult(DoubleValueExpr other) {
        return new DoubleValueExpr(value.intValue() * other.value.doubleValue());
    }

    @Override
    public NumberValueExpr div(IntegerValueExpr other) {
        return new DoubleValueExpr(value.intValue() / other.value.doubleValue());
    }

    @Override
    public NumberValueExpr div(LongValueExpr other) {
        return new DoubleValueExpr(value.intValue() / other.value.doubleValue());
    }

    @Override
    public NumberValueExpr div(DoubleValueExpr other) {
        return new DoubleValueExpr(value.intValue() / other.value.doubleValue());
    }

}
