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

package dev.kobu.interpreter.ast.eval.statement;

import dev.kobu.interpreter.ast.eval.Evaluable;
import dev.kobu.interpreter.ast.eval.Statement;
import dev.kobu.interpreter.ast.eval.context.ErrorValue;
import dev.kobu.interpreter.ast.eval.context.EvalContext;
import dev.kobu.interpreter.ast.symbol.ModuleScope;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.ast.symbol.Type;
import dev.kobu.interpreter.ast.symbol.VariableSymbol;

import java.util.List;
import java.util.Map;

public class CatchBlockStatement implements Statement {

    private final SourceCodeRef sourceCodeRef;

    private final SourceCodeRef varSourceCodeRef;

    private final ModuleScope moduleScope;

    private final String varName;

    private final Type errorType;

    private final List<Evaluable> block;

    private CatchBlockStatement nextCatch;

    public CatchBlockStatement(SourceCodeRef sourceCodeRef, SourceCodeRef varSourceCodeRef,
                               ModuleScope moduleScope,
                               String varName, Type errorType, List<Evaluable> block) {
        this.sourceCodeRef = sourceCodeRef;
        this.varSourceCodeRef = varSourceCodeRef;
        this.moduleScope = moduleScope;
        this.varName = varName;
        this.errorType = errorType;
        this.block = block;
    }

    public void setNextCatch(CatchBlockStatement nextCatch) {
        this.nextCatch = nextCatch;
    }

    @Override
    public SourceCodeRef getSourceCodeRef() {
        return sourceCodeRef;
    }

    @Override
    public void setResolvedTypes(Map<String, Type> resolvedTypes) {
        if (block != null) {
            block.forEach(evaluable -> evaluable.setResolvedTypes(resolvedTypes));
        }
        if (nextCatch != null) {
            nextCatch.setResolvedTypes(resolvedTypes);
        }
    }

    @Override
    public void analyze(EvalContext context) {
        context.pushNewScope();
        VariableSymbol varSymbol = new VariableSymbol(moduleScope, varSourceCodeRef, varName, errorType);
        context.getCurrentScope().define(context.getAnalyzerContext(), varSymbol);
        context.evalBlock(block);
        context.popScope();

        if (nextCatch != null) {
            nextCatch.analyze(context);
        }
    }

    @Override
    public void evalStat(EvalContext context) {
        if (context.getLastUserError() != null) {
            ErrorValue errorValue = context.getLastUserError().getErrorValue();
            if (errorType.isAssignableFrom(errorValue.getValue().getType())) {
                context.pushNewScope();
                try {
                    VariableSymbol varSymbol = new VariableSymbol(moduleScope, varSourceCodeRef, varName, errorType);
                    context.getCurrentScope().define(context.getAnalyzerContext(), varSymbol);
                    context.evalBlock(block);
                } finally {
                    context.popScope();
                    context.setLastUserError(null);
                }
                return;
            }
        }

        if (nextCatch != null) {
            nextCatch.evalStat(context);
        }
    }

}
