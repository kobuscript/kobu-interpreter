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

package dev.kobu.interpreter.codec.impl.command;

import dev.kobu.antlr.java.JavaParser;
import dev.kobu.antlr.java.JavaParserBaseVisitor;
import dev.kobu.interpreter.ast.eval.ValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.ArrayValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.RecordValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.StringValueExpr;
import dev.kobu.interpreter.ast.symbol.ModuleScope;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.ast.utils.RecordUtils;
import dev.kobu.interpreter.codec.command.TextFileCommandProducer;
import dev.kobu.interpreter.codec.impl.JavaParserVisitor;
import dev.kobu.interpreter.error.eval.IllegalArgumentError;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class JavaCommandProducer  extends JavaParserBaseVisitor<Void> implements TextFileCommandProducer {

    protected final ModuleScope moduleScope;

    protected int bodyStartIdx;

    protected int lastFieldStopIdx;

    protected int lastConstructorStopIdx;

    protected int lastMethodStopIdx;

    protected int lastInnerDefStopIdx;

    protected String mainClassName;

    protected boolean mainClass;

    protected final Map<String, Ref> fieldMap = new HashMap<>();

    protected final List<MethodRef> constructorList = new ArrayList<>();

    protected final List<MethodRef> methodList = new ArrayList<>();

    protected final Map<String, Ref> innerDefMap = new HashMap<>();

    public JavaCommandProducer(ModuleScope moduleScope) {
        this.moduleScope = moduleScope;
    }

    @Override
    public Void visitClassDeclaration(JavaParser.ClassDeclarationContext ctx) {
        if (mainClass) {
            lastInnerDefStopIdx = ctx.stop.getStopIndex();
            innerDefMap.put(ctx.identifier().getText(), new Ref(ctx.start.getStartIndex(), lastInnerDefStopIdx));
            return null;
        }
        mainClass = ctx.identifier().getText().equals(mainClassName);
        if (mainClass) {
            bodyStartIdx = ctx.classBody().LBRACE().getSymbol().getStartIndex();
            visit(ctx.classBody());
        }
        mainClass = false;
        return null;
    }

    @Override
    public Void visitRecordDeclaration(JavaParser.RecordDeclarationContext ctx) {
        if (mainClass) {
            lastInnerDefStopIdx = ctx.stop.getStopIndex();
            innerDefMap.put(ctx.identifier().getText(), new Ref(ctx.start.getStartIndex(), lastInnerDefStopIdx));
            return null;
        }
        mainClass = ctx.identifier().getText().equals(mainClassName);
        if (mainClass) {
            bodyStartIdx = ctx.recordBody().LBRACE().getSymbol().getStartIndex();
            visit(ctx.recordBody());
        }
        mainClass = false;
        return null;
    }

    @Override
    public Void visitInterfaceDeclaration(JavaParser.InterfaceDeclarationContext ctx) {
        if (mainClass) {
            lastInnerDefStopIdx = ctx.stop.getStopIndex();
            innerDefMap.put(ctx.identifier().getText(), new Ref(ctx.start.getStartIndex(), lastInnerDefStopIdx));
            return null;
        }
        mainClass = ctx.identifier().getText().equals(mainClassName);
        if (mainClass) {
            bodyStartIdx = ctx.interfaceBody().LBRACE().getSymbol().getStartIndex();
            visit(ctx.interfaceBody());
        }
        mainClass = false;
        return null;
    }

    @Override
    public Void visitFieldDeclaration(JavaParser.FieldDeclarationContext ctx) {
        lastFieldStopIdx = ctx.stop.getStopIndex();
        for (JavaParser.VariableDeclaratorContext variableDeclaratorCtx : ctx.variableDeclarators().variableDeclarator()) {
            fieldMap.put(variableDeclaratorCtx.variableDeclaratorId().identifier().getText(),
                    new Ref(ctx.start.getStartIndex(), lastFieldStopIdx));
        }
        return null;
    }

    @Override
    public Void visitConstructorDeclaration(JavaParser.ConstructorDeclarationContext ctx) {
        lastConstructorStopIdx = ctx.stop.getStopIndex();

        List<String> consParamTypes = new ArrayList<>();
        JavaParser.FormalParameterListContext formalParameterListCtx = ctx.formalParameters().formalParameterList();
        if (formalParameterListCtx != null) {
            for (JavaParser.FormalParameterContext formalParameterCtx : formalParameterListCtx.formalParameter()) {
                consParamTypes.add(formalParameterCtx.typeType().getText().replaceAll("\\S+", ""));
            }
            if (formalParameterListCtx.lastFormalParameter() != null) {
                consParamTypes.add(formalParameterListCtx.lastFormalParameter().typeType().getText()
                        .replaceAll("\\S+", ""));
            }
        }

        constructorList.add(new MethodRef(consParamTypes, ctx.start.getStartIndex(), lastConstructorStopIdx));
        return null;
    }

    @Override
    public Void visitMethodDeclaration(JavaParser.MethodDeclarationContext ctx) {
        lastMethodStopIdx = ctx.stop.getStopIndex();

        List<String> methodParamTypes = new ArrayList<>();
        JavaParser.FormalParameterListContext formalParameterListCtx = ctx.formalParameters().formalParameterList();
        if (formalParameterListCtx != null) {
            for (JavaParser.FormalParameterContext formalParameterCtx : formalParameterListCtx.formalParameter()) {
                methodParamTypes.add(formalParameterCtx.typeType().getText().replaceAll("\\S+", ""));
            }
            if (formalParameterListCtx.lastFormalParameter() != null) {
                methodParamTypes.add(formalParameterListCtx.lastFormalParameter().typeType().getText()
                        .replaceAll("\\S+", ""));
            }
        }
        String name = ctx.identifier().getText();
        methodList.add(new MethodRef(name, methodParamTypes, ctx.start.getStartIndex(), lastMethodStopIdx));

        return null;
    }

    protected String getMainClassName(String filePath) {
        String fileName = Path.of(filePath).getFileName().toString();
        int idx = fileName.lastIndexOf('.');
        if (idx > 0) {
            fileName = fileName.substring(0, idx);
        }
        return fileName;
    }

    protected List<String> extractParamTypes(RecordValueExpr methodRec, SourceCodeRef sourceCodeRef) {
        ArrayValueExpr paramsExpr = (ArrayValueExpr) RecordUtils.getRequiredField(methodRec, "parameters", sourceCodeRef);
        List<String> paramTypes = new ArrayList<>();
        for (ValueExpr valueExpr : paramsExpr.getValue()) {
            RecordValueExpr paramRecExpr = (RecordValueExpr) valueExpr;
            paramTypes.add(javaTypeToString(
                    (RecordValueExpr) RecordUtils.getRequiredField(paramRecExpr, "paramType", sourceCodeRef),
                    sourceCodeRef));
        }
        return paramTypes;
    }

    protected String javaTypeToString(RecordValueExpr javaTypeRec, SourceCodeRef sourceCodeRef) {
        if (RecordUtils.recordOfType(moduleScope, javaTypeRec, JavaParserVisitor.JAVA_INT_TYPE)) {
            return "int";
        } else if (RecordUtils.recordOfType(moduleScope, javaTypeRec, JavaParserVisitor.JAVA_CHAR_TYPE)) {
            return "char";
        } else if (RecordUtils.recordOfType(moduleScope, javaTypeRec, JavaParserVisitor.JAVA_BYTE_TYPE)) {
            return "byte";
        } else if (RecordUtils.recordOfType(moduleScope, javaTypeRec, JavaParserVisitor.JAVA_SHORT_TYPE)) {
            return "short";
        } else if (RecordUtils.recordOfType(moduleScope, javaTypeRec, JavaParserVisitor.JAVA_LONG_TYPE)) {
            return "long";
        } else if (RecordUtils.recordOfType(moduleScope, javaTypeRec, JavaParserVisitor.JAVA_FLOAT_TYPE)) {
            return "float";
        } else if (RecordUtils.recordOfType(moduleScope, javaTypeRec, JavaParserVisitor.JAVA_DOUBLE_TYPE)) {
            return "double";
        } else if (RecordUtils.recordOfType(moduleScope, javaTypeRec, JavaParserVisitor.JAVA_BOOLEAN_TYPE)) {
            return "boolean";
        } else if (RecordUtils.recordOfType(moduleScope, javaTypeRec, JavaParserVisitor.JAVA_VOID_TYPE)) {
            return "void";
        } else if (RecordUtils.recordOfType(moduleScope, javaTypeRec, JavaParserVisitor.JAVA_OBJECT_TYPE)) {
            StringBuilder str = new StringBuilder();
            StringValueExpr nameExpr = (StringValueExpr) RecordUtils.getRequiredField(javaTypeRec, "name", sourceCodeRef);
            str.append(nameExpr.getValue());
            ArrayValueExpr argsExpr = (ArrayValueExpr) javaTypeRec.resolveField("typeArgs");
            if (argsExpr != null && !argsExpr.getValue().isEmpty()) {
                str.append("<");
                str.append(argsExpr.getValue().stream().map(arg -> javaTypeToString((RecordValueExpr) arg, sourceCodeRef))
                        .collect(Collectors.joining(", ")));
                str.append(">");
            }
            RecordValueExpr innerTypeExpr = (RecordValueExpr) javaTypeRec.resolveField("innerType");
            if (innerTypeExpr != null) {
                str.append(".");
                str.append(javaTypeToString(innerTypeExpr, sourceCodeRef));
            }

            return str.toString();
        }

        throw new IllegalArgumentError("Invalid java type: " + javaTypeRec.getType().getName(), sourceCodeRef);
    }

    protected static class Ref {

        int startIdx;

        int stopIdx;

        public Ref(int startIdx, int stopIdx) {
            this.startIdx = startIdx;
            this.stopIdx = stopIdx;
        }

    }

    protected static class MethodRef {

        String name;

        List<String> paramTypes;

        int startIdx;

        int stopIdx;

        public MethodRef(String name, List<String> paramTypes, int startIdx, int stopIdx) {
            this.name = name;
            this.paramTypes = paramTypes;
            this.startIdx = startIdx;
            this.stopIdx = stopIdx;
        }

        public MethodRef(List<String> paramTypes, int startIdx, int stopIdx) {
            this.paramTypes = paramTypes;
            this.startIdx = startIdx;
            this.stopIdx = stopIdx;
        }

    }
}
