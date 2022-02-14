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

import dev.cgrscript.database.Database;
import dev.cgrscript.interpreter.ast.eval.ValueExpr;
import dev.cgrscript.interpreter.ast.eval.function.record.RecordEntriesMethodImpl;
import dev.cgrscript.interpreter.ast.eval.function.record.RecordValuesMethodImpl;
import dev.cgrscript.interpreter.error.analyzer.*;
import dev.cgrscript.interpreter.input.InputReader;
import dev.cgrscript.interpreter.writer.OutputWriter;

import java.util.*;

public class RecordTypeSymbol extends Symbol implements Type, HasExpr {

    private final ModuleScope module;

    private RecordTypeSymbol superType;

    private final Map<String, RecordTypeAttribute> attributes = new HashMap<>();

    private final Map<String, FunctionType> methods = new HashMap<>();

    private RecordTypeUnknownAttributes unknownAttributes;

    public RecordTypeSymbol(SourceCodeRef sourceCodeRef, String name, ModuleScope module) {
        super(sourceCodeRef, name);
        this.module = module;
    }

    public ModuleScope getModule() {
        return module;
    }

    @Override
    public String getIdentifier() {
        return getName();
    }

    public void setSuperType(RecordSuperType superType) {
        this.superType = superType.getType();
    }

    public void addAttribute(RecordTypeAttribute attribute) {
        if (attribute.getType() instanceof RuleRefTypeSymbol) {
            module.addError(new InvalidTypeError(getSourceCodeRef(), BuiltinScope.ANY_TYPE, BuiltinScope.RULE_REF_TYPE));
            return;
        }
        if (attribute.getType() instanceof ArrayType && ((ArrayType)attribute.getType()).getElementType() instanceof RuleRefTypeSymbol) {
            module.addError(new InvalidTypeError(getSourceCodeRef(), new ArrayType(BuiltinScope.ANY_TYPE),
                            new ArrayType(BuiltinScope.RULE_REF_TYPE)));
            return;
        }
        if (attribute.getType() instanceof PairType) {
            PairType pairType = (PairType) attribute.getType();
            if (pairType.getLeftType() instanceof RuleRefTypeSymbol) {
                module.addError(new InvalidTypeError(((RuleRefTypeSymbol) pairType.getLeftType()).getSourceCodeRef(),
                                BuiltinScope.ANY_TYPE,
                                BuiltinScope.RULE_REF_TYPE));
                return;
            }
            if (pairType.getRightType() instanceof RuleRefTypeSymbol) {
                module.addError(new InvalidTypeError(((RuleRefTypeSymbol) pairType.getRightType()).getSourceCodeRef(),
                                BuiltinScope.ANY_TYPE,
                                BuiltinScope.RULE_REF_TYPE));
                return;
            }
        }

        attribute.setRecordType(this);
        RecordTypeAttribute currentDef = attributes.get(attribute.getName());
        if (currentDef != null && !currentDef.getType().equals(attribute.getType())) {
            module.addError(new RecordTypeAttributeConflictError(currentDef, attribute));
        }
        attributes.put(attribute.getName(), attribute);
    }

    public void setUnknownAttributes(RecordTypeUnknownAttributes attributes) {
        if (unknownAttributes != null) {
            module.addError(new RecordTypeUnknownAttributesError(attributes.getSourceCodeRef(), this));
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

    public RecordTypeSymbol getAttributeRecordType(String name) {
        RecordTypeAttribute attr = attributes.get(name);
        if (attr != null) {
            return this;
        }
        if (superType != null) {
            var recordType = superType.getAttributeRecordType(name);
            if (recordType != null) {
                return recordType;
            }
        }
        if (unknownAttributes != null) {
            return this;
        }
        return null;
    }

    public boolean hasSuperType(RecordTypeSymbol recordTypeSymbol) {
        if (superType == null) {
            return false;
        }
        if (superType.getName().equals(recordTypeSymbol.getName())) {
            return true;
        }
        return superType.hasSuperType(recordTypeSymbol);
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
        methods.put(valMethodName, new BuiltinFunctionSymbol(valMethodName,
                new RecordValuesMethodImpl(type), new ArrayType(type)));

        var entryMethodName = "get" + type.getIdentifier() + "Entries";
        methods.put(entryMethodName, new BuiltinFunctionSymbol(entryMethodName,
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
    public void analyze(Database database, InputReader inputReader, OutputWriter outputWriter) {
        if (superType != null) {
            if (superType.hasSuperType(this)) {
                module.addError(new CyclicRecordInheritanceError(superType.getSourceCodeRef(), this, superType));
            } else if (unknownAttributes != null && superType.hasUnknownAttributes()) {
                List<SourceCodeRef> sourceCodeRefList = new ArrayList<>();
                sourceCodeRefList.add(superType.getSourceCodeRef());
                sourceCodeRefList.add(getSourceCodeRef());
                module.addError(new RecordSuperTypeConflictError(sourceCodeRefList, this,
                        superType, unknownAttributes));
            }

            for (RecordTypeAttribute attr : attributes.values()) {
                var attrSuperType = resolveSuperTypeAttribute(attr.getName());
                if (attrSuperType != null && !attr.getType().getName().equals(attrSuperType.getType().getName())) {
                    module.addError(new RecordTypeAttributeConflictError(attr, attrSuperType));
                }
            }
        }
    }
}
