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

import dev.cgrscript.database.index.Match;
import dev.cgrscript.interpreter.ast.eval.context.EvalContext;
import dev.cgrscript.interpreter.ast.eval.ValueExpr;
import dev.cgrscript.interpreter.ast.eval.expr.value.RecordValueExpr;
import dev.cgrscript.interpreter.ast.eval.function.BuiltinGlobalFunction;
import dev.cgrscript.interpreter.ast.symbol.SourceCodeRef;
import dev.cgrscript.interpreter.error.eval.InvalidCallError;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class InsertFunctionImpl extends BuiltinGlobalFunction {

    @Override
    protected ValueExpr run(EvalContext context, Map<String, ValueExpr> args, SourceCodeRef sourceCodeRef) {

        if (!context.getDatabase().isRunning()) {
            throw new InvalidCallError("Rule engine isn't running", sourceCodeRef);
        }

        RecordValueExpr recordExpr = (RecordValueExpr) args.get("value");
        Match match = context.getRuleContext().getMatch();
        RecordValueExpr rootRecord = match.getRootRecord();
        addCreatorId(recordExpr, rootRecord.getId(), new HashSet<>());
        recordExpr.setOriginRule(context.getRuleContext().getRuleSymbol().getFullName());
        context.getDatabase().insertFact(recordExpr);
        return null;
    }

    private void addCreatorId(RecordValueExpr recordExpr, int creatorId, Set<Integer> recordIdSet) {
        if (!recordIdSet.add(recordExpr.getId())) {
            return;
        }

        if (!recordExpr.initialValue() && recordExpr.getCreatorId() == 0) {
            recordExpr.setCreatorId(creatorId);
        }

        for (ValueExpr value : recordExpr.getValues()) {
            if (value instanceof RecordValueExpr) {
                addCreatorId((RecordValueExpr) value, creatorId, recordIdSet);
            }
        }
    }

    @Override
    public String getDocumentation() {
        return "Insert a new record in the working memory." +
                "\n\nThis function can only be invoked from a rule. Use 'fireRules()' to configure the " +
                "initial working memory and start the rule engine.";
    }

}
