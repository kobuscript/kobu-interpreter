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

import dev.kobu.antlr.java.JavaParser;
import dev.kobu.antlr.java.JavaParserBaseVisitor;
import dev.kobu.interpreter.ast.eval.ValueExpr;
import dev.kobu.interpreter.ast.eval.context.EvalContext;
import dev.kobu.interpreter.ast.eval.expr.value.*;
import dev.kobu.interpreter.ast.symbol.ModuleScope;
import dev.kobu.interpreter.ast.symbol.RecordTypeSymbol;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.ast.symbol.Type;
import dev.kobu.interpreter.ast.symbol.array.ArrayTypeFactory;
import dev.kobu.interpreter.ast.utils.RecordFactory;

import java.io.File;
import java.util.*;

public class JavaParserVisitor extends JavaParserBaseVisitor<ValueExpr> {

    public static final String JAVA_FILE_TYPE = "JavaFile";

    private static final String JAVA_INT_TYPE = "JavaIntType";

    private static final String JAVA_FLOAT_TYPE = "JavaFloatType";

    private static final String JAVA_DOUBLE_TYPE = "JavaDoubleType";

    private static final String JAVA_BOOLEAN_TYPE = "JavaBooleanType";

    private static final String JAVA_VOID_TYPE = "JavaVoidType";

    private static final String JAVA_OBJECT_TYPE = "JavaObjectType";

    private static final String JAVA_IMPORT = "JavaImport";

    private static final String JAVA_DEFINITION = "JavaDefinition";

    private static final String JAVA_CLASS = "JavaClass";

    private static final String JAVA_INTERFACE = "JavaInterface";

    private static final String JAVA_RECORD = "JavaRecord";

    private static final String JAVA_ENUM = "JavaEnum";

    private static final String JAVA_TYPE_PARAMETER = "JavaTypeParameter";

    private static final String JAVA_ANNOTATION_VALUE = "JavaAnnotationValue";

    private static final String JAVA_ANNOTATION_FIELD_VALUES = "JavaAnnotationFieldValues";

    private static final String JAVA_CONSTRUCTOR = "JavaConstructor";

    private static final String JAVA_METHOD = "JavaMethod";

    private static final String JAVA_METHOD_PARAMETER = "JavaMethodParameter";

    private static final String JAVA_FIELD = "JavaField";

    private static final String JAVA_VALUE = "JavaValue";

    private static final String JAVA_ARRAY_VALUE = "JavaArrayValue";

    private static final String JAVA_ATTRIBUTE_REFERENCE = "JavaAttributeReference";

    private static final String JAVA_CLASS_REFERENCE = "JavaClassReference";

    private static final String JAVA_LITERAL_STRING = "JavaLiteralString";

    private static final String JAVA_LITERAL_INT = "JavaLiteralInt";

    private static final String JAVA_LITERAL_FLOAT = "JavaLiteralFloat";

    private static final String JAVA_LITERAL_DOUBLE = "JavaLiteralDouble";

    private static final String JAVA_LITERAL_BOOLEAN = "JavaLiteralBoolean";

    private static final String JAVA_ENUM_VALUE = "JavaEnumValue";

    private final ModuleScope moduleScope;

    private final EvalContext context;

    private final String filePath;

    private final SourceCodeRef sourceCodeRef;

    private final Map<String, String> imports = new HashMap<>();

    private final Stack<RecordValueExpr> defStack = new Stack<>();

    public JavaParserVisitor(ModuleScope moduleScope, EvalContext context, String filePath, SourceCodeRef sourceCodeRef) {
        this.moduleScope = moduleScope;
        this.context = context;
        this.filePath = filePath;
        this.sourceCodeRef = sourceCodeRef;
    }

    @Override
    public ValueExpr visitCompilationUnit(JavaParser.CompilationUnitContext ctx) {
        var record = RecordFactory.create(moduleScope, context, JAVA_FILE_TYPE);
        FileValueExpr fileExpr = new FileValueExpr(new File(filePath));
        record.updateFieldValue(context, "file", fileExpr);

        if (ctx.packageDeclaration() != null && ctx.packageDeclaration().qualifiedName() != null) {
            record.updateFieldValue(context, "package",
                    new StringValueExpr(ctx.packageDeclaration().qualifiedName().getText()));
        }

        List<ValueExpr> importValues = new ArrayList<>();
        String qualifiedName = "";
        if (ctx.importDeclaration() != null) {
            for (JavaParser.ImportDeclarationContext importDeclarationContext : ctx.importDeclaration()) {
                var importRecord = RecordFactory.create(moduleScope, context, JAVA_IMPORT);
                if (importDeclarationContext.qualifiedName() != null) {
                    qualifiedName = importDeclarationContext.qualifiedName().getText();
                    int idx = qualifiedName.lastIndexOf('.');
                    if (idx >= 0) {
                        String pkg = qualifiedName.substring(0, idx);
                        String name = qualifiedName.substring(idx + 1);
                        imports.put(name, pkg);
                    }
                }
                if (importDeclarationContext.MUL() != null) {
                    qualifiedName += ".*";
                }
                importRecord.updateFieldValue(context, "qualifiedName", new StringValueExpr(qualifiedName));
                importValues.add(importRecord);
            }
        }
        record.updateFieldValue(context, "imports",
                new ArrayValueExpr(ArrayTypeFactory.getArrayTypeFor((Type) moduleScope.resolve(JAVA_IMPORT)),
                        importValues));

        List<ValueExpr> definitionValues = new ArrayList<>();

        if (ctx.typeDeclaration() != null) {
            for (JavaParser.TypeDeclarationContext typeDeclarationContext : ctx.typeDeclaration()) {
                ValueExpr javaDef = visit(typeDeclarationContext);
                if (javaDef != null) {
                    definitionValues.add(javaDef);
                }
            }
        }

        record.updateFieldValue(context, "definitions",
                new ArrayValueExpr(ArrayTypeFactory.getArrayTypeFor((Type) moduleScope.resolve(JAVA_DEFINITION)),
                        definitionValues));

        return record;
    }

    @Override
    public ValueExpr visitTypeDeclaration(JavaParser.TypeDeclarationContext ctx) {
        RecordValueExpr defRecExpr;
        if (ctx.classDeclaration() != null) {
            defRecExpr = RecordFactory.create(moduleScope, context, JAVA_CLASS);
            defStack.push(defRecExpr);
            visit(ctx.classDeclaration());
        } else if (ctx.enumDeclaration() != null) {
            defRecExpr = RecordFactory.create(moduleScope, context, JAVA_ENUM);
            defStack.push(defRecExpr);
            visit(ctx.enumDeclaration());
        } else if (ctx.interfaceDeclaration() != null) {
            defRecExpr = RecordFactory.create(moduleScope, context, JAVA_INTERFACE);
            defStack.push(defRecExpr);
            visit(ctx.interfaceDeclaration());
        } else if (ctx.recordDeclaration() != null) {
            defRecExpr = RecordFactory.create(moduleScope, context, JAVA_RECORD);
            defStack.push(defRecExpr);
            visit(ctx.recordDeclaration());
        } else {
            return null;
        }

        defRecExpr.updateFieldValue(context, "abstract", BooleanValueExpr.FALSE);
        defRecExpr.updateFieldValue(context, "final", BooleanValueExpr.FALSE);
        defRecExpr.updateFieldValue(context, "static", BooleanValueExpr.FALSE);
        defRecExpr.updateFieldValue(context, "public", BooleanValueExpr.FALSE);
        defRecExpr.updateFieldValue(context, "private", BooleanValueExpr.FALSE);
        defRecExpr.updateFieldValue(context, "protected", BooleanValueExpr.FALSE);

        defRecExpr.updateFieldValue(context, "annotations", new ArrayValueExpr(
                ArrayTypeFactory.getArrayTypeFor((Type) moduleScope.resolve(JAVA_ANNOTATION_VALUE)),
                new ArrayList<>()
        ));

        if (ctx.classOrInterfaceModifier() != null) {
            for (JavaParser.ClassOrInterfaceModifierContext classOrInterfaceModifierContext : ctx.classOrInterfaceModifier()) {
                visit(classOrInterfaceModifierContext);
            }
        }

        defStack.pop();
        return defRecExpr;
    }

    @Override
    public ValueExpr visitClassOrInterfaceModifier(JavaParser.ClassOrInterfaceModifierContext ctx) {
        RecordValueExpr defRecExpr = defStack.peek();
        if (ctx.annotation() != null) {
            var recAnn = visit(ctx.annotation());
            ArrayValueExpr annList = (ArrayValueExpr) defRecExpr.resolveField("annotations");
            annList.getValue().add(recAnn);
        } else if (ctx.PUBLIC() != null) {
            defRecExpr.updateFieldValue(context, "public", BooleanValueExpr.TRUE);
        } else if (ctx.PROTECTED() != null) {
            defRecExpr.updateFieldValue(context, "protected", BooleanValueExpr.TRUE);
        } else if (ctx.PRIVATE() != null) {
            defRecExpr.updateFieldValue(context, "private", BooleanValueExpr.TRUE);
        } else if (ctx.STATIC() != null) {
            defRecExpr.updateFieldValue(context, "static", BooleanValueExpr.TRUE);
        } else if (ctx.ABSTRACT() != null) {
            defRecExpr.updateFieldValue(context, "abstract", BooleanValueExpr.TRUE);
        } else if (ctx.FINAL() != null) {
            defRecExpr.updateFieldValue(context, "final", BooleanValueExpr.TRUE);
        }

        return null;
    }

    @Override
    public ValueExpr visitAnnotation(JavaParser.AnnotationContext ctx) {
        var recAnn = RecordFactory.create(moduleScope, context, JAVA_ANNOTATION_VALUE);
        String pkg;
        String name;

        String fullName;
        if (ctx.qualifiedName() != null) {
            fullName = ctx.qualifiedName().getText();
        } else {
            fullName = ctx.altAnnotationQualifiedName().getText().replace("@", "");
        }

        int idx = fullName.lastIndexOf('.');
        if (idx >= 0) {
            pkg = fullName.substring(0, idx);
            name = fullName.substring(idx + 1);
        } else {
            name = fullName;
            pkg = imports.get(name);
        }

        if (pkg != null) {
            recAnn.updateFieldValue(context, "package", new StringValueExpr(pkg));
        }
        recAnn.updateFieldValue(context, "name", new StringValueExpr(name));

        RecordValueExpr fieldsRec = RecordFactory.create(context,
                (RecordTypeSymbol) moduleScope.resolve(JAVA_ANNOTATION_FIELD_VALUES));

        if (ctx.elementValue() != null) {
            fieldsRec.updateFieldValue(context, "value", visit(ctx.elementValue()));
        } else if (ctx.elementValuePairs() != null && ctx.elementValuePairs().elementValuePair() != null) {
            for (JavaParser.ElementValuePairContext elementValuePairContext : ctx.elementValuePairs().elementValuePair()) {
                if (elementValuePairContext.elementValue() != null) {
                    fieldsRec.updateFieldValue(context, elementValuePairContext.identifier().getText(),
                            visit(elementValuePairContext.elementValue()));
                }
            }
        }

        recAnn.updateFieldValue(context, "fields", fieldsRec);
        return recAnn;
    }

    @Override
    public ValueExpr visitElementValueArrayInitializer(JavaParser.ElementValueArrayInitializerContext ctx) {
        RecordValueExpr arrRec = RecordFactory.create(context, (RecordTypeSymbol) moduleScope.resolve(JAVA_ARRAY_VALUE));

        List<ValueExpr> values = new ArrayList<>();

        if (ctx.elementValue() != null) {
            for (JavaParser.ElementValueContext elementValueContext : ctx.elementValue()) {
                values.add(visit(elementValueContext));
            }
        }

        arrRec.updateFieldValue(context, "items", new ArrayValueExpr(
                ArrayTypeFactory.getArrayTypeFor((Type) moduleScope.resolve(JAVA_VALUE)),
                values
        ));
        return arrRec;
    }

    @Override
    public ValueExpr visitExpression(JavaParser.ExpressionContext ctx) {
        if (ctx.primary() != null) {
            if (ctx.primary().literal() != null) {
                return visit(ctx.primary().literal());
            }
            if (ctx.primary().CLASS() != null) {
                if (ctx.primary().typeTypeOrVoid() != null && ctx.primary().typeTypeOrVoid().typeType() != null) {
                    String fullTypeName = ctx.primary().typeTypeOrVoid().typeType().classOrInterfaceType().getText();
                    String typeName = fullTypeName;
                    String typePackage;
                    int idx = fullTypeName.lastIndexOf('.');
                    if (idx >= 0) {
                        typePackage = fullTypeName.substring(0, idx);
                        typeName = fullTypeName.substring(idx + 1);
                    } else {
                        typePackage = imports.get(typeName);
                    }

                    RecordValueExpr recClass = RecordFactory.create(context,
                            (RecordTypeSymbol) moduleScope.resolve(JAVA_CLASS_REFERENCE));
                    recClass.updateFieldValue(context, "typePackage", new StringValueExpr(typePackage));
                    recClass.updateFieldValue(context, "typeName", new StringValueExpr(typeName));
                    return recClass;
                }
            }
        }
        if (ctx.bop != null && ".".equals(ctx.bop.getText())) {
            String expr = ctx.getText();
            int idx = expr.lastIndexOf('.');
            String fullTypeName = expr.substring(0, idx);
            String attrName = expr.substring(idx + 1);

            RecordValueExpr recAttrRef = RecordFactory.create(context,
                    (RecordTypeSymbol) moduleScope.resolve(JAVA_ATTRIBUTE_REFERENCE));
            String typeName = fullTypeName;
            String typePackage = null;
            idx = fullTypeName.lastIndexOf('.');
            if (idx >= 0) {
                typePackage = fullTypeName.substring(0, idx);
                typeName = fullTypeName.substring(idx + 1);
            } else {
                typePackage = imports.get(typeName);
            }

            recAttrRef.updateFieldValue(context, "typePackage", new StringValueExpr(typePackage));
            recAttrRef.updateFieldValue(context, "typeName", new StringValueExpr(typeName));
            recAttrRef.updateFieldValue(context, "attributeName", new StringValueExpr(attrName));
            return recAttrRef;
        }
        return new NullValueExpr();
    }

    @Override
    public ValueExpr visitLiteral(JavaParser.LiteralContext ctx) {
        return super.visitLiteral(ctx);
    }

}
