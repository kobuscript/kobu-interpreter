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

package dev.kobu.interpreter.ast.eval.context;

import dev.kobu.database.Database;
import dev.kobu.interpreter.ast.AnalyzerContext;
import dev.kobu.interpreter.ast.eval.RuleContext;
import dev.kobu.interpreter.ast.symbol.ModuleScope;
import dev.kobu.interpreter.ast.symbol.UserDefinedFunction;
import dev.kobu.interpreter.input.InputReader;
import dev.kobu.interpreter.writer.OutputWriter;

public class EvalContextProvider {

    private final EvalModeEnum evalMode;

    private final Database database;

    private final InputReader inputReader;

    private final OutputWriter outputWriter;

    public EvalContextProvider(EvalModeEnum evalMode,
                               Database database, InputReader inputReader, OutputWriter outputWriter) {
        this.evalMode = evalMode;
        this.database = database;
        this.inputReader = inputReader;
        this.outputWriter = outputWriter;
    }

    public EvalContext newEvalContext(AnalyzerContext analyzerContext, ModuleScope moduleScope) {
        return new EvalContext(this, analyzerContext, evalMode, moduleScope, database, inputReader, outputWriter);
    }

    public EvalContext newEvalContext(AnalyzerContext analyzerContext, ModuleScope moduleScope, UserDefinedFunction function) {
        return new EvalContext(this, analyzerContext, evalMode, moduleScope, database, inputReader, outputWriter, function);
    }

    public EvalContext newEvalContext(AnalyzerContext analyzerContext, ModuleScope moduleScope, RuleContext ruleContext) {
        return new EvalContext(this, analyzerContext, evalMode, moduleScope, database, inputReader, outputWriter, ruleContext);
    }

    public EvalContext newEvalContext(EvalContext evalContext) {
        return new EvalContext(this, evalContext.getAnalyzerContext(), evalContext.getEvalMode(),
                evalContext.getModuleScope(), evalContext.getDatabase(),
                evalContext.getInputParser(), evalContext.getOutputWriter());
    }

    public EvalContext newEvalContext(EvalContext evalContext, RuleContext ruleContext) {
        return new EvalContext(this, evalContext.getAnalyzerContext(), evalContext.getEvalMode(),
                evalContext.getModuleScope(), evalContext.getDatabase(),
                evalContext.getInputParser(), evalContext.getOutputWriter(), ruleContext);
    }

}
