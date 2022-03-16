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

import dev.cgrscript.interpreter.ast.eval.context.EvalContext;
import dev.cgrscript.interpreter.ast.eval.Expr;
import dev.cgrscript.interpreter.ast.eval.HasTargetType;
import dev.cgrscript.interpreter.ast.eval.Statement;
import dev.cgrscript.interpreter.ast.symbol.UnknownType;
import dev.cgrscript.interpreter.error.analyzer.FunctionMissingReturnValueError;
import dev.cgrscript.interpreter.error.analyzer.InvalidReturnTypeError;
import dev.cgrscript.interpreter.error.analyzer.ReturnStatInVoidFunctionError;
import dev.cgrscript.interpreter.ast.symbol.SourceCodeRef;

public class ReturnStatement implements Statement {

    private final SourceCodeRef sourceCodeRef;

    private final Expr expr;

    public ReturnStatement(SourceCodeRef sourceCodeRef, Expr expr) {
        this.sourceCodeRef = sourceCodeRef;
        this.expr = expr;
    }

    public Expr getExpr() {
        return expr;
    }

    @Override
    public void analyze(EvalContext context) {
        context.getCurrentBranch().setHasReturnStatement(true);
        var functionType = context.getFunction();
        if ((functionType == null || functionType.getReturnType() == null) && expr != null) {
            context.addAnalyzerError(new ReturnStatInVoidFunctionError(sourceCodeRef, functionType));
            return;
        } else if (functionType != null && functionType.getReturnType() != null && expr == null) {
            context.addAnalyzerError(new FunctionMissingReturnValueError(sourceCodeRef, functionType));
            return;
        }
        if (expr == null) {
            return;
        }
        if (expr instanceof HasTargetType) {
            ((HasTargetType)expr).setTargetType(functionType.getReturnType());
        }
        expr.analyze(context);
        if (expr.getType() instanceof UnknownType) {
            return;
        }
        if (expr.getType() != null && !functionType.getReturnType().isAssignableFrom(expr.getType())) {
            context.addAnalyzerError(new InvalidReturnTypeError(sourceCodeRef, functionType, expr.getType()));
        }
    }

    @Override
    public void evalStat(EvalContext context) {
        context.getCurrentBranch().setHasReturnStatement(true);
        if (expr != null) {
            var value = expr.evalExpr(context);
            context.setReturnValue(value);
        }
    }

    @Override
    public SourceCodeRef getSourceCodeRef() {
        return sourceCodeRef;
    }
}
