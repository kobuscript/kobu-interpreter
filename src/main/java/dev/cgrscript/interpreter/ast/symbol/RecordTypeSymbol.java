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

import dev.cgrscript.interpreter.ast.AnalyzerContext;
import dev.cgrscript.interpreter.ast.eval.FieldDescriptor;
import dev.cgrscript.interpreter.ast.eval.SymbolDocumentation;
import dev.cgrscript.interpreter.ast.eval.SymbolTypeEnum;
import dev.cgrscript.interpreter.ast.eval.ValueExpr;
import dev.cgrscript.interpreter.ast.eval.context.EvalModeEnum;
import dev.cgrscript.interpreter.ast.eval.function.record.RecordEntriesMethodImpl;
import dev.cgrscript.interpreter.ast.eval.function.record.RecordValuesMethodImpl;
import dev.cgrscript.interpreter.error.analyzer.CyclicRecordInheritanceError;
import dev.cgrscript.interpreter.error.analyzer.RecordSuperTypeConflictError;
import dev.cgrscript.interpreter.error.analyzer.RecordTypeAttributeConflictError;
import dev.cgrscript.interpreter.error.analyzer.RecordTypeUnknownAttributesError;

import java.util.*;

public class RecordTypeSymbol extends Symbol implements Type, HasExpr {

    private final ModuleScope module;

    private RecordTypeSymbol superType;

    private final Map<String, RecordTypeAttribute> attributes = new HashMap<>();

    private final Map<String, FunctionType> methods = new HashMap<>();

    private final String docText;

    private RecordTypeUnknownAttributes unknownAttributes;

    private SymbolDocumentation documentation;

    public RecordTypeSymbol(SourceCodeRef sourceCodeRef, String name, ModuleScope module, String docText) {
        super(module, sourceCodeRef, name);
        this.module = module;
        this.docText = docText;
    }

    @Override
    public String getName() {
        return module.getModuleId() + "." + super.getName();
    }

    @Override
    public String getNameInModule() {
        return super.getName();
    }

    public ModuleScope getModule() {
        return module;
    }

    @Override
    public String getIdentifier() {
        return getName();
    }

    @Override
    public List<FieldDescriptor> getFields() {
        List<FieldDescriptor> fields = new ArrayList<>();

        attributes.forEach((k, v) -> fields.add(new FieldDescriptor(k, v.getType().getName())));

        return fields;
    }

    @Override
    public List<FunctionType> getMethods() {
        return new ArrayList<>(methods.values());
    }

    public void setSuperType(RecordSuperType superType) {
        this.superType = superType.getType();
    }

    public void addAttribute(AnalyzerContext context, RecordTypeAttribute attribute) {
        attribute.setRecordType(this);
        RecordTypeAttribute currentDef = attributes.get(attribute.getName());
        if (currentDef != null && !currentDef.getType().equals(attribute.getType())) {
            context.getErrorScope().addError(new RecordTypeAttributeConflictError(currentDef, attribute));
        }
        attributes.put(attribute.getName(), attribute);
    }

    public void setUnknownAttributes(AnalyzerContext context, RecordTypeUnknownAttributes attributes) {
        if (unknownAttributes != null) {
            context.getErrorScope()
                    .addError(new RecordTypeUnknownAttributesError(attributes.getSourceCodeRef(), this));
        }
        unknownAttributes = attributes;
    }

    public RecordTypeSymbol getSuperType() {
        return superType;
    }

    public Map<String, RecordTypeAttribute> getAttributes() {
        return attributes;
    }

    public List<String> getAttributeNames() {
        List<String> names = new ArrayList<>(attributes.keySet());
        if (superType != null) {
            names.addAll(superType.getAttributeNames());
        }
        return names;
    }

    public RecordTypeUnknownAttributes getUnknownAttributes() {
        return unknownAttributes;
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
        if (unknownAttributes != null) {
            return unknownAttributes.getType();
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
            var ref = superType.getFieldRef(name);
            if (ref != null) {
                return ref;
            }
        }
        if (unknownAttributes != null) {
            return unknownAttributes.getSourceCodeRef();
        }
        return null;
    }

    public boolean hasSuperType(RecordTypeSymbol recordTypeSymbol) {
        return hasSuperType(recordTypeSymbol, null);
    }

    public boolean hasSuperType(RecordTypeSymbol recordTypeSymbol, List<String> path) {
        if (path != null) {
            path.add(recordTypeSymbol.getName());
        }
        if (superType == null) {
            return false;
        }
        if (superType.getName().equals(recordTypeSymbol.getName())) {
            if (path != null) {
                path.add(superType.getName());
            }
            return true;
        }
        return superType.hasSuperType(recordTypeSymbol, path);
    }

    public boolean hasUnknownAttributes() {
        if (unknownAttributes != null) {
            return true;
        }
        if (superType != null) {
            return superType.hasUnknownAttributes();
        }
        return false;
    }

    private Type resolveSuperTypeField(String name) {
        if (superType != null) {
            return superType.resolveField(name);
        }
        return null;
    }

    @Override
    public FunctionType resolveMethod(String name) {
        var method = methods.get(name);
        if (method != null) {
            return method;
        }
        if (superType != null) {
            return superType.resolveMethod(name);
        }
        return BuiltinScope.ANY_RECORD_TYPE.resolveMethod(name);
    }

    @Override
    public boolean isAssignableFrom(Type type) {
        if (type instanceof RecordTypeSymbol) {
            RecordTypeSymbol otherRecord = (RecordTypeSymbol) type;
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
            RecordTypeSymbol superType = this.superType;
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

        if (unknownAttributes != null && types.add(unknownAttributes.getType().getIdentifier())) {
            buildDynamicMethods(unknownAttributes.getType());
        }
    }

    private void buildDynamicMethods(Type type) {
        var valMethodName = "get" + type.getIdentifier() + "Values";
        methods.put(valMethodName, new BuiltinFunctionSymbol(this, valMethodName,
                new RecordValuesMethodImpl(type), new ArrayType(type)));

        var entryMethodName = "get" + type.getIdentifier() + "Entries";
        methods.put(entryMethodName, new BuiltinFunctionSymbol(this, entryMethodName,
                new RecordEntriesMethodImpl(type),
                new ArrayType(new PairType(BuiltinScope.STRING_TYPE, type))));
    }

    private RecordTypeAttribute resolveSuperTypeAttribute(String attrName) {
        if (superType != null) {
            var attr = superType.attributes.get(attrName);
            if (attr == null) {
                attr = superType.resolveSuperTypeAttribute(attrName);
            }
            return attr;
        }
        return null;
    }

    @Override
    public void analyze(AnalyzerContext context) {
        if (superType != null) {
            List<String> path = new ArrayList<>();
            if (hasSuperType(this, path)) {
                context.getErrorScope().addError(new CyclicRecordInheritanceError(superType.getSourceCodeRef(), path));
            } else if (unknownAttributes != null && superType.hasUnknownAttributes()) {
                List<SourceCodeRef> sourceCodeRefList = new ArrayList<>();
                sourceCodeRefList.add(superType.getSourceCodeRef());
                sourceCodeRefList.add(getSourceCodeRef());
                context.getErrorScope().addError(new RecordSuperTypeConflictError(sourceCodeRefList, this,
                        superType, unknownAttributes));
            }

            for (RecordTypeAttribute attr : attributes.values()) {
                var attrSuperType = resolveSuperTypeAttribute(attr.getName());
                if (attrSuperType != null && !attr.getType().getName().equals(attrSuperType.getType().getName())) {
                    context.getErrorScope().addError(new RecordTypeAttributeConflictError(attr, attrSuperType));
                }
            }
        }

        if (module.getEvalMode() == EvalModeEnum.ANALYZER_SERVICE) {
            String description = "def type " + super.getName();
            if (superType != null) {
                description += " extends " + superType.getName();
            }
            this.documentation = new SymbolDocumentation(module.getModuleId(), SymbolTypeEnum.TYPE,
                    description, docText);
        }
    }

    @Override
    public SymbolDocumentation getDocumentation() {
        return documentation;
    }

}
