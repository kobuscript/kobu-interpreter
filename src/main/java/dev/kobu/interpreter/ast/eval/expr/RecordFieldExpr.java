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

package dev.kobu.interpreter.ast.eval.expr;

import dev.kobu.interpreter.ast.eval.*;
import dev.kobu.interpreter.ast.eval.context.EvalContext;
import dev.kobu.interpreter.ast.eval.context.EvalModeEnum;
import dev.kobu.interpreter.ast.symbol.ModuleScope;
import dev.kobu.interpreter.ast.symbol.RecordTypeSymbol;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.ast.symbol.Type;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RecordFieldExpr implements Expr, HasTargetType, HasElementRef {

    private final SourceCodeRef sourceCodeRef;

    private final Type recordType;

    private final String fieldName;

    private final Expr expr;

    private Type targetType;

    private Type type;

    private SourceCodeRef elementRef;

    public RecordFieldExpr(SourceCodeRef sourceCodeRef, Type recordType, String fieldName, Expr expr) {
        this.sourceCodeRef = sourceCodeRef;
        this.recordType = recordType;
        this.fieldName = fieldName;
        this.expr = expr;
    }

    @Override
    public SourceCodeRef getSourceCodeRef() {
        return sourceCodeRef;
    }

    @Override
    public void setResolvedTypes(Map<String, Type> resolvedTypes) {
        if (expr != null) {
            expr.setResolvedTypes(resolvedTypes);
        }
    }

    @Override
    public void analyze(EvalContext context) {
        if (expr instanceof HasTargetType) {
            ((HasTargetType)expr).setTargetType(targetType);
        }
        expr.analyze(context);
        type = expr.getType();

        if (context.getEvalMode() == EvalModeEnum.ANALYZER_SERVICE && recordType instanceof RecordTypeSymbol) {
            var attr = ((RecordTypeSymbol) recordType).getAttribute(fieldName);
            if (attr != null) {
                elementRef = attr.getSourceCodeRef();
            } else {
                var starAttr = ((RecordTypeSymbol) recordType).getStarAttribute();
                if (starAttr != null) {
                    elementRef = starAttr.getSourceCodeRef();
                }
            }
        }
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public ValueExpr evalExpr(EvalContext context) {
        return expr.evalExpr(context);
    }

    public String getFieldName() {
        return fieldName;
    }

    public Expr getExpr() {
        return expr;
    }

    @Override
    public Type getTargetType() {
        return targetType;
    }

    @Override
    public void setTargetType(Type targetType) {
        this.targetType = targetType;
    }

    @Override
    public List<SymbolDescriptor> requestSuggestions(List<ModuleScope> externalModules) {
        if (recordType instanceof RecordTypeSymbol) {
            return ((RecordTypeSymbol) recordType).getAttributes().values().stream()
                    .map(attr -> new FieldDescriptor(attr.getName(), attr.getType().getName()))
                    .map(SymbolDescriptor::new)
                    .collect(Collectors.toList());
        }
        return null;
    }

    @Override
    public boolean hasOwnCompletionScope() {
        return false;
    }

    @Override
    public SourceCodeRef getElementRef() {
        return elementRef;
    }
}
