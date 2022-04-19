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

import dev.kobu.database.index.Match;
import dev.kobu.interpreter.ast.eval.ValueExpr;
import dev.kobu.interpreter.ast.eval.context.EvalContext;
import dev.kobu.interpreter.ast.eval.expr.value.ArrayValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.RecordValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.TupleValueExpr;
import dev.kobu.interpreter.ast.symbol.*;
import dev.kobu.interpreter.ast.symbol.array.ArrayType;
import dev.kobu.interpreter.ast.symbol.array.ArrayTypeFactory;
import dev.kobu.interpreter.error.analyzer.InvalidQueryType;
import dev.kobu.interpreter.error.analyzer.InvalidTypeError;

import java.util.*;

public class QueryStarTypeClause implements QueryClause {

    private final SourceCodeRef sourceCodeRef;

    private final Type type;

    private final boolean includeSubtypes;

    private String bind;

    private SourceCodeRef aliasSourceCodeRef;

    private QueryClause next;

    private Type typeScope;

    public QueryStarTypeClause(SourceCodeRef sourceCodeRef, Type type, boolean includeSubtypes) {
        this.sourceCodeRef = sourceCodeRef;
        this.type = type;
        this.includeSubtypes = includeSubtypes;
    }

    @Override
    public SourceCodeRef getSourceCodeRef() {
        return sourceCodeRef;
    }

    @Override
    public void setResolvedTypes(Map<String, Type> resolvedTypes) {
        if (next != null) {
            next.setResolvedTypes(resolvedTypes);
        }
    }

    @Override
    public void analyze(EvalContext context) {
        if (!(type instanceof RecordTypeSymbol) && !(type instanceof AnyRecordTypeSymbol)) {
            context.addAnalyzerError(new InvalidTypeError(sourceCodeRef, BuiltinScope.ANY_RECORD_TYPE, type));
            return;
        }

        if (bind != null) {
            context.getCurrentScope().define(context.getAnalyzerContext(),
                    new VariableSymbol(context.getModuleScope(), aliasSourceCodeRef, bind, type));
        }

        if (next != null) {
            next.setTypeScope(type);
            next.analyze(context);
        } else if (!(type instanceof RecordTypeSymbol)) {
            context.addAnalyzerError(new InvalidQueryType(sourceCodeRef, type));
        }
    }

    @Override
    public void setTypeScope(Type typeScope) {
        this.typeScope = typeScope;
    }

    @Override
    public void setValueScope(ValueExpr valueScope) {

    }

    @Override
    public List<Match> eval(Match match) {
        List<ValueExpr> result = new ArrayList<>();
        Set<Integer> idSet = new HashSet<>();
        findMatches(idSet, result, type, match.getValue());

        List<Match> matches = new ArrayList<>();
        for (ValueExpr recExpr : result) {
            matches.add(match.setValue((RecordValueExpr) recExpr, recExpr, bind));
        }
        return matches;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public QueryClause getNext() {
        return next;
    }

    @Override
    public String getBind() {
        return bind;
    }

    @Override
    public void setBind(String bind) {
        this.bind = bind;
    }

    @Override
    public void setNext(QueryClause next) {
        this.next = next;
    }

    public void setAliasSourceCodeRef(SourceCodeRef aliasSourceCodeRef) {
        this.aliasSourceCodeRef = aliasSourceCodeRef;
    }

    private void findMatches(Set<Integer> idSet, List<ValueExpr> result, Type targetType, ValueExpr valueExpr) {
        if (valueExpr instanceof RecordValueExpr) {
            findMatchesRecord(idSet, result, targetType, (RecordValueExpr) valueExpr);
        } else if (valueExpr instanceof ArrayValueExpr) {
            findMatchesArray(idSet, result, targetType, (ArrayValueExpr) valueExpr);
        } else if (valueExpr instanceof TupleValueExpr) {
            findMatchesTuple(idSet, result, targetType, (TupleValueExpr) valueExpr);
        }
    }

    private void findMatchesRecord(Set<Integer> idSet, List<ValueExpr> result, Type targetType, RecordValueExpr recordExpr) {
        if (!idSet.add(recordExpr.getId())) {
            return;
        }
        for (ValueExpr valueExpr : recordExpr.getValues()) {
            if (valueExpr instanceof RecordValueExpr) {
                addValue(result, targetType, (RecordValueExpr) valueExpr);
            }
            findMatches(idSet, result, targetType, valueExpr);
        }
    }

    private void findMatchesArray(Set<Integer> idSet, List<ValueExpr> result, Type targetType, ArrayValueExpr arrExpr) {
        for (ValueExpr valueExpr : arrExpr.getValue()) {
            findMatches(idSet, result, targetType, valueExpr);
        }
    }

    private void findMatchesTuple(Set<Integer> idSet, List<ValueExpr> result, Type targetType, TupleValueExpr tupleExpr) {
        for (ValueExpr valueExpr : tupleExpr.getValueExprList()) {
            findMatches(idSet, result, targetType, valueExpr);
        }
    }

    private void addValue(List<ValueExpr> result, Type targetType, RecordValueExpr valueExpr) {
        if (valueExpr.getType() == null) {
            return;
        }
        boolean matches;
        if (includeSubtypes) {
            matches = targetType.isAssignableFrom(valueExpr.getType());
        } else {
            matches = targetType.equals(valueExpr.getType());
        }

        if (matches) {
            result.add(valueExpr);
        }
    }

}
