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
import dev.kobu.interpreter.ast.eval.context.EvalContext;
import dev.kobu.interpreter.ast.eval.ValueExpr;
import dev.kobu.interpreter.ast.eval.expr.FunctionArgExpr;
import dev.kobu.interpreter.ast.eval.expr.FunctionCallExpr;
import dev.kobu.interpreter.ast.eval.expr.ProxyValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.ArrayValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.NullValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.RecordValueExpr;
import dev.kobu.interpreter.ast.symbol.*;
import dev.kobu.interpreter.error.analyzer.InvalidFunctionClauseError;
import dev.kobu.interpreter.error.analyzer.InvalidQueryType;
import dev.kobu.interpreter.error.analyzer.NotArrayTypeError;

import java.util.ArrayList;
import java.util.List;

public class QueryFunctionCallClause implements QueryPipeClause {

    private final SourceCodeRef sourceCodeRef;

    private final FunctionCallExpr functionCallExpr;

    private QueryArrayItemClause arrayItemClause;

    private String bind;

    private SourceCodeRef aliasSourceCodeRef;

    private QueryPipeClause next;

    private Type typeScope;

    private Type type;

    private ProxyValueExpr proxyValueExpr;

    public QueryFunctionCallClause(SourceCodeRef sourceCodeRef, FunctionCallExpr functionCallExpr) {
        this.sourceCodeRef = sourceCodeRef;
        this.functionCallExpr = functionCallExpr;
    }

    @Override
    public SourceCodeRef getSourceCodeRef() {
        return sourceCodeRef;
    }

    @Override
    public void analyze(EvalContext context) {

        if (typeScope.resolveMethod(functionCallExpr.getFunctionName()) != null) {
            functionCallExpr.setTypeScope(typeScope);
        } else {
            proxyValueExpr = new ProxyValueExpr(sourceCodeRef);
            proxyValueExpr.setType(typeScope);
            functionCallExpr.getArgs().add(0, new FunctionArgExpr(sourceCodeRef, proxyValueExpr));
        }

        functionCallExpr.analyze(context);

        if (functionCallExpr.getType() == null) {
            context.addAnalyzerError(new InvalidFunctionClauseError(functionCallExpr.getSourceCodeRef(),
                    functionCallExpr.getFunctionType()));
            return;
        }

        if (functionCallExpr.getType() instanceof UnknownType) {
            return;
        }

        type = functionCallExpr.getType();
        if (arrayItemClause != null) {
            if (!(functionCallExpr.getType() instanceof ArrayType)) {
                context.addAnalyzerError(new NotArrayTypeError(sourceCodeRef, typeScope));
                return;
            }
            arrayItemClause.setTypeScope(functionCallExpr.getType());
            arrayItemClause.analyze(context);
            type = ((ArrayType)functionCallExpr.getType()).getElementType();
        }

        if (bind != null) {
            context.getCurrentScope().define(context.getAnalyzerContext(), new VariableSymbol(context.getModuleScope(),
                    aliasSourceCodeRef, bind, type));
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

        var fact = match.getValue();
        if (proxyValueExpr != null) {
            proxyValueExpr.setValue(fact);
        } else {
            functionCallExpr.setValueScope(fact);
        }

        var valueExpr = functionCallExpr.evalExpr(match.getContext());

        List<Match> result = new ArrayList<>();
        if (arrayItemClause == null) {
            if (valueExpr != null && !(valueExpr instanceof NullValueExpr)) {
                if (valueExpr instanceof RecordValueExpr) {
                    result.add(match.setValue((RecordValueExpr) valueExpr, valueExpr, bind));
                } else {
                    result.add(match.setValue(valueExpr, bind));
                }
            }
        } else {
            if (valueExpr instanceof ArrayValueExpr) {
                List<ValueExpr> values = arrayItemClause.eval(match.getContext(), (ArrayValueExpr) valueExpr);
                for (ValueExpr value : values) {
                    if (value instanceof RecordValueExpr) {
                        result.add(match.setValue((RecordValueExpr) value, value, bind));
                    } else {
                        result.add(match.setValue(value, bind));
                    }
                }
            }
        }

        return result;
    }

    public QueryArrayItemClause getArrayItemClause() {
        return arrayItemClause;
    }

    public void setArrayItemClause(QueryArrayItemClause arrayItemClause) {
        this.arrayItemClause = arrayItemClause;
    }

    @Override
    public void setTypeScope(Type typeScope) {
        this.typeScope = typeScope;
    }

    @Override
    public void setValueScope(ValueExpr valueScope) {

    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public QueryPipeClause getNext() {
        return next;
    }

    @Override
    public void setNext(QueryPipeClause next) {
        this.next = next;
    }

    @Override
    public void setBind(String bind) {
        this.bind = bind;
    }

    @Override
    public String getBind() {
        return bind;
    }

    @Override
    public void setAliasSourceCodeRef(SourceCodeRef aliasSourceCodeRef) {
        this.aliasSourceCodeRef = aliasSourceCodeRef;
    }

}
