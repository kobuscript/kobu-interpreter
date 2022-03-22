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

package dev.kobu.interpreter.service;

import dev.kobu.antlr.kobulang.KobuLexer;
import dev.kobu.antlr.kobulang.KobuParser;
import dev.kobu.antlr.kobulang.KobuParserBaseVisitor;
import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.List;
import java.util.Stack;

public class KobuFormatterVisitor extends KobuParserBaseVisitor<Void> {

    private final BufferedTokenStream tokens;

    private final int tabSize;

    private final StringBuilder out = new StringBuilder();

    private final Stack<Integer> indentationStack = new Stack<>();

    private int importsCount = 0;

    public KobuFormatterVisitor(BufferedTokenStream tokens, int tabSize) {
        this.tokens = tokens;
        this.tabSize = tabSize;

        indentationStack.push(0);
    }

    @Override
    public Void visitModule(KobuParser.ModuleContext ctx) {
        if (hasCommentsBefore(ctx) && !hasNewLineBefore(ctx, false)) {
            out.append("\n");
        }
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
    public Void visitImportExpr(KobuParser.ImportExprContext ctx) {
        if (!hasNewLineBefore(ctx, false)) {
            if (importsCount == 0) {
                out.append("\n\n");
            } else {
                out.append("\n");
            }
        }
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
        importsCount++;
        return null;
    }

    @Override
    public Void visitDeftype(KobuParser.DeftypeContext ctx) {
        if (!hasNewLineBefore(ctx, false)) {
            out.append("\n");
        }
        out.append("def ");
        if (ctx.DEFTYPE() != null) {
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

            boolean inline = isInline(ctx.attributes());

            var attr = ctx.attributes();
            if (!inline) {
                if (!hasNewLineBefore(attr)) {
                    out.append("\n");
                }
                pushIndentation(tabSize);
            }

            while (attr != null) {
                boolean hasComments = printCommentsBefore(attr);
                if (inline) {
                    printAttribute(attr);
                } else {
                    if (hasComments) {
                        out.append("\n");
                    }
                    printIndentation();
                    printAttribute(attr);
                    out.append("\n");
                }
                attr = attr.attributes();
            }

            if (!inline) {
                popIndentation();
            }
        }

        printHiddenTextAfter(ctx);
        return null;
    }

    private void printAttribute(KobuParser.AttributesContext attr) {
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
        if (attr.attributes() != null) {
            out.append(", ");
        }
    }

    private boolean isInline(KobuParser.AttributesContext attr) {
        boolean inline = true;
        while (attr != null) {
            if (hasNewLineBefore(attr)) {
                inline = false;
                break;
            }
            if (attr.COLON() != null && hasNewLineBefore(attr.COLON())) {
                inline = false;
                break;
            }
            if (attr.type() != null && hasNewLineBefore(attr.type())) {
                inline = false;
                break;
            }
            if (attr.COMMA() != null && hasNewLineBefore(attr.COMMA())) {
                inline = false;
                break;
            }
            attr = attr.attributes();
        }
        return inline;
    }

    @Override
    public Void visitFunctionDecl(KobuParser.FunctionDeclContext ctx) {
        if (!hasNewLineBefore(ctx, false)) {
            out.append("\n");
        }
        int paramPos = ctx.start.getStartIndex();
        boolean paramIndent = false;

        out.append("fun ");
        if (ctx.ID() != null) {
            printCommentsBefore(ctx.ID());
            out.append(ctx.ID().getText());
        }
        if (ctx.LP() != null) {
            paramPos = ctx.LP().getSymbol().getStartIndex() + 1;
            printCommentsBefore(ctx.LP());
            out.append("(");
        }
        var param = ctx.functionDeclParam();
        while (param != null) {
            if (hasNewLineBefore(param)) {
                if (!paramIndent) {
                    paramIndent = true;
                    pushIndentation(paramPos);
                }
                out.append("\n");
                printIndentation();
            }
            if (param.ID() != null) {
                printCommentsBefore(param.ID());
                out.append(" ");
                out.append(param.ID().getText());
            }
            if (param.COLON() != null) {
                printCommentsBefore(param.COLON());
                out.append(": ");
            }
            if (param.type() != null) {
                printCommentsBefore(param.type());
                out.append(param.type().getText());
            }
            if (param.COMMA() != null && param.functionDeclParam() != null) {
                printCommentsBefore(param.COMMA());
                out.append(",");
            }

            param = param.functionDeclParam();
        }

        if (ctx.RP() != null) {
            printCommentsBefore(ctx.RP());
            out.append(")");
        }
        if (ctx.LCB() != null) {
            printCommentsBefore(ctx.LCB());
            out.append(" {");
            if (hasNewLineAfter(ctx.LCB(), false)) {
                printHiddenTextAfter(ctx.LCB());
            } else {
                out.append("\n");
            }
        }

        if (paramIndent) {
            popIndentation();
        }

        boolean inline = isInline(ctx);
        if (inline) {
            out.append(" ");
        } else {
            pushIndentation(tabSize);
        }

        if (ctx.execStat() != null) {
            for (KobuParser.ExecStatContext execStatContext : ctx.execStat()) {
                if (!inline) {
                    printIndentation();
                }
                printStat(execStatContext, !inline);
            }
        }

        if (inline) {
            out.append(" ");
        } else {
            out.append("\n");
            popIndentation();
        }

        if (ctx.RCB() != null) {
            if (!hasNewLineBefore(ctx.RCB())) {
                out.append("\n");
            }
            out.append("}");
        }

        printHiddenTextAfter(ctx);

        return null;
    }

    private boolean isInline(KobuParser.FunctionDeclContext ctx) {
        boolean inline = true;
        if (ctx.execStat() != null && !ctx.execStat().isEmpty()) {
            for (KobuParser.ExecStatContext execStatContext : ctx.execStat()) {
                if (hasNewLineBefore(execStatContext)) {
                    inline = false;
                    break;
                }
            }
        }
        if (ctx.RCB() != null && hasNewLineBefore(ctx.RCB())) {
            inline = false;
        }
        return inline;
    }

    @Override
    public Void visitDefrule(KobuParser.DefruleContext ctx) {
        if (!hasNewLineBefore(ctx, false)) {
            out.append("\n");
        }

        out.append("def ");

        if (ctx.DEFRULE() != null) {
            printCommentsBefore(ctx.DEFRULE());
            out.append("rule ");
        }
        if (ctx.ID() != null) {
            printCommentsBefore(ctx.ID());
            out.append(ctx.ID().getText());
            out.append(" ");
        }
        if (ctx.ruleExtends() != null) {
            if (ctx.ruleExtends().EXTENDS() != null) {
                printCommentsBefore(ctx.ruleExtends().EXTENDS());
                out.append("extends ");
            }
            if (ctx.ruleExtends().typeName() != null) {
                printCommentsBefore(ctx.ruleExtends().typeName());
                out.append(ctx.ruleExtends().typeName().getText());
                out.append(" ");
            }
        }
        int queryPos = ctx.start.getStartIndex();
        if (ctx.FOR() != null) {
            queryPos = ctx.FOR().getSymbol().getStartIndex();
            printCommentsBefore(ctx.FOR());
            out.append("for ");
        }

        if (ctx.queryExpr() != null) {
            appendText(ctx.queryExpr());
        }
        if (ctx.joinExpr() != null && !ctx.joinExpr().isEmpty()) {
            pushIndentation(queryPos);
            for (KobuParser.JoinExprContext joinExprContext : ctx.joinExpr()) {
                out.append("\n");
                printIndentation();
                appendText(joinExprContext);
            }
            popIndentation();
        }
        if (ctx.WHEN() != null) {
            pushIndentation(queryPos);
            out.append("\n");
            printIndentation();
            out.append("when ");
            if (ctx.expr() != null) {
                appendText(ctx.expr());
            }
            popIndentation();
        }

        if (ctx.LCB() != null) {
            printCommentsBefore(ctx.LCB());
            out.append(" {");
            if (hasNewLineAfter(ctx.LCB(), false)) {
                printHiddenTextAfter(ctx.LCB());
            } else {
                out.append("\n");
            }
        }

        boolean inline = isInline(ctx);
        if (inline) {
            out.append(" ");
        } else {
            pushIndentation(tabSize);
        }

        if (ctx.block() != null && ctx.block().execStat() != null) {
            for (KobuParser.ExecStatContext execStatContext : ctx.block().execStat()) {
                if (!inline) {
                    printIndentation();
                }
                printStat(execStatContext, !inline);
            }
        }

        if (inline) {
            out.append(" ");
        } else {
            out.append("\n");
            popIndentation();
        }

        if (ctx.RCB() != null) {
            if (!hasNewLineBefore(ctx.RCB())) {
                out.append("\n");
            }
            out.append("}");
        }

        printHiddenTextAfter(ctx);

        return null;
    }

    private boolean isInline(KobuParser.DefruleContext ctx) {
        boolean inline = true;
        if (ctx.block() != null && ctx.block().execStat() != null) {
            for (KobuParser.ExecStatContext execStatContext : ctx.block().execStat()) {
                if (hasNewLineBefore(execStatContext)) {
                    inline = false;
                    break;
                }
            }
        }
        if (ctx.RCB() != null && hasNewLineBefore(ctx.RCB())) {
            inline = false;
        }
        return inline;
    }

    @Override
    public Void visitDeftemplate(KobuParser.DeftemplateContext ctx) {
        if (!hasNewLineBefore(ctx, false)) {
            out.append("\n");
        }

        out.append("def ");

        if (ctx.DEFTEMPLATE() != null) {
            printCommentsBefore(ctx.DEFTEMPLATE());
            out.append("template ");
        }
        if (ctx.ID() != null) {
            printCommentsBefore(ctx.ID());
            out.append(ctx.ID().getText());
            out.append(" ");
        }
        if (ctx.ruleExtends() != null) {
            if (ctx.ruleExtends().EXTENDS() != null) {
                printCommentsBefore(ctx.ruleExtends().EXTENDS());
                out.append("extends ");
            }
            if (ctx.ruleExtends().typeName() != null) {
                printCommentsBefore(ctx.ruleExtends().typeName());
                out.append(ctx.ruleExtends().typeName().getText());
                out.append(" ");
            }
        }
        int queryPos = ctx.start.getStartIndex();
        if (ctx.FOR() != null) {
            queryPos = ctx.FOR().getSymbol().getStartIndex();
            printCommentsBefore(ctx.FOR());
            out.append("for ");
        }

        if (ctx.queryExpr() != null) {
            appendText(ctx.queryExpr());
        }
        if (ctx.joinExpr() != null && !ctx.joinExpr().isEmpty()) {
            pushIndentation(queryPos);
            for (KobuParser.JoinExprContext joinExprContext : ctx.joinExpr()) {
                out.append("\n");
                printIndentation();
                appendText(joinExprContext);
            }
            popIndentation();
        }
        if (ctx.WHEN() != null) {
            pushIndentation(queryPos);
            out.append("\n");
            printIndentation();
            out.append("when ");
            if (ctx.expr() != null) {
                appendText(ctx.expr());
            }
            popIndentation();
        }

        if (ctx.TEMPLATE_BEGIN() != null) {
            out.append(" <|");
        }
        if (ctx.template() != null) {
            out.append(ctx.template().getText());
        }
        if (ctx.TEMPLATE_END() != null) {
            out.append("|>");
        }

        printHiddenTextAfter(ctx);

        return null;
    }

    @Override
    public Void visitDeffile(KobuParser.DeffileContext ctx) {
        if (!hasNewLineBefore(ctx, false)) {
            out.append("\n");
        }

        out.append("def ");

        if (ctx.DEFFILE() != null) {
            printCommentsBefore(ctx.DEFFILE());
            out.append("template ");
        }
        if (ctx.ID() != null) {
            printCommentsBefore(ctx.ID());
            out.append(ctx.ID().getText());
            out.append(" ");
        }
        if (ctx.ruleExtends() != null) {
            if (ctx.ruleExtends().EXTENDS() != null) {
                printCommentsBefore(ctx.ruleExtends().EXTENDS());
                out.append("extends ");
            }
            if (ctx.ruleExtends().typeName() != null) {
                printCommentsBefore(ctx.ruleExtends().typeName());
                out.append(ctx.ruleExtends().typeName().getText());
                out.append(" ");
            }
        }
        int queryPos = ctx.start.getStartIndex();
        if (ctx.FOR() != null) {
            queryPos = ctx.FOR().getSymbol().getStartIndex();
            printCommentsBefore(ctx.FOR());
            out.append("for ");
        }

        if (ctx.queryExpr() != null) {
            appendText(ctx.queryExpr());
        }
        if (ctx.joinExpr() != null && !ctx.joinExpr().isEmpty()) {
            pushIndentation(queryPos);
            for (KobuParser.JoinExprContext joinExprContext : ctx.joinExpr()) {
                out.append("\n");
                printIndentation();
                appendText(joinExprContext);
            }
            popIndentation();
        }
        if (ctx.WHEN() != null) {
            pushIndentation(queryPos);
            out.append("\n");
            printIndentation();
            out.append("when ");
            if (ctx.expr() != null) {
                appendText(ctx.expr());
            }
            popIndentation();
        }

        if (ctx.PATH_ARROW() != null) {
            out.append(" -> ");
        }
        if (ctx.pathExpr() != null) {
            out.append(ctx.pathExpr().getText());
        }
        if (ctx.PATH_END() != null) {
            out.append(";");
        }

        printHiddenTextAfter(ctx);

        return null;
    }

    @Override
    public Void visitIfStat(KobuParser.IfStatContext ctx) {
        out.append("if ");
        if (ctx.LP() != null) {
            printCommentsBefore(ctx.LP());
            out.append("(");
        }
        if (ctx.expr() != null) {
            printCommentsBefore(ctx.expr());
            out.append(ctx.expr().getText());
        }
        if (ctx.RP() != null) {
            printCommentsBefore(ctx.RP());
            out.append(") ");
        }
        if (ctx.LCB() != null) {
            printCommentsBefore(ctx.LCB());
            out.append("{");
            if (hasNewLineAfter(ctx.LCB(), false)) {
                printHiddenTextAfter(ctx.LCB());
            } else {
                out.append("\n");
            }
        }

        if (ctx.execStat() != null) {
            pushIndentation(getIndentation() + tabSize);

            for (KobuParser.ExecStatContext execStatContext : ctx.execStat()) {
                printIndentation();
                printStat(execStatContext, true);
            }

            popIndentation();
        }

        if (ctx.RCB() != null) {
            if (!hasNewLineBefore(ctx.RCB())) {
                out.append("\n");
                printIndentation();
            }
            out.append("}");
        }

        if (ctx.elseIfStat() != null || ctx.elseStat() != null) {
            out.append(" ");
            if (ctx.elseIfStat() != null) {
                visit(ctx.elseIfStat());
            }
            if (ctx.elseStat() != null) {
                visit(ctx.elseStat());
            }
        }

        printHiddenTextAfter(ctx);

        return null;
    }

    @Override
    public Void visitElseIfStat(KobuParser.ElseIfStatContext ctx) {
        if (ctx.ELSE() != null) {
            printCommentsBefore(ctx.ELSE());
            out.append("else ");
        }
        if (ctx.IF() != null) {
            printCommentsBefore(ctx.IF());
            out.append("if ");
        }

        if (ctx.LP() != null) {
            printCommentsBefore(ctx.LP());
            out.append("(");
        }
        if (ctx.expr() != null) {
            printCommentsBefore(ctx.expr());
            out.append(ctx.expr().getText());
        }
        if (ctx.RP() != null) {
            printCommentsBefore(ctx.RP());
            out.append(") ");
        }
        if (ctx.LCB() != null) {
            printCommentsBefore(ctx.LCB());
            out.append("{");
            if (hasNewLineAfter(ctx.LCB(), false)) {
                printHiddenTextAfter(ctx.LCB());
            } else {
                out.append("\n");
            }
        }

        if (ctx.execStat() != null) {
            pushIndentation(getIndentation() + tabSize);

            for (KobuParser.ExecStatContext execStatContext : ctx.execStat()) {
                printIndentation();
                printStat(execStatContext, true);
            }

            popIndentation();
        }

        if (ctx.RCB() != null) {
            if (!hasNewLineBefore(ctx.RCB())) {
                out.append("\n");
                printIndentation();
            }
            out.append("}");
        }

        if (ctx.elseIfStat() == null) {
            printHiddenTextAfter(ctx);
        } else {
            out.append(" ");
            if (ctx.elseIfStat() != null) {
                visit(ctx.elseIfStat());
            }
        }

        return null;
    }

    @Override
    public Void visitElseStat(KobuParser.ElseStatContext ctx) {
        if (ctx.ELSE() != null) {
            printCommentsBefore(ctx.ELSE());
            out.append(" else ");
        }
        if (ctx.LCB() != null) {
            printCommentsBefore(ctx.LCB());
            out.append("{");
            if (hasNewLineAfter(ctx.LCB(), false)) {
                printHiddenTextAfter(ctx.LCB());
            } else {
                out.append("\n");
            }
        }

        if (ctx.execStat() != null) {
            pushIndentation(getIndentation() + tabSize);

            for (KobuParser.ExecStatContext execStatContext : ctx.execStat()) {
                printIndentation();
                printStat(execStatContext, true);
            }

            popIndentation();
        }

        if (ctx.RCB() != null) {
            if (!hasNewLineBefore(ctx.RCB())) {
                out.append("\n");
                printIndentation();
            }
            out.append("}");
        }

        return null;
    }

    @Override
    public Void visitForStat(KobuParser.ForStatContext ctx) {
        out.append("for ");
        if (ctx.LP() != null) {
            printCommentsBefore(ctx.LP());
            out.append("(");
        }
        if (ctx.varDeclList() != null) {
            printCommentsBefore(ctx.varDeclList());
            out.append(ctx.varDeclList().getText());
        }
        if (ctx.expr() != null) {
            out.append("; ");
            printCommentsBefore(ctx.expr());
            out.append(ctx.expr().getText());
        }
        if (ctx.assignmentSequece() != null) {
            out.append("; ");
            printCommentsBefore(ctx.assignmentSequece());
            out.append(ctx.assignmentSequece().getText());
        }
        if (ctx.RP() != null) {
            printCommentsBefore(ctx.RP());
            out.append(") ");
        }
        if (ctx.LCB() != null) {
            printCommentsBefore(ctx.LCB());
            out.append("{");
            if (hasNewLineAfter(ctx.LCB(), false)) {
                printHiddenTextAfter(ctx.LCB());
            } else {
                out.append("\n");
            }
        }

        if (ctx.execStat() != null) {
            pushIndentation(getIndentation() + tabSize);

            for (KobuParser.ExecStatContext execStatContext : ctx.execStat()) {
                printIndentation();
                printStat(execStatContext, true);
            }

            popIndentation();
        }

        if (ctx.RCB() != null) {
            if (!hasNewLineBefore(ctx.RCB())) {
                out.append("\n");
                printIndentation();
            }
            out.append("}");
        }

        printHiddenTextAfter(ctx);

        return null;
    }

    @Override
    public Void visitWhileStat(KobuParser.WhileStatContext ctx) {
        out.append("while ");
        if (ctx.LP() != null) {
            printCommentsBefore(ctx.LP());
            out.append("(");
        }
        if (ctx.expr() != null) {
            printCommentsBefore(ctx.expr());
            out.append(ctx.expr().getText());
        }
        if (ctx.RP() != null) {
            printCommentsBefore(ctx.RP());
            out.append(")");
        }
        if (ctx.LCB() != null) {
            printCommentsBefore(ctx.LCB());
            out.append("{");
            if (hasNewLineAfter(ctx.LCB(), false)) {
                printHiddenTextAfter(ctx.LCB());
            } else {
                out.append("\n");
            }
        }

        if (ctx.execStat() != null) {
            pushIndentation(getIndentation() + tabSize);

            for (KobuParser.ExecStatContext execStatContext : ctx.execStat()) {
                printIndentation();
                printStat(execStatContext, true);
            }

            popIndentation();
        }

        if (ctx.RCB() != null) {
            if (!hasNewLineBefore(ctx.RCB())) {
                out.append("\n");
                printIndentation();
            }
            out.append("}");
        }

        printHiddenTextAfter(ctx);

        return null;
    }

    public String getFormattedCode() {
        return out.toString();
    }

    private boolean isBlockContext(ParserRuleContext ctx) {
        return ctx instanceof KobuParser.IfStatContext ||
                ctx instanceof KobuParser.ForStatContext ||
                ctx instanceof KobuParser.WhileStatContext;
    }

    private void printStat(ParserRuleContext ctx, boolean indent) {
        if (isBlockContext(ctx)) {
            visit(ctx);
        } else {
            var indentation = getIndentation();
            var text = ctx.getText();
            if (indent) {
                text = text.replaceAll("\\n\\s*", "\n" + " ".repeat(indentation));
            }
            out.append(text);
            printHiddenTextAfter(ctx);
        }
    }

    private boolean printCommentsBefore(ParserRuleContext ctx) {
        return printCommentsBefore(ctx.start.getStartIndex());
    }

    private boolean printCommentsBefore(TerminalNode node) {
        return printCommentsBefore(node.getSymbol().getStartIndex());
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

    private boolean hasNewLineBefore(ParserRuleContext ctx) {
        return hasNewLineBefore(ctx.start.getStartIndex());
    }

    private boolean hasNewLineBefore(TerminalNode node) {
        return hasNewLineBefore(node.getSymbol().getStartIndex());
    }

    private boolean hasNewLineBefore(ParserRuleContext ctx, boolean includeComments) {
        return hasNewLineBefore(ctx.start.getStartIndex(), includeComments);
    }

    private boolean hasNewLineBefore(TerminalNode node, boolean includeComments) {
        return hasNewLineBefore(node.getSymbol().getStartIndex(), includeComments);
    }

    private boolean hasNewLineAfter(ParserRuleContext ctx) {
        return hasNewLineBefore(ctx.stop.getStopIndex());
    }

    private boolean hasNewLineAfter(TerminalNode node) {
        return hasNewLineBefore(node.getSymbol().getStopIndex());
    }

    private boolean hasNewLineAfter(ParserRuleContext ctx, boolean includeComments) {
        return hasNewLineBefore(ctx.stop.getStopIndex(), includeComments);
    }

    private boolean hasNewLineAfter(TerminalNode node, boolean includeComments) {
        return hasNewLineBefore(node.getSymbol().getStopIndex(), includeComments);
    }

    private boolean hasCommentsBefore(ParserRuleContext ctx) {
        return hasCommentsBefore(ctx.start.getStartIndex());
    }

    private boolean hasCommentsBefore(TerminalNode node) {
        return hasCommentsBefore(node.getSymbol().getStartIndex());
    }

    private boolean printCommentsBefore(int offset) {
        var tokenList = tokens.getHiddenTokensToLeft(offset);
        boolean hasComments = false;
        if (tokenList != null) {
            int indentation = getIndentation();
            for (Token token : tokenList) {
                if (token.getChannel() == KobuLexer.COMMENTCHANNEL || token.getChannel() == KobuLexer.BLOCKCOMMENTCHANNEL) {
                    String text = token.getText();
                    text = text.replaceAll("\\n\\s*", "\n" + " ".repeat(indentation));
                    out.append(text);
                    out.append(' ');
                    hasComments = true;
                }
            }
        }
        return hasComments;
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
        int indentation = getIndentation();
        if (tokenList != null && !tokenList.isEmpty()) {
            if (tokenList.size() > 1) {
                for (Token token : tokenList.subList(0, tokenList.size() - 1)) {
                    out.append(token.getText().replaceAll("\\n\\s*", "\n" + " ".repeat(indentation)));
                }
            }
            var lastToken = tokenList.get(tokenList.size() - 1).getText();
            out.append(lastToken
                    .replaceAll("\\n\\s*", "\n" + " ".repeat(indentation)
                    .replaceAll("\\n[\\s]+$", "\n")));
        }
    }

    private boolean hasNewLineBefore(int offset) {
        return hasNewLineBefore(offset, true);
    }

    private boolean hasNewLineBefore(int offset, boolean includeComments) {
        var tokenList = tokens.getHiddenTokensToLeft(offset);
        for (Token token : tokenList) {
            if (includeComments || token.getChannel() == KobuLexer.WSCHANNEL) {
                if (token.getText().contains("\n")) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasNewLineAfter(int offset) {
        return hasNewLineAfter(offset, true);
    }

    private boolean hasNewLineAfter(int offset, boolean includeComments) {
        var tokenList = tokens.getHiddenTokensToRight(offset);
        for (Token token : tokenList) {
            if (includeComments || token.getChannel() == KobuLexer.WSCHANNEL) {
                if (token.getText().contains("\n")) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasCommentsBefore(int offset) {
        var tokenList = tokens.getHiddenTokensToLeft(offset, KobuLexer.COMMENTCHANNEL);
        if (tokenList != null && !tokenList.isEmpty()) {
            return true;
        }
        tokenList = tokens.getHiddenTokensToLeft(offset, KobuLexer.BLOCKCOMMENTCHANNEL);
        return tokenList != null && !tokenList.isEmpty();
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

    private void appendText(ParserRuleContext ctx) {
        var text = ctx.getText();
        text = text.replaceAll("\n", " ").replaceAll("\\s[\\s]+", " ");
        out.append(text);
    }

}
