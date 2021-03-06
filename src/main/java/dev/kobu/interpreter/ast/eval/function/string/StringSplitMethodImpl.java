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

package dev.kobu.interpreter.ast.eval.function.string;

import dev.kobu.interpreter.ast.eval.context.EvalContext;
import dev.kobu.interpreter.ast.eval.expr.value.ArrayValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.number.NumberValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.StringValueExpr;
import dev.kobu.interpreter.ast.eval.function.BuiltinMethod;
import dev.kobu.interpreter.ast.symbol.BuiltinScope;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.ast.eval.ValueExpr;
import dev.kobu.interpreter.ast.symbol.array.ArrayTypeFactory;
import dev.kobu.interpreter.error.eval.IllegalArgumentError;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StringSplitMethodImpl extends BuiltinMethod {

    @Override
    protected ValueExpr run(EvalContext context, ValueExpr object, Map<String, ValueExpr> args, SourceCodeRef sourceCodeRef) {
        StringValueExpr stringExpr = (StringValueExpr) object;
        StringValueExpr regexExpr = (StringValueExpr) args.get("regex");
        NumberValueExpr limitExpr = (NumberValueExpr) args.get("limit");

        if (regexExpr == null) {
            throw new IllegalArgumentError("'regex' cannot be null", sourceCodeRef);
        }

        List<String> result;
        if (limitExpr == null) {
            result = Arrays.asList(stringExpr.getValue().split(regexExpr.getValue()));
        } else {
            result = Arrays.asList(stringExpr.getValue().split(regexExpr.getValue(), limitExpr.getValue().intValue()));
        }
        List<ValueExpr> resultExpr = result
                .stream()
                .map(StringValueExpr::new)
                .collect(Collectors.toList());

        return new ArrayValueExpr(ArrayTypeFactory.getArrayTypeFor(BuiltinScope.STRING_TYPE), resultExpr);
    }

    @Override
    public String getDocumentation() {
        return "Splits this string around matches of the given regular expression";
    }

}
