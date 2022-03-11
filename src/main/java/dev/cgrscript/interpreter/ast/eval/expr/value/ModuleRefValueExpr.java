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

package dev.cgrscript.interpreter.ast.eval.expr.value;

import dev.cgrscript.interpreter.ast.eval.context.EvalContext;
import dev.cgrscript.interpreter.ast.eval.Expr;
import dev.cgrscript.interpreter.ast.eval.HasFields;
import dev.cgrscript.interpreter.ast.eval.ValueExpr;
import dev.cgrscript.interpreter.ast.eval.context.SnapshotValue;
import dev.cgrscript.interpreter.ast.symbol.*;

public class ModuleRefValueExpr implements ValueExpr, HasFields {

    private final ModuleRefSymbol moduleRefSymbol;

    private final ModuleScope moduleScope;

    public ModuleRefValueExpr(ModuleRefSymbol moduleRefSymbol, ModuleScope moduleScope) {
        this.moduleRefSymbol = moduleRefSymbol;
        this.moduleScope = moduleScope;
    }

    @Override
    public SourceCodeRef getSourceCodeRef() {
        return null;
    }

    @Override
    public void analyze(EvalContext context) {

    }

    @Override
    public Type getType() {
        return moduleRefSymbol;
    }

    @Override
    public ValueExpr evalExpr(EvalContext context) {
        return this;
    }

    @Override
    public String getStringValue() {
        return moduleRefSymbol.getName();
    }

    @Override
    public SnapshotValue getSnapshotValue() {
        return null;
    }

    @Override
    public Expr resolveField(String fieldName) {
        var symbol = moduleScope.resolveLocal(fieldName);
        if (symbol instanceof RuleSymbol) {
            return new RuleRefValueExpr((RuleSymbol) symbol);
        }
        if (symbol instanceof RecordTypeSymbol) {
            return new RecordTypeRefValueExpr((RecordTypeSymbol) symbol);
        }
        return null;
    }

    @Override
    public void updateFieldValue(EvalContext context, String fieldName, ValueExpr value) {

    }
}
