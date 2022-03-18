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

package dev.kobu.interpreter.error.analyzer;

import dev.kobu.interpreter.ast.eval.SymbolTypeEnum;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.error.AnalyzerError;
import dev.kobu.interpreter.error.KobuActionTypeEnum;

import java.util.Objects;

public class UndefinedVariableError extends AnalyzerError {

    private static final KobuActionTypeEnum[] actions = new KobuActionTypeEnum[]{
            KobuActionTypeEnum.AUTO_IMPORT
    };

    private final String varName;

    public UndefinedVariableError(SourceCodeRef sourceCodeRef, String varName) {
        super(sourceCodeRef);
        this.varName = varName;
    }

    public String getVarName() {
        return varName;
    }

    @Override
    public String getDescription() {
        return "Undefined variable: " + varName;
    }

    @Override
    public KobuActionTypeEnum[] actions() {
        if (isRuleRef()) {
            return actions;
        }
        return null;
    }

    private boolean isRuleRef() {
        return varName.length() > 1 && Character.isUpperCase(varName.charAt(0));
    }

    @Override
    public String getTokenText() {
        return varName;
    }

    @Override
    public SymbolTypeEnum getSymbolType() {
        return SymbolTypeEnum.VARIABLE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        UndefinedVariableError that = (UndefinedVariableError) o;
        return Objects.equals(varName, that.varName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), varName);
    }
}
