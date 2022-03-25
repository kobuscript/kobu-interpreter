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

import dev.kobu.interpreter.ast.eval.*;
import dev.kobu.interpreter.ast.eval.context.EvalContext;
import dev.kobu.interpreter.ast.eval.expr.value.ArrayValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.NullValueExpr;
import dev.kobu.interpreter.ast.symbol.*;
import dev.kobu.interpreter.ast.symbol.array.ArrayType;
import dev.kobu.interpreter.ast.symbol.array.ArrayTypeFactory;
import dev.kobu.interpreter.error.analyzer.InvalidTypeError;
import dev.kobu.interpreter.error.eval.NullPointerError;

import java.util.List;
import java.util.Map;

public class EnhancedForStatement implements Statement {

    private final SourceCodeRef sourceCodeRef;

    private final VariableSymbol itElemVar;

    private final Expr arrayExpr;

    private final List<Evaluable> block;

    public EnhancedForStatement(SourceCodeRef sourceCodeRef, VariableSymbol itElemVar, Expr arrayExpr, List<Evaluable> block) {
        this.sourceCodeRef = sourceCodeRef;
        this.itElemVar = itElemVar;
        this.arrayExpr = arrayExpr;
        this.block = block;
    }

    @Override
    public SourceCodeRef getSourceCodeRef() {
        return sourceCodeRef;
    }

    @Override
    public void setResolvedTypes(Map<String, Type> resolvedTypes) {
        if (arrayExpr != null) {
            arrayExpr.setResolvedTypes(resolvedTypes);
        }
        if (block != null) {
            block.forEach(evaluable -> evaluable.setResolvedTypes(resolvedTypes));
        }
    }

    @Override
    public void analyze(EvalContext context) {
        context.pushNewScope();
        var branch = context.pushNewBranch();
        branch.setCanInterrupt(true);

        try {
            arrayExpr.analyze(context);
            Type type = arrayExpr.getType();
            if (type instanceof UnknownType) {
                return;
            }
            if (!(type instanceof ArrayType)) {
                context.addAnalyzerError(new InvalidTypeError(arrayExpr.getSourceCodeRef(),
                        ArrayTypeFactory.getArrayTypeFor(BuiltinScope.ANY_TYPE), type));
                return;
            }
            Type elemType = ((ArrayType)type).getElementType();
            if (itElemVar.getType() != null) {
                if (!itElemVar.getType().isAssignableFrom(elemType)) {
                    context.addAnalyzerError(new InvalidTypeError(arrayExpr.getSourceCodeRef(),
                            ArrayTypeFactory.getArrayTypeFor(itElemVar.getType()), type));
                }
            } else {
                itElemVar.setType(elemType);
            }
            context.getCurrentScope().define(context.getAnalyzerContext(), itElemVar);

            context.analyzeBlock(block);
        } finally {
            branch = context.popBranch();
            branch.setHasReturnStatement(false);
            context.popScope();
        }
    }

    @Override
    public void evalStat(EvalContext context) {
        context.pushNewScope();
        context.pushNewBranch();

        try {
            context.getCurrentScope().define(context.getAnalyzerContext(), itElemVar);
            ValueExpr valueExpr = arrayExpr.evalExpr(context);
            if (valueExpr instanceof NullValueExpr) {
                throw new NullPointerError(sourceCodeRef, arrayExpr.getSourceCodeRef());
            }
            ArrayValueExpr arrayValue = (ArrayValueExpr) valueExpr;
            for (ValueExpr expr : arrayValue.getValue()) {
                context.getCurrentScope().setValue(itElemVar.getName(), expr);
                var interrupt = context.evalBlock(block);
                if (interrupt != null) {
                    if (interrupt == InterruptTypeEnum.BREAK) {
                        break;
                    }
                    context.getCurrentBranch().setInterrupt(null);
                }
            }

        } finally {
            context.popBranch();
            context.popScope();
        }
    }

}
