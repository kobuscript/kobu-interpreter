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

package dev.kobu.interpreter.ast.eval.function;

import dev.kobu.interpreter.ast.eval.context.EvalContext;
import dev.kobu.interpreter.ast.eval.ValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.NullValueExpr;
import dev.kobu.interpreter.ast.symbol.FunctionParameter;
import dev.kobu.interpreter.ast.symbol.FunctionType;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.error.EvalError;
import dev.kobu.interpreter.error.eval.BuiltinFunctionError;
import dev.kobu.interpreter.error.eval.IllegalArgumentError;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class BuiltinFunction {

    private FunctionType funcDef;

    public void setFuncDef(FunctionType funcDef) {
        this.funcDef = funcDef;
    }

    public ValueExpr run(EvalContext context, List<ValueExpr> args, SourceCodeRef sourceCodeRef) {
        try {
            return run(context, toMap(args, sourceCodeRef), sourceCodeRef);
        } catch (EvalError err) {
            throw err;
        } catch (Throwable t) {
            throw new BuiltinFunctionError(t, sourceCodeRef);
        }
    }

    public ValueExpr run(EvalContext context, ValueExpr object, List<ValueExpr> args, SourceCodeRef sourceCodeRef) {
        try {
            return run(context, object, toMap(args, sourceCodeRef), sourceCodeRef);
        } catch (EvalError err) {
            throw err;
        } catch (Throwable t) {
            throw new BuiltinFunctionError(t, sourceCodeRef);
        }
    }

    public FunctionType getFuncDef() {
        return funcDef;
    }

    protected abstract ValueExpr run(EvalContext context, Map<String, ValueExpr> args, SourceCodeRef sourceCodeRef);

    protected abstract ValueExpr run(EvalContext context, ValueExpr object, Map<String, ValueExpr> args, SourceCodeRef sourceCodeRef);

    public abstract String getDocumentation();

    private Map<String, ValueExpr> toMap(List<ValueExpr> args, SourceCodeRef sourceCodeRef) {
        Map<String, ValueExpr> mapArgs = new HashMap<>();
        for (int i = 0; i < funcDef.getParameters().size() && i < args.size(); i++) {
            FunctionParameter param = funcDef.getParameters().get(i);
            ValueExpr value = args.get(i);
            if (!param.isOptional() && (value == null || value instanceof NullValueExpr)) {
                throw new IllegalArgumentError("Parameter '" + param.getName() + "' of function '"
                        + funcDef.getName() + "' cannot be null.",
                        sourceCodeRef);
            }
            if (!(value instanceof NullValueExpr)) {
                mapArgs.put(param.getName(), value);
            }
        }
        return mapArgs;
    }

}
