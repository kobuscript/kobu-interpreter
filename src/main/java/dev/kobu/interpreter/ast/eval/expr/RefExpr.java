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

package dev.kobu.interpreter.ast.eval.expr;

import dev.kobu.interpreter.ast.eval.*;
import dev.kobu.interpreter.ast.eval.context.EvalContext;
import dev.kobu.interpreter.ast.eval.context.EvalModeEnum;
import dev.kobu.interpreter.ast.eval.expr.value.*;
import dev.kobu.interpreter.ast.symbol.*;
import dev.kobu.interpreter.error.analyzer.InvalidVariableError;
import dev.kobu.interpreter.error.analyzer.UndefinedFieldError;
import dev.kobu.interpreter.error.analyzer.UndefinedSymbolError;
import dev.kobu.interpreter.error.eval.InternalInterpreterError;
import dev.kobu.interpreter.error.eval.NullPointerError;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class RefExpr implements Expr, HasTypeScope, MemoryReference, HasElementRef, UndefinedSymbolNotifier {

    private final ModuleScope moduleScope;

    private final SourceCodeRef sourceCodeRef;

    private final String symbolName;

    private Type typeScope;

    private ValueExpr valueScope;

    private Type type;

    private RuleSymbol ruleSymbol;

    private RecordTypeSymbol recordTypeSymbol;

    private KobuFunction function;

    private boolean assignMode;

    private SourceCodeRef elementRef;

    private Collection<SymbolDescriptor> symbolsInScope;

    private UndefinedSymbolListener undefinedSymbolListener;

    public RefExpr(ModuleScope moduleScope, SourceCodeRef sourceCodeRef, String symbolName) {
        this.moduleScope = moduleScope;
        this.sourceCodeRef = sourceCodeRef;
        this.symbolName = symbolName;

        if (moduleScope.getEvalMode() == EvalModeEnum.ANALYZER_SERVICE) {
            if (symbolName.equals("")) {
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
            if (context.getEvalMode() == EvalModeEnum.ANALYZER_SERVICE) {
                this.symbolsInScope = context.getCurrentScope()
                        .getSymbolDescriptors(
                                SymbolTypeEnum.VARIABLE,
                                SymbolTypeEnum.FUNCTION,
                                SymbolTypeEnum.MODULE_REF,
                                SymbolTypeEnum.RULE,
                                SymbolTypeEnum.TEMPLATE,
                                SymbolTypeEnum.FILE,
                                SymbolTypeEnum.KEYWORD,
                                SymbolTypeEnum.TYPE);
            }

            if (symbolName.equals("")) {
                return;
            }

            var symbol = context.getCurrentScope().resolve(symbolName);
            if (symbol == null) {
                if (undefinedSymbolListener != null) {
                    undefinedSymbolListener.onUndefinedSymbol(context, symbolName);
                } else {
                    context.addAnalyzerError(new UndefinedSymbolError(sourceCodeRef, symbolName));
                }
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
                if (symbol instanceof RecordTypeSymbol) {
                    this.recordTypeSymbol = (RecordTypeSymbol) symbol;
                    this.elementRef = this.recordTypeSymbol.getSourceCodeRef();
                    this.type = BuiltinScope.RECORD_TYPE_REF_TYPE;
                    return;
                }
                if (symbol instanceof ModuleRefSymbol) {
                    this.elementRef = symbol.getSourceCodeRef();
                    this.type = (ModuleRefSymbol) symbol;
                    return;
                }
                if (symbol instanceof FunctionSymbol) {
                    this.elementRef = symbol.getSourceCodeRef();
                    this.function = (KobuFunction) symbol;
                    this.type = this.function.getType();
                    return;
                }
            }

            context.addAnalyzerError(new InvalidVariableError(sourceCodeRef, symbolName, symbol));
            this.type = UnknownType.INSTANCE;

        } else {
            if (context.getEvalMode() == EvalModeEnum.ANALYZER_SERVICE) {
                List<SymbolDescriptor> symbols = new ArrayList<>();
                for (FieldDescriptor field : typeScope.getFields()) {
                    symbols.add(new SymbolDescriptor(field));
                }
                for (NamedFunction method : typeScope.getMethods()) {
                    symbols.add(new SymbolDescriptor(method));
                }
                this.symbolsInScope = symbols;
            }

            if (symbolName.equals("")) {
                return;
            }

            var field = typeScope.resolveField(symbolName);
            if (field == null) {
                if (!assignMode) {
                    var method = typeScope.resolveMethod(symbolName);
                    if (method != null) {
                        this.function = method;
                        this.type = method.getType();
                        return;
                    }
                }

                if (undefinedSymbolListener != null) {
                    undefinedSymbolListener.onUndefinedSymbol(context, typeScope, symbolName);
                } else {
                    context.addAnalyzerError(new UndefinedFieldError(sourceCodeRef, typeScope, symbolName));
                }
                this.type = UnknownType.INSTANCE;
                return;
            }
            this.elementRef = typeScope.getFieldRef(symbolName);
            this.type = field;
        }
    }

    @Override
    public ValueExpr evalExpr(EvalContext context) {
        if (ruleSymbol != null) {
            return new RuleRefValueExpr(sourceCodeRef, ruleSymbol);
        }
        if (recordTypeSymbol != null) {
            return new RecordTypeRefValueExpr(sourceCodeRef, recordTypeSymbol);
        }
        if (function != null) {
            return new FunctionRefValueExpr(sourceCodeRef, function, valueScope);
        }
        if (valueScope == null) {
            var symbol = context.getCurrentScope().resolve(symbolName);
            if (symbol == null) {
                throw new InternalInterpreterError("Variable '" + symbolName + "' not defined in scope",
                        sourceCodeRef);
            }
            if (symbol instanceof VariableSymbol) {
                var valueExpr = context.getCurrentScope().getValue(symbol.getName());
                return Objects.requireNonNullElseGet(valueExpr, () -> new NullValueExpr(sourceCodeRef));
            }
            if (symbol instanceof ModuleRefSymbol) {
                ModuleRefSymbol moduleRefSymbol = (ModuleRefSymbol) symbol;
                return new ModuleRefValueExpr(moduleRefSymbol, moduleRefSymbol.getModuleScopeRef());
            }

            throw new InternalInterpreterError("Expected: Variable. Found: " + symbol.getClass().getName(),
                    sourceCodeRef);

        } else {
            if (valueScope instanceof NullValueExpr) {
                throw new NullPointerError(valueScope.getSourceCodeRef(), valueScope.getSourceCodeRef());
            }
            if (valueScope instanceof HasFields) {
                var field = ((HasFields)valueScope).resolveField(symbolName);
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

    public String getSymbolName() {
        return symbolName;
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
            context.getCurrentScope().setValue(symbolName, value);
        } else {
            if (valueScope instanceof NullValueExpr) {
                throw new NullPointerError(valueScope.getSourceCodeRef(), valueScope.getSourceCodeRef());
            }
            if (valueScope instanceof HasFields) {
                ((HasFields)valueScope).updateFieldValue(context, symbolName, value);
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
        if (typeScope == null) {
            symbols.addAll(getExternalSymbols(moduleScope, externalModules,
                    SymbolTypeEnum.FUNCTION, SymbolTypeEnum.RULE,
                    SymbolTypeEnum.TEMPLATE, SymbolTypeEnum.FILE,
                    SymbolTypeEnum.TYPE));
        }
        return symbols;
    }

    @Override
    public boolean hasOwnCompletionScope() {
        return false;
    }

    @Override
    public void registerUndefinedSymbolListener(UndefinedSymbolListener listener) {
        this.undefinedSymbolListener = listener;
    }

}