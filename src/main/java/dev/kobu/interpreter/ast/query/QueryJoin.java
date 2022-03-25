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

package dev.kobu.interpreter.ast.query;

import dev.kobu.interpreter.ast.eval.context.EvalContext;
import dev.kobu.interpreter.ast.eval.Evaluable;
import dev.kobu.interpreter.ast.eval.Expr;
import dev.kobu.interpreter.ast.symbol.Type;
import dev.kobu.interpreter.ast.symbol.array.ArrayType;
import dev.kobu.interpreter.ast.symbol.BuiltinScope;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.ast.symbol.UnknownType;
import dev.kobu.interpreter.ast.symbol.array.ArrayTypeFactory;
import dev.kobu.interpreter.error.analyzer.InvalidTypeError;

import java.util.Map;

public class QueryJoin implements Evaluable {

    private final SourceCodeRef sourceCodeRef;

    private final QueryTypeClause typeClause;

    private final Expr ofExpr;

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
    public void setResolvedTypes(Map<String, Type> resolvedTypes) {
        if (ofExpr != null) {
            ofExpr.setResolvedTypes(resolvedTypes);
        }
    }

    @Override
    public void analyze(EvalContext context) {
        typeClause.analyze(context);
        if (ofExpr != null) {
            ofExpr.analyze(context);
            if (ofExpr.getType() instanceof UnknownType) {
                return;
            }
            if (!BuiltinScope.ANY_RECORD_TYPE.isAssignableFrom(ofExpr.getType())) {
                if (ofExpr.getType() instanceof ArrayType && BuiltinScope.ANY_RECORD_TYPE
                        .isAssignableFrom(((ArrayType) ofExpr.getType()).getElementType())) {

                    if (!(typeClause.getQueryType() instanceof ArrayType)) {
                        context.addAnalyzerError(new InvalidTypeError(ofExpr.getSourceCodeRef(),
                                BuiltinScope.ANY_RECORD_TYPE, ofExpr.getType()));
                    }
                } else {
                    context.addAnalyzerError(new InvalidTypeError(ofExpr.getSourceCodeRef(),
                            BuiltinScope.ANY_RECORD_TYPE, ofExpr.getType()));
                }
            } else if (typeClause.getQueryType() instanceof ArrayType) {
                context.addAnalyzerError(new InvalidTypeError(ofExpr.getSourceCodeRef(),
                        ArrayTypeFactory.getArrayTypeFor(BuiltinScope.ANY_RECORD_TYPE), ofExpr.getType()));
            }


        }

    }

}
