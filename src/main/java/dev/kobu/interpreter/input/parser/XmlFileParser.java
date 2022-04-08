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

package dev.kobu.interpreter.input.parser;

import dev.kobu.interpreter.ast.eval.ValueExpr;
import dev.kobu.interpreter.ast.eval.context.EvalContext;
import dev.kobu.interpreter.ast.eval.expr.value.*;
import dev.kobu.interpreter.ast.eval.expr.value.number.NumberValueFactory;
import dev.kobu.interpreter.ast.symbol.*;
import dev.kobu.interpreter.ast.symbol.array.ArrayType;
import dev.kobu.interpreter.ast.symbol.tuple.TupleType;
import dev.kobu.interpreter.ast.symbol.tuple.TupleTypeElement;
import dev.kobu.interpreter.ast.symbol.value.BooleanTypeSymbol;
import dev.kobu.interpreter.ast.symbol.value.NumberTypeSymbol;
import dev.kobu.interpreter.ast.symbol.value.StringTypeSymbol;
import dev.kobu.interpreter.ast.utils.RecordFactory;
import dev.kobu.interpreter.error.eval.BuiltinFunctionError;
import dev.kobu.interpreter.error.eval.IllegalArgumentError;
import dev.kobu.interpreter.error.eval.InvalidCallError;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.*;

public class XmlFileParser {

    public static final String XML_FILE_TYPE = "XmlFile";

    private final ModuleScope moduleScope;

    private final EvalContext context;

    private final RecordValueExpr xmlMappingExpr;

    private final String filePath;

    private final Charset charset;

    private final SourceCodeRef sourceCodeRef;

    private final Map<RecordTypeSymbol, RecordAlias> recordAliasMap = new HashMap<>();

    private final Map<RecordAttrAliasKey, RecordAttributeAlias> recordAttributeAliasMap = new HashMap<>();

    private final Map<Type, List<ImplicitCollection>> implicitCollectionMap = new HashMap<>();

    private final Map<RecordAttributeKey, TagAttribute> tagAttributeMap = new HashMap<>();

    private RecordTypeSymbol rootRecordType;

    public XmlFileParser(ModuleScope moduleScope, EvalContext context, RecordValueExpr xmlMappingExpr,
                         String filePath, Charset charset, SourceCodeRef sourceCodeRef) {
        this.moduleScope = moduleScope;
        this.context = context;
        this.xmlMappingExpr = xmlMappingExpr;
        this.filePath = filePath;
        this.charset = charset;
        this.sourceCodeRef = sourceCodeRef;
    }

    public ValueExpr parse(InputStream in) {
        var record = RecordFactory.create(moduleScope, context, XML_FILE_TYPE);
        FileValueExpr fileExpr = new FileValueExpr(new File(filePath));
        record.updateFieldValue(context, "file", fileExpr);

        readMapping();

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputSource is = new InputSource(in);
            is.setEncoding(charset.displayName());
            Document doc = db.parse(is);

            ValueExpr result = parseNode(rootRecordType, doc.getDocumentElement());

            if (!(result instanceof RecordValueExpr) && !rootRecordType.isAssignableFrom(result.getType())) {
                throw new InvalidCallError("Expected '" + rootRecordType.getName() +
                        "', but got '" + result.getType().getName() + "'", sourceCodeRef);
            }

            record.updateFieldValue(context, "xml", result);

        } catch (IOException | SAXException | ParserConfigurationException ex) {
            throw new BuiltinFunctionError(ex, sourceCodeRef);
        }

        return record;
    }

    private ValueExpr parseNode(Type targetType, Node node) {
        ValueExpr valueExpr = null;

        if (targetType instanceof RecordTypeSymbol) {
            RecordTypeSymbol recordType = (RecordTypeSymbol) targetType;
            Element element = (Element) node;
            RecordValueExpr recordExpr = RecordFactory.create(context, recordType);
            readTagAttributes(recordExpr, element);
            readTagChildren(recordExpr, element);

            valueExpr = recordExpr;

        } else if (targetType instanceof ArrayType) {
            if (node.hasChildNodes()) {
                Type elementType = ((ArrayType) targetType).getElementType();
                String tagName = getRecordAlias((RecordTypeSymbol) elementType);
                List<ValueExpr> values = new ArrayList<>();
                for (int i = 0; i < node.getChildNodes().getLength(); i++) {
                    Node childNode = node.getChildNodes().item(i);
                    if (childNode instanceof Element && tagName.equals(((Element) childNode).getTagName())) {
                        ValueExpr elementValueExpr = parseNode(elementType, childNode);
                        values.add(elementValueExpr);
                    }
                }
                valueExpr = new ArrayValueExpr((ArrayType) targetType, values);
            }
        } else if (targetType instanceof TupleType) {
            if (node.hasChildNodes()) {
                TupleTypeElement tupleTypeElement = ((TupleType) targetType).getTypeElement();
                String tagName = getRecordAlias((RecordTypeSymbol) tupleTypeElement.getElementType());
                List<ValueExpr> values = new ArrayList<>();
                for (int i = 0; i < node.getChildNodes().getLength(); i++) {
                    Node childNode = node.getChildNodes().item(i);
                    if (childNode instanceof Element && tagName.equals(((Element) childNode).getTagName())) {
                        ValueExpr elementValueExpr = parseNode(tupleTypeElement.getElementType(), childNode);
                        values.add(elementValueExpr);
                        tupleTypeElement = tupleTypeElement.getNext();
                        if (tupleTypeElement == null) {
                            break;
                        }
                        tagName = getRecordAlias((RecordTypeSymbol) tupleTypeElement.getElementType());
                    }
                }
                valueExpr = new TupleValueExpr((TupleType) targetType, values);
            }
        } else if (targetType instanceof StringTypeSymbol) {
            return new StringValueExpr(node.getNodeValue());
        } else if (targetType instanceof NumberTypeSymbol) {
            try {
                return NumberValueFactory.parse(node.getNodeValue());
            } catch (Exception ex) {
                throw new InvalidCallError("invalid number: '" + node.getNodeValue() + "'", sourceCodeRef);
            }
        } else if (targetType instanceof BooleanTypeSymbol) {
            return BooleanValueExpr.fromValue("true".equalsIgnoreCase(node.getNodeValue()));
        }

        return valueExpr;
    }

    private void readTagAttributes(RecordValueExpr recordExpr, Element element) {
        for (int i = 0; i < element.getAttributes().getLength(); i++) {
            Node attrNode = element.getAttributes().item(i);

            RecordTypeSymbol recordType = (RecordTypeSymbol) recordExpr.getType();

            String attrName = resolveAlias(recordType, attrNode.getNodeName());
            RecordAttributeKey attrKey = new RecordAttributeKey(recordType, attrName);
            TagAttribute tagAttribute = tagAttributeMap.get(attrKey);
            if (tagAttribute == null) {
                attrKey = new RecordAttributeKey(recordType, "*");
                tagAttribute = tagAttributeMap.get(attrKey);
            }

            if (tagAttribute != null) {
                Type targetType = recordType.resolveField(attrName);
                if (targetType == null) {
                    throw new InvalidCallError("Attribute '" + attrName + "' does not exist on type '" +
                            recordType.getName() + "'", sourceCodeRef);
                }
                ValueExpr valueExpr = parseNode(targetType, attrNode);
                if (!targetType.isAssignableFrom(valueExpr.getType())) {
                    throw new InvalidCallError(recordType.getName() + "." + attrName + ": Type '" +
                            targetType.getName() + "' is not assignable to type '" +
                            valueExpr.getType().getName() + "'", sourceCodeRef);
                }
                recordExpr.updateFieldValue(context, attrName, valueExpr);
            }

        }
    }

    private void readTagChildren(RecordValueExpr recordExpr, Element element) {
        RecordTypeSymbol recordType = (RecordTypeSymbol) recordExpr.getType();

        for (int i = 0; i < element.getChildNodes().getLength(); i++) {
            Node childNode = element.getChildNodes().item(i);
            if (childNode instanceof Element) {
                Element childElement = (Element) childNode;

                String attrName = resolveAlias(recordType, childElement.getTagName());
                Type targetType = recordType.resolveField(attrName);

                if (targetType != null) {
                    ValueExpr valueExpr = parseNode(targetType, childElement);
                    if (!targetType.isAssignableFrom(valueExpr.getType())) {
                        throw new InvalidCallError(recordType.getName() + "." + attrName + ": Type '" +
                                targetType.getName() + "' is not assignable to type '" +
                                valueExpr.getType().getName() + "'", sourceCodeRef);
                    }
                    recordExpr.updateFieldValue(context, attrName, valueExpr);
                }
            }
        }

        List<ImplicitCollection> implicitColList = implicitCollectionMap.get(recordExpr.getType());
        if (implicitColList != null) {
            for (ImplicitCollection implicitCol : implicitColList) {
                Type targetType = recordType.resolveField(implicitCol.recordAttr);
                if (targetType == null) {
                    throw new InvalidCallError("Attribute '" + implicitCol.recordAttr + "' does not exist on type '" +
                            recordType.getName() + "'", sourceCodeRef);
                }

                if (targetType instanceof ArrayType) {
                    validateArrayType((ArrayType) targetType);
                    Type elementType = ((ArrayType) targetType).getElementType();
                    String tagName = getRecordAlias((RecordTypeSymbol) elementType);
                    List<ValueExpr> values = new ArrayList<>();
                    for (int i = 0; i < element.getChildNodes().getLength(); i++) {
                        Node childNode = element.getChildNodes().item(i);
                        if (childNode instanceof Element && tagName.equals(((Element) childNode).getTagName())) {
                            ValueExpr valueExpr = parseNode(elementType, childNode);
                            values.add(valueExpr);
                        }
                    }
                    ArrayValueExpr arrayValueExpr = new ArrayValueExpr((ArrayType) targetType, values);
                    recordExpr.updateFieldValue(context, implicitCol.recordAttr, arrayValueExpr);
                } else if (targetType instanceof TupleType) {
                    validateTupleType((TupleType) targetType);

                    TupleTypeElement tupleElement = ((TupleType) targetType).getTypeElement();
                    String tagName = getRecordAlias((RecordTypeSymbol) tupleElement.getElementType());
                    List<ValueExpr> values = new ArrayList<>();
                    for (int i = 0; i < element.getChildNodes().getLength(); i++) {
                        Node childNode = element.getChildNodes().item(i);
                        if (childNode instanceof Element && tagName.equals(((Element) childNode).getTagName())) {
                            ValueExpr valueExpr = parseNode(tupleElement.getElementType(), childNode);
                            values.add(valueExpr);
                            tupleElement = tupleElement.getNext();
                            if (tupleElement == null) {
                                break;
                            }
                            tagName = getRecordAlias((RecordTypeSymbol) tupleElement.getElementType());
                        }
                    }
                    TupleValueExpr tupleValueExpr = new TupleValueExpr((TupleType) targetType, values);
                    recordExpr.updateFieldValue(context, implicitCol.recordAttr, tupleValueExpr);
                }
            }
        }
    }

    private void validateArrayType(ArrayType arrayType) {
        if (!(arrayType.getElementType() instanceof RecordTypeSymbol)) {
            throw new InvalidCallError("type '" + arrayType.getName() + "' is not supported in xml mapping",
                    sourceCodeRef);
        }
    }

    private void validateTupleType(TupleType tupleType) {
        TupleTypeElement tupleTypeElement = tupleType.getTypeElement();
        while (tupleTypeElement != null) {
            if (!(tupleTypeElement.getElementType() instanceof RecordTypeSymbol)) {
                throw new InvalidCallError("type '" + tupleType.getName() + "' is not supported in xml mapping",
                        sourceCodeRef);
            }
            tupleTypeElement = tupleTypeElement.getNext();
        }
    }

    private String resolveAlias(RecordTypeSymbol recordType, String alias) {
        RecordAttrAliasKey attrAliasKey = new RecordAttrAliasKey(recordType, alias);
        RecordAttributeAlias attrAlias = recordAttributeAliasMap.get(attrAliasKey);
        if (attrAlias != null) {
            return attrAlias.recordAttr;
        }
        return alias;
    }

    private String getRecordAlias(RecordTypeSymbol recordType) {
        RecordAlias recordAlias = recordAliasMap.get(recordType);
        if (recordAlias != null) {
            return recordAlias.alias;
        }
        return recordType.getNameInModule();
    }

    private void readMapping() {
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

    private static class RecordAlias {

        private final String alias;

        private final RecordTypeSymbol recordType;

        public RecordAlias(String alias, RecordTypeSymbol recordType) {
            this.alias = alias;
            this.recordType = recordType;
        }

    }

    private static class RecordAttributeAlias {

        private final String alias;

        private final RecordTypeSymbol recordType;

        private final String recordAttr;

        public RecordAttributeAlias(String recordAttr, String alias, RecordTypeSymbol recordType) {
            this.recordAttr = recordAttr;
            this.alias = alias;
            this.recordType = recordType;
        }

    }

    private static class ImplicitCollection {

        private final RecordTypeSymbol recordType;

        private final String recordAttr;

        public ImplicitCollection(RecordTypeSymbol recordType, String recordAttr) {
            this.recordType = recordType;
            this.recordAttr = recordAttr;
        }

    }

    private static class TagAttribute {

        private final RecordTypeSymbol recordType;

        private final String recordAttr;

        public TagAttribute(RecordTypeSymbol recordType, String recordAttr) {
            this.recordType = recordType;
            this.recordAttr = recordAttr;
        }

    }

    private static class RecordAttributeKey {

        private final RecordTypeSymbol recordType;

        private final String recordAttribute;

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

    private static class RecordAttrAliasKey {

        private final RecordTypeSymbol recordType;

        private final String alias;

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
