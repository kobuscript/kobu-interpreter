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

package dev.cgrscript.interpreter.ast.symbol;

import dev.cgrscript.interpreter.ast.eval.*;
import dev.cgrscript.database.Database;
import dev.cgrscript.interpreter.error.analyzer.FunctionMissingReturnStatError;
import dev.cgrscript.interpreter.input.InputReader;
import dev.cgrscript.interpreter.writer.OutputWriter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FunctionSymbol extends Symbol implements FunctionType, HasExpr {

    private final ModuleScope moduleScope;

    private final SourceCodeRef closeFunctionRef;

    private List<FunctionParameter> parameters;

    private Type returnType;

    private List<Evaluable> exprList;

    private Collection<SymbolDescriptor> symbolsModule;

    public FunctionSymbol(SourceCodeRef sourceCodeRef, SourceCodeRef closeFunctionRef, ModuleScope moduleScope, String name) {
        super(moduleScope, sourceCodeRef, name);
        this.closeFunctionRef = closeFunctionRef;
        this.moduleScope = moduleScope;

        moduleScope.registerAutoCompletionSource(closeFunctionRef.getStartOffset(), new AutoCompletionSource() {
            @Override
            public List<SymbolDescriptor> requestSuggestions(List<ModuleScope> externalModules) {
                return new ArrayList<>(symbolsModule);
            }

            @Override
            public boolean hasOwnCompletionScope() {
                return false;
            }
        });
    }

    @Override
    public Type getReturnType() {
        return returnType;
    }

    public List<FunctionParameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<FunctionParameter> parameters) {
        this.parameters = parameters;
    }

    public void setReturnType(Type returnType) {
        this.returnType = returnType;
    }

    public List<Evaluable> getExprList() {
        return exprList;
    }

    public void setExprList(List<Evaluable> exprList) {
        this.exprList = exprList;
    }

    @Override
    public void analyze(EvalModeEnum evalMode, Database database, InputReader inputReader, OutputWriter outputWriter) {
        var context = new EvalContext(evalMode, moduleScope, database, inputReader, outputWriter, this);
        var scope = context.getCurrentScope();

        symbolsModule = scope.getSymbolDescriptors(
                        SymbolTypeEnum.FUNCTION,
                        SymbolTypeEnum.MODULE,
                        SymbolTypeEnum.RULE,
                        SymbolTypeEnum.TEMPLATE,
                        SymbolTypeEnum.FILE,
                        SymbolTypeEnum.KEYWORD);

        for (FunctionParameter parameter : parameters) {
            VariableSymbol variableSymbol = new VariableSymbol(moduleScope, parameter.getSourceCodeRef(), parameter.getName(),
                    parameter.getType());
            scope.define(variableSymbol);
        }
        var branch = context.pushNewBranch();
        context.analyzeBlock(exprList);

        if (returnType != null && !branch.hasReturnStatement()) {
            context.getModuleScope().addError(new FunctionMissingReturnStatError(closeFunctionRef));
        }

        context.popBranch();
    }

    public ValueExpr eval(EvalModeEnum evalMode, List<ValueExpr> args, Database database, InputReader inputReader, OutputWriter outputWriter) {
        var context = new EvalContext(evalMode, moduleScope, database, inputReader, outputWriter, this);
        var scope = context.getCurrentScope();
        for (int i = 0; i < parameters.size(); i++) {
            FunctionParameter parameter = parameters.get(i);
            ValueExpr arg = i < args.size() ? args.get(i) : null;

            VariableSymbol variableSymbol = new VariableSymbol(moduleScope, parameter.getSourceCodeRef(), parameter.getName(),
                    parameter.getType());
            scope.define(variableSymbol);
            if (arg != null) {
                scope.setValue(parameter.getName(), arg);
            }
        }
        context.evalBlock(exprList);
        return context.getReturnValue();
    }

}
