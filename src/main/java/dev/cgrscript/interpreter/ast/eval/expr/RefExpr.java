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
import dev.cgrscript.interpreter.ast.eval.expr.value.ModuleRefValueExpr;
import dev.cgrscript.interpreter.ast.eval.expr.value.NullValueExpr;
import dev.cgrscript.interpreter.ast.eval.expr.value.RuleRefValueExpr;
import dev.cgrscript.interpreter.ast.symbol.*;
import dev.cgrscript.interpreter.error.analyzer.InvalidVariableError;
import dev.cgrscript.interpreter.error.analyzer.UndefinedFieldError;
import dev.cgrscript.interpreter.error.analyzer.UndefinedVariableError;
import dev.cgrscript.interpreter.error.eval.InternalInterpreterError;
import dev.cgrscript.interpreter.error.eval.NullPointerError;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class RefExpr implements Expr, HasTypeScope, MemoryReference, HasElementRef {

    private final ModuleScope moduleScope;

    private final SourceCodeRef sourceCodeRef;

    private final String varName;

    private Type typeScope;

    private ValueExpr valueScope;

    private Type type;

    private RuleSymbol ruleSymbol;

    private boolean assignMode;

    private SourceCodeRef elementRef;

    private Collection<SymbolDescriptor> symbolsInScope;

    public RefExpr(ModuleScope moduleScope, SourceCodeRef sourceCodeRef, String varName) {
        this.moduleScope = moduleScope;
        this.sourceCodeRef = sourceCodeRef;
        this.varName = varName;

        if (moduleScope.getEvalMode() == EvalModeEnum.ANALYZER_SERVICE) {
            if (varName.equals("")) {
                int offset = sourceCodeRef.getStartOffset()
                        + (sourceCodeRef.getEndOffset() - sourceCodeRef.getStartOffset()) + 1;
                moduleScope.registerRef(offset, this);
                moduleScope.registerAutoCompletionSource(offset, this);
            } else {
                moduleScope.registerRef(sourceCodeRef.getStartOffset(), this);
                moduleScope.registerAutoCompletionSource(sourceCodeRef.getStartOffset(), this);
            }
        }
    }

    @Override
    public void analyze(EvalContext context) {
        if (typeScope == null) {
            if (context.getEvalMode() != EvalModeEnum.EXECUTION) {
                this.symbolsInScope = context.getCurrentScope()
                        .getSymbolDescriptors(
                                SymbolTypeEnum.VARIABLE,
                                SymbolTypeEnum.FUNCTION,
                                SymbolTypeEnum.MODULE_REF,
                                SymbolTypeEnum.RULE,
                                SymbolTypeEnum.TEMPLATE,
                                SymbolTypeEnum.FILE,
                                SymbolTypeEnum.KEYWORD);
            }

            if (varName.equals("")) {
                return;
            }

            var symbol = context.getCurrentScope().resolve(varName);
            if (symbol == null) {
                context.addAnalyzerError(new UndefinedVariableError(sourceCodeRef, varName));
                this.type = UnknownType.INSTANCE;
                return;
            }
            if (symbol instanceof VariableSymbol) {
                this.elementRef = symbol.getSourceCodeRef();
                this.type = ((VariableSymbol) symbol).getType();
                return;
            }
            if (!assignMode) {
                if (symbol instanceof RuleSymbol) {
                    this.ruleSymbol = (RuleSymbol) symbol;
                    this.elementRef = this.ruleSymbol.getSourceCodeRef();
                    this.type = BuiltinScope.RULE_REF_TYPE;
                    return;
                }
                if (symbol instanceof ModuleRefSymbol) {
                    this.elementRef = symbol.getSourceCodeRef();
                    this.type = (ModuleRefSymbol) symbol;
                    return;
                }
            }

            context.addAnalyzerError(new InvalidVariableError(sourceCodeRef, varName, symbol));
            this.type = UnknownType.INSTANCE;

        } else {
            if (context.getEvalMode() != EvalModeEnum.EXECUTION) {
                List<SymbolDescriptor> symbols = new ArrayList<>();
                for (FieldDescriptor field : typeScope.getFields()) {
                    symbols.add(new SymbolDescriptor(field));
                }
                for (FunctionType method : typeScope.getMethods()) {
                    symbols.add(new SymbolDescriptor(method));
                }
                this.symbolsInScope = symbols;
            }

            if (varName.equals("")) {
                return;
            }

            var field = typeScope.resolveField(varName);
            if (field == null) {
                context.addAnalyzerError(new UndefinedFieldError(sourceCodeRef, typeScope, varName));
                this.type = UnknownType.INSTANCE;
                return;
            }
            this.elementRef = typeScope.getFieldRef(varName);
            this.type = field;
        }
    }

    @Override
    public ValueExpr evalExpr(EvalContext context) {
        if (ruleSymbol != null) {
            return new RuleRefValueExpr(sourceCodeRef, ruleSymbol);
        }
        if (valueScope == null) {
            var symbol = context.getCurrentScope().resolve(varName);
            if (symbol == null) {
                throw new InternalInterpreterError("Variable '" + varName + "' not defined in scope",
                        sourceCodeRef);
            }
            if (symbol instanceof VariableSymbol) {
                var valueExpr = context.getCurrentScope().getValue(symbol.getName());
                return Objects.requireNonNullElseGet(valueExpr, () -> new NullValueExpr(sourceCodeRef));
            }
            if (symbol instanceof ModuleRefSymbol) {
                ModuleRefSymbol moduleRefSymbol = (ModuleRefSymbol) symbol;
                return new ModuleRefValueExpr(moduleRefSymbol, moduleRefSymbol.getModuleScope());
            }

            throw new InternalInterpreterError("Expected: Variable. Found: " + symbol.getClass().getName(),
                    sourceCodeRef);

        } else {
            if (valueScope instanceof NullValueExpr) {
                throw new NullPointerError(valueScope.getSourceCodeRef(), valueScope.getSourceCodeRef());
            }
            if (valueScope instanceof HasFields) {
                var field = ((HasFields)valueScope).resolveField(varName);
                if (field == null || field instanceof NullValueExpr) {
                    return new NullValueExpr(sourceCodeRef);
                }
                return field.evalExpr(context);
            }

            throw new InternalInterpreterError(
                    "Invalid value for this operation: " + valueScope.getStringValue(), sourceCodeRef);

        }
    }

    @Override
    public SourceCodeRef getSourceCodeRef() {
        return sourceCodeRef;
    }

    @Override
    public Type getType() {
        return type;
    }

    public String getVarName() {
        return varName;
    }

    @Override
    public void setTypeScope(Type typeScope) {
        this.typeScope = typeScope;
    }

    @Override
    public void setValueScope(ValueExpr valueScope) {
        this.valueScope = valueScope;
    }

    @Override
    public void setAssignMode() {
        assignMode = true;
    }

    @Override
    public void assign(EvalContext context, ValueExpr value) {
        if (valueScope == null) {
            context.getCurrentScope().setValue(varName, value);
        } else {
            if (valueScope instanceof NullValueExpr) {
                throw new NullPointerError(valueScope.getSourceCodeRef(), valueScope.getSourceCodeRef());
            }
            if (valueScope instanceof HasFields) {
                ((HasFields)valueScope).updateFieldValue(context, varName, value);
                return;
            }
            throw new InternalInterpreterError("Can't update a field of: " + valueScope.getClass().getName(),
                    valueScope.getSourceCodeRef());
        }
    }

    @Override
    public SourceCodeRef getElementRef() {
        return elementRef;
    }

    @Override
    public List<SymbolDescriptor> requestSuggestions(List<ModuleScope> externalModules) {
        if (symbolsInScope == null) {
            return EMPTY_LIST;
        }
        var symbols = new ArrayList<>(symbolsInScope);
        if (symbolsInScope.isEmpty()) {
            symbols.addAll(getExternalSymbols(moduleScope, externalModules,
                    SymbolTypeEnum.FUNCTION, SymbolTypeEnum.RULE, SymbolTypeEnum.TEMPLATE, SymbolTypeEnum.FILE));
        }
        return symbols;
    }

    @Override
    public boolean hasOwnCompletionScope() {
        return false;
    }

}
