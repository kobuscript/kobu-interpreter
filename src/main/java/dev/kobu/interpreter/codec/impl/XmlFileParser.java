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
import dev.kobu.interpreter.ast.eval.context.EvalContext;
import dev.kobu.interpreter.ast.eval.expr.value.*;
import dev.kobu.interpreter.ast.eval.expr.value.number.NumberValueFactory;
import dev.kobu.interpreter.ast.symbol.ModuleScope;
import dev.kobu.interpreter.ast.symbol.RecordTypeSymbol;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.ast.symbol.Type;
import dev.kobu.interpreter.ast.symbol.array.ArrayType;
import dev.kobu.interpreter.ast.symbol.tuple.TupleType;
import dev.kobu.interpreter.ast.symbol.tuple.TupleTypeElement;
import dev.kobu.interpreter.ast.symbol.value.BooleanTypeSymbol;
import dev.kobu.interpreter.ast.symbol.value.NumberTypeSymbol;
import dev.kobu.interpreter.ast.symbol.value.StringTypeSymbol;
import dev.kobu.interpreter.ast.utils.RecordFactory;
import dev.kobu.interpreter.error.eval.BuiltinFunctionError;
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
import java.util.ArrayList;
import java.util.List;

public class XmlFileParser extends XmlCodec {

    public static final String XML_FILE_TYPE = "XmlFile";

    private final ModuleScope moduleScope;

    private final EvalContext context;

    private final RecordValueExpr xmlMappingExpr;

    private final String filePath;

    private final Charset charset;

    private final SourceCodeRef sourceCodeRef;

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

        readMapping(xmlMappingExpr, sourceCodeRef);

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputSource is = new InputSource(in);
            is.setEncoding(charset.displayName());
            Document doc = db.parse(is);

            ValueExpr result = parseNode(rootRecordType, doc.getDocumentElement());

            if (!(result instanceof RecordValueExpr) && !rootRecordType.isAssignableFrom(result.getType())) {
                throw new InvalidCallError(getErrorMessage("Expected '" + rootRecordType.getName() +
                        "', but got '" + result.getType().getName() + "'"), sourceCodeRef);
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
            String value = node.getNodeValue();
            if (value == null && node.getFirstChild() != null) {
                value = node.getFirstChild().getNodeValue();
            }
            if (value == null) {
                return new NullValueExpr();
            }
            return new StringValueExpr(value);
        } else if (targetType instanceof NumberTypeSymbol) {
            try {
                String value = node.getNodeValue();
                if (value == null && node.getFirstChild() != null) {
                    value = node.getFirstChild().getNodeValue();
                }
                if (value == null) {
                    return new NullValueExpr();
                }
                return NumberValueFactory.parse(value);
            } catch (Exception ex) {
                throw new InvalidCallError(getErrorMessage("invalid number: '" + node.getNodeValue() + "'"), sourceCodeRef);
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
                    throw new InvalidCallError(getErrorMessage("Attribute '" + attrName + "' does not exist on type '" +
                            recordType.getName() + "'"), sourceCodeRef);
                }
                ValueExpr valueExpr = parseNode(targetType, attrNode);
                if (!targetType.isAssignableFrom(valueExpr.getType())) {
                    throw new InvalidCallError(getErrorMessage(recordType.getName() + "." + attrName + ": Type '" +
                            targetType.getName() + "' is not assignable to type '" +
                            valueExpr.getType().getName() + "'"), sourceCodeRef);
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
                    if (!(valueExpr instanceof NullValueExpr) && !targetType.isAssignableFrom(valueExpr.getType())) {
                        throw new InvalidCallError(getErrorMessage(recordType.getName() + "." + attrName + ": Type '" +
                                targetType.getName() + "' is not assignable to type '" +
                                valueExpr.getType().getName() + "'"), sourceCodeRef);
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
                    throw new InvalidCallError(getErrorMessage("Attribute '" + implicitCol.recordAttr + "' does not exist on type '" +
                            recordType.getName() + "'"), sourceCodeRef);
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
            throw new InvalidCallError(getErrorMessage("type '" + arrayType.getName() + "' is not supported in xml mapping"),
                    sourceCodeRef);
        }
    }

    private void validateTupleType(TupleType tupleType) {
        TupleTypeElement tupleTypeElement = tupleType.getTypeElement();
        while (tupleTypeElement != null) {
            if (!(tupleTypeElement.getElementType() instanceof RecordTypeSymbol)) {
                throw new InvalidCallError(getErrorMessage("type '" + tupleType.getName() + "' is not supported in xml mapping"),
                        sourceCodeRef);
            }
            tupleTypeElement = tupleTypeElement.getNext();
        }
    }

    private String getErrorMessage(String errorDescription) {
        return "Error while parsing '" + filePath + "': " + errorDescription;
    }

}
