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

package dev.cgrscript.interpreter.ast.eval.function.global.rules;

import dev.cgrscript.interpreter.ast.eval.context.EvalContext;
import dev.cgrscript.interpreter.ast.eval.ValueExpr;
import dev.cgrscript.interpreter.ast.eval.expr.value.ArrayValueExpr;
import dev.cgrscript.interpreter.ast.eval.expr.value.RecordValueExpr;
import dev.cgrscript.interpreter.ast.eval.function.BuiltinGlobalFunction;
import dev.cgrscript.interpreter.ast.symbol.SourceCodeRef;
import dev.cgrscript.interpreter.error.eval.InvalidCallError;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FireRulesFunctionImpl extends BuiltinGlobalFunction {

    @Override
    protected ValueExpr run(EvalContext context, Map<String, ValueExpr> args, SourceCodeRef sourceCodeRef) {

        if (context.getDatabase().isRunning()) {
            throw new InvalidCallError("Rule engine is already running", sourceCodeRef);
        }

        ArrayValueExpr recordsExpr = (ArrayValueExpr) args.get("records");

        context.getDatabase().clear();
        var recordIdSet = new HashSet<Integer>();
        for (ValueExpr valueExpr : recordsExpr.getValue()) {
            RecordValueExpr recordExpr = (RecordValueExpr) valueExpr;
            setInitialValue(recordExpr, recordIdSet);
            context.getDatabase().insertFact(recordExpr);
        }
        context.getDatabase().linkRules();
        context.getDatabase().fireRules(context);
        context.getOutputWriter().write();

        return null;
    }

    private void setInitialValue(RecordValueExpr recordExpr, Set<Integer> recordIdSet) {
        if (!recordIdSet.add(recordExpr.getId())) {
            return;
        }

        recordExpr.setInitialValue();

        for (ValueExpr value : recordExpr.getValues()) {
            if (value instanceof RecordValueExpr) {
                setInitialValue((RecordValueExpr) value, recordIdSet);
            }
        }
    }

    @Override
    public String getDocumentation() {
        return "Configure the initial working memory and starts the rule engine";
    }

}
