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

package dev.kobu.interpreter.ast.symbol;

import dev.kobu.interpreter.ast.AnalyzerContext;
import dev.kobu.interpreter.ast.eval.FieldDescriptor;
import dev.kobu.interpreter.ast.eval.SymbolDocumentation;
import dev.kobu.interpreter.ast.eval.SymbolTypeEnum;
import dev.kobu.interpreter.ast.eval.context.EvalContextProvider;
import dev.kobu.interpreter.ast.eval.context.EvalModeEnum;
import dev.kobu.interpreter.ast.symbol.function.NamedFunction;
import dev.kobu.interpreter.ast.symbol.generics.TypeAlias;
import dev.kobu.interpreter.error.analyzer.CyclicTypeInheritanceError;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class TemplateTypeSymbol extends Symbol implements Type, HasExpr {

    private final String docText;

    private TemplateSuperType superType;

    private SymbolDocumentation documentation;

    public TemplateTypeSymbol(ModuleScope moduleScope, SourceCodeRef sourceCodeRef, String name, String docText) {
        super(moduleScope, sourceCodeRef, name, false);
        this.docText = docText;
    }

    public TemplateSuperType getSuperType() {
        return superType;
    }

    public void setSuperType(TemplateSuperType superType) {
        this.superType = superType;
    }

    @Override
    public List<FieldDescriptor> getFields() {
        return BuiltinScope.ANY_TEMPLATE_TYPE.getFields();
    }

    @Override
    public List<NamedFunction> getMethods() {
        return BuiltinScope.ANY_TEMPLATE_TYPE.getMethods();
    }

    @Override
    public Type resolveField(String name) {
        return BuiltinScope.ANY_TEMPLATE_TYPE.resolveField(name);
    }

    @Override
    public SourceCodeRef getFieldRef(String name) {
        return BuiltinScope.ANY_TEMPLATE_TYPE.getFieldRef(name);
    }

    @Override
    public NamedFunction resolveMethod(String name) {
        return BuiltinScope.ANY_TEMPLATE_TYPE.resolveMethod(name);
    }

    @Override
    public boolean isAssignableFrom(Type type) {
        if (type instanceof TemplateTypeSymbol) {
            TemplateTypeSymbol otherType = (TemplateTypeSymbol) type;
            if (getModuleScope().equals(otherType.getModuleScope()) && getName().equals(otherType.getName())) {
                return true;
            }
            return otherType.hasSuperType(this);
        }
        return false;
    }

    @Override
    public Type getCommonSuperType(Type type) {
        if (type instanceof AnyTemplateTypeSymbol || type instanceof AnyTypeSymbol) {
            return type;
        }
        if (type instanceof TemplateTypeSymbol) {
            var otherType = (TemplateTypeSymbol) type;
            if (isAssignableFrom(otherType)) {
                return this;
            }
            if (otherType.isAssignableFrom(this)) {
                return otherType;
            }
            TemplateTypeSymbol superType = this.superType != null ? this.superType.getType() : null;
            while(superType != null) {
                if (otherType.hasSuperType(superType)) {
                    return superType;
                }
                superType = superType.getSuperType() != null ? superType.getSuperType().getType() : null;
            }

            return BuiltinScope.ANY_TEMPLATE_TYPE;
        }
        return BuiltinScope.ANY_TYPE;
    }

    @Override
    public Collection<TypeAlias> aliases() {
        return null;
    }

    @Override
    public Type constructFor(Map<String, Type> typeArgs) {
        return this;
    }

    @Override
    public void resolveAliases(Map<String, Type> typeArgs, Type targetType) {

    }

    public boolean hasSuperType(TemplateTypeSymbol templateTypeSymbol) {
        return hasSuperType(templateTypeSymbol, null);
    }

    public boolean hasSuperType(TemplateTypeSymbol templateTypeSymbol, List<String> path) {
        if (path != null) {
            path.add(getName());
        }
        if (superType == null) {
            return false;
        }
        if (superType.getType().getModuleScope().equals(templateTypeSymbol.getModuleScope()) &&
                superType.getType().getName().equals(templateTypeSymbol.getName())) {
            return true;
        }
        return superType.getType().hasSuperType(templateTypeSymbol, path);
    }

    @Override
    public int getAnalyzerPriority() {
        return 1;
    }

    @Override
    public void analyze(AnalyzerContext context, EvalContextProvider evalContextProvider) {
        if (superType != null) {
            List<String> path = new ArrayList<>();
            if (hasSuperType(this, path)) {
                context.getErrorScope().addError(new CyclicTypeInheritanceError(superType.getSourceCodeRef(), path));
            }
        }

        if (getModuleScope().getEvalMode() == EvalModeEnum.ANALYZER_SERVICE) {
            String description = "type template " + super.getName();
            if (superType != null) {
                description += " extends " + superType.getType().getName();
            }
            this.documentation = new SymbolDocumentation(getModuleScope().getModuleId(), SymbolTypeEnum.TYPE,
                    description, docText);
        }
    }

    @Override
    public SymbolDocumentation getDocumentation() {
        return documentation;
    }

}
