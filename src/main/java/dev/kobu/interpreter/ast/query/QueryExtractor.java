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
import dev.kobu.interpreter.ast.eval.context.EvalContext;
import dev.kobu.interpreter.ast.symbol.ModuleScope;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.ast.symbol.Type;
import dev.kobu.interpreter.error.analyzer.InvalidExtractorError;

import java.util.List;
import java.util.Map;

public class QueryExtractor implements Matcher, Evaluable {

    private final ModuleScope moduleScope;

    private final SourceCodeRef sourceCodeRef;

    private final QueryClause queryClause;

    private Type typeScope;

    public QueryExtractor(ModuleScope moduleScope, SourceCodeRef sourceCodeRef, QueryClause queryClause) {
        this.moduleScope = moduleScope;
        this.sourceCodeRef = sourceCodeRef;
        this.queryClause = queryClause;
    }

    @Override
    public SourceCodeRef getSourceCodeRef() {
        return sourceCodeRef;
    }

    @Override
    public void setResolvedTypes(Map<String, Type> resolvedTypes) {
        queryClause.setResolvedTypes(resolvedTypes);
    }

    @Override
    public void analyze(EvalContext context) {
        if (!(queryClause instanceof QueryFieldClause)) {
            context.addAnalyzerError(new InvalidExtractorError(queryClause.getSourceCodeRef()));
            return;
        }

        queryClause.setExtractorMode();
        queryClause.setTypeScope(typeScope);
        queryClause.analyze(context);
    }

    @Override
    public List<Match> eval(Match match) {
        queryClause.setValueScope(match.getRootRecord());
        return queryClause.eval(match);
    }

    public QueryClause getQueryClause() {
        return queryClause;
    }

    public void setTypeScope(Type typeScope) {
        this.typeScope = typeScope;
    }
}
