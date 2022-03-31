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
import dev.kobu.interpreter.ast.eval.context.EvalContext;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.ast.symbol.Type;
import dev.kobu.interpreter.error.eval.UserDefinedError;

import java.util.List;
import java.util.Map;

public class TryCatchStatement implements Statement {

    private final SourceCodeRef sourceCodeRef;

    private final List<Evaluable> block;

    private final CatchBlockStatement catchBlock;

    public TryCatchStatement(SourceCodeRef sourceCodeRef, List<Evaluable> block, CatchBlockStatement catchBlock) {
        this.sourceCodeRef = sourceCodeRef;
        this.block = block;
        this.catchBlock = catchBlock;
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
        if (catchBlock != null) {
            catchBlock.setResolvedTypes(resolvedTypes);
        }
    }

    @Override
    public void analyze(EvalContext context) {
        context.analyzeBlock(block);

        if (catchBlock != null) {
            catchBlock.analyze(context);
        }
    }

    @Override
    public void evalStat(EvalContext context) {
        try {
            context.evalBlock(block);
        } catch (UserDefinedError error) {
            context.setLastUserError(error);
            if (catchBlock != null) {
                catchBlock.evalStat(context);
            }
        }
    }

}
