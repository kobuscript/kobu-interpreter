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

package dev.cgrscript.interpreter.ast.eval.statement;

import dev.cgrscript.interpreter.ast.eval.*;
import dev.cgrscript.interpreter.ast.eval.context.EvalContext;
import dev.cgrscript.interpreter.ast.symbol.ModuleRefSymbol;
import dev.cgrscript.interpreter.error.analyzer.InvalidAssignExprTypeError;
import dev.cgrscript.interpreter.error.eval.InternalInterpreterError;
import dev.cgrscript.interpreter.error.analyzer.InvalidAssignmentError;
import dev.cgrscript.interpreter.ast.symbol.SourceCodeRef;
import dev.cgrscript.interpreter.ast.symbol.UnknownType;

public class AssignElemValueStatement implements Statement, Assignment {

    private final SourceCodeRef sourceCodeRef;

    private final Expr exprLeft;

    private final Expr exprRight;

    public AssignElemValueStatement(SourceCodeRef sourceCodeRef, Expr exprLeft, Expr exprRight) {
        this.sourceCodeRef = sourceCodeRef;
        this.exprLeft = exprLeft;
        this.exprRight = exprRight;
    }

    public Expr getExprLeft() {
        return exprLeft;
    }

    public Expr getExprRight() {
        return exprRight;
    }

    @Override
    public void analyze(EvalContext context) {
        if (!(exprLeft instanceof MemoryReference)) {
            context.addAnalyzerError(new InvalidAssignmentError(sourceCodeRef, exprLeft));
            return;
        }
        ((MemoryReference)exprLeft).setAssignMode();
        exprLeft.analyze(context);
        var type = exprLeft.getType();
        if (type instanceof UnknownType) {
            return;
        }
        if (exprRight instanceof HasTargetType) {
            ((HasTargetType)exprRight).setTargetType(type);
        }
        exprRight.analyze(context);

        if (exprRight.getType() instanceof ModuleRefSymbol || !type.isAssignableFrom(exprRight.getType())) {
            context.addAnalyzerError(new InvalidAssignExprTypeError(exprRight.getSourceCodeRef(),
                    type, exprRight.getType()));
        }
    }

    @Override
    public void evalStat(EvalContext context) {
        if (exprLeft instanceof MemoryReference) {
            ((MemoryReference)exprLeft).assign(context, exprRight.evalExpr(context));
            return;
        }

        throw new InternalInterpreterError("Can't assign a value to: " + exprLeft.getClass().getName(),
                exprLeft.getSourceCodeRef());
    }

    @Override
    public SourceCodeRef getSourceCodeRef() {
        return sourceCodeRef;
    }

}
