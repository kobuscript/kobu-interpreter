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

package dev.kobu.interpreter.ast.eval.expr.value;

import dev.kobu.interpreter.ast.AnalyzerContext;
import dev.kobu.interpreter.ast.eval.Evaluable;
import dev.kobu.interpreter.ast.eval.LocalScope;
import dev.kobu.interpreter.ast.eval.ValueExpr;
import dev.kobu.interpreter.ast.eval.context.EvalContext;
import dev.kobu.interpreter.ast.eval.context.EvalContextProvider;
import dev.kobu.interpreter.ast.eval.context.SnapshotValue;
import dev.kobu.interpreter.ast.symbol.*;
import dev.kobu.interpreter.ast.symbol.function.FunctionParameter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnonymousFunctionValueExpr implements ValueExpr, UserDefinedFunction {

    private final AnonymousFunctionDefinitionExpr definition;

    private final ModuleScope moduleScope;

    private final LocalScope evalScope;

    public AnonymousFunctionValueExpr(AnonymousFunctionDefinitionExpr definition, ModuleScope moduleScope, LocalScope evalScope) {
        this.definition = definition;
        this.moduleScope = moduleScope;
        this.evalScope = evalScope;
    }

    @Override
    public SourceCodeRef getSourceCodeRef() {
        return definition.getSourceCodeRef();
    }

    @Override
    public Map<String, Type> providedTypeArguments() {
        return new HashMap<>();
    }

    @Override
    public List<FunctionParameter> getParameters() {
        return definition.getParameters();
    }

    @Override
    public Type getReturnType() {
        return definition.getReturnType();
    }

    @Override
    public void analyze(EvalContext context) {

    }

    @Override
    public ValueExpr evalExpr(EvalContext context) {
        return this;
    }

    @Override
    public ValueExpr eval(AnalyzerContext analyzerContext, EvalContextProvider evalContextProvider, List<ValueExpr> args) {
        var context = evalContextProvider.newEvalContext(analyzerContext, moduleScope, this);
        var scope = context.getCurrentScope();
        scope.addAll(evalScope);

        for (int i = 0; i < definition.getParameters().size(); i++) {
            FunctionParameter parameter = definition.getParameters().get(i);
            ValueExpr arg = i < args.size() ? args.get(i) : null;

            VariableSymbol variableSymbol = new VariableSymbol(moduleScope, parameter.getSourceCodeRef(), parameter.getName(),
                    parameter.getType());
            scope.define(analyzerContext, variableSymbol);
            if (arg != null) {
                scope.setValue(parameter.getName(), arg);
            }
        }
        context.evalBlock(definition.getBlock());
        return context.getReturnValue();
    }

    @Override
    public Type getType() {
        return definition.getType();
    }

    @Override
    public String getStringValue() {
        return definition.getType().getName();
    }

    @Override
    public SnapshotValue getSnapshotValue() {
        return null;
    }

    @Override
    public SourceCodeRef getCloseBlockSourceRef() {
        return definition.getCloseBlockSourceRef();
    }

    @Override
    public List<Evaluable> getBlock() {
        return definition.getBlock();
    }

    @Override
    public boolean inferReturnType() {
        return false;
    }

}
