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

package dev.cgrscript.interpreter.ast.eval.expr;

import dev.cgrscript.interpreter.ast.eval.*;
import dev.cgrscript.interpreter.ast.eval.expr.value.NullValueExpr;
import dev.cgrscript.interpreter.ast.eval.expr.value.NumberValueExpr;
import dev.cgrscript.interpreter.ast.symbol.*;
import dev.cgrscript.interpreter.error.analyzer.InvalidTypeError;
import dev.cgrscript.interpreter.error.analyzer.InvalidVariableError;
import dev.cgrscript.interpreter.error.analyzer.UndefinedFieldError;
import dev.cgrscript.interpreter.error.analyzer.UndefinedVariableError;
import dev.cgrscript.interpreter.error.eval.InternalInterpreterError;
import dev.cgrscript.interpreter.error.eval.NullPointerError;

public class PostIncDecExpr implements Statement, Expr, HasTypeScope, Assignment {

    private final SourceCodeRef sourceCodeRef;

    private final String varName;

    private final IncDecOperatorEnum operator;

    private Type typeScope;

    private ValueExpr valueScope;

    private Type type;

    public PostIncDecExpr(SourceCodeRef sourceCodeRef, String varName, IncDecOperatorEnum operator) {
        this.sourceCodeRef = sourceCodeRef;
        this.varName = varName;
        this.operator = operator;
    }

    @Override
    public SourceCodeRef getSourceCodeRef() {
        return sourceCodeRef;
    }

    @Override
    public void analyze(EvalContext context) {
        if (typeScope == null) {
            var symbol = context.getCurrentScope().resolve(varName);
            if (symbol == null) {
                context.getModuleScope().addError(new UndefinedVariableError(sourceCodeRef, varName));
                this.type = UnknownType.INSTANCE;
                return;
            }
            if (!(symbol instanceof VariableSymbol)) {
                context.getModuleScope().addError(new InvalidVariableError(sourceCodeRef, varName, symbol));
                this.type = UnknownType.INSTANCE;
                return;
            }
            var varType = ((VariableSymbol) symbol).getType();
            if (!(varType instanceof NumberTypeSymbol)) {
                var numberType = BuiltinScope.NUMBER_TYPE;
                context.getModuleScope().addError(new InvalidTypeError(sourceCodeRef, numberType, varType));
                this.type = UnknownType.INSTANCE;
                return;
            }

            this.type = varType;
        } else {
            var field = typeScope.resolveField(varName);
            if (field == null) {
                context.getModuleScope().addError(new UndefinedFieldError(sourceCodeRef, typeScope, varName));
                this.type = UnknownType.INSTANCE;
                return;
            }
            if (!(field instanceof NumberTypeSymbol)) {
                var numberType = BuiltinScope.NUMBER_TYPE;
                context.getModuleScope().addError(new InvalidTypeError(sourceCodeRef, numberType, field));
                this.type = UnknownType.INSTANCE;
                return;
            }
            this.type = field;
        }
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public ValueExpr evalExpr(EvalContext context) {
        return eval(context);
    }

    @Override
    public void evalStat(EvalContext context) {
        eval(context);
    }

    private ValueExpr eval(EvalContext context) {
        if (valueScope == null) {
            var symbol = context.getCurrentScope().resolve(varName);
            if (symbol == null) {
                throw new InternalInterpreterError("Variable not defined in scope",
                        sourceCodeRef);
            }
            if (!(symbol instanceof VariableSymbol)) {
                throw new InternalInterpreterError("Expected: Variable. Found: " + symbol.getClass().getName(),
                        sourceCodeRef);
            }

            var valueExpr = ((LocalScope) context.getCurrentScope()).getValue(symbol.getName());
            if (valueExpr == null || valueExpr instanceof NullValueExpr) {
                throw new NullPointerError(sourceCodeRef, sourceCodeRef);
            }
            if (valueExpr instanceof NumberValueExpr) {
                ValueExpr newValue = null;
                if (operator.equals(IncDecOperatorEnum.INC)) {
                    newValue = new NumberValueExpr(sourceCodeRef, ((NumberValueExpr)valueExpr).inc());
                } else {
                    newValue = new NumberValueExpr(sourceCodeRef, ((NumberValueExpr)valueExpr).dec());
                }
                context.getCurrentScope().setValue(varName, newValue);
                return valueExpr;
            }
            throw new InternalInterpreterError("Expected: Number. Found: " + valueExpr.getStringValue(), sourceCodeRef);
        } else {
            if (valueScope instanceof NullValueExpr) {
                throw new NullPointerError(valueScope.getSourceCodeRef(), valueScope.getSourceCodeRef());
            }
            if (valueScope instanceof HasFields) {
                var field = ((HasFields)valueScope).resolveField(varName);
                if (field == null || field instanceof NullValueExpr) {
                    return new NullValueExpr(sourceCodeRef);
                }
                ValueExpr valueExpr = field.evalExpr(context);
                if (valueExpr instanceof NumberValueExpr) {
                    ValueExpr newValue = null;
                    if (operator.equals(IncDecOperatorEnum.INC)) {
                        newValue = new NumberValueExpr(sourceCodeRef, ((NumberValueExpr)valueExpr).inc());
                    } else {
                        newValue = new NumberValueExpr(sourceCodeRef, ((NumberValueExpr)valueExpr).dec());
                    }
                    ((HasFields)valueScope).updateFieldValue(context, varName, newValue);
                    return valueExpr;
                }
                throw new InternalInterpreterError("Expected: Number. Found: " + valueExpr.getStringValue(), sourceCodeRef);
            }

            throw new InternalInterpreterError(
                    "Invalid value for this operation: " + valueScope.getStringValue(), sourceCodeRef);
        }
    }

    @Override
    public void setTypeScope(Type typeScope) {
        this.typeScope = typeScope;
    }

    @Override
    public void setValueScope(ValueExpr valueScope) {
        this.valueScope = valueScope;
    }
}
