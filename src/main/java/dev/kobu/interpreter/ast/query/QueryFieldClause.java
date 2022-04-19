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
import dev.kobu.interpreter.ast.eval.Evaluable;
import dev.kobu.interpreter.ast.eval.HasTypeScope;
import dev.kobu.interpreter.ast.eval.expr.value.ArrayValueExpr;
import dev.kobu.interpreter.ast.eval.context.EvalContext;
import dev.kobu.interpreter.ast.eval.ValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.NullValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.RecordValueExpr;
import dev.kobu.interpreter.ast.symbol.*;
import dev.kobu.interpreter.ast.symbol.array.ArrayType;
import dev.kobu.interpreter.error.analyzer.InvalidQueryType;
import dev.kobu.interpreter.error.analyzer.NotArrayTypeError;
import dev.kobu.interpreter.error.analyzer.UndefinedAttributeError;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class QueryFieldClause implements QueryClause {

    private final SourceCodeRef sourceCodeRef;

    private final String field;

    private String bind;

    private SourceCodeRef aliasSourceCodeRef;

    private QueryArrayItemClause arrayItemClause;

    private QueryClause next;

    private Type typeScope;

    private Type type;

    public QueryFieldClause(SourceCodeRef sourceCodeRef, String field) {
        this.sourceCodeRef = sourceCodeRef;
        this.field = field;
    }

    public SourceCodeRef getSourceCodeRef() {
        return sourceCodeRef;
    }

    @Override
    public void setResolvedTypes(Map<String, Type> resolvedTypes) {
        if (arrayItemClause != null) {
            arrayItemClause.setResolvedTypes(resolvedTypes);
        }
        if (next != null) {
            next.setResolvedTypes(resolvedTypes);
        }
    }

    @Override
    public void analyze(EvalContext context) {
        var fieldType = typeScope.resolveField(field);
        if (fieldType == null) {
            context.addAnalyzerError(new UndefinedAttributeError(sourceCodeRef, typeScope, field));
            return;
        }

        type = fieldType;
        if (arrayItemClause != null) {
            if (!(fieldType instanceof ArrayType)) {
                context.addAnalyzerError(new NotArrayTypeError(sourceCodeRef, fieldType));
                return;
            }
            arrayItemClause.setTypeScope(fieldType);
            arrayItemClause.analyze(context);
            type = ((ArrayType)fieldType).getElementType();
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
    public List<Match> eval(Match match) {

        List<Match> result = new ArrayList<>();
        if (arrayItemClause == null) {
            if (match.getValue() instanceof RecordValueExpr) {
                RecordValueExpr record = (RecordValueExpr) match.getValue();
                var value = record.resolveField(field);
                if (value != null && !(value instanceof NullValueExpr)) {
                    if (value instanceof RecordValueExpr) {
                        result.add(match.setValue((RecordValueExpr) value, value, bind));
                    } else {
                        result.add(match.setValue(value, bind));
                    }
                }
            }
        } else {
            if (match.getValue() instanceof ArrayValueExpr) {
                ArrayValueExpr list = (ArrayValueExpr) match.getValue();
                List<ValueExpr> values = arrayItemClause.eval(match.getContext(), list);
                for (ValueExpr value : values) {
                    if (value != null && !(value instanceof NullValueExpr)) {
                        if (value instanceof RecordValueExpr) {
                            result.add(match.setValue((RecordValueExpr) value, value, bind));
                        } else {
                            result.add(match.setValue(value, bind));
                        }
                    }
                }
            }
        }

        return result;
    }

    public String getField() {
        return field;
    }

    @Override
    public String getBind() {
        return bind;
    }

    @Override
    public void setBind(String bind) {
        this.bind = bind;
    }

    public void setAliasSourceCodeRef(SourceCodeRef aliasSourceCodeRef) {
        this.aliasSourceCodeRef = aliasSourceCodeRef;
    }

    public QueryArrayItemClause getArrayItemClause() {
        return arrayItemClause;
    }

    public void setArrayItemClause(QueryArrayItemClause arrayItemClause) {
        this.arrayItemClause = arrayItemClause;
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
    public void setNext(QueryClause next) {
        this.next = next;
    }

    @Override
    public void setTypeScope(Type typeScope) {
        this.typeScope = typeScope;
    }

    @Override
    public void setValueScope(ValueExpr valueScope) {

    }

}
