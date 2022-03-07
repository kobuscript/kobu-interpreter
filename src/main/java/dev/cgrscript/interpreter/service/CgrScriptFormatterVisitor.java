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

package dev.cgrscript.interpreter.service;

import dev.cgrscript.antlr.cgrscript.CgrScriptLexer;
import dev.cgrscript.antlr.cgrscript.CgrScriptParser;
import dev.cgrscript.antlr.cgrscript.CgrScriptParserBaseVisitor;
import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.List;
import java.util.Stack;

public class CgrScriptFormatterVisitor extends CgrScriptParserBaseVisitor<Void> {

    private final BufferedTokenStream tokens;

    private final int tabSize;

    private final StringBuilder out = new StringBuilder();

    private Stack<Integer> indentationStack = new Stack<>();

    public CgrScriptFormatterVisitor(BufferedTokenStream tokens, int tabSize) {
        this.tokens = tokens;
        this.tabSize = tabSize;

        indentationStack.push(0);
    }

    @Override
    public Void visitModule(CgrScriptParser.ModuleContext ctx) {
        printHiddenTextBefore(ctx);
        out.append("module ");
        if (ctx.moduleId() != null) {
            printCommentsBefore(ctx.moduleId());
            out.append(ctx.moduleId().getText());
        }
        if (ctx.MODULE_ID_END() != null) {
            printCommentsBefore(ctx.MODULE_ID_END());
            out.append(';');
        }
        printHiddenTextAfter(ctx);
        return null;
    }

    @Override
    public Void visitImportExpr(CgrScriptParser.ImportExprContext ctx) {
        out.append("import ");
        if (ctx.moduleId() != null) {
            printCommentsBefore(ctx.moduleId());
            out.append(ctx.moduleId().getText());
        }
        if (ctx.moduleScope() != null) {
            printCommentsBefore(ctx.moduleScope());
            out.append(" as ");
            if (ctx.moduleScope().MODULE_ID() != null) {
                printCommentsBefore(ctx.moduleScope().MODULE_ID());
                out.append(ctx.moduleScope().MODULE_ID().getText());
            }
        }
        if (ctx.MODULE_ID_END() != null) {
            printCommentsBefore(ctx.MODULE_ID_END());
            out.append(';');
        }
        printHiddenTextAfter(ctx);
        return null;
    }

    @Override
    public Void visitDeftype(CgrScriptParser.DeftypeContext ctx) {
        out.append("def ");
        if (ctx.DEFTYPE() != null) {
            printCommentsBefore(ctx.DEFTYPE());
            out.append("type ");
        }
        if (ctx.ID() != null) {
            printCommentsBefore(ctx.ID());
            out.append(ctx.ID().getText());
        }
        if (ctx.inheritance() != null) {
            printCommentsBefore(ctx.inheritance());
            out.append(" extends ");
            if (ctx.inheritance().typeName() != null) {
                printCommentsBefore(ctx.inheritance().typeName());
                out.append(ctx.inheritance().typeName().toString());
            }
        }
        if (ctx.LCB() != null) {
            printCommentsBefore(ctx.LCB());
            out.append(" {");
            printHiddenTextAfter(ctx.LCB());
            pushIndentation(tabSize);
            var attr = ctx.attributes();
            while (attr != null) {
                printCommentsBefore(attr);
                printIndentation();
                if (attr.ID() != null) {
                    out.append(attr.ID());
                } else if (attr.STAR() != null) {
                    out.append("*");
                }
                if (attr.COLON() != null) {
                    printCommentsBefore(attr.COLON());
                    out.append(": ");
                }
                if (attr.type() != null) {
                    printCommentsBefore(attr.type());
                    out.append(attr.type().getText());
                }
                attr = attr.attributes();
            }

            popIndentation();
        }
        return null;
    }

    @Override
    public Void visitFunctionDecl(CgrScriptParser.FunctionDeclContext ctx) {
        return super.visitFunctionDecl(ctx);
    }

    @Override
    public Void visitDefrule(CgrScriptParser.DefruleContext ctx) {
        return super.visitDefrule(ctx);
    }

    @Override
    public Void visitDeftemplate(CgrScriptParser.DeftemplateContext ctx) {
        return super.visitDeftemplate(ctx);
    }

    @Override
    public Void visitDeffile(CgrScriptParser.DeffileContext ctx) {
        return super.visitDeffile(ctx);
    }

    public String getFormattedCode() {
        return out.toString();
    }

    private void printCommentsBefore(ParserRuleContext ctx) {
        printCommentsBefore(ctx.start.getStartIndex());
    }

    private void printCommentsBefore(TerminalNode node) {
        printCommentsBefore(node.getSymbol().getStartIndex());
    }

    private void printHiddenTextBefore(ParserRuleContext ctx) {
        printHiddenTextBefore(ctx.start.getStartIndex());
    }

    private void printHiddenTextBefore(TerminalNode node) {
        printHiddenTextBefore(node.getSymbol().getStartIndex());
    }

    private void printHiddenTextAfter(ParserRuleContext ctx) {
        printHiddenTextAfter(ctx.stop.getStopIndex());
    }

    private void printHiddenTextAfter(TerminalNode node) {
        printHiddenTextAfter(node.getSymbol().getStopIndex());
    }

    private void printCommentsBefore(int offset) {
        var tokenList = tokens.getHiddenTokensToLeft(offset);
        if (tokenList != null) {
            int indentation = getIndentation();
            for (Token token : tokenList) {
                if (token.getChannel() == CgrScriptLexer.COMMENTCHANNEL || token.getChannel() == CgrScriptLexer.BLOCKCOMMENTCHANNEL) {
                    String text = token.getText();
                    text = text.replaceAll("\\n\\s*", "\n" + " ".repeat(indentation));
                    out.append(text);
                    out.append(' ');
                }
            }
        }
    }

    private void printHiddenTextBefore(int offset) {
        var tokenList = tokens.getHiddenTokensToLeft(offset);
        printTokens(tokenList);
    }

    private void printHiddenTextAfter(int offset) {
        var tokenList = tokens.getHiddenTokensToRight(offset);
        printTokens(tokenList);
    }

    private void printTokens(List<Token> tokenList) {
        if (tokenList != null && !tokenList.isEmpty()) {
            if (tokenList.size() > 1) {
                for (Token token : tokenList.subList(0, tokenList.size() - 1)) {
                    out.append(token.getText());
                }
            }
            var lastToken = tokenList.get(tokenList.size() - 1).getText();
            out.append(lastToken.replaceAll("\\n\\s+$", "\n"));
        }
    }

    private boolean hasNewLineBefore(int offset) {
        var tokenList = tokens.getHiddenTokensToLeft(offset);
        for (Token token : tokenList) {
            if (token.getText().contains("\n")) {
                return true;
            }
        }
        return false;
    }

    private int getIndentation() {
        return indentationStack.peek();
    }

    private void pushIndentation(int indentation) {
        indentationStack.push(indentation);
    }

    private void popIndentation() {
        indentationStack.pop();
    }

    private void printIndentation() {
        int indentation = getIndentation();
        out.append(" ".repeat(indentation));
    }

}
