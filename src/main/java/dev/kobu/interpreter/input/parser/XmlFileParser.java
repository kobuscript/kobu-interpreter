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
import dev.kobu.interpreter.ast.symbol.*;
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
import java.util.HashMap;
import java.util.Map;

public class XmlFileParser {

    public static final String XML_FILE_TYPE = "XmlFile";

    private final ModuleScope moduleScope;

    private final EvalContext context;

    private final RecordValueExpr xmlMappingExpr;

    private final String filePath;

    private final Charset charset;

    private final SourceCodeRef sourceCodeRef;

    private final Map<String, XmlRecord> recordMap = new HashMap<>();

    private RecordTypeSymbol rootRecordType;

    private XmlRecord parentRecord;

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

            Element element = doc.getDocumentElement();
            ValueExpr result = parseElement(element);

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

    private ValueExpr parseElement(Element element) {
        String tag = element.getTagName();
        ValueExpr valueExpr = null;

        if (parentRecord == null) {
            XmlRecord xmlRecord = recordMap.get(tag);
            if (xmlRecord != null) {
                RecordValueExpr recordExpr = RecordFactory.create(context, xmlRecord.recordType);
                valueExpr = recordExpr;

                for (int i = 0; i < element.getAttributes().getLength(); i++) {
                    Node attrNode = element.getAttributes().item(i);
                    RecordAttribute recordAttr = xmlRecord.attrMap.get(attrNode.getNodeName());
                    if (recordAttr != null && recordAttr.asTagAttribute) {
                        recordExpr.updateFieldValue(context, attrNode.getNodeName(),
                                parseValue(xmlRecord.recordType.resolveField(attrNode.getNodeName()), attrNode.getNodeValue()));
                    }
                }


            }
        } else {

        }

        return valueExpr;
    }

    private ValueExpr parseValue(Type targetType, String value) {
        return null;
    }

    private void readMapping() {
        RecordTypeRefValueExpr rootRecordExpr = (RecordTypeRefValueExpr) xmlMappingExpr.resolveField("rootRecord");
        if (rootRecordExpr == null) {
            throw new IllegalArgumentError("'xmlMapping.rootRecord' cannot be null", sourceCodeRef);
        }
        this.rootRecordType = rootRecordExpr.getValue();

        ArrayValueExpr recordsExpr = (ArrayValueExpr) xmlMappingExpr.resolveField("records");
        if (recordsExpr == null) {
            throw new IllegalArgumentError("'xmlMapping.records' cannot be null", sourceCodeRef);
        }

        int recIndex = 0;
        for (ValueExpr valueExpr : recordsExpr.getValue()) {
            if (valueExpr == null) {
                recIndex++;
                continue;
            }

            RecordValueExpr recordExpr = (RecordValueExpr) valueExpr;

            RecordTypeRefValueExpr recordTypeExpr = (RecordTypeRefValueExpr) recordExpr.resolveField("recordType");
            if (recordTypeExpr == null) {
                throw new IllegalArgumentError("'xmlMapping.records[" + recIndex + "].recordType' cannot be null", sourceCodeRef);
            }
            StringValueExpr aliasExpr = (StringValueExpr) recordExpr.resolveField("alias");
            if (aliasExpr == null) {
                throw new IllegalArgumentError("'xmlMapping.records[" + recIndex + "].alias' cannot be null", sourceCodeRef);
            }

            String alias = aliasExpr.getValue();
            XmlRecord xmlRecord = new XmlRecord(alias, recordTypeExpr.getValue());
            recordMap.put(alias, xmlRecord);

            ArrayValueExpr recordAttrsExpr = (ArrayValueExpr) recordExpr.resolveField("recordAttributes");
            if (recordAttrsExpr != null) {
                int attrIndex = 0;
                for (ValueExpr attrExpr : recordAttrsExpr.getValue()) {
                    if (attrExpr == null) {
                        attrIndex++;
                        continue;
                    }
                    RecordValueExpr recExpr = (RecordValueExpr) attrExpr;

                    boolean asTagAttribute = false;
                    boolean implicitCollection = false;

                    StringValueExpr recordAttrExpr = (StringValueExpr) recExpr.resolveField("recordAttr");
                    if (recordAttrExpr == null) {
                        throw new IllegalArgumentError("'xmlMapping.record[" + recIndex +
                                "].recordAttributes[" + attrIndex + "].recordAttr' cannot be null", sourceCodeRef);
                    }
                    StringValueExpr aliasAttrExpr = (StringValueExpr) recExpr.resolveField("alias");
                    if (aliasAttrExpr == null) {
                        throw new IllegalArgumentError("'xmlMapping.record[" + recIndex +
                                "].recordAttributes[" + attrIndex + "].alias' cannot be null", sourceCodeRef);
                    }
                    BooleanValueExpr asTagAttributeExpr = (BooleanValueExpr) recExpr.resolveField("asTagAttribute");
                    BooleanValueExpr implicitCollectionExpr = (BooleanValueExpr) recExpr.resolveField("implicitCollection");

                    if (asTagAttributeExpr != null) {
                        asTagAttribute = asTagAttributeExpr.getValue();
                    }
                    if (implicitCollectionExpr != null) {
                        implicitCollection = implicitCollectionExpr.getValue();
                    }

                    RecordAttribute recordAttribute = new RecordAttribute(recordAttrExpr.getValue(),
                            aliasAttrExpr.getValue(), asTagAttribute, implicitCollection);
                    xmlRecord.attrMap.put(aliasAttrExpr.getValue(), recordAttribute);

                    attrIndex++;
                }
            }

            recIndex++;

        }

    }

    private static class XmlRecord {

        private final String alias;

        private final RecordTypeSymbol recordType;

        private final Map<String, RecordAttribute> attrMap = new HashMap<>();

        public XmlRecord(String alias, RecordTypeSymbol recordType) {
            this.alias = alias;
            this.recordType = recordType;
        }

    }

    private static class RecordAttribute {

        private final String recordAttr;

        private final String alias;

        private final boolean asTagAttribute;

        private final boolean implicitCollection;

        public RecordAttribute(String recordAttr, String alias, boolean asTagAttribute, boolean implicitCollection) {
            this.recordAttr = recordAttr;
            this.alias = alias;
            this.asTagAttribute = asTagAttribute;
            this.implicitCollection = implicitCollection;
        }

    }

}
