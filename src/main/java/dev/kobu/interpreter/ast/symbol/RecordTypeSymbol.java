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
import dev.kobu.interpreter.ast.eval.ValueExpr;
import dev.kobu.interpreter.ast.eval.context.EvalContextProvider;
import dev.kobu.interpreter.ast.eval.context.EvalModeEnum;
import dev.kobu.interpreter.ast.symbol.function.NamedFunction;
import dev.kobu.interpreter.ast.symbol.generics.TypeAlias;
import dev.kobu.interpreter.ast.symbol.generics.TypeParameter;
import dev.kobu.interpreter.error.analyzer.CyclicRecordInheritanceError;
import dev.kobu.interpreter.error.analyzer.RecordSuperTypeConflictError;
import dev.kobu.interpreter.error.analyzer.RecordTypeAttributeConflictError;
import dev.kobu.interpreter.error.analyzer.RecordTypeStarAttributeError;

import java.util.*;
import java.util.stream.Collectors;

public class RecordTypeSymbol extends Symbol implements Type, HasExpr {

    private static final List<Type> EMPTY_TYPE_ARGS = List.of();

    private RecordSuperType superType;

    private final Map<String, RecordTypeAttribute> attributes = new HashMap<>();

    private final Map<String, NamedFunction> methods = new HashMap<>();

    private final String docText;

    private final List<Type> typeArgs;

    private RecordTypeStarAttribute starAttribute;

    private List<TypeParameter> typeParameters;

    private SymbolDocumentation documentation;

    public RecordTypeSymbol(SourceCodeRef sourceCodeRef, String name, ModuleScope module, String docText) {
        super(module, sourceCodeRef, name);
        this.docText = docText;
        this.typeArgs = EMPTY_TYPE_ARGS;
    }

    public RecordTypeSymbol(RecordTypeSymbol recordType, List<Type> typeArgs) {
        super(recordType.getModuleScope(), recordType.getSourceCodeRef(), recordType.getName());
        this.docText = recordType.docText;
        this.typeArgs = typeArgs;
        Map<String, Type> typeAliasMap = getTypeAliasMap(typeArgs);
        addAttributesFrom(recordType, typeAliasMap);
    }

    @Override
    public String getName() {
        return getModuleScope().getModuleId() + "." + super.getName() + getTypeParametersDescription();
    }

    @Override
    public String getNameInModule() {
        return super.getName();
    }

    @Override
    public String getIdentifier() {
        return getName();
    }

    public SourceCodeRef getSuperTypeSourceCodeRef() {
        return superType.getSourceCodeRef();
    }

    public List<TypeParameter> getTypeParameters() {
        return typeParameters;
    }

    public void setTypeParameters(List<TypeParameter> typeParameters) {
        this.typeParameters = typeParameters;
    }

    @Override
    public List<FieldDescriptor> getFields() {
        List<FieldDescriptor> fields = new ArrayList<>();

        attributes.forEach((k, v) -> fields.add(new FieldDescriptor(k, v.getType().getName())));

        return fields;
    }

    @Override
    public List<NamedFunction> getMethods() {
        return new ArrayList<>(methods.values());
    }

    public void setSuperType(RecordSuperType superType) {
        this.superType = superType;
    }

    public void addAttribute(AnalyzerContext analyzerContext, RecordTypeAttribute attribute) {
        attribute.setRecordType(this);
        RecordTypeAttribute currentDef = getAttribute(attribute.getName());
        if (currentDef != null) {
            analyzerContext.getErrorScope().addError(new RecordTypeAttributeConflictError(currentDef, attribute));
        }
        attributes.put(attribute.getName(), attribute);
    }

    public RecordTypeStarAttribute getStarAttribute() {
        return starAttribute;
    }

    public void setStarAttribute(AnalyzerContext analyzerContext, RecordTypeStarAttribute attribute) {
        if (starAttribute != null) {
            analyzerContext.getErrorScope()
                    .addError(new RecordTypeStarAttributeError(attribute.getSourceCodeRef(), this));
            return;
        }
        starAttribute = attribute;
    }

    public RecordTypeSymbol getSuperType() {
        return superType != null ? superType.getType() : null;
    }

    public Map<String, RecordTypeAttribute> getAttributes() {
        return attributes;
    }

    public RecordTypeAttribute getAttribute(String name) {
        return attributes.get(name);
    }

    @Override
    public Type resolveField(String name) {
        RecordTypeAttribute attr = attributes.get(name);
        if (attr != null) {
            return attr.getType();
        }
        Type fieldType = resolveSuperTypeField(name);
        if (fieldType != null) {
            return fieldType;
        }
        if (starAttribute != null) {
            return starAttribute.getType();
        }
        return null;
    }

    @Override
    public SourceCodeRef getFieldRef(String name) {
        RecordTypeAttribute attr = attributes.get(name);
        if (attr != null) {
            return attr.getSourceCodeRef();
        }
        if (superType != null) {
            var ref = superType.getType().getFieldRef(name);
            if (ref != null) {
                return ref;
            }
        }
        if (starAttribute != null) {
            return starAttribute.getSourceCodeRef();
        }
        return null;
    }

    public boolean hasSuperType(RecordTypeSymbol recordTypeSymbol) {
        return hasSuperType(recordTypeSymbol, null);
    }

    public boolean hasSuperType(RecordTypeSymbol recordTypeSymbol, List<String> path) {
        if (path != null) {
            path.add(getName());
        }
        if (superType == null) {
            return false;
        }
        if (superType.getType().getModuleScope().equals(recordTypeSymbol.getModuleScope()) &&
                superType.getType().getName().equals(recordTypeSymbol.getName())) {

            if (superType.getType().typeArgs.size() == recordTypeSymbol.typeArgs.size()) {

                for (int i = 0; i < superType.getType().typeArgs.size(); i++) {
                    if (!superType.getType().typeArgs.get(i).isAssignableFrom(recordTypeSymbol.typeArgs.get(i))) {
                        return false;
                    }
                }

                if (path != null) {
                    path.add(superType.getType().getName());
                }
                return true;
            }
            return false;
        }
        return superType.getType().hasSuperType(recordTypeSymbol, path);
    }

    public boolean hasStarAttribute() {
        if (starAttribute != null) {
            return true;
        }
        if (superType != null) {
            return superType.getType().hasStarAttribute();
        }
        return false;
    }

    private String getTypeParametersDescription() {
        if (typeParameters != null) {
            return "<" +
                    typeParameters.stream().map(TypeParameter::getAlias).collect(Collectors.joining(", ")) +
                    ">";
        }
        return "";
    }

    private Map<String, Type> getTypeAliasMap(List<Type> typeArgs) {
        Map<String, Type> typeAliasMap = new HashMap<>();
        for (int i = 0; i < typeArgs.size(); i++) {
            typeAliasMap.put(typeParameters.get(i).getAlias(), typeArgs.get(i));
        }
        return typeAliasMap;
    }

    private void addAttributesFrom(RecordTypeSymbol recordType, Map<String, Type> typeAliasMap) {
        recordType.attributes.forEach((name, attrType) -> {
            attributes.put(name, new RecordTypeAttribute(attrType, typeAliasMap));
        });
        if (recordType.starAttribute != null) {
            recordType.starAttribute = new RecordTypeStarAttribute(recordType.starAttribute, typeAliasMap);
        }
    }

    private Type resolveSuperTypeField(String name) {
        if (superType != null) {
            return superType.getType().resolveField(name);
        }
        return null;
    }

    @Override
    public NamedFunction resolveMethod(String name) {
        var method = methods.get(name);
        if (method != null) {
            return method;
        }
        if (superType != null) {
            return superType.getType().resolveMethod(name);
        }
        return BuiltinScope.ANY_RECORD_TYPE.resolveMethod(name);
    }

    @Override
    public boolean isAssignableFrom(Type type) {
        if (type instanceof RecordTypeSymbol) {
            RecordTypeSymbol otherRecord = (RecordTypeSymbol) type;
            if (getModuleScope().equals(otherRecord.getModuleScope()) && getName().equals(otherRecord.getName())) {
                if (typeArgs.size() == otherRecord.typeArgs.size()) {
                    for (int i = 0; i < typeArgs.size(); i++) {
                        if (!typeArgs.get(i).isAssignableFrom(otherRecord.typeArgs.get(i))) {
                            return false;
                        }
                    }
                    return true;
                }
                return false;
            }

            return getName().equals(otherRecord.getName()) || otherRecord.hasSuperType(this);
        }
        return false;
    }

    @Override
    public Type getCommonSuperType(Type type) {
        if (type instanceof AnyRecordTypeSymbol || type instanceof AnyTypeSymbol) {
            return type;
        }
        if (type instanceof RecordTypeSymbol) {
            var otherRecordType = (RecordTypeSymbol) type;
            if (isAssignableFrom(otherRecordType)) {
                return this;
            }
            if (otherRecordType.isAssignableFrom(this)) {
                return otherRecordType;
            }
            RecordTypeSymbol superType = this.superType != null ? this.superType.getType() : null;
            while(superType != null) {
                if (otherRecordType.hasSuperType(superType)) {
                    return superType;
                }
                superType = superType.getSuperType();
            }

            return BuiltinScope.ANY_RECORD_TYPE;
        }
        return BuiltinScope.ANY_TYPE;
    }

    @Override
    public Comparator<ValueExpr> getComparator() {
        return null;
    }

    @Override
    public Collection<TypeAlias> aliases() {
        return List.of();
    }

    @Override
    public Type constructFor(Map<String, Type> typeArgs) {
        return this;
    }

    @Override
    public void resolveAliases(Map<String, Type> typeArgs, Type targetType) {

    }

    public void buildMethods() {
        var types = new HashSet<String>();
        for (RecordTypeAttribute attribute : attributes.values()) {
            Type attrType = attribute.getType();
            while (types.add(attrType.getIdentifier())) {
                buildDynamicMethods(attribute.getType());
                if (attrType instanceof RecordTypeSymbol) {
                    attrType = ((RecordTypeSymbol)attrType).getSuperType();
                    if (attrType == null) {
                        attrType = BuiltinScope.ANY_RECORD_TYPE;
                    }
                } else {
                    attrType = BuiltinScope.ANY_TYPE;
                }
            }
        }

        if (starAttribute != null && types.add(starAttribute.getType().getIdentifier())) {
            buildDynamicMethods(starAttribute.getType());
        }
    }

    private void buildDynamicMethods(Type type) {
//        var valMethodName = "get" + type.getIdentifier() + "Values";
//        methods.put(valMethodName, new BuiltinFunctionSymbol(this, valMethodName,
//                new RecordValuesMethodImpl(type), ArrayTypeFactory.getArrayTypeFor(type)));
//
//        var entryMethodName = "get" + type.getIdentifier() + "Entries";
//        methods.put(entryMethodName, new BuiltinFunctionSymbol(this, entryMethodName,
//                new RecordEntriesMethodImpl(type),
//                ArrayTypeFactory.getArrayTypeFor(new TupleType(List.of(BuiltinScope.STRING_TYPE, type)))));
    }

    private RecordTypeAttribute resolveSuperTypeAttribute(String attrName) {
        if (superType != null) {
            var attr = superType.getType().attributes.get(attrName);
            if (attr == null) {
                attr = superType.getType().resolveSuperTypeAttribute(attrName);
            }
            return attr;
        }
        return null;
    }

    @Override
    public void analyze(AnalyzerContext context, EvalContextProvider evalContextProvider) {
        if (superType != null) {
            List<String> path = new ArrayList<>();
            if (hasSuperType(this, path)) {
                context.getErrorScope().addError(new CyclicRecordInheritanceError(superType.getSourceCodeRef(), path));
            } else if (starAttribute != null && superType.getType().hasStarAttribute()) {
                context.getErrorScope().addError(new RecordSuperTypeConflictError(this,
                        superType.getType()));
            }

            for (RecordTypeAttribute attr : attributes.values()) {
                var attrSuperType = resolveSuperTypeAttribute(attr.getName());
                if (attrSuperType != null && !attr.getType().getName().equals(attrSuperType.getType().getName())) {
                    context.getErrorScope().addError(new RecordTypeAttributeConflictError(attr, attrSuperType));
                }
            }

            superType.applyTypeArgs(context);
        }

        if (getModuleScope().getEvalMode() == EvalModeEnum.ANALYZER_SERVICE) {
            String description = "def type " + super.getName();
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

    @Override
    public boolean equals(Object object) {
        if (object instanceof RecordTypeSymbol) {
            if (!super.equals(object)) {
                return false;
            }
            RecordTypeSymbol other = (RecordTypeSymbol) object;
            return typeArgs.equals(other.typeArgs);
        }
        return false;
    }

}
