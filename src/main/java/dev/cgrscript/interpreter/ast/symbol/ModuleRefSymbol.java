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

import dev.cgrscript.interpreter.ast.eval.FieldDescriptor;
import dev.cgrscript.interpreter.ast.eval.ValueExpr;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ModuleRefSymbol extends Symbol implements Type {

    private final ModuleScope moduleScope;

    public ModuleRefSymbol(SourceCodeRef sourceCodeRef, ModuleScope moduleScope) {
        super(sourceCodeRef, "$" + moduleScope.getModuleId());
        this.moduleScope = moduleScope;
    }

    public ModuleScope getModuleScope() {
        return moduleScope;
    }

    @Override
    public String getIdentifier() {
        return getName();
    }

    @Override
    public List<FieldDescriptor> getFields() {
        List<FieldDescriptor> fields = new ArrayList<>();
        for (Symbol symbol : moduleScope.getSymbols()) {
            if (symbol instanceof RuleSymbol) {
                fields.add(new FieldDescriptor(symbol.getName(), ((RuleSymbol)symbol).getRuleType().name()));
            }
        }
        return fields;
    }

    @Override
    public List<FunctionType> getMethods() {
        return null;
    }

    @Override
    public Type resolveField(String name) {
        var symbol = moduleScope.resolveLocal(name);
        if (symbol instanceof RuleSymbol) {
            return BuiltinScope.RULE_REF_TYPE;
        }
        return null;
    }

    @Override
    public SourceCodeRef getFieldRef(String name) {
        return null;
    }

    @Override
    public FunctionType resolveMethod(String name) {
        var symbol = moduleScope.resolveLocal(name);
        if (symbol instanceof FunctionType) {
            return (FunctionType) symbol;
        }
        return null;
    }

    @Override
    public boolean isAssignableFrom(Type type) {
        return false;
    }

    @Override
    public Type getCommonSuperType(Type type) {
        return null;
    }

    @Override
    public Comparator<ValueExpr> getComparator() {
        return null;
    }
}
