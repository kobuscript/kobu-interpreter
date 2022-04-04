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
import dev.kobu.interpreter.error.eval.InvalidCallError;

import java.text.ParseException;
import java.util.Date;
import java.util.Map;

public class DateParserMethodImpl extends BuiltinMethod {

    @Override
    protected ValueExpr run(EvalContext context, ValueExpr object, Map<String, ValueExpr> args, SourceCodeRef sourceCodeRef) {
        DateFormatterValueExpr formatter = (DateFormatterValueExpr) object;
        ValueExpr sourceExpr = args.get("source");

        if (sourceExpr == null || sourceExpr instanceof NullValueExpr) {
            throw new IllegalArgumentError("source cannot be null", sourceCodeRef);
        }

        Date date;
        try {
            date = formatter.getDateFormatter().parse(((StringValueExpr)sourceExpr).getValue());
        } catch (ParseException e) {
             throw new InvalidCallError(e.getMessage(), sourceCodeRef);
        }

        return new DateValueExpr(date);
    }

    @Override
    public String getDocumentation() {
        return "Parses a string to produce a Date";
    }

}
