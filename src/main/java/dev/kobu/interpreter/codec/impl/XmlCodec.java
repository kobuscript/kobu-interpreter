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

package dev.kobu.interpreter.codec.impl;

import dev.kobu.interpreter.ast.eval.ValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.ArrayValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.RecordTypeRefValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.RecordValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.StringValueExpr;
import dev.kobu.interpreter.ast.symbol.RecordTypeSymbol;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.ast.symbol.Type;
import dev.kobu.interpreter.error.eval.IllegalArgumentError;

import java.util.*;

public abstract class XmlCodec {

    protected final Map<RecordTypeSymbol, RecordAlias> recordAliasMap = new HashMap<>();

    protected final Map<RecordAttrAliasKey, RecordAttributeAlias> recordAttributeAliasMap = new HashMap<>();

    protected final Map<RecordAttributeKey, RecordAttributeAlias> recordAttributeMap = new HashMap<>();

    protected final Map<Type, List<ImplicitCollection>> implicitCollectionMap = new HashMap<>();

    protected final Map<RecordAttributeKey, TagAttribute> tagAttributeMap = new HashMap<>();

    protected RecordTypeSymbol rootRecordType;

    protected void readMapping(RecordValueExpr xmlMappingExpr, SourceCodeRef sourceCodeRef) {
        RecordTypeRefValueExpr rootRecordExpr = (RecordTypeRefValueExpr) xmlMappingExpr.resolveField("rootRecord");
        if (rootRecordExpr == null) {
            throw new IllegalArgumentError("'xmlMapping.rootRecord' cannot be null", sourceCodeRef);
        }
        this.rootRecordType = rootRecordExpr.getValue();

        ArrayValueExpr aliasesExpr = (ArrayValueExpr) xmlMappingExpr.resolveField("aliases");

        if (aliasesExpr != null) {
            int recIndex = 0;
            for (ValueExpr recordAliasValueExpr : aliasesExpr.getValue()) {
                if (recordAliasValueExpr == null) {
                    recIndex++;
                    continue;
                }

                RecordValueExpr recordAliasExpr = (RecordValueExpr) recordAliasValueExpr;

                RecordTypeRefValueExpr recordTypeExpr = (RecordTypeRefValueExpr) recordAliasExpr.resolveField("record");
                if (recordTypeExpr == null) {
                    throw new IllegalArgumentError("'xmlMapping.aliases[" + recIndex + "].recordType' cannot be null", sourceCodeRef);
                }
                StringValueExpr aliasExpr = (StringValueExpr) recordAliasExpr.resolveField("alias");
                if (aliasExpr == null) {
                    throw new IllegalArgumentError("'xmlMapping.aliases[" + recIndex + "].alias' cannot be null", sourceCodeRef);
                }

                String alias = aliasExpr.getValue();
                RecordAlias recordAlias = new RecordAlias(alias, recordTypeExpr.getValue());
                recordAliasMap.put(recordTypeExpr.getValue(), recordAlias);

                recIndex++;

            }
        }

        ArrayValueExpr attrAliasesExpr = (ArrayValueExpr) xmlMappingExpr.resolveField("attrAliases");

        if (attrAliasesExpr != null) {
            int recIndex = 0;
            for (ValueExpr recordAttrAliasvalueExpr : attrAliasesExpr.getValue()) {
                if (recordAttrAliasvalueExpr == null) {
                    recIndex++;
                    continue;
                }

                RecordValueExpr recordAttrAliasExpr = (RecordValueExpr) recordAttrAliasvalueExpr;

                RecordTypeRefValueExpr recordTypeExpr = (RecordTypeRefValueExpr) recordAttrAliasExpr.resolveField("record");
                if (recordTypeExpr == null) {
                    throw new IllegalArgumentError("'xmlMapping.aliases[" + recIndex + "].recordType' cannot be null", sourceCodeRef);
                }
                StringValueExpr aliasExpr = (StringValueExpr) recordAttrAliasExpr.resolveField("alias");
                if (aliasExpr == null) {
                    throw new IllegalArgumentError("'xmlMapping.aliases[" + recIndex + "].alias' cannot be null", sourceCodeRef);
                }

                StringValueExpr recordAttributeExpr = (StringValueExpr) recordAttrAliasExpr.resolveField("recordAttribute");
                if (recordAttributeExpr == null) {
                    throw new IllegalArgumentError("'xmlMapping.aliases[" + recIndex + "].recordAttribute' cannot be null", sourceCodeRef);
                }

                RecordAttributeAlias recAttributeAlias = new RecordAttributeAlias(recordAttributeExpr.getValue(),
                        aliasExpr.getValue(), recordTypeExpr.getValue());
                recordAttributeAliasMap.put(new RecordAttrAliasKey(recordTypeExpr.getValue(), aliasExpr.getValue()),
                        recAttributeAlias);
                recordAttributeMap.put(new RecordAttributeKey(recordTypeExpr.getValue(), recordAttributeExpr.getValue()),
                        recAttributeAlias);

                recIndex++;
            }
        }

        ArrayValueExpr implicitCollectionsExpr = (ArrayValueExpr) xmlMappingExpr.resolveField("implicitCollections");

        if (implicitCollectionsExpr != null) {
            int recIndex = 0;
            for (ValueExpr implicitColValueExpr : implicitCollectionsExpr.getValue()) {
                if (implicitColValueExpr == null) {
                    recIndex++;
                    continue;
                }

                RecordValueExpr implicitColExpr = (RecordValueExpr) implicitColValueExpr;
                RecordTypeRefValueExpr recordTypeExpr = (RecordTypeRefValueExpr) implicitColExpr.resolveField("record");
                if (recordTypeExpr == null) {
                    throw new IllegalArgumentError("'xmlMapping.implicitCollections[" + recIndex + "].recordType' cannot be null", sourceCodeRef);
                }
                StringValueExpr recordAttributeExpr = (StringValueExpr) implicitColExpr.resolveField("recordAttribute");
                if (recordAttributeExpr == null) {
                    throw new IllegalArgumentError("'xmlMapping.implicitCollections[" + recIndex + "].recordAttribute' cannot be null", sourceCodeRef);
                }

                ImplicitCollection implicitCol = new ImplicitCollection(recordTypeExpr.getValue(),
                        recordAttributeExpr.getValue());

                List<ImplicitCollection> implicitColList = implicitCollectionMap.computeIfAbsent(recordTypeExpr.getValue(),
                        k -> new ArrayList<>());
                implicitColList.add(implicitCol);

            }
        }

        ArrayValueExpr tagAttributesExpr = (ArrayValueExpr) xmlMappingExpr.resolveField("tagAttributes");

        if (tagAttributesExpr != null) {
            int recIndex = 0;
            for (ValueExpr tagAttributeValueExpr : tagAttributesExpr.getValue()) {
                if (tagAttributeValueExpr == null) {
                    recIndex++;
                    continue;
                }

                RecordValueExpr tagAttributeExpr = (RecordValueExpr) tagAttributeValueExpr;
                RecordTypeRefValueExpr recordTypeExpr = (RecordTypeRefValueExpr) tagAttributeExpr.resolveField("record");
                if (recordTypeExpr == null) {
                    throw new IllegalArgumentError("'xmlMapping.tagAttributes[" + recIndex + "].recordType' cannot be null", sourceCodeRef);
                }
                StringValueExpr recordAttributeExpr = (StringValueExpr) tagAttributeExpr.resolveField("recordAttribute");
                if (recordAttributeExpr == null) {
                    throw new IllegalArgumentError("'xmlMapping.tagAttributes[" + recIndex + "].recordAttribute' cannot be null", sourceCodeRef);
                }

                TagAttribute tagAttribute = new TagAttribute(recordTypeExpr.getValue(), recordAttributeExpr.getValue());
                tagAttributeMap.put(new RecordAttributeKey(recordTypeExpr.getValue(), recordAttributeExpr.getValue()),
                        tagAttribute);

            }
        }

    }

    protected String resolveAlias(RecordTypeSymbol recordType, String alias) {
        RecordAttrAliasKey attrAliasKey = new RecordAttrAliasKey(recordType, alias);
        RecordAttributeAlias attrAlias = recordAttributeAliasMap.get(attrAliasKey);
        if (attrAlias != null) {
            return attrAlias.recordAttr;
        }
        return alias;
    }

    protected String getRecordAlias(RecordTypeSymbol recordType) {
        RecordAlias recordAlias = recordAliasMap.get(recordType);
        if (recordAlias != null) {
            return recordAlias.alias;
        }
        return recordType.getNameInModule();
    }

    protected String getRecordAttrAlias(RecordTypeSymbol recordType, String attr) {
        RecordAttributeKey key = new RecordAttributeKey(recordType, attr);
        RecordAttributeAlias alias = recordAttributeMap.get(key);
        if (alias != null) {
            return alias.alias;
        }
        return attr;
    }

    protected boolean isImplicitCollection(RecordTypeSymbol recordType, String attr) {
        List<ImplicitCollection> implicitCollections = implicitCollectionMap.get(recordType);
        if (implicitCollections != null) {
            return implicitCollections.stream().anyMatch(c -> c.recordAttr.equals(attr));
        }
        return false;
    }

    protected static class RecordAlias {

        final String alias;

        final RecordTypeSymbol recordType;

        public RecordAlias(String alias, RecordTypeSymbol recordType) {
            this.alias = alias;
            this.recordType = recordType;
        }

    }

    protected static class RecordAttributeAlias {

        final String alias;

        final RecordTypeSymbol recordType;

        final String recordAttr;

        public RecordAttributeAlias(String recordAttr, String alias, RecordTypeSymbol recordType) {
            this.recordAttr = recordAttr;
            this.alias = alias;
            this.recordType = recordType;
        }

    }

    protected static class ImplicitCollection {

        final RecordTypeSymbol recordType;

        final String recordAttr;

        public ImplicitCollection(RecordTypeSymbol recordType, String recordAttr) {
            this.recordType = recordType;
            this.recordAttr = recordAttr;
        }

    }

    protected static class TagAttribute {

        final RecordTypeSymbol recordType;

        final String recordAttr;

        public TagAttribute(RecordTypeSymbol recordType, String recordAttr) {
            this.recordType = recordType;
            this.recordAttr = recordAttr;
        }

    }

    protected static class RecordAttributeKey {

        final RecordTypeSymbol recordType;

        final String recordAttribute;

        public RecordAttributeKey(RecordTypeSymbol recordType, String recordAttribute) {
            this.recordType = recordType;
            this.recordAttribute = recordAttribute;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RecordAttributeKey that = (RecordAttributeKey) o;
            return Objects.equals(recordType, that.recordType) && Objects.equals(recordAttribute, that.recordAttribute);
        }

        @Override
        public int hashCode() {
            return Objects.hash(recordType, recordAttribute);
        }

    }

    protected static class RecordAttrAliasKey {

        final RecordTypeSymbol recordType;

        final String alias;

        public RecordAttrAliasKey(RecordTypeSymbol recordType, String alias) {
            this.recordType = recordType;
            this.alias = alias;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RecordAttrAliasKey that = (RecordAttrAliasKey) o;
            return Objects.equals(recordType, that.recordType) && Objects.equals(alias, that.alias);
        }

        @Override
        public int hashCode() {
            return Objects.hash(recordType, alias);
        }

    }
}
