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

package dev.kobu.interpreter.ast.eval.function.date;

import dev.kobu.interpreter.ast.eval.ValueExpr;
import dev.kobu.interpreter.ast.eval.context.EvalContext;
import dev.kobu.interpreter.ast.eval.expr.value.DateFormatterValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.DateValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.NullValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.StringValueExpr;
import dev.kobu.interpreter.ast.eval.function.BuiltinMethod;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.error.eval.IllegalArgumentError;

import java.util.Map;

public class DateFormatMethodImpl extends BuiltinMethod {

    @Override
    protected ValueExpr run(EvalContext context, ValueExpr object, Map<String, ValueExpr> args, SourceCodeRef sourceCodeRef) {
        DateFormatterValueExpr formatter = (DateFormatterValueExpr) object;
        ValueExpr dateExpr = args.get("date");

        if (dateExpr == null || dateExpr instanceof NullValueExpr) {
            throw new IllegalArgumentError("date cannot be null", sourceCodeRef);
        }

        String text = formatter.getDateFormatter().format(((DateValueExpr)dateExpr).getValue());

        return new StringValueExpr(text);
    }

    @Override
    public String getDocumentation() {
        return "Formats a Date into a string";
    }

}
