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

import dev.cgrscript.interpreter.ast.eval.expr.value.ArrayValueExpr;
import dev.cgrscript.interpreter.ast.symbol.*;
import dev.cgrscript.interpreter.ast.eval.EvalContext;
import dev.cgrscript.interpreter.ast.eval.Evaluable;
import dev.cgrscript.interpreter.error.analyzer.InvalidQueryType;

import java.util.ArrayList;

public class QueryTypeClause implements Evaluable {

    private static final String DEFAULT_BIND = "$_rootRecord";

    private final SourceCodeRef sourceCodeRef;

    private final Type type;

    private final boolean includeSubtypes;

    private final String bind;

    private QueryPipeClause pipeClause;

    public QueryTypeClause(SourceCodeRef sourceCodeRef, Type type, boolean includeSubtypes, String bind) {
        this.sourceCodeRef = sourceCodeRef;
        this.type = type;
        this.includeSubtypes = includeSubtypes;
        this.bind = bind != null ? bind : DEFAULT_BIND;
    }

    public Type getType() {
        return type;
    }

    public String getBind() {
        return bind;
    }

    public boolean includeSubtypes() {
        return includeSubtypes;
    }

    @Override
    public SourceCodeRef getSourceCodeRef() {
        return sourceCodeRef;
    }

    public QueryPipeClause getPipeClause() {
        return pipeClause;
    }

    public void setPipeClause(QueryPipeClause pipeClause) {
        this.pipeClause = pipeClause;
    }

    @Override
    public void analyze(EvalContext context) {
        if (!(type instanceof RecordTypeSymbol) && !(type instanceof TemplateTypeSymbol)) {
            context.getModuleScope().addError(new InvalidQueryType(sourceCodeRef, type));
            return;
        }
        VariableSymbol variableSymbol = new VariableSymbol(sourceCodeRef, bind, type);
        context.getCurrentScope().define(variableSymbol);

        if (pipeClause != null) {
            pipeClause.setTypeScope(type);
            pipeClause.analyze(context);
        }
    }

    public boolean compatibleWith(QueryTypeClause typeClause) {
        return type.isAssignableFrom(typeClause.getType());
    }

    public void createEmptyArray(EvalContext context) {

        VariableSymbol variableSymbol = new VariableSymbol(sourceCodeRef, bind, type);
        context.getCurrentScope().define(variableSymbol);
        context.getCurrentScope().setValue(bind, new ArrayValueExpr(new ArrayType(type), new ArrayList<>()));

        if (pipeClause != null) {
            pipeClause.createEmptyArray(context);
        }
    }

    public String getKey() {
        return type.getName() + "|" + bind;
    }

}
