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
import dev.kobu.interpreter.ast.symbol.*;
import dev.kobu.interpreter.ast.symbol.array.ArrayType;
import dev.kobu.interpreter.error.analyzer.InvalidJoinQueryType;
import dev.kobu.interpreter.error.analyzer.InvalidQueryType;

import java.util.Map;

public class QueryTypeClause implements Evaluable {

    private static final String DEFAULT_BIND = "$_rootRecord";

    private final ModuleScope moduleScope;

    private final SourceCodeRef sourceCodeRef;

    private final SourceCodeRef bindSourceCodeRef;

    private final Type type;

    private final boolean includeSubtypes;

    private String bind;

    private QueryClause queryClause;

    private Type queryType;

    private String mainRecordBind;

    private boolean joinMode;

    private boolean accumulator;

    public QueryTypeClause(ModuleScope moduleScope,
                           SourceCodeRef sourceCodeRef, SourceCodeRef bindSourceCodeRef,
                           Type type, boolean includeSubtypes, String bind) {
        this.moduleScope = moduleScope;
        this.sourceCodeRef = sourceCodeRef;
        this.bindSourceCodeRef = bindSourceCodeRef;
        this.type = type;
        this.includeSubtypes = includeSubtypes;
        this.bind = bind != null ? bind : DEFAULT_BIND;
    }

    public ModuleScope getModuleScope() {
        return moduleScope;
    }

    public Type getType() {
        return type;
    }

    public String getBind() {
        return bind;
    }

    public Type getQueryType() {
        return queryType;
    }

    public String getMainRecordBind() {
        return mainRecordBind;
    }

    public boolean includeSubtypes() {
        return includeSubtypes;
    }

    public boolean joinMode() {
        return joinMode;
    }

    public void setJoinMode(boolean joinMode) {
        if (joinMode && bind.equals(DEFAULT_BIND)) {
            this.bind = null;
        }
        this.joinMode = joinMode;
    }

    public boolean accumulator() {
        return accumulator;
    }

    @Override
    public SourceCodeRef getSourceCodeRef() {
        return sourceCodeRef;
    }

    @Override
    public void setResolvedTypes(Map<String, Type> resolvedTypes) {
        if (queryClause != null) {
            queryClause.setResolvedTypes(resolvedTypes);
        }
    }

    public QueryClause getQueryClause() {
        return queryClause;
    }

    public void setQueryClause(QueryClause queryClause) {
        this.queryClause = queryClause;
    }

    @Override
    public void analyze(EvalContext context) {
        if (!(type instanceof RecordTypeSymbol) && !(type instanceof AnyTemplateTypeSymbol) &&
                !(type instanceof TemplateTypeSymbol)) {
            if (joinMode) {
                if (!(type instanceof ArrayType)) {
                    context.addAnalyzerError(new InvalidJoinQueryType(sourceCodeRef, type));
                    return;
                }
                Type elemType = ((ArrayType)type).getElementType();
                if (!(elemType instanceof RecordTypeSymbol) && !(elemType instanceof AnyTemplateTypeSymbol) &&
                        !(elemType instanceof TemplateTypeSymbol)) {
                    context.addAnalyzerError(new InvalidJoinQueryType(sourceCodeRef, type));
                    return;
                }
                accumulator = true;
            } else {
                context.addAnalyzerError(new InvalidQueryType(sourceCodeRef, type));
                return;
            }
        }
        VariableSymbol variableSymbol = null;
        if (bindSourceCodeRef != null) {
            variableSymbol = new VariableSymbol(context.getModuleScope(), bindSourceCodeRef, bind, type);
        } else if (bind != null) {
            variableSymbol = new VariableSymbol(context.getModuleScope(), bind, type);
        }

        if (variableSymbol != null) {
            context.getCurrentScope().define(context.getAnalyzerContext(), variableSymbol);
        }

        if (queryClause != null) {
            queryClause.setTypeScope(type);
            queryClause.analyze(context);

            var clause = queryClause;
            while (clause != null) {
                queryType = clause.getType();
                mainRecordBind = clause.getBind();
                clause = clause.getNext();
            }
        } else {
            queryType = type;
            mainRecordBind = bind;
        }
    }

    public boolean compatibleWith(QueryTypeClause typeClause) {
        if (queryType == null || typeClause == null) {
            return true;
        }
        return queryType.isAssignableFrom(typeClause.getQueryType());
    }

    public String getKey() {
        return type.getName() + "|" + bind;
    }

}
