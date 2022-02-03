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

package dev.cgrscript.interpreter.ast;

import dev.cgrscript.interpreter.ast.eval.*;
import dev.cgrscript.interpreter.ast.eval.expr.*;
import dev.cgrscript.interpreter.ast.eval.expr.value.*;
import dev.cgrscript.interpreter.ast.eval.statement.*;
import dev.cgrscript.interpreter.ast.file.PathSegmentStatement;
import dev.cgrscript.interpreter.ast.file.PathStatement;
import dev.cgrscript.interpreter.ast.file.PathStaticSegmentStatement;
import dev.cgrscript.interpreter.ast.query.*;
import dev.cgrscript.interpreter.ast.symbol.*;
import dev.cgrscript.interpreter.ast.template.TemplateContentStatement;
import dev.cgrscript.interpreter.ast.template.TemplateStatement;
import dev.cgrscript.interpreter.ast.template.TemplateStaticContentStatement;
import dev.cgrscript.interpreter.ast.utils.NumberParser;
import dev.cgrscript.interpreter.error.AnalyzerError;
import dev.cgrscript.interpreter.error.analyzer.DuplicatedFunctionParamError;
import dev.cgrscript.interpreter.error.analyzer.FunctionMissingReturnStatError;
import dev.cgrscript.interpreter.error.analyzer.InvalidRequiredFunctionParamError;
import dev.cgrscript.interpreter.error.analyzer.UndefinedTypeError;
import dev.cgrscript.interpreter.module.AnalyzerStepEnum;
import dev.cgrscript.interpreter.module.ModuleLoader;
import dev.cgrscript.parser.CgrScriptParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EvalTreeParserVisitor extends CgrScriptParserVisitor<AstNode> {

    public EvalTreeParserVisitor(ModuleLoader moduleLoader, ModuleScope moduleScope) {
        super(moduleLoader);
        this.moduleScope = moduleScope;
    }

    @Override
    public AstNode visitImportExpr(CgrScriptParser.ImportExprContext ctx) {
        var moduleId = ctx.moduleId().getText();
        try {
            var module = moduleLoader.getScope(moduleId, getSourceCodeRef(ctx));
            moduleLoader.visit(moduleId, new EvalTreeParserVisitor(moduleLoader, module), AnalyzerStepEnum.EVAL_TREE);
        } catch (AnalyzerError e) {
            moduleScope.addError(e);
        }
        return null;
    }

    @Override
    public AstNode visitDeftype(CgrScriptParser.DeftypeContext ctx) {
        RecordTypeSymbol recordType = (RecordTypeSymbol) moduleScope.resolve(ctx.ID().getText());
        if (ctx.attributes() != null) {

            CgrScriptParser.AttributesContext attrCtx = ctx.attributes();
            while (attrCtx != null) {
                var type = (Type) visit(attrCtx.type());
                if (attrCtx.ID() != null) {
                    var attr = new RecordTypeAttribute(getSourceCodeRef(attrCtx.ID()), attrCtx.ID().getText(), type);
                    recordType.addAttribute(attr);
                } else {
                    recordType.setUnknownAttributes(new RecordTypeUnknownAttributes(getSourceCodeRef(attrCtx), type));
                }
                attrCtx = attrCtx.attributes();
            }

        }
        recordType.buildMethods();

        return null;
    }

    @Override
    public AstNode visitFunctionDecl(CgrScriptParser.FunctionDeclContext ctx) {
        var function = (FunctionSymbol) moduleScope.resolve(ctx.ID().getText());
        List<FunctionParameter> parameters = new ArrayList<>();
        if (ctx.functionDeclParam() != null) {
            CgrScriptParser.FunctionDeclParamContext paramCtx = ctx.functionDeclParam();
            parameters = getFunctionParameters(paramCtx);
        }

        List<Evaluable> exprList = new ArrayList<>();
        if (ctx.execStat() != null) {
            for (CgrScriptParser.ExecStatContext execStatContext : ctx.execStat()) {
                Evaluable evaluable = (Evaluable) visit(execStatContext);
                if (evaluable != null) {
                    exprList.add(evaluable);
                }
            }
        }
        if (ctx.functionDeclRet().VOID() == null && exprList.isEmpty()) {
            moduleScope.addError(new FunctionMissingReturnStatError(getSourceCodeRef(ctx.RCB())));
        }

        function.setParameters(parameters);
        if (ctx.functionDeclRet().type() != null) {
            function.setReturnType((Type) visit(ctx.functionDeclRet().type()));
        }
        function.setExprList(exprList);

        return null;
    }

    @Override
    public AstNode visitExprStat(CgrScriptParser.ExprStatContext ctx) {
        if (ctx.expr() != null) {
            return visit(ctx.expr());
        }
        return null;
    }

    @Override
    public AstNode visitNativeDecl(CgrScriptParser.NativeDeclContext ctx) {
        var function = (NativeFunctionSymbol) moduleScope.resolve(ctx.ID().getText());
        List<FunctionParameter> parameters = new ArrayList<>();
        if (ctx.functionDeclParam() != null) {
            CgrScriptParser.FunctionDeclParamContext paramCtx = ctx.functionDeclParam();
            parameters = getFunctionParameters(paramCtx);
        }
        function.setParameters(parameters);

        if (ctx.functionDeclRet().type() != null) {
            function.setReturnType((Type) visit(ctx.functionDeclRet().type()));
        }

        return null;
    }

    @Override
    public AstNode visitDeftemplate(CgrScriptParser.DeftemplateContext ctx) {
        var rule = (RuleSymbol) moduleScope.resolve(ctx.ID().getText());
        var query = (Query) visit(ctx.queryExpr());

        for (CgrScriptParser.JoinExprContext joinExprContext : ctx.joinExpr()) {
            query.addJoin((QueryJoin) visit(joinExprContext));
        }

        if (ctx.expr() != null) {
            query.setWhenExpr((Expr) visit(ctx.expr()));
        }

        rule.setQuery(query);

        List<Evaluable> exprList = new ArrayList<>();
        exprList.add((Evaluable) visit(ctx.template()));
        rule.setBlock(exprList);

        return null;
    }

    @Override
    public AstNode visitTemplate(CgrScriptParser.TemplateContext ctx) {
        TemplateStatement templateStatement = (TemplateStatement) visit(ctx.templateExpr());
        TemplateStatement root = templateStatement;
        var nextCtx = ctx.template();
        while (nextCtx != null) {
            TemplateStatement next = (TemplateStatement) visit(nextCtx.templateExpr());
            templateStatement.setNext(next);
            templateStatement = next;
            nextCtx = nextCtx.template();
        }

        return root;
    }

    @Override
    public AstNode visitTemplateStaticContentExpr(CgrScriptParser.TemplateStaticContentExprContext ctx) {
        return new TemplateStaticContentStatement(getSourceCodeRef(ctx.CONTENT()), ctx.CONTENT().getText());
    }

    @Override
    public AstNode visitTemplateContentExpr(CgrScriptParser.TemplateContentExprContext ctx) {
        Expr expr = (Expr) visit(ctx.expr());
        return new TemplateContentStatement(getSourceCodeRef(ctx.expr()), expr);
    }

    @Override
    public AstNode visitPathExpr(CgrScriptParser.PathExprContext ctx) {
        PathStatement pathStatement = (PathStatement) visit(ctx.pathSegmentExpr());
        PathStatement root = pathStatement;
        var nextCtx = ctx.pathExpr();
        while (nextCtx != null) {
            PathStatement next = (PathStatement) visit(nextCtx.pathSegmentExpr());
            pathStatement.setNext(next);
            pathStatement = next;
            nextCtx = nextCtx.pathExpr();
        }

        return root;
    }

    @Override
    public AstNode visitPathStaticSegmentExpr(CgrScriptParser.PathStaticSegmentExprContext ctx) {
        return new PathStaticSegmentStatement(getSourceCodeRef(ctx.PATH_SEGMENT()), ctx.PATH_SEGMENT().getText());
    }

    @Override
    public AstNode visitPathVariableExpr(CgrScriptParser.PathVariableExprContext ctx) {
        Expr expr = (Expr) visit(ctx.expr());
        return new PathSegmentStatement(getSourceCodeRef(ctx.expr()), expr);
    }

    @Override
    public AstNode visitDefrule(CgrScriptParser.DefruleContext ctx) {
        var rule = (RuleSymbol) moduleScope.resolve(ctx.ID().getText());
        var query = (Query) visit(ctx.queryExpr());

        for (CgrScriptParser.JoinExprContext joinExprContext : ctx.joinExpr()) {
            query.addJoin((QueryJoin) visit(joinExprContext));
        }

        if (ctx.expr() != null) {
            query.setWhenExpr((Expr) visit(ctx.expr()));
        }

        rule.setQuery(query);

        List<Evaluable> exprList = new ArrayList<>();
        if (ctx.block().execStat() != null) {
            for (CgrScriptParser.ExecStatContext execStatContext : ctx.block().execStat()) {
                Evaluable evaluable = (Evaluable) visit(execStatContext);
                if (evaluable != null) {
                    exprList.add(evaluable);
                }
            }
        }
        rule.setBlock(exprList);

        return null;
    }

    @Override
    public AstNode visitDeffile(CgrScriptParser.DeffileContext ctx) {
        var rule = (RuleSymbol) moduleScope.resolve(ctx.ID().getText());
        var query = (Query) visit(ctx.queryExpr());

        for (CgrScriptParser.JoinExprContext joinExprContext : ctx.joinExpr()) {
            query.addJoin((QueryJoin) visit(joinExprContext));
        }

        String bind = query.getTypeClause().getBind();

        SourceCodeRef sourceCodeRef = getSourceCodeRef(ctx);
        QueryTypeClause templateClause = new QueryTypeClause(sourceCodeRef, BuiltinScope.TEMPLATE_TYPE,
                false, "$_templateRef");
        QueryJoin templateJoin = new QueryJoin(sourceCodeRef, templateClause, new RefExpr(sourceCodeRef, bind));
        query.addJoin(templateJoin);

        if (ctx.expr() != null) {
            query.setWhenExpr((Expr) visit(ctx.expr()));
        }

        rule.setQuery(query);

        List<Evaluable> exprList = new ArrayList<>();
        exprList.add((Evaluable) visit(ctx.pathExpr()));
        rule.setBlock(exprList);

        return null;
    }

    @Override
    public AstNode visitFunctionReturnStat(CgrScriptParser.FunctionReturnStatContext ctx) {
        Expr expr = null;
        if (ctx.exprWrapper() != null) {
            expr = (Expr) visit(ctx.exprWrapper());
        }
        return new ReturnStatement(getSourceCodeRef(ctx), expr);
    }

    @Override
    public AstNode visitVarDecl(CgrScriptParser.VarDeclContext ctx) {
        return visit(ctx.varDeclBody());
    }

    @Override
    public AstNode visitVarDeclBody(CgrScriptParser.VarDeclBodyContext ctx) {
        Type type = null;
        if (ctx.type() != null) {
            type = (Type) visit(ctx.type());
        }
        VarDeclExpr expr = new VarDeclExpr(
                new VariableSymbol(getSourceCodeRef(ctx.ID()), ctx.ID().getText(), type));
        if (ctx.exprWrapper() != null) {
            var exprNode = visit(ctx.exprWrapper());
            expr.setValueExpr((Expr) exprNode);
        }
        return expr;
    }

    @Override
    public AstNode visitAssignElemValue(CgrScriptParser.AssignElemValueContext ctx) {
        var leftNode = visit(ctx.expr(0));
        var rightNode = visit(ctx.expr(1));
        return new AssignElemValueStatement(getSourceCodeRef(ctx),
                (Expr) leftNode, (Expr) rightNode);
    }

    @Override
    public AstNode visitAssignPostIncDec(CgrScriptParser.AssignPostIncDecContext ctx) {
        IncDecOperatorEnum operator = null;
        if (ctx.INC() != null) {
            operator = IncDecOperatorEnum.INC;
        } else {
            operator = IncDecOperatorEnum.DEC;
        }

        return new PostIncDecExpr(getSourceCodeRef(ctx), (Expr) visit(ctx.expr()), operator);
    }

    @Override
    public AstNode visitAssignPreIncDec(CgrScriptParser.AssignPreIncDecContext ctx) {
        IncDecOperatorEnum operator = null;
        if (ctx.INC() != null) {
            operator = IncDecOperatorEnum.INC;
        } else {
            operator = IncDecOperatorEnum.DEC;
        }

        return new PreIncDecExpr(getSourceCodeRef(ctx), (Expr) visit(ctx.expr()), operator);
    }

    @Override
    public AstNode visitIfStat(CgrScriptParser.IfStatContext ctx) {
        var condExprNode = visit(ctx.expr());
        List<Evaluable> block = new ArrayList<>();
        if (ctx.execStat() != null) {
            for (CgrScriptParser.ExecStatContext execStatContext : ctx.execStat()) {
                var statNode = (Evaluable) visit(execStatContext);
                if (statNode != null) {
                    block.add(statNode);
                }
            }
        }
        IfStatement ifStatement = new IfStatement(getSourceCodeRef(ctx), (Expr) condExprNode, block);
        if (ctx.elseIfStat() != null) {
            var elseIfNode = visit(ctx.elseIfStat());
            ifStatement.setElseIf((ElseIfStatement) elseIfNode);
        }
        if (ctx.elseStat() != null) {
            List<Evaluable> elseBlock = new ArrayList<>();
            for (CgrScriptParser.ExecStatContext execStatContext : ctx.elseStat().execStat()) {
                var execStatNode = (Evaluable) visit(execStatContext);
                if (execStatNode != null) {
                    elseBlock.add(execStatNode);
                }
            }
            ifStatement.setElseBlock(elseBlock);
        }
        return ifStatement;
    }

    @Override
    public AstNode visitElseIfStat(CgrScriptParser.ElseIfStatContext ctx) {
        var condExprNode = visit(ctx.expr());
        List<Evaluable> block = new ArrayList<>();
        if (ctx.execStat() != null) {
            for (CgrScriptParser.ExecStatContext execStatContext : ctx.execStat()) {
                var statNode = (Evaluable) visit(execStatContext);
                if (statNode != null) {
                    block.add(statNode);
                }
            }
        }
        ElseIfStatement elseIfStatement = new ElseIfStatement(getSourceCodeRef(ctx),
                (Expr) condExprNode, block);
        if (ctx.elseIfStat() != null) {
            var elseIfNode = visit(ctx.elseIfStat());
            elseIfStatement.setElseIf((ElseIfStatement) elseIfNode);
        }
        return elseIfStatement;
    }

    @Override
    public AstNode visitForStat(CgrScriptParser.ForStatContext ctx) {
        List<VarDeclExpr> varDeclList = new ArrayList<>();
        if (ctx.varDeclList() != null) {
            for (CgrScriptParser.VarDeclBodyContext varDeclBodyContext : ctx.varDeclList().varDeclBody()) {
                var varDeclNode = visit(varDeclBodyContext);
                varDeclList.add((VarDeclExpr) varDeclNode);
            }
        }
        List<Expr> condExprList = new ArrayList<>();
        for (CgrScriptParser.ExprWrapperContext exprContext : ctx.exprSequence().exprWrapper()) {
            var exprNode = visit(exprContext);
            if (exprNode != null) {
                condExprList.add((Expr) exprNode);
            }
        }
        List<Statement> stepStatementList = new ArrayList<>();
        for (CgrScriptParser.AssignmentContext assignmentContext : ctx.assignmentSequece().assignment()) {
            var statNode = visit(assignmentContext);
            if (statNode != null) {
                stepStatementList.add((Statement) statNode);
            }
        }
        List<Evaluable> block = new ArrayList<>();
        for (CgrScriptParser.ExecStatContext execStatContext : ctx.execStat()) {
            var statNode = (Evaluable) visit(execStatContext);
            if (statNode != null) {
                block.add(statNode);
            }
        }
        return new ForStatement(getSourceCodeRef(ctx),
                varDeclList, condExprList, stepStatementList, block);
    }

    @Override
    public AstNode visitWhileStat(CgrScriptParser.WhileStatContext ctx) {
        Expr condExpr = (Expr) visit(ctx.expr());
        List<Evaluable> block = new ArrayList<>();
        for (CgrScriptParser.ExecStatContext execStatContext : ctx.execStat()) {
            var statNode = (Evaluable) visit(execStatContext);
            if (statNode != null) {
                block.add(statNode);
            }
        }

        return new WhileStatement(getSourceCodeRef(ctx), condExpr, block);
    }

    @Override
    public AstNode visitBreakStat(CgrScriptParser.BreakStatContext ctx) {
        return new BreakStatement(getSourceCodeRef(ctx.BREAK()));
    }

    @Override
    public AstNode visitContinueStat(CgrScriptParser.ContinueStatContext ctx) {
        return new ContinueStatement(getSourceCodeRef(ctx.CONTINUE()));
    }

    @Override
    public AstNode visitRecord(CgrScriptParser.RecordContext ctx) {
        String recordType = ctx.ID().getText();
        RecordConstructorCallExpr recordConstructor = new RecordConstructorCallExpr(getSourceCodeRef(ctx.ID()), recordType);

        CgrScriptParser.RecordFieldContext fieldCtx = ctx.recordField();
        while (fieldCtx != null) {
            var exprNode = visit(fieldCtx.exprWrapper());
            recordConstructor.addField(new RecordFieldExpr(getSourceCodeRef(fieldCtx.ID()), fieldCtx.ID().getText(),
                    (Expr) exprNode));
            fieldCtx = fieldCtx.recordField();
        }

        return recordConstructor;
    }

    @Override
    public AstNode visitArrayExpr(CgrScriptParser.ArrayExprContext ctx) {
        List<Expr> elements = new ArrayList<>();
        if (ctx.exprSequence() != null) {
            for (CgrScriptParser.ExprWrapperContext exprContext : ctx.exprSequence().exprWrapper()) {
                var exprNode = visit(exprContext);
                if (exprNode != null) {
                    elements.add((Expr) exprNode);
                }
            }
        }
        return new ArrayConstructorCallExpr(getSourceCodeRef(ctx), elements);
    }

    @Override
    public AstNode visitPairExpr(CgrScriptParser.PairExprContext ctx) {
        Expr leftExpr = (Expr) visit(ctx.expr(0));
        Expr rightExpr = (Expr) visit(ctx.expr(1));

        return new PairConstructorCallExpr(getSourceCodeRef(ctx), leftExpr, rightExpr);
    }

    @Override
    public AstNode visitFunctionCallExpr(CgrScriptParser.FunctionCallExprContext ctx) {

        List<FunctionArgExpr> args = new ArrayList<>();
        if (ctx.exprSequence() != null) {
            for (CgrScriptParser.ExprWrapperContext exprContext : ctx.exprSequence().exprWrapper()) {
                var exprNode = visit(exprContext);
                FunctionArgExpr argExpr = new FunctionArgExpr(getSourceCodeRef(exprContext),
                        (Expr) exprNode);
                args.add(argExpr);
            }
        }

        return new FunctionCallExpr(getSourceCodeRef(ctx),
                ctx.ID().getText(), args);
    }

    @Override
    public AstNode visitArrayAccessExpr(CgrScriptParser.ArrayAccessExprContext ctx) {
        var arrayRefNode = visit(ctx.expr());
        var indexNode = visit(ctx.arrayIndexExpr());

        return new ArrayAccessExpr(getSourceCodeRef(ctx),
                (Expr) arrayRefNode, (ArrayIndexExpr) indexNode);
    }

    @Override
    public AstNode visitArrayIndexSliceExpr(CgrScriptParser.ArrayIndexSliceExprContext ctx) {
        Expr beginExpr = (Expr) visit(ctx.expr(0));
        Expr endExpr = (Expr) visit(ctx.expr(1));
        return new ArraySliceIndexExpr(getSourceCodeRef(ctx), beginExpr, endExpr);
    }

    @Override
    public AstNode visitArrayIndexSliceEndExpr(CgrScriptParser.ArrayIndexSliceEndExprContext ctx) {
        Expr endExpr = (Expr) visit(ctx.expr());
        return new ArraySliceIndexExpr(getSourceCodeRef(ctx), null, endExpr);
    }

    @Override
    public AstNode visitArrayIndexSliceBeginExpr(CgrScriptParser.ArrayIndexSliceBeginExprContext ctx) {
        Expr beginExpr = (Expr) visit(ctx.expr());
        return new ArraySliceIndexExpr(getSourceCodeRef(ctx), beginExpr, null);
    }

    @Override
    public AstNode visitArrayIndexItemExpr(CgrScriptParser.ArrayIndexItemExprContext ctx) {
        Expr expr = (Expr) visit(ctx.expr());
        return new ArrayItemIndexExpr(getSourceCodeRef(ctx.expr()), expr);
    }

    @Override
    public AstNode visitNotExpr(CgrScriptParser.NotExprContext ctx) {
        var exprNode = visit(ctx.expr());
        return new NotExpr(getSourceCodeRef(ctx), (Expr) exprNode);
    }

    @Override
    public AstNode visitFactorExpr(CgrScriptParser.FactorExprContext ctx) {
        var leftExprNode = visit(ctx.expr(0));
        var rightExprNode = visit(ctx.expr(1));
        Expr expr;
        if (ctx.STAR() != null) {
            expr = new MultExpr(getSourceCodeRef(ctx), (Expr) leftExprNode, (Expr) rightExprNode);
        } else {
            expr = new DivExpr(getSourceCodeRef(ctx), (Expr) leftExprNode, (Expr) rightExprNode);
        }
        return expr;
    }

    @Override
    public AstNode visitAddSubExpr(CgrScriptParser.AddSubExprContext ctx) {
        var leftExprNode = visit(ctx.expr(0));
        var rightExprNode = visit(ctx.expr(1));
        Expr expr;
        if (ctx.PLUS() != null) {
            expr = new AddExpr(getSourceCodeRef(ctx), (Expr) leftExprNode, (Expr) rightExprNode);
        } else {
            expr = new SubExpr(getSourceCodeRef(ctx), (Expr) leftExprNode, (Expr) rightExprNode);
        }
        return expr;
    }

    @Override
    public AstNode visitEqExpr(CgrScriptParser.EqExprContext ctx) {
        var leftExprNode = visit(ctx.expr(0));
        var rightExprNode = visit(ctx.expr(1));
        EqOperatorEnum operator = null;

        if (ctx.EQUALS() != null) {
            operator = EqOperatorEnum.EQUALS;
        } else if (ctx.NOT_EQUALS() != null) {
            operator = EqOperatorEnum.NOT_EQUALS;
        } else if (ctx.LESS() != null) {
            operator = EqOperatorEnum.LESS;
        } else if (ctx.LESS_OR_EQUALS() != null) {
            operator = EqOperatorEnum.LESS_OR_EQUALS;
        } else if (ctx.GREATER() != null) {
            operator = EqOperatorEnum.GREATER;
        } else if (ctx.GREATER_OR_EQUALS() != null) {
            operator = EqOperatorEnum.GREATER_OR_EQUALS;
        }

        return new EqExpr(getSourceCodeRef(ctx),
                (Expr) leftExprNode, operator, (Expr) rightExprNode);
    }

    @Override
    public AstNode visitLogicExpr(CgrScriptParser.LogicExprContext ctx) {
        var leftExprNode = visit(ctx.expr(0));
        var rightExprNode = visit(ctx.expr(1));
        LogicOperatorEnum operator = null;

        if (ctx.AND() != null) {
            operator = LogicOperatorEnum.AND;
        } else if (ctx.OR() != null) {
            operator = LogicOperatorEnum.OR;
        }

        return new LogicExpr(getSourceCodeRef(ctx),
                (Expr) leftExprNode, operator, (Expr) rightExprNode);
    }

    @Override
    public AstNode visitFieldAccessExpr(CgrScriptParser.FieldAccessExprContext ctx) {
        var leftExprNode = visit(ctx.expr(0));
        var rightExprNode = visit(ctx.expr(1));
        return new FieldAccessExpr(getSourceCodeRef(ctx),
                (Expr) leftExprNode, (Expr) rightExprNode);
    }

    @Override
    public AstNode visitIdExpr(CgrScriptParser.IdExprContext ctx) {
        return new RefExpr(getSourceCodeRef(ctx.ID()), ctx.ID().getText());
    }

    @Override
    public AstNode visitStringExpr(CgrScriptParser.StringExprContext ctx) {
        StringBuilder str = new StringBuilder();
        String source = ctx.STRING().getText();
        boolean escape = false;
        StringBuilder unicode = null;
        for (int i = 1; i < source.length() - 1; i++) {
            char c = source.charAt(i);
            if (c == '\\') {
                escape = true;
            } else if (escape) {
                if (c == 'n') {
                    str.append('\n');
                } else if (c == 'r') {
                    str.append('\r');
                } else if (c == 't') {
                    str.append('\t');
                } else if (c == 'b') {
                    str.append('\b');
                } else if (c == 'f') {
                    str.append('\f');
                } else if (c == 'u') {
                    unicode = new StringBuilder();
                }
                escape = false;
            } else if (unicode != null) {
                unicode.append(c);
                if (unicode.length() == 4) {
                    str.append(Character.toChars(Integer.parseInt(unicode.toString(), 16)));
                }
            } else {
                str.append(c);
            }
        }
        return new StringValueExpr(getSourceCodeRef(ctx.STRING()), str.toString());
    }

    @Override
    public AstNode visitNumberExpr(CgrScriptParser.NumberExprContext ctx) {
        String numberText = ctx.NUMBER().getText();
        return NumberParser.getNumberValueExpr(numberText, getSourceCodeRef(ctx.NUMBER()));
    }

    @Override
    public AstNode visitTrueExpr(CgrScriptParser.TrueExprContext ctx) {
        return new BooleanValueExpr(getSourceCodeRef(ctx.TRUE()), true);
    }

    @Override
    public AstNode visitFalseExpr(CgrScriptParser.FalseExprContext ctx) {
        return new BooleanValueExpr(getSourceCodeRef(ctx.FALSE()), false);
    }

    @Override
    public AstNode visitNullExpr(CgrScriptParser.NullExprContext ctx) {
        return new NullValueExpr(getSourceCodeRef(ctx.NULL()));
    }

    @Override
    public AstNode visitQueryExpr(CgrScriptParser.QueryExprContext ctx) {
        String bind = null;
        if (ctx.queryExprAlias() != null) {
            bind = ctx.queryExprAlias().ID().getText();
        }
        QueryTypeClause queryTypeClause = new QueryTypeClause(getSourceCodeRef(ctx), (Type) visit(ctx.type()),
                ctx.ANY() != null, bind);

        if (ctx.queryExprSegment() != null) {
            QueryPipeClause pipeClause = (QueryPipeClause) visit(ctx.queryExprSegment());
            queryTypeClause.setPipeClause(pipeClause);
        }

        return new Query(getSourceCodeRef(ctx), queryTypeClause);
    }

    @Override
    public AstNode visitQueryExprSegment(CgrScriptParser.QueryExprSegmentContext ctx) {
        String alias = null;
        if (ctx.queryExprAlias() != null) {
            alias = ctx.queryExprAlias().ID().getText();
        }

        QueryPipeClause pipeClause = (QueryPipeClause) visit(ctx.queryPipeExpr());
        if (alias != null) {
            pipeClause.setAlias(alias);
        }

        if (ctx.queryExprSegment() != null) {
            QueryPipeClause next = (QueryPipeClause) visit(ctx.queryExprSegment());
            pipeClause.setNext(next);
        }
        return pipeClause;
    }

    @Override
    public AstNode visitQueryFieldExpr(CgrScriptParser.QueryFieldExprContext ctx) {

        var field = new QueryFieldClause(getSourceCodeRef(ctx), ctx.ID().getText());
        if (ctx.queryExprArraySelect() != null) {
            field.setAlias(null);
            QueryArrayItemClause arrayItemClause = (QueryArrayItemClause) visit(ctx.queryExprArraySelect());
            field.setArrayItemClause(arrayItemClause);
        }

        return field;
    }

    @Override
    public AstNode visitQueryFunctionCallExpr(CgrScriptParser.QueryFunctionCallExprContext ctx) {

        var functionCallExpr = (FunctionCallExpr) visit(ctx.functionCallExpr());
        var functionCallClause = new QueryFunctionCallClause(getSourceCodeRef(ctx), functionCallExpr);

        if (ctx.queryExprArraySelect() != null) {
            QueryArrayItemClause arrayItemClause = (QueryArrayItemClause) visit(ctx.queryExprArraySelect());
            functionCallClause.setArrayItemClause(arrayItemClause);
        }

        return functionCallClause;
    }

    @Override
    public AstNode visitQueryExprArrayItemIndex(CgrScriptParser.QueryExprArrayItemIndexContext ctx) {
        var index = (ArrayIndexExpr) visit(ctx.arrayIndexExpr());
        return new QueryArrayIndexClause(getSourceCodeRef(ctx), index);
    }

    @Override
    public AstNode visitQueryExprArrayItemAll(CgrScriptParser.QueryExprArrayItemAllContext ctx) {
        return new QueryArrayAllClause(getSourceCodeRef(ctx));
    }

    @Override
    public AstNode visitJoinExpr(CgrScriptParser.JoinExprContext ctx) {
        Query query = (Query) visit(ctx.queryExpr());

        Expr ofExpr = null;
        if (ctx.joinOfExpr() != null) {
            ofExpr = (Expr) visit(ctx.joinOfExpr().expr());
        }

        return new QueryJoin(getSourceCodeRef(ctx), query.getTypeClause(), ofExpr);
    }

    @Override
    public AstNode visitArrayType(CgrScriptParser.ArrayTypeContext ctx) {
        return new ArrayType((Type) visit(ctx.type()));
    }

    @Override
    public AstNode visitSingleType(CgrScriptParser.SingleTypeContext ctx) {
        var typeName = ctx.ID().getText();
        var symbol = moduleScope.resolve(typeName);

        if (!(symbol instanceof Type)) {
            moduleScope.addError(new UndefinedTypeError(getSourceCodeRef(ctx.ID()), typeName));
            return UnknownType.INSTANCE;
        }

        return (Type) symbol;
    }

    @Override
    public AstNode visitPairType(CgrScriptParser.PairTypeContext ctx) {
        return new PairType((Type) visit(ctx.type(0)), (Type) visit(ctx.type(1)));
    }

    public ModuleScope getModuleScope() {
        return moduleScope;
    }

    private List<FunctionParameter> getFunctionParameters(CgrScriptParser.FunctionDeclParamContext paramCtx) {
        List<FunctionParameter> parameters;
        parameters = new ArrayList<>();
        Map<String, FunctionParameter> paramsMap = new HashMap<>();
        FunctionParameter lastOptionalParam = null;

        while (paramCtx != null) {
            var type = (Type) visit(paramCtx.type());
            FunctionParameter param = new FunctionParameter(getSourceCodeRef(paramCtx.ID()), paramCtx.ID().getText(), type,
                    paramCtx.QM() != null);

            FunctionParameter currentParam = paramsMap.get(param.getName());
            if (currentParam != null) {
                moduleScope.addError(new DuplicatedFunctionParamError(currentParam, param));
                continue;
            }
            if (param.isOptional()) {
                lastOptionalParam = param;
            } else {
                if (lastOptionalParam != null) {
                    moduleScope.addError(new InvalidRequiredFunctionParamError(param));
                }
            }
            paramsMap.put(param.getName(), param);
            parameters.add(param);

            paramCtx = paramCtx.functionDeclParam();
        }
        return parameters;
    }

}
