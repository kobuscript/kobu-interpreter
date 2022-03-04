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

import dev.cgrscript.database.match.Match;
import dev.cgrscript.interpreter.ast.eval.EvalContext;
import dev.cgrscript.interpreter.ast.eval.ValueExpr;
import dev.cgrscript.interpreter.ast.eval.expr.FunctionArgExpr;
import dev.cgrscript.interpreter.ast.eval.expr.FunctionCallExpr;
import dev.cgrscript.interpreter.ast.eval.expr.ProxyValueExpr;
import dev.cgrscript.interpreter.ast.eval.expr.value.ArrayValueExpr;
import dev.cgrscript.interpreter.ast.eval.expr.value.NullValueExpr;
import dev.cgrscript.interpreter.ast.eval.expr.value.RecordValueExpr;
import dev.cgrscript.interpreter.ast.symbol.*;
import dev.cgrscript.interpreter.error.analyzer.InvalidFunctionClauseError;
import dev.cgrscript.interpreter.error.analyzer.InvalidQueryType;
import dev.cgrscript.interpreter.error.analyzer.NotArrayTypeError;

import java.util.ArrayList;
import java.util.List;

public class QueryFunctionCallClause implements QueryPipeClause {

    private final SourceCodeRef sourceCodeRef;

    private final FunctionCallExpr functionCallExpr;

    private QueryArrayItemClause arrayItemClause;

    private String alias;

    private SourceCodeRef aliasSourceCodeRef;

    private QueryPipeClause next;

    private Type typeScope;

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

        Type type = functionCallExpr.getType();
        if (arrayItemClause != null) {
            if (!(functionCallExpr.getType() instanceof ArrayType)) {
                context.addAnalyzerError(new NotArrayTypeError(sourceCodeRef, typeScope));
                return;
            }
            arrayItemClause.setTypeScope(functionCallExpr.getType());
            arrayItemClause.analyze(context);
            type = ((ArrayType)functionCallExpr.getType()).getElementType();
        }

        if (alias != null) {
            context.getCurrentScope().define(context.getAnalyzerContext(), new VariableSymbol(context.getModuleScope(),
                    aliasSourceCodeRef, alias, type));
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

        var fact = match.getFact();
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
                    result.add(match.setFact((RecordValueExpr) valueExpr, valueExpr, alias));
                } else {
                    result.add(match.setFact(valueExpr, alias));
                }
            }
        } else {
            if (valueExpr instanceof ArrayValueExpr) {
                List<ValueExpr> values = arrayItemClause.eval(match.getContext(), (ArrayValueExpr) valueExpr);
                for (ValueExpr value : values) {
                    if (value instanceof RecordValueExpr) {
                        result.add(match.setFact((RecordValueExpr) value, value, alias));
                    } else {
                        result.add(match.setFact(value, alias));
                    }
                }
            }
        }

        return result;
    }

    @Override
    public void createEmptyArray(EvalContext context) {
        if (alias == null) {
            if (next != null) {
                next.createEmptyArray(context);
            }
            return;
        }
        var returnType = functionCallExpr.getType();
        if (arrayItemClause == null) {
            context.getCurrentScope().setValue(alias, new ArrayValueExpr((ArrayType) returnType, new ArrayList<>()));
        } else {
            context.getCurrentScope().setValue(alias, new ArrayValueExpr(new ArrayType(returnType), new ArrayList<>()));
        }

        if (next != null) {
            next.createEmptyArray(context);
        }
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
    public QueryPipeClause getNext() {
        return next;
    }

    @Override
    public void setNext(QueryPipeClause next) {
        this.next = next;
    }

    @Override
    public void setAlias(String alias) {
        this.alias = alias;
    }

    @Override
    public void setAliasSourceCodeRef(SourceCodeRef aliasSourceCodeRef) {
        this.aliasSourceCodeRef = aliasSourceCodeRef;
    }

}
