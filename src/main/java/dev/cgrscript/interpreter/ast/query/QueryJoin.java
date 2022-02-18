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

package dev.cgrscript.interpreter.ast.query;

import dev.cgrscript.interpreter.ast.eval.EvalContext;
import dev.cgrscript.interpreter.ast.eval.Evaluable;
import dev.cgrscript.interpreter.ast.eval.Expr;
import dev.cgrscript.interpreter.ast.eval.LocalScope;
import dev.cgrscript.interpreter.ast.symbol.*;
import dev.cgrscript.interpreter.error.analyzer.InvalidTypeError;

public class QueryJoin implements Evaluable {

    private final SourceCodeRef sourceCodeRef;

    private final QueryTypeClause typeClause;

    private final Expr ofExpr;

    private boolean joinArray = false;

    public QueryJoin(SourceCodeRef sourceCodeRef, QueryTypeClause typeClause, Expr ofExpr) {
        this.sourceCodeRef = sourceCodeRef;
        this.typeClause = typeClause;
        this.ofExpr = ofExpr;
    }

    public QueryTypeClause getTypeClause() {
        return typeClause;
    }

    public Expr getOfExpr() {
        return ofExpr;
    }

    @Override
    public SourceCodeRef getSourceCodeRef() {
        return sourceCodeRef;
    }

    @Override
    public void analyze(EvalContext context) {
        if (ofExpr != null) {
            ofExpr.analyze(context);
            if (ofExpr.getType() instanceof UnknownType) {
                return;
            }
            if (!BuiltinScope.ANY_RECORD_TYPE.isAssignableFrom(ofExpr.getType())) {
                if (ofExpr.getType() instanceof ArrayType && BuiltinScope.ANY_RECORD_TYPE
                        .isAssignableFrom(((ArrayType) ofExpr.getType()).getElementType())) {
                    joinArray = true;
                } else {
                    context.getModuleScope().addError(new InvalidTypeError(ofExpr.getSourceCodeRef(),
                            BuiltinScope.ANY_RECORD_TYPE, ofExpr.getType()));
                    return;
                }
            }
        }

        if (!joinArray) {
            typeClause.analyze(context);
        } else {
            context.pushNewScope();
            typeClause.analyze(context);
            LocalScope scope = context.getCurrentScope();
            context.popScope();
            for (String key : scope.getKeys()) {
                Symbol symbol = scope.resolve(key);
                if (symbol instanceof VariableSymbol) {
                    context.getCurrentScope().define(new VariableSymbol(symbol.getName(),
                            new ArrayType(((VariableSymbol)symbol).getType())));
                }
            }
        }
    }

    public boolean joinArray() {
        return joinArray;
    }

}
