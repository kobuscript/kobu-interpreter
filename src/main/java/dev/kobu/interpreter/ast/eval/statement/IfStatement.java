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

import dev.kobu.interpreter.ast.eval.context.EvalContext;
import dev.kobu.interpreter.ast.eval.expr.value.BooleanValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.NullValueExpr;
import dev.kobu.interpreter.ast.symbol.*;
import dev.kobu.interpreter.ast.symbol.value.BooleanTypeSymbol;
import dev.kobu.interpreter.error.eval.InternalInterpreterError;
import dev.kobu.interpreter.error.analyzer.InvalidTypeError;
import dev.kobu.interpreter.error.eval.NullPointerError;
import dev.kobu.interpreter.ast.eval.Evaluable;
import dev.kobu.interpreter.ast.eval.Expr;
import dev.kobu.interpreter.ast.eval.Statement;
import dev.kobu.interpreter.ast.eval.ValueExpr;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class IfStatement implements Statement {

    private final SourceCodeRef sourceCodeRef;

    private final Expr condExpr;

    private final List<Evaluable> block;

    private ElseIfStatement elseIf;

    private List<Evaluable> elseBlock;

    public IfStatement(SourceCodeRef sourceCodeRef, Expr condExpr, List<Evaluable> block) {
        this.sourceCodeRef = sourceCodeRef;
        this.condExpr = condExpr;
        this.block = block;
    }

    public Expr getCondExpr() {
        return condExpr;
    }

    public List<Evaluable> getBlock() {
        return block;
    }

    public ElseIfStatement getElseIf() {
        return elseIf;
    }

    public void setElseIf(ElseIfStatement elseIf) {
        this.elseIf = elseIf;
    }

    public List<Evaluable> getElseBlock() {
        return elseBlock;
    }

    public void setElseBlock(List<Evaluable> elseBlock) {
        this.elseBlock = elseBlock;
    }

    @Override
    public void analyze(EvalContext context) {
        if (condExpr == null) {
            return;
        }

        condExpr.analyze(context);
        if (condExpr.getType() instanceof UnknownType) {
            return;
        }
        if (!(condExpr.getType() instanceof BooleanTypeSymbol)) {
            BooleanTypeSymbol booleanType = BuiltinScope.BOOLEAN_TYPE;
            context.addAnalyzerError(new InvalidTypeError(condExpr.getSourceCodeRef(),
                    booleanType, condExpr.getType()));
        }

        var branch = context.pushNewBranch();
        context.analyzeBlock(block);

        if (elseIf != null) {
            elseIf.analyze(context);
        }

        if (elseBlock != null) {
            context.analyzeBlock(elseBlock);
        }

        if (elseBlock == null) {
            branch.setHasReturnStatement(false);
        }

        context.popBranch();
    }

    @Override
    public void evalStat(EvalContext context) {
        ValueExpr condValueExpr = condExpr.evalExpr(context);
        boolean condValue;
        if (condValueExpr instanceof NullValueExpr) {
            condValue = false;
        } else if (condValueExpr instanceof BooleanValueExpr) {
            condValue = ((BooleanValueExpr) condValueExpr).getValue();
        } else {
            throw new InternalInterpreterError("Expected: boolean. Found: " + condValueExpr.getStringValue(new HashSet<>()),
                    condExpr.getSourceCodeRef());
        }

        if (condValue) {
            context.evalBlock(block);
        } else if (elseIf != null) {
            ElseIfStatement match = elseIf.findMatch(context);
            if (match != null) {
                match.evalStat(context);
            } else if (elseBlock != null) {
                context.evalBlock(elseBlock);
            }
        } else if (elseBlock != null) {
            context.evalBlock(elseBlock);
        }

    }

    @Override
    public SourceCodeRef getSourceCodeRef() {
        return sourceCodeRef;
    }

    @Override
    public void setResolvedTypes(Map<String, Type> resolvedTypes) {
        if (condExpr != null) {
            condExpr.setResolvedTypes(resolvedTypes);
        }
        if (block != null) {
            block.forEach(evaluable -> evaluable.setResolvedTypes(resolvedTypes));
        }
        if (elseIf != null) {
            elseIf.setResolvedTypes(resolvedTypes);
        }
        if (elseBlock != null) {
            elseBlock.forEach(evaluable -> evaluable.setResolvedTypes(resolvedTypes));
        }
    }
}
