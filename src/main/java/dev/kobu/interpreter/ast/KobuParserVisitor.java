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

package dev.kobu.interpreter.ast;

import dev.kobu.antlr.kobulang.KobuParserBaseVisitor;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.module.ModuleLoader;
import dev.kobu.interpreter.ast.symbol.ModuleScope;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;

public abstract class KobuParserVisitor<T> extends KobuParserBaseVisitor<T> {

    protected final ModuleLoader moduleLoader;

    protected ModuleScope moduleScope;

    protected KobuParserVisitor(ModuleLoader moduleLoader) {
        this.moduleLoader = moduleLoader;
    }

    protected SourceCodeRef getSourceCodeRef(ParserRuleContext ctx) {
        if (ctx == null) {
            return null;
        }
        int startIdx, stopIdx, lineStart, lineStop, charStart, charStop;
        if (ctx.getStart().getStartIndex() <= ctx.getStop().getStopIndex()) {
            startIdx = ctx.getStart().getStartIndex();
            stopIdx = ctx.getStop().getStopIndex();
            lineStart = ctx.getStart().getLine();
            charStart = ctx.getStart().getCharPositionInLine();
            lineStop = ctx.getStop().getLine();
            charStop = ctx.getStop().getCharPositionInLine() + ctx.getStop().getText().length();
        } else {
            startIdx = ctx.getStart().getStartIndex();
            stopIdx = ctx.getStart().getStopIndex();
            lineStart = ctx.getStart().getLine();
            charStart = ctx.getStart().getCharPositionInLine();
            lineStop = ctx.getStart().getLine();
            charStop = ctx.getStart().getCharPositionInLine() + ctx.getStart().getText().length();
        }
        return new SourceCodeRef(
                moduleScope.getScript(),
                moduleScope.getModuleId(),
                lineStart,
                charStart,
                lineStop,
                charStop,
                startIdx,
                stopIdx);
    }

    protected SourceCodeRef getSourceCodeRef(TerminalNode node) {
        if (node == null) {
            return null;
        }
        return new SourceCodeRef(
                moduleScope.getScript(),
                moduleScope.getModuleId(),
                node.getSymbol().getLine(),
                node.getSymbol().getCharPositionInLine(),
                node.getSymbol().getLine(),
                node.getSymbol().getCharPositionInLine() + node.getSymbol().getText().length(),
                node.getSymbol().getStartIndex(),
                node.getSymbol().getStopIndex());
    }
}
