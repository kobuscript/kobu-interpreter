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
import dev.kobu.interpreter.ast.eval.expr.value.number.DoubleValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.number.IntegerValueExpr;
import dev.kobu.interpreter.ast.symbol.*;
import dev.kobu.interpreter.ast.symbol.array.ArrayTypeFactory;
import dev.kobu.interpreter.ast.utils.RecordFactory;
import dev.kobu.interpreter.ast.utils.StringFunctions;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.File;
import java.util.*;

public class JavaParserVisitor extends JavaParserBaseVisitor<ValueExpr> {

    public static final String JAVA_FILE_TYPE = "JavaFile";

    private static final String JAVA_INT_TYPE = "JavaIntType";

    private static final String JAVA_CHAR_TYPE = "JavaCharType";

    private static final String JAVA_BYTE_TYPE = "JavaByteType";

    private static final String JAVA_SHORT_TYPE = "JavaShortType";

    private static final String JAVA_LONG_TYPE = "JavaLongType";

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

    private static final String JAVA_ENUM_VALUE = "JavaEnumValue";

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

    private static final String JAVA_LITERAL_CHAR = "JavaLiteralChar";

    private static final String JAVA_LITERAL_FLOAT = "JavaLiteralFloat";

    private static final String JAVA_LITERAL_BOOLEAN = "JavaLiteralBoolean";


    private final ModuleScope moduleScope;

    private final EvalContext context;

    private final String filePath;

    private final RecordValueExpr filterExpr;

    private final Map<String, String> imports = new HashMap<>();

    private final Stack<RecordValueExpr> defStack = new Stack<>();

    private String currentPackage;

    private RecordValueExpr currentClassMember;

    public JavaParserVisitor(ModuleScope moduleScope, EvalContext context, String filePath, RecordValueExpr filterExpr) {
        this.moduleScope = moduleScope;
        this.context = context;
        this.filePath = filePath;
        this.filterExpr = filterExpr;
    }

    @Override
    public ValueExpr visitCompilationUnit(JavaParser.CompilationUnitContext ctx) {
        var record = RecordFactory.create(moduleScope, context, JAVA_FILE_TYPE);
        FileValueExpr fileExpr = new FileValueExpr(new File(filePath));
        record.updateFieldValue(context, "file", fileExpr);

        if (ctx.packageDeclaration() != null && ctx.packageDeclaration().qualifiedName() != null) {
            currentPackage = ctx.packageDeclaration().qualifiedName().getText();
            record.updateFieldValue(context, "package",
                    new StringValueExpr(currentPackage));
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
                RecordValueExpr javaDef = (RecordValueExpr) visit(typeDeclarationContext);
                if (javaDef != null && runFilter(javaDef)) {
                    definitionValues.add(javaDef);
                }
            }
        }

        if (definitionValues.isEmpty()) {
            return null;
        }

        record.updateFieldValue(context, "definitions",
                new ArrayValueExpr(ArrayTypeFactory.getArrayTypeFor((Type) moduleScope.resolve(JAVA_DEFINITION)),
                        definitionValues));

        return record;
    }

    private boolean runFilter(RecordValueExpr javaDefExpr) {
        if (filterExpr == null) {
            return true;
        }
        ArrayValueExpr annFilterListExpr = (ArrayValueExpr) filterExpr.resolveField("typeAnnotations");
        RecordValueExpr superTypeFilterExpr = (RecordValueExpr) filterExpr.resolveField("superType");
        ArrayValueExpr implementsFilterExpr = (ArrayValueExpr) filterExpr.resolveField("implements");

        List<ValueExpr> annFilterValueList = annFilterListExpr != null ? annFilterListExpr.getValue() : new ArrayList<>();
        List<ValueExpr> implementsFilterValueList = implementsFilterExpr != null ? implementsFilterExpr.getValue() : new ArrayList<>();

        ArrayValueExpr annListExpr = (ArrayValueExpr) javaDefExpr.resolveField("annotations");
        List<ValueExpr> annValueList = annListExpr != null ? annListExpr.getValue() : new ArrayList<>();
        ArrayValueExpr implementsListExpr = (ArrayValueExpr) javaDefExpr.resolveField("implements");
        List<ValueExpr> implementsValueList = implementsListExpr != null ? implementsListExpr.getValue() : new ArrayList<>();

        boolean matchAnnotations;
        boolean matchInterfaces;
        boolean matchSuperType = false;
        matchAnnotations = annFilterValueList.stream().allMatch(annFilterValue -> {
            if (!(annFilterValue instanceof RecordValueExpr)) {
                return false;
            }
            RecordValueExpr annFilterValueRec = (RecordValueExpr) annFilterValue;
            ValueExpr pkgExpr = annFilterValueRec.resolveField("package");
            ValueExpr nameExpr = annFilterValueRec.resolveField("name");

            return annValueList.stream().anyMatch(annValue -> {
                RecordValueExpr annValueRec = (RecordValueExpr) annValue;
                ValueExpr annPkgExpr = annValueRec.resolveField("package");
                ValueExpr annNameExpr = annValueRec.resolveField("name");

                return (pkgExpr == null || pkgExpr.equals(annPkgExpr)) &&
                        (nameExpr == null || nameExpr.equals(annNameExpr));
            });
        });
        matchInterfaces = implementsFilterValueList.stream().allMatch(interfaceFilterValue -> {
            if (!(interfaceFilterValue instanceof RecordValueExpr)) {
                return false;
            }
            RecordValueExpr interfaceFilterValueRec = (RecordValueExpr) interfaceFilterValue;
            ValueExpr pkgExpr = interfaceFilterValueRec.resolveField("package");
            ValueExpr nameExpr = interfaceFilterValueRec.resolveField("name");

            return implementsValueList.stream().anyMatch(interfaceValue -> {
                RecordValueExpr interfaceValueRec = (RecordValueExpr) interfaceValue;
                ValueExpr annPkgExpr = interfaceValueRec.resolveField("package");
                ValueExpr annNameExpr = interfaceValueRec.resolveField("name");

                return (pkgExpr == null || pkgExpr.equals(annPkgExpr)) &&
                        (nameExpr == null || nameExpr.equals(annNameExpr));
            });
        });

        if (superTypeFilterExpr != null) {
            RecordValueExpr superTypeExpr = (RecordValueExpr) javaDefExpr.resolveField("superType");
            if (superTypeExpr != null) {
                StringValueExpr pkgExpr = (StringValueExpr) superTypeFilterExpr.resolveField("package");
                StringValueExpr nameExpr = (StringValueExpr) superTypeFilterExpr.resolveField("name");
                matchSuperType = (pkgExpr == null || pkgExpr.equals(superTypeExpr.resolveField("package"))) &&
                        (nameExpr == null || nameExpr.equals(superTypeExpr.resolveField("name")));
            }
        } else {
            matchSuperType = true;
        }

        return matchAnnotations && matchSuperType && matchInterfaces;
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
        JavaParser.IntegerLiteralContext integerCtx = ctx.integerLiteral();
        JavaParser.FloatLiteralContext floatCtx = ctx.floatLiteral();
        if (integerCtx != null) {
            RecordValueExpr recValExpr = RecordFactory.create(context,
                    (RecordTypeSymbol) moduleScope.resolve(JAVA_LITERAL_INT));
            if (integerCtx.DECIMAL_LITERAL() != null) {
                recValExpr.updateFieldValue(context, "originalRadix", new IntegerValueExpr(10));
                recValExpr.updateFieldValue(context, "value",
                        new IntegerValueExpr(Integer.parseInt(integerCtx.DECIMAL_LITERAL().getText())));
                recValExpr.updateFieldValue(context, "source", new StringValueExpr(integerCtx.DECIMAL_LITERAL().getText()));
            } else if (integerCtx.HEX_LITERAL() != null) {
                recValExpr.updateFieldValue(context, "originalRadix", new IntegerValueExpr(16));
                recValExpr.updateFieldValue(context, "value",
                        new IntegerValueExpr(Integer.parseInt(integerCtx.HEX_LITERAL().getText().substring(2), 16)));
                recValExpr.updateFieldValue(context, "source", new StringValueExpr(integerCtx.HEX_LITERAL().getText()));
            } else if (integerCtx.OCT_LITERAL() != null) {
                recValExpr.updateFieldValue(context, "originalRadix", new IntegerValueExpr(8));
                String source = integerCtx.OCT_LITERAL().getText();
                String numberStr;
                int idx = source.lastIndexOf('_');
                if (idx >= 0) {
                    numberStr = source.substring(idx + 1);
                } else {
                    numberStr = source.substring(1);
                }
                recValExpr.updateFieldValue(context, "value",
                        new IntegerValueExpr(Integer.parseInt(numberStr, 8)));
                recValExpr.updateFieldValue(context, "source", new StringValueExpr(source));
            } else if (integerCtx.BINARY_LITERAL() != null) {
                recValExpr.updateFieldValue(context, "originalRadix", new IntegerValueExpr(2));
                recValExpr.updateFieldValue(context, "value",
                        new IntegerValueExpr(Integer.parseInt(integerCtx.BINARY_LITERAL().getText().substring(1), 2)));
                recValExpr.updateFieldValue(context, "source", new StringValueExpr(integerCtx.BINARY_LITERAL().getText()));
            }
            return recValExpr;
        } else if (floatCtx != null) {
            RecordValueExpr recValExpr = RecordFactory.create(context, (RecordTypeSymbol) moduleScope.resolve(JAVA_LITERAL_FLOAT));
            if (floatCtx.FLOAT_LITERAL() != null) {
                recValExpr.updateFieldValue(context, "originalRadix", new IntegerValueExpr(10));
                recValExpr.updateFieldValue(context, "value",
                        new DoubleValueExpr(Double.parseDouble(floatCtx.FLOAT_LITERAL().getText())));
                recValExpr.updateFieldValue(context, "source", new StringValueExpr(floatCtx.FLOAT_LITERAL().getText()));
            } else if (floatCtx.HEX_FLOAT_LITERAL() != null) {
                recValExpr.updateFieldValue(context, "originalRadix", new IntegerValueExpr(16));
                //TODO: Implement a parse for the hexadecimal float literal. For now, we are just saving the source string.
                recValExpr.updateFieldValue(context, "source", new StringValueExpr(floatCtx.HEX_FLOAT_LITERAL().getText()));
            }
            return recValExpr;
        } else if (ctx.CHAR_LITERAL() != null) {
            RecordValueExpr recValExpr = RecordFactory.create(context, (RecordTypeSymbol) moduleScope.resolve(JAVA_LITERAL_CHAR));
            recValExpr.updateFieldValue(context, "value",
                    new StringValueExpr(StringFunctions.parseLiteralString(ctx.CHAR_LITERAL().getText())));
            recValExpr.updateFieldValue(context, "source", new StringValueExpr(ctx.CHAR_LITERAL().getText()));
            return recValExpr;
        } else if (ctx.STRING_LITERAL() != null) {
            RecordValueExpr recValExpr = RecordFactory.create(context, (RecordTypeSymbol) moduleScope.resolve(JAVA_LITERAL_STRING));
            recValExpr.updateFieldValue(context, "value",
                    new StringValueExpr(StringFunctions.parseLiteralString(ctx.STRING_LITERAL().getText())));
            recValExpr.updateFieldValue(context, "source", new StringValueExpr(ctx.STRING_LITERAL().getText()));
            return recValExpr;
        } else if (ctx.BOOL_LITERAL() != null) {
            RecordValueExpr recValExpr = RecordFactory.create(context, (RecordTypeSymbol) moduleScope.resolve(JAVA_LITERAL_BOOLEAN));
            recValExpr.updateFieldValue(context, "value", BooleanValueExpr.fromValue("true".equals(ctx.BOOL_LITERAL().getText())));
            recValExpr.updateFieldValue(context, "source", new StringValueExpr(ctx.STRING_LITERAL().getText()));
            return recValExpr;
        }

        return null;
    }

    @Override
    public ValueExpr visitTypeParameters(JavaParser.TypeParametersContext ctx) {
        List<ValueExpr> typeList = new ArrayList<>();
        if (ctx.typeParameter() != null) {
            for (JavaParser.TypeParameterContext typeCtx : ctx.typeParameter()) {
                RecordValueExpr typeRec = RecordFactory.create(context, (RecordTypeSymbol) moduleScope.resolve(JAVA_TYPE_PARAMETER));
                typeRec.updateFieldValue(context, "alias", new StringValueExpr(typeCtx.identifier().getText()));
                typeList.add(typeRec);
            }
        }
        return new ArrayValueExpr(
                ArrayTypeFactory.getArrayTypeFor((Type) moduleScope.resolve(JAVA_TYPE_PARAMETER)),
                typeList
        );
    }

    @Override
    public ValueExpr visitClassDeclaration(JavaParser.ClassDeclarationContext ctx) {
        RecordValueExpr defRecExpr = defStack.peek();
        defRecExpr.updateFieldValue(context, "package", new StringValueExpr(currentPackage));
        defRecExpr.updateFieldValue(context, "name", new StringValueExpr(ctx.identifier().getText()));

        if (ctx.typeParameters() != null) {
            defRecExpr.updateFieldValue(context, "typeParameters", visit(ctx.typeParameters()));
        } else {
            defRecExpr.updateFieldValue(context, "typeParameters", new ArrayValueExpr(
                    ArrayTypeFactory.getArrayTypeFor((Type) moduleScope.resolve(JAVA_TYPE_PARAMETER)),
                    new ArrayList<>()
            ));
        }

        if (ctx.typeType() != null) {
            defRecExpr.updateFieldValue(context, "superType", visit(ctx.typeType()));
        }

        if (ctx.typeList() != null && ctx.typeList().size() == 1) {
            JavaParser.TypeListContext typeListContext = ctx.typeList(1);
            if (ctx.PERMITS() == null || ctx.PERMITS().getSymbol().getStartIndex() > typeListContext.start.getStartIndex()) {
                if (typeListContext.typeType() != null) {
                    List<ValueExpr> interfaceList = new ArrayList<>();
                    for (JavaParser.TypeTypeContext typeTypeContext : typeListContext.typeType()) {
                        interfaceList.add(visit(typeTypeContext));
                    }
                    defRecExpr.updateFieldValue(context, "implements", new ArrayValueExpr(
                            ArrayTypeFactory.getArrayTypeFor((Type) moduleScope.resolve(JAVA_OBJECT_TYPE)),
                            interfaceList
                    ));
                }
            }
        }

        defRecExpr.updateFieldValue(context, "constructors", new ArrayValueExpr(
                ArrayTypeFactory.getArrayTypeFor((Type) moduleScope.resolve(JAVA_CONSTRUCTOR)),
                new ArrayList<>()
        ));
        defRecExpr.updateFieldValue(context, "fields", new ArrayValueExpr(
                ArrayTypeFactory.getArrayTypeFor((Type) moduleScope.resolve(JAVA_FIELD)),
                new ArrayList<>()
        ));
        defRecExpr.updateFieldValue(context, "methods", new ArrayValueExpr(
                ArrayTypeFactory.getArrayTypeFor((Type) moduleScope.resolve(JAVA_METHOD)),
                new ArrayList<>()
        ));
        defRecExpr.updateFieldValue(context, "innerClasses", new ArrayValueExpr(
                ArrayTypeFactory.getArrayTypeFor((Type) moduleScope.resolve(JAVA_CLASS)),
                new ArrayList<>()
        ));
        defRecExpr.updateFieldValue(context, "innerInterfaces", new ArrayValueExpr(
                ArrayTypeFactory.getArrayTypeFor((Type) moduleScope.resolve(JAVA_INTERFACE)),
                new ArrayList<>()
        ));
        defRecExpr.updateFieldValue(context, "innerEnums", new ArrayValueExpr(
                ArrayTypeFactory.getArrayTypeFor((Type) moduleScope.resolve(JAVA_ENUM)),
                new ArrayList<>()
        ));
        defRecExpr.updateFieldValue(context, "innerRecords", new ArrayValueExpr(
                ArrayTypeFactory.getArrayTypeFor((Type) moduleScope.resolve(JAVA_RECORD)),
                new ArrayList<>()
        ));

        if (ctx.classBody() != null && ctx.classBody().classBodyDeclaration() != null) {
            for (JavaParser.ClassBodyDeclarationContext classBodyDeclarationContext : ctx.classBody().classBodyDeclaration()) {
                visit(classBodyDeclarationContext);
            }
        }

        return null;
    }

    @Override
    public ValueExpr visitInterfaceDeclaration(JavaParser.InterfaceDeclarationContext ctx) {
        RecordValueExpr defRecExpr = defStack.peek();
        defRecExpr.updateFieldValue(context, "package", new StringValueExpr(currentPackage));
        defRecExpr.updateFieldValue(context, "name", new StringValueExpr(ctx.identifier().getText()));

        if (ctx.typeParameters() != null) {
            defRecExpr.updateFieldValue(context, "typeParameters", visit(ctx.typeParameters()));
        } else {
            defRecExpr.updateFieldValue(context, "typeParameters", new ArrayValueExpr(
                    ArrayTypeFactory.getArrayTypeFor((Type) moduleScope.resolve(JAVA_TYPE_PARAMETER)),
                    new ArrayList<>()
            ));
        }

        if (ctx.typeList() != null) {
            JavaParser.TypeListContext typeListContext = ctx.typeList();
            if (typeListContext.typeType() != null) {
                List<ValueExpr> interfaceList = new ArrayList<>();
                for (JavaParser.TypeTypeContext typeTypeContext : typeListContext.typeType()) {
                    interfaceList.add(visit(typeTypeContext));
                }
                defRecExpr.updateFieldValue(context, "superTypes", new ArrayValueExpr(
                        ArrayTypeFactory.getArrayTypeFor((Type) moduleScope.resolve(JAVA_OBJECT_TYPE)),
                        interfaceList
                ));
            }
        }

        defRecExpr.updateFieldValue(context, "fields", new ArrayValueExpr(
                ArrayTypeFactory.getArrayTypeFor((Type) moduleScope.resolve(JAVA_FIELD)),
                new ArrayList<>()
        ));
        defRecExpr.updateFieldValue(context, "methods", new ArrayValueExpr(
                ArrayTypeFactory.getArrayTypeFor((Type) moduleScope.resolve(JAVA_METHOD)),
                new ArrayList<>()
        ));
        defRecExpr.updateFieldValue(context, "innerClasses", new ArrayValueExpr(
                ArrayTypeFactory.getArrayTypeFor((Type) moduleScope.resolve(JAVA_CLASS)),
                new ArrayList<>()
        ));
        defRecExpr.updateFieldValue(context, "innerInterfaces", new ArrayValueExpr(
                ArrayTypeFactory.getArrayTypeFor((Type) moduleScope.resolve(JAVA_INTERFACE)),
                new ArrayList<>()
        ));
        defRecExpr.updateFieldValue(context, "innerEnums", new ArrayValueExpr(
                ArrayTypeFactory.getArrayTypeFor((Type) moduleScope.resolve(JAVA_ENUM)),
                new ArrayList<>()
        ));
        defRecExpr.updateFieldValue(context, "innerRecords", new ArrayValueExpr(
                ArrayTypeFactory.getArrayTypeFor((Type) moduleScope.resolve(JAVA_RECORD)),
                new ArrayList<>()
        ));

        if (ctx.interfaceBody() != null && ctx.interfaceBody().interfaceBodyDeclaration() != null) {
            for (JavaParser.InterfaceBodyDeclarationContext interfaceBodyDeclarationContext : ctx.interfaceBody().interfaceBodyDeclaration()) {
                visit(interfaceBodyDeclarationContext);
            }
        }

        return null;
    }

    @Override
    public ValueExpr visitEnumDeclaration(JavaParser.EnumDeclarationContext ctx) {
        RecordValueExpr defRecExpr = defStack.peek();
        defRecExpr.updateFieldValue(context, "package", new StringValueExpr(currentPackage));
        defRecExpr.updateFieldValue(context, "name", new StringValueExpr(ctx.identifier().getText()));

        if (ctx.typeList() != null) {
            JavaParser.TypeListContext typeListContext = ctx.typeList();
            if (typeListContext.typeType() != null) {
                List<ValueExpr> interfaceList = new ArrayList<>();
                for (JavaParser.TypeTypeContext typeTypeContext : typeListContext.typeType()) {
                    interfaceList.add(visit(typeTypeContext));
                }
                defRecExpr.updateFieldValue(context, "implements", new ArrayValueExpr(
                        ArrayTypeFactory.getArrayTypeFor((Type) moduleScope.resolve(JAVA_OBJECT_TYPE)),
                        interfaceList
                ));
            }
        }

        List<ValueExpr> values = new ArrayList<>();
        if (ctx.enumConstants() != null) {
            for (JavaParser.EnumConstantContext enumConstantContext : ctx.enumConstants().enumConstant()) {
                RecordValueExpr valueRec = RecordFactory.create(context,
                        (RecordTypeSymbol) moduleScope.resolve(JAVA_ENUM_VALUE));
                valueRec.updateFieldValue(context, "name",
                        new StringValueExpr(enumConstantContext.identifier().getText()));

                if (enumConstantContext.arguments() != null && enumConstantContext.arguments().expressionList() != null) {
                    List<ValueExpr> args = new ArrayList<>();
                    for (JavaParser.ExpressionContext exprContext : enumConstantContext.arguments().expressionList().expression()) {
                        args.add(visit(exprContext));
                    }
                    valueRec.updateFieldValue(context, "args", new ArrayValueExpr(
                            ArrayTypeFactory.getArrayTypeFor((Type) moduleScope.resolve(JAVA_VALUE)),
                            args
                    ));
                }

                values.add(valueRec);
            }
        }

        defRecExpr.updateFieldValue(context, "values", new ArrayValueExpr(
                ArrayTypeFactory.getArrayTypeFor((Type) moduleScope.resolve(JAVA_ENUM_VALUE)),
                values
        ));

        defRecExpr.updateFieldValue(context, "constructors", new ArrayValueExpr(
                ArrayTypeFactory.getArrayTypeFor((Type) moduleScope.resolve(JAVA_CONSTRUCTOR)),
                new ArrayList<>()
        ));
        defRecExpr.updateFieldValue(context, "fields", new ArrayValueExpr(
                ArrayTypeFactory.getArrayTypeFor((Type) moduleScope.resolve(JAVA_FIELD)),
                new ArrayList<>()
        ));
        defRecExpr.updateFieldValue(context, "methods", new ArrayValueExpr(
                ArrayTypeFactory.getArrayTypeFor((Type) moduleScope.resolve(JAVA_METHOD)),
                new ArrayList<>()
        ));
        defRecExpr.updateFieldValue(context, "innerClasses", new ArrayValueExpr(
                ArrayTypeFactory.getArrayTypeFor((Type) moduleScope.resolve(JAVA_CLASS)),
                new ArrayList<>()
        ));
        defRecExpr.updateFieldValue(context, "innerInterfaces", new ArrayValueExpr(
                ArrayTypeFactory.getArrayTypeFor((Type) moduleScope.resolve(JAVA_INTERFACE)),
                new ArrayList<>()
        ));
        defRecExpr.updateFieldValue(context, "innerEnums", new ArrayValueExpr(
                ArrayTypeFactory.getArrayTypeFor((Type) moduleScope.resolve(JAVA_ENUM)),
                new ArrayList<>()
        ));
        defRecExpr.updateFieldValue(context, "innerRecords", new ArrayValueExpr(
                ArrayTypeFactory.getArrayTypeFor((Type) moduleScope.resolve(JAVA_RECORD)),
                new ArrayList<>()
        ));

        if (ctx.enumBodyDeclarations() != null && ctx.enumBodyDeclarations().classBodyDeclaration() != null) {
            for (JavaParser.ClassBodyDeclarationContext classBodyDeclarationContext : ctx.enumBodyDeclarations().classBodyDeclaration()) {
                visit(classBodyDeclarationContext);
            }
        }

        return null;
    }

    @Override
    public ValueExpr visitRecordDeclaration(JavaParser.RecordDeclarationContext ctx) {
        RecordValueExpr defRecExpr = defStack.peek();
        defRecExpr.updateFieldValue(context, "package", new StringValueExpr(currentPackage));
        defRecExpr.updateFieldValue(context, "name", new StringValueExpr(ctx.identifier().getText()));

        List<ValueExpr> typeParametersList = new ArrayList<>();
        if (ctx.typeParameters() != null) {
            defRecExpr.updateFieldValue(context, "typeParameters", visit(ctx.typeParameters()));
        }
        defRecExpr.updateFieldValue(context, "typeParameters", new ArrayValueExpr(
                ArrayTypeFactory.getArrayTypeFor((Type) moduleScope.resolve(JAVA_TYPE_PARAMETER)),
                typeParametersList
        ));

        if (ctx.typeList() != null) {
            JavaParser.TypeListContext typeListContext = ctx.typeList();
            if (typeListContext.typeType() != null) {
                List<ValueExpr> interfaceList = new ArrayList<>();
                for (JavaParser.TypeTypeContext typeTypeContext : typeListContext.typeType()) {
                    interfaceList.add(visit(typeTypeContext));
                }
                defRecExpr.updateFieldValue(context, "implements", new ArrayValueExpr(
                        ArrayTypeFactory.getArrayTypeFor((Type) moduleScope.resolve(JAVA_OBJECT_TYPE)),
                        interfaceList
                ));
            }
        }

        defRecExpr.updateFieldValue(context, "constructors", new ArrayValueExpr(
                ArrayTypeFactory.getArrayTypeFor((Type) moduleScope.resolve(JAVA_CONSTRUCTOR)),
                new ArrayList<>()
        ));
        defRecExpr.updateFieldValue(context, "fields", new ArrayValueExpr(
                ArrayTypeFactory.getArrayTypeFor((Type) moduleScope.resolve(JAVA_FIELD)),
                new ArrayList<>()
        ));
        defRecExpr.updateFieldValue(context, "methods", new ArrayValueExpr(
                ArrayTypeFactory.getArrayTypeFor((Type) moduleScope.resolve(JAVA_METHOD)),
                new ArrayList<>()
        ));
        defRecExpr.updateFieldValue(context, "innerClasses", new ArrayValueExpr(
                ArrayTypeFactory.getArrayTypeFor((Type) moduleScope.resolve(JAVA_CLASS)),
                new ArrayList<>()
        ));
        defRecExpr.updateFieldValue(context, "innerInterfaces", new ArrayValueExpr(
                ArrayTypeFactory.getArrayTypeFor((Type) moduleScope.resolve(JAVA_INTERFACE)),
                new ArrayList<>()
        ));
        defRecExpr.updateFieldValue(context, "innerEnums", new ArrayValueExpr(
                ArrayTypeFactory.getArrayTypeFor((Type) moduleScope.resolve(JAVA_ENUM)),
                new ArrayList<>()
        ));
        defRecExpr.updateFieldValue(context, "innerRecords", new ArrayValueExpr(
                ArrayTypeFactory.getArrayTypeFor((Type) moduleScope.resolve(JAVA_RECORD)),
                new ArrayList<>()
        ));

        if (ctx.recordBody() != null && ctx.recordBody().classBodyDeclaration() != null) {
            for (JavaParser.ClassBodyDeclarationContext classBodyDeclarationContext : ctx.recordBody().classBodyDeclaration()) {
                visit(classBodyDeclarationContext);
            }
        }

        return null;
    }

    @Override
    public ValueExpr visitTypeType(JavaParser.TypeTypeContext ctx) {
        RecordValueExpr typeRec = null;

        if (ctx.classOrInterfaceType() != null) {
            typeRec = RecordFactory.create(context, (RecordTypeSymbol) moduleScope.resolve(JAVA_OBJECT_TYPE));
            RecordValueExpr currentType = null;
            for (ParseTree child : ctx.classOrInterfaceType().children) {
                if (child instanceof JavaParser.IdentifierContext) {
                    if (currentType == null) {
                        currentType = typeRec;
                    } else {
                        RecordValueExpr innerType = RecordFactory.create(context,
                                (RecordTypeSymbol) moduleScope.resolve(JAVA_OBJECT_TYPE));
                        currentType.updateFieldValue(context, "innerType", innerType);
                        currentType = innerType;
                    }

                    String fullName = child.getText();
                    String pkg;
                    String name;
                    int idx = fullName.lastIndexOf('.');
                    if (idx >= 0) {
                        pkg = fullName.substring(0, idx);
                        name = fullName.substring(idx + 1);
                    } else {
                        name = fullName;
                        pkg = imports.get(name);
                    }
                    if (pkg != null) {
                        currentType.updateFieldValue(context, "package", new StringValueExpr(pkg));
                    }
                    currentType.updateFieldValue(context, "name", new StringValueExpr(name));
                } else if (child instanceof JavaParser.TypeArgumentsContext) {
                    if (currentType == null) {
                        continue;
                    }
                    JavaParser.TypeArgumentsContext typeArgsCtx = (JavaParser.TypeArgumentsContext) child;

                    List<ValueExpr> typeArgList = new ArrayList<>();
                    if (typeArgsCtx.typeArgument() != null) {
                        for (JavaParser.TypeArgumentContext typeArgumentContext : typeArgsCtx.typeArgument()) {
                            if (typeArgumentContext.typeType() != null) {
                                typeArgList.add(visit(typeArgumentContext.typeType()));
                            }
                        }
                    }
                    currentType.updateFieldValue(context, "typeArgs", new ArrayValueExpr(
                            ArrayTypeFactory.getArrayTypeFor((Type) moduleScope.resolve(JAVA_OBJECT_TYPE)),
                            typeArgList
                    ));
                }
            }
        } else if (ctx.primitiveType() != null) {
            typeRec = (RecordValueExpr) visit(ctx.primitiveType());
        }

        return typeRec;
    }

    @Override
    public ValueExpr visitPrimitiveType(JavaParser.PrimitiveTypeContext ctx) {
        if (ctx.BOOLEAN() != null) {
            return RecordFactory.create(context, (RecordTypeSymbol) moduleScope.resolve(JAVA_BOOLEAN_TYPE));
        } else if (ctx.CHAR() != null) {
            return RecordFactory.create(context, (RecordTypeSymbol) moduleScope.resolve(JAVA_CHAR_TYPE));
        } else if (ctx.BYTE() != null) {
            return RecordFactory.create(context, (RecordTypeSymbol) moduleScope.resolve(JAVA_BYTE_TYPE));
        } else if (ctx.SHORT() != null) {
            return RecordFactory.create(context, (RecordTypeSymbol) moduleScope.resolve(JAVA_SHORT_TYPE));
        } else if (ctx.INT() != null) {
            return RecordFactory.create(context, (RecordTypeSymbol) moduleScope.resolve(JAVA_INT_TYPE));
        } else if (ctx.LONG() != null) {
            return RecordFactory.create(context, (RecordTypeSymbol) moduleScope.resolve(JAVA_LONG_TYPE));
        } else if (ctx.FLOAT() != null) {
            return RecordFactory.create(context, (RecordTypeSymbol) moduleScope.resolve(JAVA_FLOAT_TYPE));
        } else if (ctx.DOUBLE() != null) {
            return RecordFactory.create(context, (RecordTypeSymbol) moduleScope.resolve(JAVA_DOUBLE_TYPE));
        }
        return null;
    }

    @Override
    public ValueExpr visitClassBodyDeclaration(JavaParser.ClassBodyDeclarationContext ctx) {
        RecordValueExpr defRecExpr = defStack.peek();

        if (ctx.memberDeclaration() != null) {
            var memberCtx = ctx.memberDeclaration();
            if (memberCtx.constructorDeclaration() != null) {
                currentClassMember = RecordFactory.create(context, (RecordTypeSymbol) moduleScope.resolve(JAVA_CONSTRUCTOR));
                visit(memberCtx.constructorDeclaration());
                currentClassMember.updateFieldValue(context, "typeParameters", new ArrayValueExpr(
                        ArrayTypeFactory.getArrayTypeFor((Type) moduleScope.resolve(JAVA_TYPE_PARAMETER)),
                        new ArrayList<>()
                ));
                ArrayValueExpr constructors = (ArrayValueExpr) defRecExpr.resolveField("constructors");
                constructors.getValue().add(currentClassMember);
            } else if (memberCtx.genericConstructorDeclaration() != null) {
                currentClassMember = RecordFactory.create(context, (RecordTypeSymbol) moduleScope.resolve(JAVA_CONSTRUCTOR));
                visit(memberCtx.genericConstructorDeclaration().constructorDeclaration());
                if (memberCtx.genericConstructorDeclaration().typeParameters() != null) {
                    currentClassMember.updateFieldValue(context, "typeParameters",
                            visit(memberCtx.genericConstructorDeclaration().typeParameters()));
                }
                ArrayValueExpr constructors = (ArrayValueExpr) defRecExpr.resolveField("constructors");
                constructors.getValue().add(currentClassMember);
            } else if (memberCtx.methodDeclaration() != null) {
                currentClassMember = RecordFactory.create(context, (RecordTypeSymbol) moduleScope.resolve(JAVA_METHOD));
                visit(memberCtx.methodDeclaration());
                currentClassMember.updateFieldValue(context, "typeParameters", new ArrayValueExpr(
                        ArrayTypeFactory.getArrayTypeFor((Type) moduleScope.resolve(JAVA_TYPE_PARAMETER)),
                        new ArrayList<>()
                ));
                ArrayValueExpr methods = (ArrayValueExpr) defRecExpr.resolveField("methods");
                methods.getValue().add(currentClassMember);
            } else if (memberCtx.genericMethodDeclaration() != null) {
                currentClassMember = RecordFactory.create(context, (RecordTypeSymbol) moduleScope.resolve(JAVA_METHOD));
                visit(memberCtx.genericMethodDeclaration().methodDeclaration());
                if (memberCtx.genericMethodDeclaration().typeParameters() != null) {
                    currentClassMember.updateFieldValue(context, "typeParameters",
                            visit(memberCtx.genericMethodDeclaration().typeParameters()));
                }
                ArrayValueExpr methods = (ArrayValueExpr) defRecExpr.resolveField("methods");
                methods.getValue().add(currentClassMember);
            } else if (memberCtx.fieldDeclaration() != null) {
                RecordValueExpr typeRec = (RecordValueExpr) visit(memberCtx.fieldDeclaration().typeType());
                ArrayValueExpr fields = (ArrayValueExpr) defRecExpr.resolveField("fields");
                if (memberCtx.fieldDeclaration().variableDeclarators() != null) {
                    for (JavaParser.VariableDeclaratorContext declCtx : memberCtx.fieldDeclaration().variableDeclarators().variableDeclarator()) {
                        currentClassMember = RecordFactory.create(context, (RecordTypeSymbol) moduleScope.resolve(JAVA_FIELD));
                        currentClassMember.updateFieldValue(context, "fieldType", typeRec);
                        visit(declCtx);
                        fields.getValue().add(currentClassMember);
                    }
                }
            } else if (memberCtx.classDeclaration() != null) {
                RecordValueExpr innerClassRec = RecordFactory.create(moduleScope, context, JAVA_CLASS);
                defStack.push(innerClassRec);
                visit(memberCtx.classDeclaration());
                defStack.pop();
                ArrayValueExpr classes = (ArrayValueExpr) defRecExpr.resolveField("innerClasses");
                classes.getValue().add(innerClassRec);
                currentClassMember = innerClassRec;
            } else if (memberCtx.interfaceDeclaration() != null) {
                RecordValueExpr innerInterfaceRec = RecordFactory.create(moduleScope, context, JAVA_INTERFACE);
                defStack.push(innerInterfaceRec);
                visit(memberCtx.interfaceDeclaration());
                defStack.pop();
                ArrayValueExpr interfaces = (ArrayValueExpr) defRecExpr.resolveField("innerInterfaces");
                interfaces.getValue().add(innerInterfaceRec);
                currentClassMember = innerInterfaceRec;
            } else if (memberCtx.enumDeclaration() != null) {
                RecordValueExpr innerEnumRec = RecordFactory.create(moduleScope, context, JAVA_ENUM);
                defStack.push(innerEnumRec);
                visit(memberCtx.enumDeclaration());
                defStack.pop();
                ArrayValueExpr enums = (ArrayValueExpr) defRecExpr.resolveField("innerEnums");
                enums.getValue().add(innerEnumRec);
                currentClassMember = innerEnumRec;
            } else if (memberCtx.recordDeclaration() != null) {
                RecordValueExpr innerRecordRec = RecordFactory.create(moduleScope, context, JAVA_RECORD);
                defStack.push(innerRecordRec);
                visit(memberCtx.recordDeclaration());
                defStack.pop();
                ArrayValueExpr records = (ArrayValueExpr) defRecExpr.resolveField("innerRecords");
                records.getValue().add(innerRecordRec);
                currentClassMember = innerRecordRec;
            }
        }

        if (currentClassMember == null) {
            return null;
        }

        currentClassMember.updateFieldValue(context, "abstract", BooleanValueExpr.FALSE);
        currentClassMember.updateFieldValue(context, "final", BooleanValueExpr.FALSE);
        currentClassMember.updateFieldValue(context, "static", BooleanValueExpr.FALSE);
        currentClassMember.updateFieldValue(context, "public", BooleanValueExpr.FALSE);
        currentClassMember.updateFieldValue(context, "private", BooleanValueExpr.FALSE);
        currentClassMember.updateFieldValue(context, "protected", BooleanValueExpr.FALSE);
        currentClassMember.updateFieldValue(context, "default", BooleanValueExpr.FALSE);
        currentClassMember.updateFieldValue(context, "annotations", new ArrayValueExpr(
                ArrayTypeFactory.getArrayTypeFor((Type) moduleScope.resolve(JAVA_ANNOTATION_VALUE)),
                new ArrayList<>()
        ));

        if (ctx.modifier() != null) {
            for (JavaParser.ModifierContext modifierContext : ctx.modifier()) {
                JavaParser.ClassOrInterfaceModifierContext ciModifierCtx = modifierContext.classOrInterfaceModifier();
                if (ciModifierCtx != null) {
                    if (ciModifierCtx.annotation() != null) {
                        var recAnn = visit(ciModifierCtx.annotation());
                        ArrayValueExpr annList = (ArrayValueExpr) currentClassMember.resolveField("annotations");
                        annList.getValue().add(recAnn);
                    } else if (ciModifierCtx.PUBLIC() != null) {
                        currentClassMember.updateFieldValue(context, "public", BooleanValueExpr.TRUE);
                    } else if (ciModifierCtx.PROTECTED() != null) {
                        currentClassMember.updateFieldValue(context, "protected", BooleanValueExpr.TRUE);
                    } else if (ciModifierCtx.PRIVATE() != null) {
                        currentClassMember.updateFieldValue(context, "private", BooleanValueExpr.TRUE);
                    } else if (ciModifierCtx.STATIC() != null) {
                        currentClassMember.updateFieldValue(context, "static", BooleanValueExpr.TRUE);
                    } else if (ciModifierCtx.ABSTRACT() != null) {
                        currentClassMember.updateFieldValue(context, "abstract", BooleanValueExpr.TRUE);
                    } else if (ciModifierCtx.FINAL() != null) {
                        currentClassMember.updateFieldValue(context, "final", BooleanValueExpr.TRUE);
                    }
                }
            }
        }

        currentClassMember = null;
        return null;
    }

    @Override
    public ValueExpr visitInterfaceBodyDeclaration(JavaParser.InterfaceBodyDeclarationContext ctx) {
        RecordValueExpr defRecExpr = defStack.peek();

        if (ctx.interfaceMemberDeclaration() != null) {
            var memberCtx = ctx.interfaceMemberDeclaration();
            if (memberCtx.interfaceMethodDeclaration() != null) {
                currentClassMember = RecordFactory.create(context, (RecordTypeSymbol) moduleScope.resolve(JAVA_METHOD));
                visit(memberCtx.interfaceMethodDeclaration());
                currentClassMember.updateFieldValue(context, "typeParameters", new ArrayValueExpr(
                        ArrayTypeFactory.getArrayTypeFor((Type) moduleScope.resolve(JAVA_TYPE_PARAMETER)),
                        new ArrayList<>()
                ));
                ArrayValueExpr methods = (ArrayValueExpr) defRecExpr.resolveField("methods");
                methods.getValue().add(currentClassMember);
            } else if (memberCtx.genericInterfaceMethodDeclaration() != null) {
                currentClassMember = RecordFactory.create(context, (RecordTypeSymbol) moduleScope.resolve(JAVA_METHOD));
                if (memberCtx.genericInterfaceMethodDeclaration().interfaceMethodModifier() != null) {
                    for (JavaParser.InterfaceMethodModifierContext modifierCtx : memberCtx.genericInterfaceMethodDeclaration().interfaceMethodModifier()) {
                        visit(modifierCtx);
                    }
                }
                visit(memberCtx.genericInterfaceMethodDeclaration().interfaceCommonBodyDeclaration());
                if (memberCtx.genericInterfaceMethodDeclaration().typeParameters() != null) {
                    currentClassMember.updateFieldValue(context, "typeParameters",
                            visit(memberCtx.genericInterfaceMethodDeclaration().typeParameters()));
                }
                ArrayValueExpr methods = (ArrayValueExpr) defRecExpr.resolveField("methods");
                methods.getValue().add(currentClassMember);
            } else if (memberCtx.constDeclaration() != null) {
                RecordValueExpr typeRec = (RecordValueExpr) visit(memberCtx.constDeclaration().typeType());
                ArrayValueExpr fields = (ArrayValueExpr) defRecExpr.resolveField("fields");
                if (memberCtx.constDeclaration().constantDeclarator() != null) {
                    for (JavaParser.ConstantDeclaratorContext constantDeclaratorContext : memberCtx.constDeclaration().constantDeclarator()) {
                        currentClassMember = RecordFactory.create(context, (RecordTypeSymbol) moduleScope.resolve(JAVA_FIELD));
                        currentClassMember.updateFieldValue(context, "fieldType", typeRec);
                        visit(constantDeclaratorContext);
                        fields.getValue().add(currentClassMember);
                    }
                }
            } else if (memberCtx.classDeclaration() != null) {
                RecordValueExpr innerClassRec = RecordFactory.create(moduleScope, context, JAVA_CLASS);
                defStack.push(innerClassRec);
                visit(memberCtx.classDeclaration());
                defStack.pop();
                ArrayValueExpr classes = (ArrayValueExpr) defRecExpr.resolveField("innerClasses");
                classes.getValue().add(innerClassRec);
                currentClassMember = innerClassRec;
            } else if (memberCtx.interfaceDeclaration() != null) {
                RecordValueExpr innerInterfaceRec = RecordFactory.create(moduleScope, context, JAVA_INTERFACE);
                defStack.push(innerInterfaceRec);
                visit(memberCtx.interfaceDeclaration());
                defStack.pop();
                ArrayValueExpr interfaces = (ArrayValueExpr) defRecExpr.resolveField("innerInterfaces");
                interfaces.getValue().add(innerInterfaceRec);
                currentClassMember = innerInterfaceRec;
            } else if (memberCtx.enumDeclaration() != null) {
                RecordValueExpr innerEnumRec = RecordFactory.create(moduleScope, context, JAVA_ENUM);
                defStack.push(innerEnumRec);
                visit(memberCtx.enumDeclaration());
                defStack.pop();
                ArrayValueExpr enums = (ArrayValueExpr) defRecExpr.resolveField("innerEnums");
                enums.getValue().add(innerEnumRec);
                currentClassMember = innerEnumRec;
            } else if (memberCtx.recordDeclaration() != null) {
                RecordValueExpr innerRecordRec = RecordFactory.create(moduleScope, context, JAVA_RECORD);
                defStack.push(innerRecordRec);
                visit(memberCtx.recordDeclaration());
                defStack.pop();
                ArrayValueExpr records = (ArrayValueExpr) defRecExpr.resolveField("innerRecords");
                records.getValue().add(innerRecordRec);
                currentClassMember = innerRecordRec;
            }
        }

        if (currentClassMember == null) {
            return null;
        }

        currentClassMember.updateFieldValue(context, "abstract", BooleanValueExpr.FALSE);
        currentClassMember.updateFieldValue(context, "final", BooleanValueExpr.FALSE);
        currentClassMember.updateFieldValue(context, "static", BooleanValueExpr.FALSE);
        currentClassMember.updateFieldValue(context, "public", BooleanValueExpr.FALSE);
        currentClassMember.updateFieldValue(context, "private", BooleanValueExpr.FALSE);
        currentClassMember.updateFieldValue(context, "protected", BooleanValueExpr.FALSE);
        currentClassMember.updateFieldValue(context, "default", BooleanValueExpr.FALSE);

        if (ctx.modifier() != null) {
            for (JavaParser.ModifierContext modifierContext : ctx.modifier()) {
                JavaParser.ClassOrInterfaceModifierContext ciModifierCtx = modifierContext.classOrInterfaceModifier();
                if (ciModifierCtx != null) {
                    if (ciModifierCtx.annotation() != null) {
                        var recAnn = visit(ciModifierCtx.annotation());
                        ArrayValueExpr annList = (ArrayValueExpr) currentClassMember.resolveField("annotations");
                        annList.getValue().add(recAnn);
                    } else if (ciModifierCtx.PUBLIC() != null) {
                        currentClassMember.updateFieldValue(context, "public", BooleanValueExpr.TRUE);
                    } else if (ciModifierCtx.PROTECTED() != null) {
                        currentClassMember.updateFieldValue(context, "protected", BooleanValueExpr.TRUE);
                    } else if (ciModifierCtx.PRIVATE() != null) {
                        currentClassMember.updateFieldValue(context, "private", BooleanValueExpr.TRUE);
                    } else if (ciModifierCtx.STATIC() != null) {
                        currentClassMember.updateFieldValue(context, "static", BooleanValueExpr.TRUE);
                    } else if (ciModifierCtx.ABSTRACT() != null) {
                        currentClassMember.updateFieldValue(context, "abstract", BooleanValueExpr.TRUE);
                    } else if (ciModifierCtx.FINAL() != null) {
                        currentClassMember.updateFieldValue(context, "final", BooleanValueExpr.TRUE);
                    }
                }
            }
        }

        currentClassMember = null;
        return null;
    }

    @Override
    public ValueExpr visitConstructorDeclaration(JavaParser.ConstructorDeclarationContext ctx) {
        List<ValueExpr> params = new ArrayList<>();
        if (ctx.formalParameters() != null && ctx.formalParameters().formalParameterList() != null) {
            JavaParser.FormalParameterListContext paramListCtx = ctx.formalParameters().formalParameterList();
            if (paramListCtx.formalParameter() != null) {
                for (JavaParser.FormalParameterContext paramContext : paramListCtx.formalParameter()) {
                    params.add(visit(paramContext));
                }
            }
            if (paramListCtx.lastFormalParameter() != null) {
                params.add(visit(paramListCtx.lastFormalParameter()));
            }

        }
        currentClassMember.updateFieldValue(context, "parameters", new ArrayValueExpr(
                ArrayTypeFactory.getArrayTypeFor((Type) moduleScope.resolve(JAVA_METHOD_PARAMETER)),
                params
        ));
        return null;
    }

    @Override
    public ValueExpr visitFormalParameter(JavaParser.FormalParameterContext ctx) {
        RecordValueExpr paramRec = RecordFactory.create(context,
                (RecordTypeSymbol) moduleScope.resolve(JAVA_METHOD_PARAMETER));
        String name = ctx.variableDeclaratorId().identifier().getText();
        RecordValueExpr type = (RecordValueExpr) visit(ctx.typeType());
        List<ValueExpr> annotations = new ArrayList<>();
        if (ctx.variableModifier() != null) {
            for (JavaParser.VariableModifierContext modifierCtx : ctx.variableModifier()) {
                if (modifierCtx.annotation() != null) {
                    annotations.add(visit(modifierCtx.annotation()));
                } else if (modifierCtx.FINAL() != null) {
                    paramRec.updateFieldValue(context, "final", BooleanValueExpr.TRUE);
                }
            }
        }
        paramRec.updateFieldValue(context, "paramName", new StringValueExpr(name));
        paramRec.updateFieldValue(context, "paramType", type);
        paramRec.updateFieldValue(context, "annotations", new ArrayValueExpr(
                ArrayTypeFactory.getArrayTypeFor((Type) moduleScope.resolve(JAVA_ANNOTATION_VALUE)),
                annotations
        ));
        paramRec.updateFieldValue(context, "varArg", BooleanValueExpr.FALSE);
        return paramRec;
    }

    @Override
    public ValueExpr visitLastFormalParameter(JavaParser.LastFormalParameterContext ctx) {
        RecordValueExpr paramRec = RecordFactory.create(context,
                (RecordTypeSymbol) moduleScope.resolve(JAVA_METHOD_PARAMETER));
        String name = ctx.variableDeclaratorId().identifier().getText();
        RecordValueExpr type = (RecordValueExpr) visit(ctx.typeType());
        List<ValueExpr> annotations = new ArrayList<>();
        if (ctx.variableModifier() != null) {
            for (JavaParser.VariableModifierContext modifierCtx : ctx.variableModifier()) {
                if (modifierCtx.annotation() != null) {
                    annotations.add(visit(modifierCtx.annotation()));
                } else if (modifierCtx.FINAL() != null) {
                    paramRec.updateFieldValue(context, "final", BooleanValueExpr.TRUE);
                }
            }
        }
        paramRec.updateFieldValue(context, "paramName", new StringValueExpr(name));
        paramRec.updateFieldValue(context, "paramType", type);
        paramRec.updateFieldValue(context, "annotations", new ArrayValueExpr(
                ArrayTypeFactory.getArrayTypeFor((Type) moduleScope.resolve(JAVA_ANNOTATION_VALUE)),
                annotations
        ));
        paramRec.updateFieldValue(context, "varArg", BooleanValueExpr.TRUE);
        return paramRec;
    }

    @Override
    public ValueExpr visitVariableDeclarator(JavaParser.VariableDeclaratorContext ctx) {
        currentClassMember.updateFieldValue(context, "name",
                new StringValueExpr(ctx.variableDeclaratorId().identifier().getText()));

        if (ctx.variableInitializer() != null && ctx.variableInitializer().expression() != null) {
            currentClassMember.updateFieldValue(context, "value",
                    visit(ctx.variableInitializer().expression()));
        }

        return null;
    }

    @Override
    public ValueExpr visitConstantDeclarator(JavaParser.ConstantDeclaratorContext ctx) {
        currentClassMember.updateFieldValue(context, "name",
                new StringValueExpr(ctx.identifier().getText()));

        if (ctx.variableInitializer() != null && ctx.variableInitializer().expression() != null) {
            currentClassMember.updateFieldValue(context, "value",
                    visit(ctx.variableInitializer().expression()));
        }

        return null;
    }

    @Override
    public ValueExpr visitMethodDeclaration(JavaParser.MethodDeclarationContext ctx) {
        String name = ctx.identifier().getText();
        RecordValueExpr returnTypeRec = null;
        if (ctx.typeTypeOrVoid() != null) {
            if (ctx.typeTypeOrVoid().VOID() != null) {
                returnTypeRec = RecordFactory.create(context, (RecordTypeSymbol) moduleScope.resolve(JAVA_VOID_TYPE));
            } else if (ctx.typeTypeOrVoid().typeType() != null) {
                returnTypeRec = (RecordValueExpr) visit(ctx.typeTypeOrVoid().typeType());
            }
        }

        List<ValueExpr> params = new ArrayList<>();
        if (ctx.formalParameters() != null && ctx.formalParameters().formalParameterList() != null) {
            JavaParser.FormalParameterListContext paramListCtx = ctx.formalParameters().formalParameterList();
            if (paramListCtx.formalParameter() != null) {
                for (JavaParser.FormalParameterContext paramContext : paramListCtx.formalParameter()) {
                    params.add(visit(paramContext));
                }
            }
            if (paramListCtx.lastFormalParameter() != null) {
                params.add(visit(paramListCtx.lastFormalParameter()));
            }

        }

        currentClassMember.updateFieldValue(context, "name", new StringValueExpr(name));
        currentClassMember.updateFieldValue(context, "returnType", returnTypeRec);
        currentClassMember.updateFieldValue(context, "parameters", new ArrayValueExpr(
                ArrayTypeFactory.getArrayTypeFor((Type) moduleScope.resolve(JAVA_METHOD_PARAMETER)),
                params
        ));
        return null;
    }

    @Override
    public ValueExpr visitInterfaceMethodDeclaration(JavaParser.InterfaceMethodDeclarationContext ctx) {
        if (ctx.interfaceMethodModifier() != null) {
            for (JavaParser.InterfaceMethodModifierContext modifierCtx : ctx.interfaceMethodModifier()) {
                visit(modifierCtx);
            }
        }
        if (ctx.interfaceCommonBodyDeclaration() != null) {
            visit(ctx.interfaceCommonBodyDeclaration());
        }
        return null;
    }

    @Override
    public ValueExpr visitInterfaceMethodModifier(JavaParser.InterfaceMethodModifierContext ctx) {
        if (ctx.DEFAULT() != null) {
            currentClassMember.updateFieldValue(context, "default", BooleanValueExpr.TRUE);
        }
        return null;
    }

    @Override
    public ValueExpr visitInterfaceCommonBodyDeclaration(JavaParser.InterfaceCommonBodyDeclarationContext ctx) {
        String name = ctx.identifier().getText();
        RecordValueExpr returnTypeRec = null;
        if (ctx.typeTypeOrVoid() != null) {
            if (ctx.typeTypeOrVoid().VOID() != null) {
                returnTypeRec = RecordFactory.create(context, (RecordTypeSymbol) moduleScope.resolve(JAVA_VOID_TYPE));
            } else if (ctx.typeTypeOrVoid().typeType() != null) {
                returnTypeRec = (RecordValueExpr) visit(ctx.typeTypeOrVoid().typeType());
            }
        }

        List<ValueExpr> params = new ArrayList<>();
        if (ctx.formalParameters() != null && ctx.formalParameters().formalParameterList() != null) {
            JavaParser.FormalParameterListContext paramListCtx = ctx.formalParameters().formalParameterList();
            if (paramListCtx.formalParameter() != null) {
                for (JavaParser.FormalParameterContext paramContext : paramListCtx.formalParameter()) {
                    params.add(visit(paramContext));
                }
            }
            if (paramListCtx.lastFormalParameter() != null) {
                params.add(visit(paramListCtx.lastFormalParameter()));
            }

        }

        currentClassMember.updateFieldValue(context, "name", new StringValueExpr(name));
        currentClassMember.updateFieldValue(context, "returnType", returnTypeRec);
        currentClassMember.updateFieldValue(context, "parameters", new ArrayValueExpr(
                ArrayTypeFactory.getArrayTypeFor((Type) moduleScope.resolve(JAVA_METHOD_PARAMETER)),
                params
        ));
        return null;
    }
}
