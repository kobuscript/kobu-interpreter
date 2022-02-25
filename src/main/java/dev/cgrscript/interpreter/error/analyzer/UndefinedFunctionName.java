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

package dev.cgrscript.interpreter.error.analyzer;

import dev.cgrscript.interpreter.ast.AstNode;
import dev.cgrscript.interpreter.ast.eval.SymbolTypeEnum;
import dev.cgrscript.interpreter.ast.eval.expr.FunctionCallExpr;
import dev.cgrscript.interpreter.error.AnalyzerError;
import dev.cgrscript.interpreter.error.CgrScriptActionTypeEnum;

public class UndefinedFunctionName extends AnalyzerError {

    private static final CgrScriptActionTypeEnum[] actions = new CgrScriptActionTypeEnum[]{
            CgrScriptActionTypeEnum.AUTO_IMPORT,
            CgrScriptActionTypeEnum.CREATE_FUNCTION
    };

    private final FunctionCallExpr functionCallExpr;

    private final String moduleId;

    private final String functionName;

    private final int newDefInsertionPoint;

    public UndefinedFunctionName(FunctionCallExpr functionCallExpr, String moduleId, String functionName,
                                 int newDefInsertionPoint) {
        super(functionCallExpr.getSourceCodeRef());
        this.functionCallExpr = functionCallExpr;
        this.moduleId = moduleId;
        this.functionName = functionName;
        this.newDefInsertionPoint = newDefInsertionPoint;
    }

    @Override
    public String getDescription() {
        if (moduleId != null) {
            return "'" + functionName + "' is not defined in module '" + moduleId + "'";
        } else {
            return "'" + functionName + "' is not defined";
        }
    }

    @Override
    public CgrScriptActionTypeEnum[] actions() {
        return actions;
    }

    @Override
    public AstNode getAstNode() {
        return functionCallExpr;
    }

    @Override
    public SymbolTypeEnum getSymbolType() {
        return SymbolTypeEnum.FUNCTION;
    }

    @Override
    public int getNewDefInsertionPoint() {
        return newDefInsertionPoint;
    }
}
