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

import dev.kobu.antlr.kobulang.KobuParser;
import dev.kobu.interpreter.ast.eval.*;
import dev.kobu.interpreter.ast.eval.context.EvalModeEnum;
import dev.kobu.interpreter.ast.eval.expr.*;
import dev.kobu.interpreter.ast.eval.expr.value.*;
import dev.kobu.interpreter.ast.eval.statement.*;
import dev.kobu.interpreter.ast.file.PathSegmentStatement;
import dev.kobu.interpreter.ast.file.PathStatement;
import dev.kobu.interpreter.ast.file.PathStaticSegmentStatement;
import dev.kobu.interpreter.ast.query.*;
import dev.kobu.interpreter.ast.symbol.*;
import dev.kobu.interpreter.ast.template.TemplateContentStatement;
import dev.kobu.interpreter.ast.template.TemplateStatement;
import dev.kobu.interpreter.ast.template.TemplateStaticContentStatement;
import dev.kobu.interpreter.ast.utils.NumberParser;
import dev.kobu.interpreter.ast.utils.SymbolDescriptorUtils;
import dev.kobu.interpreter.error.analyzer.*;
import dev.kobu.interpreter.module.ModuleLoader;

import java.util.ArrayList;
import java.util.List;

public class EvalTreeParserVisitor extends KobuParserVisitor<AstNode> {

    private final AnalyzerContext context;

    private boolean topLevelExpression = true;

    private boolean functionReturnType = false;

    private int scopeEndOffset = 0;

    public EvalTreeParserVisitor(ModuleLoader moduleLoader, ModuleScope moduleScope, AnalyzerContext context) {
        super(moduleLoader);
        this.context = context;
        this.moduleScope = moduleScope;
    }

    @Override
    public AstNode visitModule(KobuParser.ModuleContext ctx) {
        scopeEndOffset = ctx.getStop().getStopIndex() + 1;
        return null;
    }

    @Override
    public AstNode visitImportExpr(KobuParser.ImportExprContext ctx) {
        scopeEndOffset = ctx.getStop().getStopIndex() + 1;
        return null;
    }

    @Override
    public AstNode visitInvalidStat(KobuParser.InvalidStatContext ctx) {
        //add a reference element for auto-completion service
        if (moduleLoader.getEvalMode() == EvalModeEnum.ANALYZER_SERVICE) {
            moduleScope.registerAutoCompletionSource(ctx.ID().getSymbol().getStartIndex(), new AutoCompletionSource() {
                @Override
                public List<SymbolDescriptor> requestSuggestions(List<ModuleScope> externalModules) {
                    return SymbolDescriptorUtils.getGlobalKeywords();
                }

                @Override
                public boolean hasOwnCompletionScope() {
                    return true;
                }
            });
        }
        return null;
    }

    @Override
    public AstNode visitInvalidDef(KobuParser.InvalidDefContext ctx) {
        context.getErrorScope().addError(new InvalidDefinitionError(getSourceCodeRef(ctx.DEF())));
        if (moduleLoader.getEvalMode() == EvalModeEnum.ANALYZER_SERVICE && ctx.elem != null) {
            //add a reference element for auto-completion service
            moduleScope.registerAutoCompletionSource(ctx.elem.getStartIndex(), new AutoCompletionSource() {
                @Override
                public List<SymbolDescriptor> requestSuggestions(List<ModuleScope> externalModules) {
                    return SymbolDescriptorUtils.getDefKeywords();
                }

                @Override
                public boolean hasOwnCompletionScope() {
                    return true;
                }
            });
        }
        return null;
    }

    @Override
    public AstNode visitDeftype(KobuParser.DeftypeContext ctx) {
        if (ctx.ID() == null) {
            return null;
        }
        RecordTypeSymbol recordType = (RecordTypeSymbol) moduleScope.resolve(ctx.ID().getText());
        if (recordType == null) {
            return null;
        }

        if (ctx.RCB() != null) {
            scopeEndOffset = ctx.RCB().getSymbol().getStopIndex() + 1;
        }

        if (ctx.attributes() != null) {

            KobuParser.AttributesContext attrCtx = ctx.attributes();
            while (attrCtx != null) {
                if (attrCtx.type() != null) {
                    var type = (Type) visit(attrCtx.type());
                    if (attrCtx.ID() != null) {
                        var attr = new RecordTypeAttribute(moduleScope, getSourceCodeRef(attrCtx.ID()), attrCtx.ID().getText(), type);
                        recordType.addAttribute(context, attr);
                    } else if (attrCtx.STAR() != null) {
                        var sourceCodeRef = getSourceCodeRef(attrCtx);
                        recordType.setStarAttribute(context, new RecordTypeStarAttribute(sourceCodeRef, type));
                    }
                }
                attrCtx = attrCtx.attributes();
            }

        }

        if (ctx.inheritance() != null && ctx.inheritance().typeName() != null) {
            var typeNameExpr = ctx.inheritance().typeName();
            Type superType = (Type) visit(typeNameExpr);
            if (!(superType instanceof RecordTypeSymbol)) {
                if (!(superType instanceof AnyRecordTypeSymbol)) {
                    context.getErrorScope().addError(new RecordInvalidSuperTypeError(getSourceCodeRef(typeNameExpr), recordType,
                            typeNameExpr.getText()));
                }
            } else {
                recordType.setSuperType(new RecordSuperType(getSourceCodeRef(typeNameExpr), (RecordTypeSymbol) superType));
            }
        }

        recordType.buildMethods();

        return null;
    }

    @Override
    public AstNode visitFunctionDecl(KobuParser.FunctionDeclContext ctx) {
        if (ctx.ID() == null) {
            return null;
        }
        var function = (FunctionSymbol) moduleScope.resolve(ctx.ID().getText());
        if (function == null) {
            return null;
        }

        if (ctx.RCB() != null) {
            scopeEndOffset = ctx.RCB().getSymbol().getStopIndex() + 1;
        }

        List<FunctionParameter> parameters = new ArrayList<>();
        if (ctx.functionDeclParam() != null) {
            KobuParser.FunctionDeclParamContext paramCtx = ctx.functionDeclParam();
            parameters = getFunctionParameters(paramCtx);
        }

        List<Evaluable> exprList = new ArrayList<>();
        if (ctx.execStat() != null) {
            for (KobuParser.ExecStatContext execStatContext : ctx.execStat()) {
                Evaluable evaluable = (Evaluable) visit(execStatContext);
                if (evaluable != null) {
                    exprList.add(evaluable);
                }
            }
        }

        function.setParameters(parameters);
        if (ctx.functionDeclRet() != null && ctx.functionDeclRet().type() != null) {
            functionReturnType = true;
            try {
                function.setReturnType((Type) visit(ctx.functionDeclRet().type()));
            } finally {
                functionReturnType = false;
            }
        }
        function.setExprList(exprList);

        return null;
    }

    @Override
    public AstNode visitNativeDecl(KobuParser.NativeDeclContext ctx) {
        var function = (NativeFunctionSymbol) moduleScope.resolve(ctx.ID().getText());
        if (function == null) {
            return null;
        }
        List<FunctionParameter> parameters = new ArrayList<>();
        if (ctx.functionDeclParam() != null) {
            KobuParser.FunctionDeclParamContext paramCtx = ctx.functionDeclParam();
            parameters = getFunctionParameters(paramCtx);
        }
        function.setParameters(parameters);

        if (ctx.functionDeclRet().type() != null) {
            function.setReturnType((Type) visit(ctx.functionDeclRet().type()));
        }

        return null;
    }

    @Override
    public AstNode visitDeftemplate(KobuParser.DeftemplateContext ctx) {
        if (ctx.ID() == null) {
            return null;
        }
        var rule = (RuleSymbol) moduleScope.resolve(ctx.ID().getText());
        if (rule == null) {
            return null;
        }

        if (ctx.ruleExtends() != null && ctx.ruleExtends().typeName() != null) {
            var parentRule = visit(ctx.ruleExtends().typeName());
            if (parentRule instanceof RuleSymbol) {
                rule.setParentRuleSymbol((RuleSymbol) parentRule);
            } else {
                context.getErrorScope().addError(new InvalidParentRuleError(getSourceCodeRef(ctx.ruleExtends().typeName()),
                        ctx.ruleExtends().getText()));
            }
        }

        if (ctx.TEMPLATE_END() != null) {
            scopeEndOffset = ctx.TEMPLATE_END().getSymbol().getStopIndex() + 1;
        }

        if (ctx.queryExpr() != null) {
            topLevelExpression = false;
            var query = (Query) visit(ctx.queryExpr());

            for (KobuParser.JoinExprContext joinExprContext : ctx.joinExpr()) {
                query.addJoin((QueryJoin) visit(joinExprContext));
            }

            if (ctx.expr() != null) {
                query.setWhenExpr((Expr) visit(ctx.expr()));
            }
            topLevelExpression = true;

            rule.setQuery(query);

            List<Evaluable> exprList = new ArrayList<>();
            if (ctx.template() != null) {
                exprList.add((Evaluable) visit(ctx.template()));
            }
            rule.setBlock(exprList);
        }

        return null;
    }

    @Override
    public AstNode visitTemplate(KobuParser.TemplateContext ctx) {
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
    public AstNode visitTemplateStaticContentExpr(KobuParser.TemplateStaticContentExprContext ctx) {
        return new TemplateStaticContentStatement(getSourceCodeRef(ctx.CONTENT()), ctx.CONTENT().getText());
    }

    @Override
    public AstNode visitTemplateContentExpr(KobuParser.TemplateContentExprContext ctx) {
        if (ctx.expr() == null) {
            context.getErrorScope().addError(new MissingExpressionError(getSourceCodeRef(ctx)));
            return new TemplateStaticContentStatement(getSourceCodeRef(ctx), "");
        }

        topLevelExpression = false;
        Expr expr = (Expr) visit(ctx.expr());
        topLevelExpression = true;
        return new TemplateContentStatement(getSourceCodeRef(ctx.expr()), expr);
    }

    @Override
    public AstNode visitPathExpr(KobuParser.PathExprContext ctx) {
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
    public AstNode visitPathStaticSegmentExpr(KobuParser.PathStaticSegmentExprContext ctx) {
        return new PathStaticSegmentStatement(getSourceCodeRef(ctx.PATH_SEGMENT()), ctx.PATH_SEGMENT().getText());
    }

    @Override
    public AstNode visitPathVariableExpr(KobuParser.PathVariableExprContext ctx) {
        topLevelExpression = false;
        Expr expr = (Expr) visit(ctx.expr());
        topLevelExpression = true;
        return new PathSegmentStatement(getSourceCodeRef(ctx.expr()), expr);
    }

    @Override
    public AstNode visitDefrule(KobuParser.DefruleContext ctx) {
        if (ctx.ID() == null) {
            return null;
        }
        var rule = (RuleSymbol) moduleScope.resolve(ctx.ID().getText());
        if (rule == null) {
            return null;
        }

        if (ctx.ruleExtends() != null && ctx.ruleExtends().typeName() != null) {
            Type parentRule = (Type) visit(ctx.ruleExtends().typeName());
            if (parentRule instanceof RuleSymbol) {
                rule.setParentRuleSymbol((RuleSymbol) parentRule);
            } else {
                context.getErrorScope().addError(new InvalidParentRuleError(getSourceCodeRef(ctx.ruleExtends().typeName()),
                        parentRule.getName()));
            }
        }

        if (ctx.RCB() != null) {
            scopeEndOffset = ctx.RCB().getSymbol().getStopIndex() + 1;
        }

        if (ctx.queryExpr() != null) {
            topLevelExpression = false;
            var query = (Query) visit(ctx.queryExpr());

            for (KobuParser.JoinExprContext joinExprContext : ctx.joinExpr()) {
                query.addJoin((QueryJoin) visit(joinExprContext));
            }

            if (ctx.expr() != null) {
                query.setWhenExpr((Expr) visit(ctx.expr()));
            }
            rule.setQuery(query);
            topLevelExpression = true;
        }

        List<Evaluable> exprList = new ArrayList<>();
        if (ctx.block() != null && ctx.block().execStat() != null) {
            for (KobuParser.ExecStatContext execStatContext : ctx.block().execStat()) {
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
    public AstNode visitDeffile(KobuParser.DeffileContext ctx) {
        if (ctx.ID() == null) {
            return null;
        }
        var rule = (RuleSymbol) moduleScope.resolve(ctx.ID().getText());
        if (rule == null) {
            return null;
        }

        if (ctx.ruleExtends() != null && ctx.ruleExtends().typeName() != null) {
            Type parentRule = (Type) visit(ctx.ruleExtends().typeName());
            if (parentRule instanceof RuleSymbol) {
                rule.setParentRuleSymbol((RuleSymbol) parentRule);
            } else {
                context.getErrorScope().addError(new InvalidParentRuleError(getSourceCodeRef(ctx.ruleExtends().typeName()),
                        parentRule.getName()));
            }
        }

        if (ctx.PATH_END() != null) {
            scopeEndOffset = ctx.PATH_END().getSymbol().getStopIndex() + 1;
        }

        if (ctx.queryExpr() != null) {
            topLevelExpression = false;
            var query = (Query) visit(ctx.queryExpr());

            for (KobuParser.JoinExprContext joinExprContext : ctx.joinExpr()) {
                query.addJoin((QueryJoin) visit(joinExprContext));
            }

            String bind = query.getTypeClause().getBind();

            SourceCodeRef sourceCodeRef = getSourceCodeRef(ctx);
            QueryTypeClause templateClause = new QueryTypeClause(moduleScope, sourceCodeRef, null, BuiltinScope.TEMPLATE_TYPE,
                    false, "$_templateRef");
            QueryJoin templateJoin = new QueryJoin(sourceCodeRef, templateClause, new RefExpr(moduleScope, sourceCodeRef, bind));
            query.addJoin(templateJoin);

            if (ctx.expr() != null) {
                query.setWhenExpr((Expr) visit(ctx.expr()));
            }
            rule.setQuery(query);
            topLevelExpression = true;
        }

        List<Evaluable> exprList = new ArrayList<>();
        exprList.add((Evaluable) visit(ctx.pathExpr()));
        rule.setBlock(exprList);

        return null;
    }

    @Override
    public AstNode visitInvalidKeyword(KobuParser.InvalidKeywordContext ctx) {
        context.getErrorScope().addError(new InvalidKeywordError(getSourceCodeRef(ctx), ctx.keyword.getText()));
        return null;
    }

    @Override
    public AstNode visitFunctionReturnStat(KobuParser.FunctionReturnStatContext ctx) {
        Expr expr = null;
        if (ctx.exprWrapper() != null) {
            topLevelExpression = false;
            expr = (Expr) visit(ctx.exprWrapper());
            topLevelExpression = true;
        }
        return new ReturnStatement(getSourceCodeRef(ctx), expr);
    }

    @Override
    public AstNode visitVarDecl(KobuParser.VarDeclContext ctx) {
        return ctx.varDeclBody() != null ? visit(ctx.varDeclBody()) : null;
    }

    @Override
    public AstNode visitVarDeclBody(KobuParser.VarDeclBodyContext ctx) {
        topLevelExpression = false;

        Type type = null;
        if (ctx.type() != null) {
            type = (Type) visit(ctx.type());
        }
        VarDeclExpr expr = new VarDeclExpr(
                new VariableSymbol(moduleScope, getSourceCodeRef(ctx.ID()), ctx.ID().getText(), type));
        if (ctx.exprWrapper() != null) {
            var exprNode = visit(ctx.exprWrapper());
            expr.setValueExpr((Expr) exprNode);
        }

        topLevelExpression = true;
        return expr;
    }

    @Override
    public AstNode visitAssignElemValue(KobuParser.AssignElemValueContext ctx) {
        topLevelExpression = false;

        var leftNode = visit(ctx.expr(0));
        var rightNode = visit(ctx.expr(1));

        topLevelExpression = true;
        return new AssignElemValueStatement(getSourceCodeRef(ctx),
                (Expr) leftNode, (Expr) rightNode);
    }

    @Override
    public AstNode visitAssignPostIncDec(KobuParser.AssignPostIncDecContext ctx) {
        boolean exprStatus = topLevelExpression;
        topLevelExpression = false;

        IncDecOperatorEnum operator;
        if (ctx.INC() != null) {
            operator = IncDecOperatorEnum.INC;
        } else {
            operator = IncDecOperatorEnum.DEC;
        }

        topLevelExpression = exprStatus;
        return new PostIncDecExpr(getSourceCodeRef(ctx), (Expr) visit(ctx.expr()), operator);
    }

    @Override
    public AstNode visitAssignPreIncDec(KobuParser.AssignPreIncDecContext ctx) {
        boolean exprStatus = topLevelExpression;
        topLevelExpression = false;

        IncDecOperatorEnum operator;
        if (ctx.INC() != null) {
            operator = IncDecOperatorEnum.INC;
        } else {
            operator = IncDecOperatorEnum.DEC;
        }

        topLevelExpression = exprStatus;
        return new PreIncDecExpr(getSourceCodeRef(ctx), (Expr) visit(ctx.expr()), operator);
    }

    @Override
    public AstNode visitIfStat(KobuParser.IfStatContext ctx) {
        if (ctx.LP() != null && ctx.RP() != null && ctx.expr() == null) {
            context.getErrorScope().addError(new MissingExpressionError(getSourceCodeRef(ctx.LP())));
        }

        topLevelExpression = false;
        var condExprNode = ctx.expr() != null ? visit(ctx.expr()) : null;
        topLevelExpression = true;

        List<Evaluable> block = new ArrayList<>();
        if (ctx.execStat() != null) {
            for (KobuParser.ExecStatContext execStatContext : ctx.execStat()) {
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
            for (KobuParser.ExecStatContext execStatContext : ctx.elseStat().execStat()) {
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
    public AstNode visitElseIfStat(KobuParser.ElseIfStatContext ctx) {

        topLevelExpression = false;
        var condExprNode = visit(ctx.expr());
        topLevelExpression = true;

        List<Evaluable> block = new ArrayList<>();
        if (ctx.execStat() != null) {
            for (KobuParser.ExecStatContext execStatContext : ctx.execStat()) {
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
    public AstNode visitForStat(KobuParser.ForStatContext ctx) {
        List<VarDeclExpr> varDeclList = new ArrayList<>();

        topLevelExpression = false;
        if (ctx.varDeclList() != null) {
            for (KobuParser.VarDeclBodyContext varDeclBodyContext : ctx.varDeclList().varDeclBody()) {
                var varDeclNode = visit(varDeclBodyContext);
                varDeclList.add((VarDeclExpr) varDeclNode);
            }
        }
        topLevelExpression = false;
        Expr condExpr = null;
        if (ctx.expr() != null) {
            condExpr = (Expr) visit(ctx.expr());
        }
        topLevelExpression = false;
        List<Statement> stepStatementList = new ArrayList<>();
        if (ctx.assignmentSequece() != null) {
            for (KobuParser.AssignmentContext assignmentContext : ctx.assignmentSequece().assignment()) {
                var statNode = visit(assignmentContext);
                if (statNode != null) {
                    stepStatementList.add((Statement) statNode);
                }
            }
        }
        topLevelExpression = true;

        List<Evaluable> block = new ArrayList<>();
        if (ctx.execStat() != null) {
            for (KobuParser.ExecStatContext execStatContext : ctx.execStat()) {
                var statNode = (Evaluable) visit(execStatContext);
                if (statNode != null) {
                    block.add(statNode);
                }
            }
        }
        return new ForStatement(getSourceCodeRef(ctx),
                varDeclList, condExpr, stepStatementList, block);
    }

    @Override
    public AstNode visitWhileStat(KobuParser.WhileStatContext ctx) {

        topLevelExpression = false;
        Expr condExpr = (Expr) visit(ctx.expr());
        topLevelExpression = true;

        List<Evaluable> block = new ArrayList<>();
        for (KobuParser.ExecStatContext execStatContext : ctx.execStat()) {
            var statNode = (Evaluable) visit(execStatContext);
            if (statNode != null) {
                block.add(statNode);
            }
        }

        return new WhileStatement(getSourceCodeRef(ctx), condExpr, block);
    }

    @Override
    public AstNode visitBreakStat(KobuParser.BreakStatContext ctx) {
        return new BreakStatement(getSourceCodeRef(ctx.BREAK()));
    }

    @Override
    public AstNode visitContinueStat(KobuParser.ContinueStatContext ctx) {
        return new ContinueStatement(getSourceCodeRef(ctx.CONTINUE()));
    }

    @Override
    public AstNode visitRecord(KobuParser.RecordContext ctx) {
        boolean exprStatus = topLevelExpression;
        if (topLevelExpression) {
            context.getErrorScope().addError(new InvalidStatementError(getSourceCodeRef(ctx)));
            topLevelExpression = false;
        }

        var sourceCodeRef = getSourceCodeRef(ctx.typeName());
        Type type = (Type) visit(ctx.typeName());
        RecordConstructorCallExpr recordConstructor = new RecordConstructorCallExpr(sourceCodeRef, type);

        KobuParser.RecordFieldContext fieldCtx = ctx.recordField();
        while (fieldCtx != null && fieldCtx.exprWrapper() != null) {
            var exprNode = visit(fieldCtx.exprWrapper());
            recordConstructor.addField(new RecordFieldExpr(getSourceCodeRef(fieldCtx.ID()), fieldCtx.ID().getText(),
                    (Expr) exprNode));
            fieldCtx = fieldCtx.recordField();
        }

        topLevelExpression = exprStatus;

        return recordConstructor;
    }

    @Override
    public AstNode visitArrayExpr(KobuParser.ArrayExprContext ctx) {
        boolean exprStatus = topLevelExpression;
        if (topLevelExpression) {
            context.getErrorScope().addError(new InvalidStatementError(getSourceCodeRef(ctx)));
            topLevelExpression = false;
        }

        List<Expr> elements = new ArrayList<>();
        if (ctx.exprSequence() != null) {
            for (KobuParser.ExprWrapperContext exprContext : ctx.exprSequence().exprWrapper()) {
                var exprNode = visit(exprContext);
                if (exprNode != null) {
                    elements.add((Expr) exprNode);
                }
            }
        }

        topLevelExpression = exprStatus;
        return new ArrayConstructorCallExpr(getSourceCodeRef(ctx), elements);
    }

    @Override
    public AstNode visitPairExpr(KobuParser.PairExprContext ctx) {
        boolean exprStatus = topLevelExpression;
        if (topLevelExpression) {
            context.getErrorScope().addError(new InvalidStatementError(getSourceCodeRef(ctx)));
            topLevelExpression = false;
        }

        Expr leftExpr = (Expr) visit(ctx.exprWrapper(0));
        Expr rightExpr = (Expr) visit(ctx.exprWrapper(1));

        topLevelExpression = exprStatus;
        return new PairConstructorCallExpr(getSourceCodeRef(ctx), leftExpr, rightExpr);
    }

    @Override
    public AstNode visitFunctionCallExpr(KobuParser.FunctionCallExprContext ctx) {
        boolean exprStatus = topLevelExpression;
        topLevelExpression = false;

        List<FunctionArgExpr> args = new ArrayList<>();
        if (ctx.exprSequence() != null) {
            for (KobuParser.ExprWrapperContext exprContext : ctx.exprSequence().exprWrapper()) {
                var exprNode = visit(exprContext);
                FunctionArgExpr argExpr = new FunctionArgExpr(getSourceCodeRef(exprContext),
                        (Expr) exprNode);
                args.add(argExpr);
            }
        }

        topLevelExpression = exprStatus;
        return new FunctionCallExpr(moduleScope, getSourceCodeRef(ctx),
                ctx.ID().getText(), args);
    }

    @Override
    public AstNode visitCastExpr(KobuParser.CastExprContext ctx) {
        boolean exprStatus = topLevelExpression;
        if (topLevelExpression) {
            context.getErrorScope().addError(new InvalidStatementError(getSourceCodeRef(ctx)));
            topLevelExpression = false;
        }

        Type type = null;
        Expr expr = null;
        if (ctx.typeName() != null) {
            type = (Type) visit(ctx.typeName());
        }
        if (ctx.expr() != null) {
            expr = (Expr) visit(ctx.expr());
        }

        topLevelExpression = exprStatus;
        return new CastExpr(getSourceCodeRef(ctx), type, expr);
    }

    @Override
    public AstNode visitArrayAccessExpr(KobuParser.ArrayAccessExprContext ctx) {
        boolean exprStatus = topLevelExpression;
        if (topLevelExpression) {
            context.getErrorScope().addError(new InvalidStatementError(getSourceCodeRef(ctx)));
            topLevelExpression = false;
        }

        var arrayRefNode = visit(ctx.expr());
        var indexNode = visit(ctx.arrayIndexExpr());

        topLevelExpression = exprStatus;
        return new ArrayAccessExpr(getSourceCodeRef(ctx),
                (Expr) arrayRefNode, (ArrayIndexExpr) indexNode);
    }

    @Override
    public AstNode visitArrayIndexSliceExpr(KobuParser.ArrayIndexSliceExprContext ctx) {
        Expr beginExpr = (Expr) visit(ctx.expr(0));
        Expr endExpr = (Expr) visit(ctx.expr(1));
        return new ArraySliceIndexExpr(getSourceCodeRef(ctx), beginExpr, endExpr);
    }

    @Override
    public AstNode visitArrayIndexSliceEndExpr(KobuParser.ArrayIndexSliceEndExprContext ctx) {
        Expr endExpr = (Expr) visit(ctx.expr());
        return new ArraySliceIndexExpr(getSourceCodeRef(ctx), null, endExpr);
    }

    @Override
    public AstNode visitArrayIndexSliceBeginExpr(KobuParser.ArrayIndexSliceBeginExprContext ctx) {
        Expr beginExpr = (Expr) visit(ctx.expr());
        return new ArraySliceIndexExpr(getSourceCodeRef(ctx), beginExpr, null);
    }

    @Override
    public AstNode visitArrayIndexItemExpr(KobuParser.ArrayIndexItemExprContext ctx) {
        Expr expr = (Expr) visit(ctx.exprWrapper());
        return new ArrayItemIndexExpr(getSourceCodeRef(ctx.exprWrapper()), expr);
    }

    @Override
    public AstNode visitNotExpr(KobuParser.NotExprContext ctx) {
        boolean exprStatus = topLevelExpression;
        if (topLevelExpression) {
            context.getErrorScope().addError(new InvalidStatementError(getSourceCodeRef(ctx)));
            topLevelExpression = false;
        }

        var exprNode = visit(ctx.expr());

        topLevelExpression = exprStatus;
        return new NotExpr(getSourceCodeRef(ctx), (Expr) exprNode);
    }

    @Override
    public AstNode visitNotErr(KobuParser.NotErrContext ctx) {
        boolean exprStatus = topLevelExpression;
        if (topLevelExpression) {
            context.getErrorScope().addError(new InvalidStatementError(getSourceCodeRef(ctx)));
            topLevelExpression = false;
        }

        var exprNode = new RefExpr(moduleScope, getSourceCodeRef(ctx.NOT()), "");
        context.getErrorScope().addError(new IdentifierExpectedError(getSourceCodeRef(ctx.NOT())));

        topLevelExpression = exprStatus;
        return new NotExpr(getSourceCodeRef(ctx), exprNode);
    }

    @Override
    public AstNode visitFactorExpr(KobuParser.FactorExprContext ctx) {
        boolean exprStatus = topLevelExpression;
        if (topLevelExpression) {
            context.getErrorScope().addError(new InvalidStatementError(getSourceCodeRef(ctx)));
            topLevelExpression = false;
        }

        var leftExprNode = visit(ctx.expr(0));
        var rightExprNode = visit(ctx.expr(1));
        Expr expr;
        if (ctx.STAR() != null) {
            expr = new MultExpr(getSourceCodeRef(ctx), (Expr) leftExprNode, (Expr) rightExprNode);
        } else if (ctx.DIV() != null) {
            expr = new DivExpr(getSourceCodeRef(ctx), (Expr) leftExprNode, (Expr) rightExprNode);
        } else {
            expr = new ModExpr(getSourceCodeRef(ctx), (Expr) leftExprNode, (Expr) rightExprNode);
        }

        topLevelExpression = exprStatus;
        return expr;
    }

    @Override
    public AstNode visitFactorErr(KobuParser.FactorErrContext ctx) {
        boolean exprStatus = topLevelExpression;
        if (topLevelExpression) {
            context.getErrorScope().addError(new InvalidStatementError(getSourceCodeRef(ctx)));
            topLevelExpression = false;
        }

        var leftExprNode = visit(ctx.expr());
        Expr expr;
        if (ctx.STAR() != null) {
            var rightExprNode = new RefExpr(moduleScope, getSourceCodeRef(ctx.STAR()), "");
            context.getErrorScope().addError(new IdentifierExpectedError(getSourceCodeRef(ctx.STAR())));
            expr = new MultExpr(getSourceCodeRef(ctx), (Expr) leftExprNode, rightExprNode);
        } else {
            var rightExprNode = new RefExpr(moduleScope, getSourceCodeRef(ctx.DIV()), "");
            context.getErrorScope().addError(new IdentifierExpectedError(getSourceCodeRef(ctx.DIV())));
            expr = new DivExpr(getSourceCodeRef(ctx), (Expr) leftExprNode, rightExprNode);
        }

        topLevelExpression = exprStatus;
        return expr;
    }

    @Override
    public AstNode visitAddSubExpr(KobuParser.AddSubExprContext ctx) {
        boolean exprStatus = topLevelExpression;
        if (topLevelExpression) {
            context.getErrorScope().addError(new InvalidStatementError(getSourceCodeRef(ctx)));
            topLevelExpression = false;
        }

        var leftExprNode = visit(ctx.expr(0));
        var rightExprNode = visit(ctx.expr(1));
        Expr expr;
        if (ctx.PLUS() != null) {
            expr = new AddExpr(getSourceCodeRef(ctx), (Expr) leftExprNode, (Expr) rightExprNode);
        } else {
            expr = new SubExpr(getSourceCodeRef(ctx), (Expr) leftExprNode, (Expr) rightExprNode);
        }

        topLevelExpression = exprStatus;
        return expr;
    }

    @Override
    public AstNode visitAddSubErr(KobuParser.AddSubErrContext ctx) {
        boolean exprStatus = topLevelExpression;
        if (topLevelExpression) {
            context.getErrorScope().addError(new InvalidStatementError(getSourceCodeRef(ctx)));
            topLevelExpression = false;
        }

        var leftExprNode = visit(ctx.expr());
        Expr expr;
        if (ctx.PLUS() != null) {
            var rightExprNode = new RefExpr(moduleScope, getSourceCodeRef(ctx.PLUS()), "");
            context.getErrorScope().addError(new IdentifierExpectedError(getSourceCodeRef(ctx.PLUS())));
            expr = new AddExpr(getSourceCodeRef(ctx), (Expr) leftExprNode, rightExprNode);
        } else {
            var rightExprNode = new RefExpr(moduleScope, getSourceCodeRef(ctx.MINUS()), "");
            context.getErrorScope().addError(new IdentifierExpectedError(getSourceCodeRef(ctx.MINUS())));
            expr = new SubExpr(getSourceCodeRef(ctx), (Expr) leftExprNode, rightExprNode);
        }

        topLevelExpression = exprStatus;
        return expr;
    }

    @Override
    public AstNode visitEqExpr(KobuParser.EqExprContext ctx) {
        boolean exprStatus = topLevelExpression;
        if (topLevelExpression) {
            context.getErrorScope().addError(new InvalidStatementError(getSourceCodeRef(ctx)));
            topLevelExpression = false;
        }

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

        topLevelExpression = exprStatus;
        return new EqExpr(getSourceCodeRef(ctx),
                (Expr) leftExprNode, operator, (Expr) rightExprNode);
    }

    @Override
    public AstNode visitEqErr(KobuParser.EqErrContext ctx) {
        boolean exprStatus = topLevelExpression;
        if (topLevelExpression) {
            context.getErrorScope().addError(new InvalidStatementError(getSourceCodeRef(ctx)));
            topLevelExpression = false;
        }

        var leftExprNode = visit(ctx.expr());
        EqOperatorEnum operator = null;

        Expr rightExprNode = null;
        if (ctx.EQUALS() != null) {
            rightExprNode = new RefExpr(moduleScope, getSourceCodeRef(ctx.EQUALS()), "");
            context.getErrorScope().addError(new IdentifierExpectedError(getSourceCodeRef(ctx.EQUALS())));
            operator = EqOperatorEnum.EQUALS;
        } else if (ctx.NOT_EQUALS() != null) {
            rightExprNode = new RefExpr(moduleScope, getSourceCodeRef(ctx.NOT_EQUALS()), "");
            context.getErrorScope().addError(new IdentifierExpectedError(getSourceCodeRef(ctx.NOT_EQUALS())));
            operator = EqOperatorEnum.NOT_EQUALS;
        } else if (ctx.LESS() != null) {
            rightExprNode = new RefExpr(moduleScope, getSourceCodeRef(ctx.LESS()), "");
            context.getErrorScope().addError(new IdentifierExpectedError(getSourceCodeRef(ctx.LESS())));
            operator = EqOperatorEnum.LESS;
        } else if (ctx.LESS_OR_EQUALS() != null) {
            rightExprNode = new RefExpr(moduleScope, getSourceCodeRef(ctx.LESS_OR_EQUALS()), "");
            context.getErrorScope().addError(new IdentifierExpectedError(getSourceCodeRef(ctx.LESS_OR_EQUALS())));
            operator = EqOperatorEnum.LESS_OR_EQUALS;
        } else if (ctx.GREATER() != null) {
            rightExprNode = new RefExpr(moduleScope, getSourceCodeRef(ctx.GREATER()), "");
            context.getErrorScope().addError(new IdentifierExpectedError(getSourceCodeRef(ctx.GREATER())));
            operator = EqOperatorEnum.GREATER;
        } else if (ctx.GREATER_OR_EQUALS() != null) {
            rightExprNode = new RefExpr(moduleScope, getSourceCodeRef(ctx.GREATER_OR_EQUALS()), "");
            context.getErrorScope().addError(new IdentifierExpectedError(getSourceCodeRef(ctx.GREATER_OR_EQUALS())));
            operator = EqOperatorEnum.GREATER_OR_EQUALS;
        }

        topLevelExpression = exprStatus;
        return new EqExpr(getSourceCodeRef(ctx),
                (Expr) leftExprNode, operator, rightExprNode);
    }

    @Override
    public AstNode visitLogicExpr(KobuParser.LogicExprContext ctx) {
        boolean exprStatus = topLevelExpression;
        if (topLevelExpression) {
            context.getErrorScope().addError(new InvalidStatementError(getSourceCodeRef(ctx)));
            topLevelExpression = false;
        }

        var leftExprNode = visit(ctx.expr(0));
        var rightExprNode = visit(ctx.expr(1));
        LogicOperatorEnum operator = null;

        if (ctx.AND() != null) {
            operator = LogicOperatorEnum.AND;
        } else if (ctx.OR() != null) {
            operator = LogicOperatorEnum.OR;
        }

        topLevelExpression = exprStatus;
        return new LogicExpr(getSourceCodeRef(ctx),
                (Expr) leftExprNode, operator, (Expr) rightExprNode);
    }

    @Override
    public AstNode visitLogicErr(KobuParser.LogicErrContext ctx) {
        boolean exprStatus = topLevelExpression;
        if (topLevelExpression) {
            context.getErrorScope().addError(new InvalidStatementError(getSourceCodeRef(ctx)));
            topLevelExpression = false;
        }

        var leftExprNode = visit(ctx.expr());
        LogicOperatorEnum operator = null;

        Expr rightExprNode = null;
        if (ctx.AND() != null) {
            rightExprNode = new RefExpr(moduleScope, getSourceCodeRef(ctx.AND()), "");
            context.getErrorScope().addError(new IdentifierExpectedError(getSourceCodeRef(ctx.AND())));
            operator = LogicOperatorEnum.AND;
        } else if (ctx.OR() != null) {
            rightExprNode = new RefExpr(moduleScope, getSourceCodeRef(ctx.OR()), "");
            context.getErrorScope().addError(new IdentifierExpectedError(getSourceCodeRef(ctx.OR())));
            operator = LogicOperatorEnum.OR;
        }

        topLevelExpression = exprStatus;
        return new LogicExpr(getSourceCodeRef(ctx),
                (Expr) leftExprNode, operator, rightExprNode);
    }

    @Override
    public AstNode visitFieldAccessExpr(KobuParser.FieldAccessExprContext ctx) {
        boolean exprStatus = topLevelExpression;
        topLevelExpression = false;

        var leftExprNode = visit(ctx.expr(0));
        var rightExprNode = visit(ctx.expr(1));

        if (exprStatus && !(rightExprNode instanceof FunctionCallExpr)) {
            context.getErrorScope().addError(new InvalidStatementError(getSourceCodeRef(ctx)));
        }
        topLevelExpression = exprStatus;

        return new FieldAccessExpr(getSourceCodeRef(ctx),
                (Expr) leftExprNode, (Expr) rightExprNode);
    }

    @Override
    public AstNode visitFieldAccessErr(KobuParser.FieldAccessErrContext ctx) {
        boolean exprStatus = topLevelExpression;
        topLevelExpression = false;

        var leftExprNode = visit(ctx.expr());
        var rightExprNode = new RefExpr(moduleScope, getSourceCodeRef(ctx.DOT()), "");
        context.getErrorScope().addError(new IdentifierExpectedError(getSourceCodeRef(ctx.DOT())));

        topLevelExpression = exprStatus;

        return new FieldAccessExpr(getSourceCodeRef(ctx),
                (Expr) leftExprNode, rightExprNode);
    }

    @Override
    public AstNode visitParenthesizedExpr(KobuParser.ParenthesizedExprContext ctx) {
        return visit(ctx.expr());
    }

    @Override
    public AstNode visitIdExpr(KobuParser.IdExprContext ctx) {
        if (topLevelExpression) {
            context.getErrorScope().addError(new InvalidStatementError(getSourceCodeRef(ctx)));
        }
        return new RefExpr(moduleScope, getSourceCodeRef(ctx.ID()), ctx.ID().getText());
    }

    @Override
    public AstNode visitStringExpr(KobuParser.StringExprContext ctx) {
        if (topLevelExpression) {
            context.getErrorScope().addError(new InvalidStatementError(getSourceCodeRef(ctx)));
        }

        StringBuilder str = new StringBuilder();
        String source = ctx.stringLiteral().getText();
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
                } else if (c == '"') {
                    str.append('"');
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
        return new StringValueExpr(getSourceCodeRef(ctx.stringLiteral()), str.toString());
    }

    @Override
    public AstNode visitNumberExpr(KobuParser.NumberExprContext ctx) {
        if (topLevelExpression) {
            context.getErrorScope().addError(new InvalidStatementError(getSourceCodeRef(ctx)));
        }

        if (moduleScope.getEvalMode() == EvalModeEnum.ANALYZER_SERVICE) {
            AutoCompletionSource autoCompletionSource = new AutoCompletionSource() {
                @Override
                public List<SymbolDescriptor> requestSuggestions(List<ModuleScope> externalModules) {
                    return EMPTY_LIST;
                }

                @Override
                public boolean hasOwnCompletionScope() {
                    return false;
                }
            };
            moduleScope.registerAutoCompletionSource(ctx.NUMBER().getSymbol().getStartIndex(), autoCompletionSource);
            moduleScope.registerAutoCompletionSource(ctx.NUMBER().getSymbol().getStopIndex() + 1, autoCompletionSource);
        }

        String numberText = ctx.NUMBER().getText();
        return NumberParser.getNumberValueExpr(numberText, getSourceCodeRef(ctx.NUMBER()));
    }

    @Override
    public AstNode visitTrueExpr(KobuParser.TrueExprContext ctx) {
        if (topLevelExpression) {
            context.getErrorScope().addError(new InvalidStatementError(getSourceCodeRef(ctx)));
        }
        return new BooleanValueExpr(getSourceCodeRef(ctx.TRUE()), true);
    }

    @Override
    public AstNode visitFalseExpr(KobuParser.FalseExprContext ctx) {
        if (topLevelExpression) {
            context.getErrorScope().addError(new InvalidStatementError(getSourceCodeRef(ctx)));
        }
        return new BooleanValueExpr(getSourceCodeRef(ctx.FALSE()), false);
    }

    @Override
    public AstNode visitNullExpr(KobuParser.NullExprContext ctx) {
        if (topLevelExpression) {
            context.getErrorScope().addError(new InvalidStatementError(getSourceCodeRef(ctx)));
        }
        return new NullValueExpr(getSourceCodeRef(ctx.NULL()));
    }

    @Override
    public AstNode visitQueryExpr(KobuParser.QueryExprContext ctx) {
        String bind = null;
        SourceCodeRef bindSourceCodeRef = null;
        if (ctx.queryExprAlias() != null) {
            bind = ctx.queryExprAlias().ID().getText();
            bindSourceCodeRef = getSourceCodeRef(ctx.queryExprAlias().ID());
        }
        QueryTypeClause queryTypeClause = new QueryTypeClause(moduleScope, getSourceCodeRef(ctx), bindSourceCodeRef, (Type) visit(ctx.type()),
                ctx.ANY() != null, bind);

        if (ctx.queryExprSegment() != null) {
            QueryPipeClause pipeClause = (QueryPipeClause) visit(ctx.queryExprSegment());
            queryTypeClause.setPipeClause(pipeClause);
        }

        return new Query(getSourceCodeRef(ctx), queryTypeClause);
    }

    @Override
    public AstNode visitQueryExprSegment(KobuParser.QueryExprSegmentContext ctx) {
        String alias = null;
        if (ctx.queryExprAlias() != null) {
            alias = ctx.queryExprAlias().ID().getText();
        }

        QueryPipeClause pipeClause = (QueryPipeClause) visit(ctx.queryPipeExpr());
        if (alias != null) {
            pipeClause.setBind(alias);
        }

        if (ctx.queryExprSegment() != null) {
            QueryPipeClause next = (QueryPipeClause) visit(ctx.queryExprSegment());
            pipeClause.setNext(next);
        }
        return pipeClause;
    }

    @Override
    public AstNode visitQueryFieldExpr(KobuParser.QueryFieldExprContext ctx) {

        var field = new QueryFieldClause(getSourceCodeRef(ctx), ctx.ID().getText());
        if (ctx.queryExprArraySelect() != null) {
            field.setBind(null);
            QueryArrayItemClause arrayItemClause = (QueryArrayItemClause) visit(ctx.queryExprArraySelect());
            field.setArrayItemClause(arrayItemClause);
        }

        return field;
    }

    @Override
    public AstNode visitQueryFunctionCallExpr(KobuParser.QueryFunctionCallExprContext ctx) {

        var functionCallExpr = (FunctionCallExpr) visit(ctx.functionCallExpr());
        var functionCallClause = new QueryFunctionCallClause(getSourceCodeRef(ctx), functionCallExpr);

        if (ctx.queryExprArraySelect() != null) {
            QueryArrayItemClause arrayItemClause = (QueryArrayItemClause) visit(ctx.queryExprArraySelect());
            functionCallClause.setArrayItemClause(arrayItemClause);
        }

        return functionCallClause;
    }

    @Override
    public AstNode visitQueryExprArrayItemIndex(KobuParser.QueryExprArrayItemIndexContext ctx) {
        var index = (ArrayIndexExpr) visit(ctx.arrayIndexExpr());
        return new QueryArrayIndexClause(getSourceCodeRef(ctx), index);
    }

    @Override
    public AstNode visitQueryExprArrayItemAll(KobuParser.QueryExprArrayItemAllContext ctx) {
        return new QueryArrayAllClause(getSourceCodeRef(ctx));
    }

    @Override
    public AstNode visitJoinExpr(KobuParser.JoinExprContext ctx) {
        Query query = (Query) visit(ctx.queryExpr());
        query.getTypeClause().setJoinMode(true);

        Expr ofExpr = null;
        if (ctx.joinOfExpr() != null) {
            ofExpr = (Expr) visit(ctx.joinOfExpr().expr());
        }

        return new QueryJoin(getSourceCodeRef(ctx), query.getTypeClause(), ofExpr);
    }

    @Override
    public AstNode visitArrayType(KobuParser.ArrayTypeContext ctx) {
        return new ArrayType((Type) visit(ctx.type()));
    }

    @Override
    public AstNode visitTypeName(KobuParser.TypeNameContext ctx) {
        if (ctx.ID() == null || ctx.ID().isEmpty()) {
            return UnknownType.INSTANCE;
        }
        if (ctx.ID().size() == 1) {
            var typeName = ctx.ID(0).getText();
            var symbol = moduleScope.resolve(typeName);

            if (moduleLoader.getEvalMode() == EvalModeEnum.ANALYZER_SERVICE) {
                moduleScope.registerAutoCompletionSource(ctx.ID(0).getSymbol().getStartIndex(), new AutoCompletionSource() {
                    @Override
                    public List<SymbolDescriptor> requestSuggestions(List<ModuleScope> externalModules) {
                        List<SymbolDescriptor> symbols = new ArrayList<>();
                        symbols.addAll(getGlobalSymbols(moduleScope, SymbolTypeEnum.TYPE, SymbolTypeEnum.MODULE_REF));
                        symbols.addAll(getExternalSymbols(moduleScope, externalModules, SymbolTypeEnum.TYPE));
                        if (functionReturnType) {
                            symbols.add(SymbolDescriptorUtils.voidKeyword);
                        }
                        return symbols;
                    }

                    @Override
                    public boolean hasOwnCompletionScope() {
                        return false;
                    }
                });
            }

            if (!(symbol instanceof Type) && !(symbol instanceof RuleSymbol)) {
                context.getErrorScope().addError(new UndefinedTypeError(getSourceCodeRef(ctx.ID(0)),
                        typeName, scopeEndOffset));
                return UnknownType.INSTANCE;
            }

            return (AstNode) symbol;
        } else {
            var moduleAlias = ctx.ID(0).getText();
            var typeName = ctx.ID(1).getText();

            if (moduleLoader.getEvalMode() == EvalModeEnum.ANALYZER_SERVICE) {
                moduleScope.registerAutoCompletionSource(ctx.ID(1).getSymbol().getStartIndex(), new AutoCompletionSource() {
                    @Override
                    public List<SymbolDescriptor> requestSuggestions(List<ModuleScope> externalModules) {
                        var symbol = moduleScope.resolve(moduleAlias);
                        if (symbol instanceof ModuleRefSymbol) {
                            var otherModule = ((ModuleRefSymbol)symbol).getModuleScopeRef();
                            return getTypeSymbols(otherModule, moduleAlias);
                        }
                        return EMPTY_LIST;
                    }

                    @Override
                    public boolean hasOwnCompletionScope() {
                        return false;
                    }
                });
            }

            ModuleRefSymbol moduleRefSymbol = (ModuleRefSymbol) moduleScope.resolveLocal(moduleAlias);

            if (moduleRefSymbol == null) {
                context.getErrorScope().addError(new UndefinedTypeError(getSourceCodeRef(ctx),
                        typeName, scopeEndOffset));
                return UnknownType.INSTANCE;
            }

            var symbol = moduleRefSymbol.getModuleScopeRef().resolve(typeName);

            if (!(symbol instanceof Type) && !(symbol instanceof RuleSymbol)) {
                context.getErrorScope().addError(new UndefinedTypeError(getSourceCodeRef(ctx),
                        moduleAlias + "." + typeName, scopeEndOffset));
                return UnknownType.INSTANCE;
            }

            return (Type) symbol;
        }
    }

    @Override
    public AstNode visitPairType(KobuParser.PairTypeContext ctx) {
        return new PairType((Type) visit(ctx.type(0)), (Type) visit(ctx.type(1)));
    }

    public ModuleScope getModuleScope() {
        return moduleScope;
    }

    private List<FunctionParameter> getFunctionParameters(KobuParser.FunctionDeclParamContext paramCtx) {
        List<FunctionParameter> parameters;
        parameters = new ArrayList<>();

        while (paramCtx != null) {
            var type = paramCtx.type() != null ? (Type) visit(paramCtx.type()) : UnknownType.INSTANCE;
            FunctionParameter param = new FunctionParameter(getSourceCodeRef(paramCtx.ID()), paramCtx.ID().getText(), type,
                    paramCtx.QM() != null);
            parameters.add(param);

            paramCtx = paramCtx.functionDeclParam();
        }
        return parameters;
    }

}
