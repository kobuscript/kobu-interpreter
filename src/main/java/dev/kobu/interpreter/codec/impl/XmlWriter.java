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
import dev.kobu.interpreter.ast.symbol.ModuleScope;
import dev.kobu.interpreter.ast.symbol.RecordTypeSymbol;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.error.eval.BuiltinFunctionError;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class XmlWriter extends XmlCodec {

    private final RecordValueExpr xmlMappingExpr;

    private final SourceCodeRef sourceCodeRef;

    private final RecordValueExpr recordValueExpr;

    private Document doc;

    public XmlWriter(RecordValueExpr xmlMappingExpr, SourceCodeRef sourceCodeRef, RecordValueExpr recordValueExpr) {
        this.xmlMappingExpr = xmlMappingExpr;
        this.sourceCodeRef = sourceCodeRef;
        this.recordValueExpr = recordValueExpr;
    }

    public void write(OutputStream out, Charset charset) {

        readMapping(xmlMappingExpr, sourceCodeRef);

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            doc = db.newDocument();

            Node node = createNode(recordValueExpr);
            if (node != null) {
                doc.appendChild(node);

                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                transformer.setOutputProperty(OutputKeys.ENCODING, charset.displayName());
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
                DOMSource domSource = new DOMSource(doc);
                StreamResult streamResult = new StreamResult(out);
                transformer.transform(domSource, streamResult);

            }
        } catch (ParserConfigurationException | TransformerException ex) {
            throw new BuiltinFunctionError(ex, sourceCodeRef);
        }
    }

    private Node createNode(ValueExpr valueExpr) {
        if (valueExpr instanceof RecordValueExpr) {
            RecordValueExpr recordValueExpr = (RecordValueExpr) valueExpr;
            String tagName = getRecordAlias((RecordTypeSymbol) recordValueExpr.getType());
            Element element = doc.createElement(tagName);

            RecordAttributeKey starAttrKey = new RecordAttributeKey((RecordTypeSymbol) recordValueExpr.getType(), "*");
            TagAttribute starTagAttribute = tagAttributeMap.get(starAttrKey);

            for (String field : recordValueExpr.getFields()) {
                ValueExpr fieldValueExpr = recordValueExpr.resolveField(field);
                if (fieldValueExpr == null || fieldValueExpr instanceof NullValueExpr) {
                    continue;
                }
                RecordAttributeKey attrKey = new RecordAttributeKey((RecordTypeSymbol) recordValueExpr.getType(), field);
                TagAttribute tagAttribute = tagAttributeMap.get(attrKey);
                if (tagAttribute != null) {
                    String valueStr = getValue(fieldValueExpr);
                    addAttribute(element, field, valueStr);
                } else {
                    if (starTagAttribute != null && !((RecordTypeSymbol) recordValueExpr.getType()).hasAttribute(field)) {
                        String valueStr = getValue(fieldValueExpr);
                        addAttribute(element, field, valueStr);
                    } else if (fieldValueExpr instanceof ArrayValueExpr) {
                        List<ValueExpr> valueExprList = ((ArrayValueExpr) fieldValueExpr).getValue();
                        addCollectionItem(recordValueExpr, element, field, valueExprList);
                    } else if (fieldValueExpr instanceof TupleValueExpr) {
                        List<ValueExpr> valueExprList = ((TupleValueExpr) fieldValueExpr).getValueExprList();
                        addCollectionItem(recordValueExpr, element, field, valueExprList);
                    } else {
                        String childTagName = getRecordAttrAlias((RecordTypeSymbol) recordValueExpr.getType(), field);
                        Element childElement = doc.createElement(childTagName);
                        Node node = createNode(fieldValueExpr);
                        childElement.appendChild(node);
                        element.appendChild(childElement);
                    }
                }
            }

            return element;
        } else {
            return doc.createTextNode(getValue(valueExpr));
        }
    }

    private String getValue(ValueExpr valueExpr) {
        if (valueExpr instanceof StringValueExpr) {
            return ((StringValueExpr) valueExpr).getValue();
        }
        return valueExpr.getStringValue(new HashSet<>());
    }

    private void addCollectionItem(RecordValueExpr recordValueExpr, Element element, String field, List<ValueExpr> valueExprList) {
        Element parent = element;
        if (!isImplicitCollection((RecordTypeSymbol) recordValueExpr.getType(), field)) {
            String colTagName = getRecordAttrAlias((RecordTypeSymbol) recordValueExpr.getType(), field);
            parent = doc.createElement(colTagName);
            element.appendChild(parent);
        }
        for (ValueExpr itemExpr : valueExprList) {
            parent.appendChild(createNode(itemExpr));
        }
    }

    private void addAttribute(Element element, String name, String value) {
        Attr attr = doc.createAttribute(name);
        attr.setValue(value);
        element.setAttributeNode(attr);
    }
}
