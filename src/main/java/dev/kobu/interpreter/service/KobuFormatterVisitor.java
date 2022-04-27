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
        if (ctx.MODULE_ID_BREAK() != null) {
            out.append("\n");
        }
        printHiddenTextAfter(ctx);
        return null;
    }

    @Override
    public Void visitImportExpr(KobuParser.ImportExprContext ctx) {
        if (!hasNewLineBefore(ctx, false)) {
            if (importsCount == 0) {
                out.append("\n\n");
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
        if (ctx.MODULE_ID_BREAK() != null) {
            out.append("\n");
        }
        printHiddenTextAfter(ctx);
        importsCount++;
        return null;
    }

    @Override
    public Void visitGlobalConstDecl(KobuParser.GlobalConstDeclContext ctx) {
        if (!hasNewLineBefore(ctx, false)) {
            out.append("\n");
        }
        out.append(tokens.getText(ctx.getSourceInterval()));
        return null;
    }

    @Override
    public Void visitTypetemplate(KobuParser.TypetemplateContext ctx) {
        if (!hasNewLineBefore(ctx, false)) {
            out.append("\n");
        }
        out.append("type ");
        if (ctx.TYPE_TEMPLATE() != null) {
            out.append("template ");
        }
        if (ctx.ID() != null) {
            printCommentsBefore(ctx.ID());
            out.append(ctx.ID().getText());
        }
        if (ctx.templateInheritance() != null) {
            printCommentsBefore(ctx.templateInheritance());
            out.append(" extends ");
            if (ctx.templateInheritance().typeName() != null) {
                printCommentsBefore(ctx.templateInheritance().typeName());
                printTypeName(ctx.templateInheritance().typeName());
            }
        }

        printHiddenTextAfter(ctx);
        return null;
    }

    @Override
    public Void visitTyperecord(KobuParser.TyperecordContext ctx) {
        if (!hasNewLineBefore(ctx, false)) {
            out.append("\n");
        }
        out.append("type ");
        if (ctx.TYPE_RECORD() != null) {
            out.append("record ");
        }
        if (ctx.ID() != null) {
            printCommentsBefore(ctx.ID());
            out.append(ctx.ID().getText());
        }
        if (ctx.typeParameters() != null) {
            out.append(ctx.typeParameters().getText().replace(",", ", "));
        }
        if (ctx.inheritance() != null) {
            printCommentsBefore(ctx.inheritance());
            out.append(" extends ");
            if (ctx.inheritance().typeName() != null) {
                printCommentsBefore(ctx.inheritance().typeName());
                printTypeName(ctx.inheritance().typeName());
            }
        }
        if (ctx.LCB() != null) {
            printCommentsBefore(ctx.LCB());
            out.append(" {");

            boolean inline = isInline(ctx.attributes());
            if (!inline) {
                out.append("\n");
            }

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

            if (ctx.RCB() != null) {
                out.append("}");
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
            out.append(",");
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

        if (ctx.PRIVATE() != null) {
            out.append("private ");
        }
        out.append("fun ");
        if (ctx.ID() != null) {
            printCommentsBefore(ctx.ID());
            out.append(ctx.ID().getText());
        }
        if (ctx.typeParameters() != null) {
            out.append(ctx.typeParameters().getText().replace(",", ", "));
        }
        if (ctx.LP() != null) {
            paramPos = ctx.LP().getSymbol().getCharPositionInLine() + 1;
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
                out.append(param.ID().getText());
            }
            if (param.QM() != null) {
                out.append("?");
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
                if (hasNewLineAfter(param.COMMA())) {
                    out.append(",");
                } else {
                    out.append(", ");
                }
            }

            param = param.functionDeclParam();
        }

        if (ctx.RP() != null) {
            printCommentsBefore(ctx.RP());
            out.append(")");
        }

        if (ctx.COLON() != null) {
            out.append(": ");
        }
        if (ctx.functionDeclRet() != null) {
            out.append(ctx.functionDeclRet().getText());
        }

        boolean inline = isInline(ctx);
        if (ctx.LCB() != null) {
            printCommentsBefore(ctx.LCB());
            out.append(" {");
            if (!inline) {
                out.append("\n");
            }
        }

        if (paramIndent) {
            popIndentation();
        }

        if (inline) {
            out.append(" ");
        } else {
            pushIndentation(tabSize);
        }

        if (ctx.execStat() != null && !ctx.execStat().isEmpty()) {
            if (!inline) {
                printIndentation();
            }
            for (KobuParser.ExecStatContext execStatContext : ctx.execStat()) {
                printStat(execStatContext, !inline);
            }
        }

        if (inline) {
            out.append(" ");
        } else {
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

    @Override
    public Void visitNativeDecl(KobuParser.NativeDeclContext ctx) {
        if (!hasNewLineBefore(ctx, false)) {
            out.append("\n");
        }
        int paramPos = ctx.start.getStartIndex();
        boolean paramIndent = false;

        out.append("def native ");
        if (ctx.ID() != null) {
            printCommentsBefore(ctx.ID());
            out.append(ctx.ID().getText());
        }
        if (ctx.typeParameters() != null) {
            out.append(ctx.typeParameters().getText().replace(",", ", "));
        }
        if (ctx.LP() != null) {
            paramPos = ctx.LP().getSymbol().getCharPositionInLine() + 1;
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
                out.append(param.ID().getText());
            }
            if (param.QM() != null) {
                out.append("?");
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
                if (hasNewLineAfter(param.COMMA())) {
                    out.append(",");
                } else {
                    out.append(", ");
                }
            }

            param = param.functionDeclParam();
        }

        if (ctx.RP() != null) {
            printCommentsBefore(ctx.RP());
            out.append(")");
        }

        if (ctx.COLON() != null) {
            out.append(": ");
        }
        if (ctx.functionDeclRet() != null) {
            out.append(ctx.functionDeclRet().getText());
        }

        if (paramIndent) {
            popIndentation();
        }

        out.append(";");

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

        int queryPos = ctx.start.getCharPositionInLine();;

        if (ctx.DEFRULE() != null) {
            printCommentsBefore(ctx.DEFRULE());
            out.append("rule ");
        }
        if (ctx.ID() != null) {
            queryPos = ctx.ID().getSymbol().getCharPositionInLine();;
            printCommentsBefore(ctx.ID());
            out.append(ctx.ID().getText());
        }
        if (ctx.ruleExtends() != null) {
            if (ctx.ruleExtends().EXTENDS() != null) {
                printCommentsBefore(ctx.ruleExtends().EXTENDS());
                out.append(" extends ");
            }
            if (ctx.ruleExtends().typeName() != null) {
                printCommentsBefore(ctx.ruleExtends().typeName());
                printTypeName(ctx.ruleExtends().typeName());
                out.append(" ");
            }
        }

        if (ctx.FOR() != null) {
            out.append("\n");
            pushIndentation(queryPos);
            printIndentation();
            if (printCommentsBefore(ctx.FOR())) {
                out.append("\n");
                printIndentation();
            }
            out.append("for ");
        }

        if (ctx.queryExpr() != null) {
            appendText(ctx.queryExpr());
        }
        if (ctx.extractExpr() != null && !ctx.extractExpr().isEmpty()) {
            for (KobuParser.ExtractExprContext extractExprCtx : ctx.extractExpr()) {
                out.append("\n");
                printIndentation();
                appendText(extractExprCtx);
            }
        }
        if (ctx.joinExpr() != null && !ctx.joinExpr().isEmpty()) {
            for (KobuParser.JoinExprContext joinExprContext : ctx.joinExpr()) {
                out.append("\n");
                printIndentation();
                appendText(joinExprContext);
            }
        }
        if (ctx.WHEN() != null) {
            out.append("\n");
            printIndentation();
            out.append("when ");
            if (ctx.expr() != null) {
                pushIndentation(ctx.WHEN().getSymbol().getCharPositionInLine() + 4);
                printStat(ctx.expr(), true, false);
                popIndentation();
            }
        }

        popIndentation();

        boolean inline = isInline(ctx);
        if (ctx.LCB() != null) {
            out.append(" {");
            if (!inline) {
                out.append("\n");
            }
        }

        if (inline) {
            out.append(" ");
        } else {
            pushIndentation(tabSize);
        }

        if (ctx.block() != null && ctx.block().execStat() != null && !ctx.block().execStat().isEmpty()) {
            if (!inline) {
                printIndentation();
            }
            for (KobuParser.ExecStatContext execStatContext : ctx.block().execStat()) {
                printStat(execStatContext, !inline);
            }
        }

        if (inline) {
            out.append(" ");
        } else {
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

    private boolean isInline(KobuParser.DefactionContext ctx) {
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

        int queryPos = ctx.start.getCharPositionInLine();

        if (ctx.DEFTEMPLATE() != null) {
            printCommentsBefore(ctx.DEFTEMPLATE());
            out.append("template ");
        }
        if (ctx.ID() != null) {
            queryPos = ctx.ID().getSymbol().getCharPositionInLine();
            printCommentsBefore(ctx.ID());
            out.append(ctx.ID().getText());
        }
        if (ctx.ruleExtends() != null) {
            if (ctx.ruleExtends().EXTENDS() != null) {
                printCommentsBefore(ctx.ruleExtends().EXTENDS());
                out.append(" extends ");
            }
            if (ctx.ruleExtends().typeName() != null) {
                printCommentsBefore(ctx.ruleExtends().typeName());
                printTypeName(ctx.ruleExtends().typeName());
            }
        }

        if (ctx.FOR() != null) {
            out.append("\n");
            pushIndentation(queryPos);
            printIndentation();
            if (printCommentsBefore(ctx.FOR())) {
                out.append("\n");
                printIndentation();
            }
            out.append("for ");
        }

        if (ctx.queryExpr() != null) {
            appendText(ctx.queryExpr());
        }
        if (ctx.extractExpr() != null && !ctx.extractExpr().isEmpty()) {
            for (KobuParser.ExtractExprContext extractExprCtx : ctx.extractExpr()) {
                out.append("\n");
                printIndentation();
                appendText(extractExprCtx);
            }
        }
        if (ctx.joinExpr() != null && !ctx.joinExpr().isEmpty()) {
            for (KobuParser.JoinExprContext joinExprContext : ctx.joinExpr()) {
                out.append("\n");
                printIndentation();
                appendText(joinExprContext);
            }
        }
        if (ctx.WHEN() != null) {
            out.append("\n");
            printIndentation();
            out.append("when ");
            if (ctx.expr() != null) {
                pushIndentation(ctx.WHEN().getSymbol().getCharPositionInLine() + 4);
                printStat(ctx.expr(), true, false);
                popIndentation();
            }
        }

        popIndentation();

        if (ctx.TEMPLATE_BEGIN() != null) {
            out.append(" <|");
        }
        if (ctx.template() != null) {
            out.append(tokens.getText(ctx.template().getSourceInterval()));
        }
        if (ctx.TEMPLATE_END() != null) {
            out.append("|>");
        }

        if (ctx.templateTargetType() != null) {
            if (ctx.templateTargetType().AS() != null) {
                out.append(" as ");
            }
            if (ctx.templateTargetType().typeName() != null) {
                printTypeName(ctx.templateTargetType().typeName());
            }
        }

        printHiddenTextAfter(ctx);

        return null;
    }

    @Override
    public Void visitDefaction(KobuParser.DefactionContext ctx) {
        if (!hasNewLineBefore(ctx, false)) {
            out.append("\n");
        }

        out.append("def ");

        int queryPos = ctx.start.getCharPositionInLine();;

        if (ctx.DEFACTION() != null) {
            printCommentsBefore(ctx.DEFACTION());
            out.append("action ");
        }
        if (ctx.ID() != null) {
            queryPos = ctx.ID().getSymbol().getCharPositionInLine();;
            printCommentsBefore(ctx.ID());
            out.append(ctx.ID().getText());
        }
        if (ctx.ruleExtends() != null) {
            if (ctx.ruleExtends().EXTENDS() != null) {
                printCommentsBefore(ctx.ruleExtends().EXTENDS());
                out.append(" extends ");
            }
            if (ctx.ruleExtends().typeName() != null) {
                printCommentsBefore(ctx.ruleExtends().typeName());
                printTypeName(ctx.ruleExtends().typeName());
                out.append(" ");
            }
        }

        if (ctx.FOR() != null) {
            out.append("\n");
            pushIndentation(queryPos);
            printIndentation();
            if (printCommentsBefore(ctx.FOR())) {
                out.append("\n");
                printIndentation();
            }
            out.append("for ");
        }

        if (ctx.queryExpr() != null) {
            appendText(ctx.queryExpr());
        }
        if (ctx.extractExpr() != null && !ctx.extractExpr().isEmpty()) {
            for (KobuParser.ExtractExprContext extractExprCtx : ctx.extractExpr()) {
                out.append("\n");
                printIndentation();
                appendText(extractExprCtx);
            }
        }
        if (ctx.joinExpr() != null && !ctx.joinExpr().isEmpty()) {
            for (KobuParser.JoinExprContext joinExprContext : ctx.joinExpr()) {
                out.append("\n");
                printIndentation();
                appendText(joinExprContext);
            }
        }
        if (ctx.WHEN() != null) {
            out.append("\n");
            printIndentation();
            out.append("when ");
            if (ctx.expr() != null) {
                pushIndentation(ctx.WHEN().getSymbol().getCharPositionInLine() + 4);
                printStat(ctx.expr(), true, false);
                popIndentation();
            }
        }

        popIndentation();

        boolean inline = isInline(ctx);
        if (ctx.LCB() != null) {
            printCommentsBefore(ctx.LCB());
            out.append(" {");
            if (!inline) {
                out.append("\n");
            }
        }

        if (inline) {
            out.append(" ");
        } else {
            pushIndentation(tabSize);
        }

        if (ctx.block() != null && ctx.block().execStat() != null && !ctx.block().execStat().isEmpty()) {
            if (!inline) {
                printIndentation();
            }
            for (KobuParser.ExecStatContext execStatContext : ctx.block().execStat()) {
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

    @Override
    public Void visitIfStat(KobuParser.IfStatContext ctx) {
        out.append("if ");
        if (ctx.LP() != null) {
            printCommentsBefore(ctx.LP());
            out.append("(");
        }
        if (ctx.expr() != null) {
            printCommentsBefore(ctx.expr());
            out.append(tokens.getText(ctx.expr().getSourceInterval()));
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
            out.append(tokens.getText(ctx.expr().getSourceInterval()));
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
    public Void visitEnhancedForStat(KobuParser.EnhancedForStatContext ctx) {
        out.append("for ");
        if (ctx.LP() != null) {
            printCommentsBefore(ctx.LP());
            out.append("(");
        }
        if (ctx.VAR() != null) {
            printCommentsBefore(ctx.VAR());
            out.append("var");
        }
        if (ctx.ID() != null) {
            printCommentsBefore(ctx.ID());
            out.append(" ");
            out.append(ctx.ID().getText());
        }
        if (ctx.COLON() != null) {
            out.append(": ");
        }
        if (ctx.type() != null) {
            out.append(ctx.type().getText().replace(",", ", "));
        }
        if (ctx.OF() != null) {
            out.append(" of ");
        }
        if (ctx.expr() != null) {
            pushIndentation(ctx.LP().getSymbol().getCharPositionInLine() + tabSize);
            printStat(ctx.expr(), true, false);
            popIndentation();
        }
        if (ctx.RP() != null) {
            printCommentsBefore(ctx.ID());
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
    public Void visitForStat(KobuParser.ForStatContext ctx) {
        out.append("for ");
        if (ctx.LP() != null) {
            printCommentsBefore(ctx.LP());
            out.append("(");
        }
        if (ctx.varDeclList() != null) {
            printCommentsBefore(ctx.varDeclList());
            out.append(tokens.getText(ctx.varDeclList().getSourceInterval()));
        }
        if (ctx.expr() != null) {
            out.append("; ");
            printCommentsBefore(ctx.expr());
            out.append(tokens.getText(ctx.expr().getSourceInterval()));
        }
        if (ctx.assignmentSequece() != null) {
            out.append("; ");
            printCommentsBefore(ctx.assignmentSequece());
            out.append(tokens.getText(ctx.assignmentSequece().getSourceInterval()));
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
            out.append(tokens.getText(ctx.expr().getSourceInterval()));
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
        printStat(ctx, indent, true);
    }

    private void printStat(ParserRuleContext ctx, boolean indent, boolean printTextAfter) {
        if (isBlockContext(ctx)) {
            visit(ctx);
        } else {
            var indentation = getIndentation();
            var text = tokens.getText(ctx.getSourceInterval());
            if (indent) {
                String margin = " ".repeat(indentation);
                text = text
                        .replaceAll("\\n[ \\t]{0," + indentation + "}", "\n" + margin)
                        .replace("\n" + margin + "\n", "\n\n")
                        .replaceAll("[ \\t]+$", "");
            }
            out.append(text);
            if (printTextAfter) {
                printHiddenTextAfter(ctx);
            }
        }
    }

    private void printTypeName(KobuParser.TypeNameContext typeNameCtx) {
        out.append(typeNameCtx.getText().replace(",", ", "));
    }

    private boolean printCommentsBefore(ParserRuleContext ctx) {
        return printCommentsBefore(ctx.start.getTokenIndex());
    }

    private boolean printCommentsBefore(TerminalNode node) {
        return printCommentsBefore(node.getSymbol().getTokenIndex());
    }

    private void printHiddenTextBefore(ParserRuleContext ctx) {
        printHiddenTextBefore(ctx.start.getTokenIndex());
    }

    private void printHiddenTextBefore(TerminalNode node) {
        printHiddenTextBefore(node.getSymbol().getTokenIndex());
    }

    private void printHiddenTextAfter(ParserRuleContext ctx) {
        printHiddenTextAfter(ctx.stop.getTokenIndex());
    }

    private void printHiddenTextAfter(TerminalNode node) {
        printHiddenTextAfter(node.getSymbol().getTokenIndex());
    }

    private boolean hasNewLineBefore(ParserRuleContext ctx) {
        return hasNewLineBefore(ctx.start.getTokenIndex());
    }

    private boolean hasNewLineBefore(TerminalNode node) {
        return hasNewLineBefore(node.getSymbol().getTokenIndex());
    }

    private boolean hasNewLineBefore(ParserRuleContext ctx, boolean includeComments) {
        return hasNewLineBefore(ctx.start.getTokenIndex(), includeComments);
    }

    private boolean hasNewLineBefore(TerminalNode node, boolean includeComments) {
        return hasNewLineBefore(node.getSymbol().getTokenIndex(), includeComments);
    }

    private boolean hasNewLineAfter(ParserRuleContext ctx) {
        return hasNewLineAfter(ctx.stop.getTokenIndex());
    }

    private boolean hasNewLineAfter(TerminalNode node) {
        return hasNewLineAfter(node.getSymbol().getTokenIndex());
    }

    private boolean hasNewLineAfter(ParserRuleContext ctx, boolean includeComments) {
        return hasNewLineBefore(ctx.stop.getTokenIndex(), includeComments);
    }

    private boolean hasNewLineAfter(TerminalNode node, boolean includeComments) {
        return hasNewLineAfter(node.getSymbol().getTokenIndex(), includeComments);
    }

    private boolean hasCommentsBefore(ParserRuleContext ctx) {
        return hasCommentsBefore(ctx.start.getTokenIndex());
    }

    private boolean hasCommentsBefore(TerminalNode node) {
        return hasCommentsBefore(node.getSymbol().getTokenIndex());
    }

    private boolean printCommentsBefore(int tokenIndex) {
        var tokenList = tokens.getHiddenTokensToLeft(tokenIndex);
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

    private void printHiddenTextBefore(int tokenIndex) {
        var tokenList = tokens.getHiddenTokensToLeft(tokenIndex);
        printTokens(tokenList);
    }

    private void printHiddenTextAfter(int tokenIndex) {
        var tokenList = tokens.getHiddenTokensToRight(tokenIndex);
        printTokens(tokenList);
    }

    private void printTokens(List<Token> tokenList) {
        int indentation = getIndentation();
        if (tokenList != null && !tokenList.isEmpty()) {
            String margin = " ".repeat(indentation);
            if (tokenList.size() > 1) {
                for (Token token : tokenList.subList(0, tokenList.size() - 1)) {
                    if (indentation > 0) {
                        out.append(token.getText()
                                .replaceAll("\\n[ \\t]*", "\n" + margin)
                                .replace("\n" + margin + "\n", "\n\n"));
                    } else {
                        out.append(token.getText());
                    }
                }
            }
            var lastToken = tokenList.get(tokenList.size() - 1).getText();
            if (indentation > 0) {
                String text = lastToken
                        .replaceAll("\\n[ \\t]*", "\n" + margin)
                        .replace("\n" + margin + "\n", "\n\n");
                if (lastToken.endsWith("\n")) {
                    out.append(text.replaceAll("\\n[ \\t]+$", "\n"));
                } else {
                    out.append(text);
                }
            } else {
                out.append(lastToken);
            }
        }
    }

    private boolean hasNewLineBefore(int tokenIndex) {
        return hasNewLineBefore(tokenIndex, true);
    }

    private boolean hasNewLineBefore(int tokenIndex, boolean includeComments) {
        var tokenList = tokens.getHiddenTokensToLeft(tokenIndex);
        if (tokenList != null) {
            for (Token token : tokenList) {
                if (includeComments || token.getChannel() == KobuLexer.HIDDEN) {
                    if (token.getText().contains("\n")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean hasNewLineAfter(int tokenIndex) {
        return hasNewLineAfter(tokenIndex, true);
    }

    private boolean hasNewLineAfter(int tokenIndex, boolean includeComments) {
        var tokenList = tokens.getHiddenTokensToRight(tokenIndex);
        for (Token token : tokenList) {
            if (includeComments || token.getChannel() == KobuLexer.HIDDEN) {
                if (token.getText().contains("\n")) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasCommentsBefore(int tokenIndex) {
        var tokenList = tokens.getHiddenTokensToLeft(tokenIndex, KobuLexer.COMMENTCHANNEL);
        if (tokenList != null && !tokenList.isEmpty()) {
            return true;
        }
        tokenList = tokens.getHiddenTokensToLeft(tokenIndex, KobuLexer.BLOCKCOMMENTCHANNEL);
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
        var text = tokens.getText(ctx.getSourceInterval());
        text = text.replaceAll("\n", " ")
                .replaceAll("\\s[\\s]+", " ");
        out.append(text);
    }

}
