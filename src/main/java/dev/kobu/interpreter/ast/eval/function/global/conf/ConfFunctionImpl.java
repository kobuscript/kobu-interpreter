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

package dev.kobu.interpreter.ast.eval.function.global.conf;

import dev.kobu.interpreter.ast.eval.context.EvalContext;
import dev.kobu.interpreter.ast.eval.expr.value.NullValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.StringValueExpr;
import dev.kobu.interpreter.ast.eval.ValueExpr;
import dev.kobu.interpreter.ast.eval.function.BuiltinGlobalFunction;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;

import java.util.Locale;
import java.util.Map;

public class ConfFunctionImpl extends BuiltinGlobalFunction {

    @Override
    protected ValueExpr run(EvalContext context, Map<String, ValueExpr> args, SourceCodeRef sourceCodeRef) {
        StringValueExpr nameExpr = (StringValueExpr) args.get("name");
        StringValueExpr defaultExpr = (StringValueExpr) args.get("default");

        String var = nameExpr.getValue().replaceAll("([A-Z]+)", "_\1").toUpperCase(Locale.ROOT);

        String value = System.getenv(var);

        if (value != null) {
            return new StringValueExpr(value);
        }

        value = context.getProperty(nameExpr.getValue());

        if (value != null) {
            return new StringValueExpr(value);
        }

        if (defaultExpr != null) {
            return new StringValueExpr(defaultExpr.getValue());
        }

        return new NullValueExpr();
    }

    @Override
    public String getDocumentation() {
        return "";
    }

}
