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
import dev.kobu.interpreter.ast.eval.expr.value.number.NumberValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.number.NumberValueFactory;
import dev.kobu.interpreter.ast.eval.statement.*;
import dev.kobu.interpreter.ast.query.*;
import dev.kobu.interpreter.ast.symbol.*;
import dev.kobu.interpreter.ast.symbol.array.ArrayType;
import dev.kobu.interpreter.ast.symbol.array.ArrayTypeFactory;
import dev.kobu.interpreter.ast.symbol.function.*;
import dev.kobu.interpreter.ast.symbol.generics.TypeAlias;
import dev.kobu.interpreter.ast.symbol.generics.TypeArgs;
import dev.kobu.interpreter.ast.symbol.generics.TypeParameter;
import dev.kobu.interpreter.ast.symbol.generics.TypeParameterContext;
import dev.kobu.interpreter.ast.symbol.tuple.TupleType;
import dev.kobu.interpreter.ast.symbol.tuple.TupleTypeElement;
import dev.kobu.interpreter.ast.symbol.tuple.TupleTypeFactory;
import dev.kobu.interpreter.ast.template.TemplateContentStatement;
import dev.kobu.interpreter.ast.template.TemplateStatement;
import dev.kobu.interpreter.ast.template.TemplateStaticContentStatement;
import dev.kobu.interpreter.ast.utils.StringFunctions;
import dev.kobu.interpreter.ast.utils.SymbolDescriptorUtils;
import dev.kobu.interpreter.error.analyzer.*;
import dev.kobu.interpreter.module.ModuleLoader;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.List;

public class EvalTreeParserVisitor extends KobuParserVisitor<AstNode> {

    private final AnalyzerContext context;

    private boolean topLevelExpression = true;

    private boolean functionReturnType = false;

    private int scopeEndOffset = 0;

    private boolean ruleTypeScope = false;

    private TypeParameterContext typeParameterContext;

    private boolean allowTypeArgs = false;

    public EvalTreeParserVisitor(ModuleLoader moduleLoader, ModuleScope moduleScope, AnalyzerContext context) {
        super(moduleLoader);
        this.context = context;
        this.moduleScope = moduleScope;
    }

    @Override
    public AstNode visitModule(KobuParser.ModuleContext ctx) {
        if (ctx != null && ctx.getStop() != null) {
            scopeEndOffset = ctx.getStop().getStopIndex() + 1;
        }
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
    public AstNode visitInvalidType(KobuParser.InvalidTypeContext ctx) {
        context.getErrorScope().addError(new InvalidTypeDefinitionError(getSourceCodeRef(ctx.TYPE())));
        if (moduleLoader.getEvalMode() == EvalModeEnum.ANALYZER_SERVICE && ctx.elem != null) {
            //add a reference element for auto-completion service
            moduleScope.registerAutoCompletionSource(ctx.elem.getStartIndex(), new AutoCompletionSource() {
                @Override
                public List<SymbolDescriptor> requestSuggestions(List<ModuleScope> externalModules) {
                    return SymbolDescriptorUtils.getTypeKeywords();
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
    public AstNode visitTypetemplate(KobuParser.TypetemplateContext ctx) {
        if (ctx.ID() == null) {
            return null;
        }

        Symbol symbol = moduleScope.resolve(ctx.ID().getText());
        if (!(symbol instanceof TemplateTypeSymbol)) {
            return null;
        }

        TemplateTypeSymbol templateType = (TemplateTypeSymbol) symbol;

        if (ctx.LCB() != null) {
            context.getErrorScope().addError(new InvalidTemplateTypeError(getSourceCodeRef(ctx.ID())));
        }

        if (ctx.templateInheritance() != null && ctx.templateInheritance().typeName() != null) {
            var typeNameExpr = ctx.templateInheritance().typeName();
            Type superType = (Type) visit(typeNameExpr);
            if (!(superType instanceof TemplateTypeSymbol)) {
                if (!(superType instanceof AnyTemplateTypeSymbol)) {
                    context.getErrorScope().addError(new TemplateInvalidSuperTypeError(getSourceCodeRef(typeNameExpr),
                            templateType, typeNameExpr.getText()));
                }
            } else {
                templateType.setSuperType(new TemplateSuperType(getSourceCodeRef(typeNameExpr),
                        (TemplateTypeSymbol) superType));
            }
        }

        return null;
    }

    @Override
    public AstNode visitTyperecord(KobuParser.TyperecordContext ctx) {
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

        if (ctx.typeParameters() != null) {
            typeParameterContext = new TypeParameterContext();
            List<TypeParameter> typeParameters = new ArrayList<>();
            var typeParamCtx = ctx.typeParameters().typeParameter();
            while (typeParamCtx != null) {
                TypeParameter typeParameter = new TypeParameter(getSourceCodeRef(typeParamCtx.ID()), typeParamCtx.ID().getText());
                typeParameters.add(typeParameter);
                if (!typeParameterContext.set(typeParameter)) {
                    context.getErrorScope().addError(new DuplicatedTypeParamError(typeParameter.getSourceCodeRef(), typeParameter));
                }
                typeParamCtx = typeParamCtx.typeParameter();
            }
            recordType.setTypeParameters(typeParameters);
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
                        recordType.setStarAttribute(context, new RecordTypeStarAttribute(moduleScope, sourceCodeRef, type, recordType));
                    }
                } else {
                    context.getErrorScope().addError(new MissingTypeError(getSourceCodeRef(attrCtx.ID())));
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
                recordType.setSuperType(new RecordSuperType(getSourceCodeRef(typeNameExpr),
                        (RecordTypeSymbol) superType));
            }
        }

        typeParameterContext = null;

        return null;
    }

    @Override
    public AstNode visitGlobalConstDecl(KobuParser.GlobalConstDeclContext ctx) {
        if (ctx.varDeclBody() != null && ctx.varDeclBody().ID() != null) {

            String varName = ctx.varDeclBody().ID().getText();
            Type type = null;
            Expr expr = null;

            topLevelExpression = false;

            if (ctx.varDeclBody().type() != null) {
                type = (Type) visit(ctx.varDeclBody().type());
            }
            if (ctx.varDeclBody().expr() != null) {
                expr = (Expr) visit(ctx.varDeclBody().expr());
            }

            topLevelExpression = true;

            ConstantSymbol constSymbol = new ConstantSymbol(moduleScope, getSourceCodeRef(ctx.varDeclBody().ID()),
                    varName, expr, type, ctx.PRIVATE() != null);
            moduleScope.define(context, constSymbol);

        }
        return null;
    }

    @Override
    public AstNode visitConstDecl(KobuParser.ConstDeclContext ctx) {
        if (ctx.varDeclBody() != null && ctx.varDeclBody().ID() != null) {

            topLevelExpression = false;

            Type type = null;
            if (ctx.varDeclBody().type() != null) {
                type = (Type) visit(ctx.varDeclBody().type());
            }
            Expr valueExpr = null;
            if (ctx.varDeclBody().expr() != null) {
                valueExpr = (Expr) visit(ctx.varDeclBody().expr());
            }

            ConstDeclExpr expr = new ConstDeclExpr(
                    new ConstantSymbol(moduleScope, getSourceCodeRef(ctx.varDeclBody().ID()),
                            ctx.varDeclBody().ID().getText(), valueExpr, type, false));

            topLevelExpression = true;
            return expr;

        }
        return null;
    }

    @Override
    public AstNode visitFunctionDecl(KobuParser.FunctionDeclContext ctx) {
        if (ctx.ID() == null) {
            return null;
        }

        Symbol symbol = moduleScope.resolve(ctx.ID().getText());
        if (!(symbol instanceof FunctionSymbol)) {
            return null;
        }

        var function = (FunctionSymbol) symbol;

        if (ctx.RCB() != null) {
            scopeEndOffset = ctx.RCB().getSymbol().getStopIndex() + 1;
        }

        if (ctx.typeParameters() != null) {
            typeParameterContext = new TypeParameterContext();
            List<TypeParameter> typeParameters = new ArrayList<>();
            var typeParamCtx = ctx.typeParameters().typeParameter();
            while (typeParamCtx != null) {
                SourceCodeRef sourceCodeRef = getSourceCodeRef(typeParamCtx.ID());
                TypeParameter typeParameter = new TypeParameter(sourceCodeRef, typeParamCtx.ID().getText());
                typeParameters.add(typeParameter);
                if (!typeParameterContext.set(typeParameter)) {
                    context.getErrorScope().addError(new DuplicatedTypeParamError(sourceCodeRef, typeParameter));
                }
                typeParamCtx = typeParamCtx.typeParameter();
            }
            function.setTypeParameters(typeParameters);
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
        function.setBlock(exprList);
        function.buildType();

        typeParameterContext = null;

        return null;
    }

    @Override
    public AstNode visitNativeDecl(KobuParser.NativeDeclContext ctx) {
        var function = (NativeFunctionSymbol) moduleScope.resolve(ctx.ID().getText());
        if (function == null) {
            return null;
        }

        if (ctx.typeParameters() != null) {
            typeParameterContext = new TypeParameterContext();
            List<TypeParameter> typeParameters = new ArrayList<>();
            var typeParamCtx = ctx.typeParameters().typeParameter();
            while (typeParamCtx != null) {
                SourceCodeRef sourceCodeRef = getSourceCodeRef(typeParamCtx.ID());
                TypeParameter typeParameter = new TypeParameter(sourceCodeRef, typeParamCtx.ID().getText());
                typeParameters.add(typeParameter);
                if (!typeParameterContext.set(typeParameter)) {
                    context.getErrorScope().addError(new DuplicatedTypeParamError(sourceCodeRef, typeParameter));
                }
                typeParamCtx = typeParamCtx.typeParameter();
            }
            function.setTypeParameters(typeParameters);
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
        function.buildType();

        typeParameterContext = null;

        return null;
    }

    @Override
    public AstNode visitSingleArgAnonymousFunction(KobuParser.SingleArgAnonymousFunctionContext ctx) {
        SourceCodeRef sourceCodeRef = getSourceCodeRef(ctx.ID());
        SourceCodeRef closeBlockSourceCodeRef = null;
        if (ctx.anonymousFunctionBody() != null && ctx.anonymousFunctionBody().RCB() != null) {
            closeBlockSourceCodeRef = getSourceCodeRef(ctx.anonymousFunctionBody().RCB());
        }

        List<FunctionParameter> params = new ArrayList<>();
        params.add(new FunctionParameter(getSourceCodeRef(ctx.ID()), ctx.ID().getText(), null, false));

        List<Evaluable> block = new ArrayList<>();
        if (ctx.anonymousFunctionBody() != null) {
            if (ctx.anonymousFunctionBody().expr() != null) {
                Expr expr = (Expr) visit(ctx.anonymousFunctionBody().expr());
                block.add(new ReturnStatement(expr.getSourceCodeRef(), expr, true));
            } else if (ctx.anonymousFunctionBody().execStat() != null) {
                for (KobuParser.ExecStatContext execStatContext : ctx.anonymousFunctionBody().execStat()) {
                    block.add((Evaluable) visit(execStatContext));
                }
            }
        }

        return new AnonymousFunctionDefinitionExpr(sourceCodeRef, closeBlockSourceCodeRef,
                moduleScope, params, block);
    }

    @Override
    public AstNode visitFullArgsAnonymousFunction(KobuParser.FullArgsAnonymousFunctionContext ctx) {
        SourceCodeRef sourceCodeRef = getSourceCodeRef(ctx.anonymousFunctionHeader());
        SourceCodeRef closeBlockSourceCodeRef = null;
        if (ctx.anonymousFunctionBody() != null && ctx.anonymousFunctionBody().RCB() != null) {
            closeBlockSourceCodeRef = getSourceCodeRef(ctx.anonymousFunctionBody().RCB());
        }

        List<FunctionParameter> params = new ArrayList<>();
        if (ctx.anonymousFunctionHeader() != null) {
            var paramCtx = ctx.anonymousFunctionHeader().anonymousFunctionParams();
            while (paramCtx != null) {
                Type type = null;
                if (paramCtx.type() != null) {
                    type = (Type) visit(paramCtx.type());
                }
                var param = new FunctionParameter(getSourceCodeRef(paramCtx), paramCtx.ID().getText(),
                        type, paramCtx.QM() != null);
                params.add(param);
                paramCtx = paramCtx.anonymousFunctionParams();
            }
        }

        List<Evaluable> block = new ArrayList<>();
        if (ctx.anonymousFunctionBody() != null) {
            if (ctx.anonymousFunctionBody().expr() != null) {
                Evaluable expr = (Evaluable) visit(ctx.anonymousFunctionBody().expr());
                block.add(new ReturnStatement(expr.getSourceCodeRef(), (Expr) expr, true));
            } else if (ctx.anonymousFunctionBody().execStat() != null) {
                for (KobuParser.ExecStatContext execStatContext : ctx.anonymousFunctionBody().execStat()) {
                    block.add((Evaluable) visit(execStatContext));
                }
            }
        }

        return new AnonymousFunctionDefinitionExpr(sourceCodeRef, closeBlockSourceCodeRef,
                moduleScope, params, block);
    }

    @Override
    public AstNode visitDeftemplate(KobuParser.DeftemplateContext ctx) {
        if (ctx.ID() == null) {
            return null;
        }

        Symbol symbol = moduleScope.resolve(ctx.ID().getText());
        if (!(symbol instanceof RuleSymbol)) {
            return null;
        }

        var rule = (RuleSymbol) symbol;

        if (ctx.ruleExtends() != null && ctx.ruleExtends().typeName() != null) {
            ruleTypeScope = true;
            var parentRule = visit(ctx.ruleExtends().typeName());
            ruleTypeScope = false;
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

            for (KobuParser.ExtractExprContext extractExprContext : ctx.extractExpr()) {
                query.addExtractor((QueryExtractor) visit(extractExprContext));
            }

            for (KobuParser.JoinExprContext joinExprContext : ctx.joinExpr()) {
                QueryJoin queryJoin = (QueryJoin) visit(joinExprContext);
                queryJoin.setRuleSymbol(rule);
                query.addJoin(queryJoin);
            }

            if (ctx.expr() != null) {
                query.setWhenExpr((Expr) visit(ctx.expr()));
            }
            topLevelExpression = true;

            rule.setQuery(query);

            List<Evaluable> exprList = new ArrayList<>();
            if (ctx.template() != null) {
                TemplateStatement templateStat = (TemplateStatement) visit(ctx.template());
                if (ctx.templateTargetType() != null && ctx.templateTargetType().typeName() != null) {
                    Type type = (Type) visit(ctx.templateTargetType().typeName());
                    templateStat.setTargetType(type);
                }

                exprList.add(templateStat);
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
        return new TemplateContentStatement(getSourceCodeRef(ctx.expr()), expr, ctx.TEMPLATE_SHIFT_EXPR_BEGIN() != null);
    }

    @Override
    public AstNode visitDefrule(KobuParser.DefruleContext ctx) {
        if (ctx.ID() == null) {
            return null;
        }

        Symbol symbol = moduleScope.resolve(ctx.ID().getText());
        if (!(symbol instanceof RuleSymbol)) {
            return null;
        }

        var rule = (RuleSymbol) symbol;

        if (ctx.ruleExtends() != null && ctx.ruleExtends().typeName() != null) {
            ruleTypeScope = true;
            var parentRule = visit(ctx.ruleExtends().typeName());
            ruleTypeScope = false;
            if (parentRule instanceof RuleSymbol) {
                rule.setParentRuleSymbol((RuleSymbol) parentRule);
            } else {
                context.getErrorScope().addError(new InvalidParentRuleError(getSourceCodeRef(ctx.ruleExtends().typeName()),
                        ctx.ruleExtends().getText()));
            }
        }

        if (ctx.RCB() != null) {
            scopeEndOffset = ctx.RCB().getSymbol().getStopIndex() + 1;
        }

        if (ctx.queryExpr() != null) {
            topLevelExpression = false;
            var query = (Query) visit(ctx.queryExpr());

            for (KobuParser.ExtractExprContext extractExprContext : ctx.extractExpr()) {
                query.addExtractor((QueryExtractor) visit(extractExprContext));
            }

            for (KobuParser.JoinExprContext joinExprContext : ctx.joinExpr()) {
                QueryJoin queryJoin = (QueryJoin) visit(joinExprContext);
                queryJoin.setRuleSymbol(rule);
                query.addJoin(queryJoin);
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
    public AstNode visitDefaction(KobuParser.DefactionContext ctx) {
        if (ctx.ID() == null) {
            return null;
        }

        Symbol symbol = moduleScope.resolve(ctx.ID().getText());
        if (!(symbol instanceof RuleSymbol)) {
            return null;
        }

        var rule = (RuleSymbol) symbol;

        if (ctx.ruleExtends() != null && ctx.ruleExtends().typeName() != null) {
            ruleTypeScope = true;
            var parentRule = visit(ctx.ruleExtends().typeName());
            ruleTypeScope = false;
            if (parentRule instanceof RuleSymbol) {
                rule.setParentRuleSymbol((RuleSymbol) parentRule);
            } else {
                context.getErrorScope().addError(new InvalidParentRuleError(getSourceCodeRef(ctx.ruleExtends().typeName()),
                        ctx.ruleExtends().getText()));
            }
        }

        if (ctx.RCB() != null) {
            scopeEndOffset = ctx.RCB().getSymbol().getStopIndex() + 1;
        }

        if (ctx.queryExpr() != null) {
            topLevelExpression = false;
            var query = (Query) visit(ctx.queryExpr());

            for (KobuParser.ExtractExprContext extractExprContext : ctx.extractExpr()) {
                query.addExtractor((QueryExtractor) visit(extractExprContext));
            }

            for (KobuParser.JoinExprContext joinExprContext : ctx.joinExpr()) {
                QueryJoin queryJoin = (QueryJoin) visit(joinExprContext);
                queryJoin.setRuleSymbol(rule);
                query.addJoin(queryJoin);
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
    public AstNode visitInvalidKeyword(KobuParser.InvalidKeywordContext ctx) {
        context.getErrorScope().addError(new InvalidKeywordError(getSourceCodeRef(ctx), ctx.keyword.getText()));
        return null;
    }

    @Override
    public AstNode visitFunctionReturnStat(KobuParser.FunctionReturnStatContext ctx) {
        Expr expr = null;
        if (ctx.expr() != null) {
            topLevelExpression = false;
            expr = (Expr) visit(ctx.expr());
            topLevelExpression = true;
        }
        return new ReturnStatement(getSourceCodeRef(ctx), expr, false);
    }

    @Override
    public AstNode visitVarDecl(KobuParser.VarDeclContext ctx) {
        return ctx.varDeclBody() != null ? visit(ctx.varDeclBody()) : null;
    }

    @Override
    public AstNode visitVarDeclBody(KobuParser.VarDeclBodyContext ctx) {
        topLevelExpression = false;

        if (ctx.ID() == null) {
            topLevelExpression = true;
            return null;
        }

        Type type = null;
        if (ctx.type() != null) {
            type = (Type) visit(ctx.type());
        }
        VarDeclExpr expr = new VarDeclExpr(
                new VariableSymbol(moduleScope, getSourceCodeRef(ctx.ID()), ctx.ID().getText(), type));
        if (ctx.expr() != null) {
            var exprNode = visit(ctx.expr());
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
    public AstNode visitPreIncDecExpr(KobuParser.PreIncDecExprContext ctx) {
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
    public AstNode visitPostIncDecExpr(KobuParser.PostIncDecExprContext ctx) {
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
    public AstNode visitEnhancedForStat(KobuParser.EnhancedForStatContext ctx) {
        if (ctx.expr() != null) {
            topLevelExpression = false;
            Expr arrayExpr = (Expr) visit(ctx.expr());
            topLevelExpression = true;
            Type type = null;
            if (ctx.type() != null) {
                type = (Type) visit(ctx.type());
            }
            VariableSymbol varSymbol = new VariableSymbol(moduleScope, getSourceCodeRef(ctx.ID()),
                    ctx.ID().getText(), type);

            List<Evaluable> block = new ArrayList<>();
            if (ctx.execStat() != null) {
                for (KobuParser.ExecStatContext execStatContext : ctx.execStat()) {
                    var statNode = (Evaluable) visit(execStatContext);
                    if (statNode != null) {
                        block.add(statNode);
                    }
                }
            }

            return new EnhancedForStatement(getSourceCodeRef(ctx), varSymbol, arrayExpr, block);
        }

        return null;
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
        RecordConstructorCallExpr recordConstructor = new RecordConstructorCallExpr(sourceCodeRef, moduleScope, type);

        KobuParser.RecordFieldContext fieldCtx = ctx.recordField();
        while (fieldCtx != null) {
            if (fieldCtx.expr() == null) {
                context.getErrorScope().addError(new MissingExpressionError(getSourceCodeRef(fieldCtx.ID())));
                recordConstructor.addField(new RecordFieldExpr(getSourceCodeRef(fieldCtx.ID()), type, fieldCtx.ID().getText(),
                        null));
            } else {
                var exprNode = visit(fieldCtx.expr());
                recordConstructor.addField(new RecordFieldExpr(getSourceCodeRef(fieldCtx.ID()), type, fieldCtx.ID().getText(),
                        (Expr) exprNode));
            }
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
            for (KobuParser.ExprContext exprContext : ctx.exprSequence().expr()) {
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
    public AstNode visitTupleExpr(KobuParser.TupleExprContext ctx) {
        boolean exprStatus = topLevelExpression;
        if (topLevelExpression) {
            context.getErrorScope().addError(new InvalidStatementError(getSourceCodeRef(ctx)));
            topLevelExpression = false;
        }

        List<Expr> exprList = new ArrayList<>();
        if (ctx.expr() != null) {
            exprList.add((Expr) visit(ctx.expr()));
        }
        if (ctx.exprSequence() != null && ctx.exprSequence().expr() != null) {
            for (KobuParser.ExprContext exprWrapperContext : ctx.exprSequence().expr()) {
                exprList.add((Expr) visit(exprWrapperContext));
            }
        }

        topLevelExpression = exprStatus;
        return new TupleConstructorCallExpr(getSourceCodeRef(ctx), exprList);
    }

    @Override
    public AstNode visitFunctionCallExpr(KobuParser.FunctionCallExprContext ctx) {
        boolean exprStatus = topLevelExpression;
        topLevelExpression = false;

        List<FunctionArgExpr> args = new ArrayList<>();
        if (ctx.exprSequence() != null) {
            for (KobuParser.ExprContext exprContext : ctx.exprSequence().expr()) {
                var exprNode = visit(exprContext);
                FunctionArgExpr argExpr = new FunctionArgExpr(getSourceCodeRef(exprContext),
                        (Expr) exprNode);
                args.add(argExpr);
            }
        }

        this.allowTypeArgs = true;
        Expr refExpr = (Expr) visit(ctx.expr());
        this.allowTypeArgs = false;
        topLevelExpression = exprStatus;
        FunctionCallExpr functionCallExpr = new FunctionCallExpr(getSourceCodeRef(ctx),
                moduleScope, refExpr, args);

        if (refExpr instanceof RefExpr) {
            functionCallExpr.setTypeArgs(((RefExpr) refExpr).getTypeArgs());
        } else if (refExpr instanceof FieldAccessExpr) {
            if (((FieldAccessExpr) refExpr).getRightExpr() instanceof RefExpr) {
                functionCallExpr.setTypeArgs(((RefExpr) ((FieldAccessExpr) refExpr).getRightExpr()).getTypeArgs());
            }
        }
        return functionCallExpr;
    }

    @Override
    public AstNode visitInstanceOfExpr(KobuParser.InstanceOfExprContext ctx) {
        boolean exprStatus = topLevelExpression;
        if (topLevelExpression) {
            context.getErrorScope().addError(new InvalidStatementError(getSourceCodeRef(ctx)));
            topLevelExpression = false;
        }

        Type type = null;
        Expr expr = null;
        if (ctx.type() != null) {
            type = (Type) visit(ctx.type());
        }
        if (ctx.expr() != null) {
            expr = (Expr) visit(ctx.expr());
        }

        topLevelExpression = exprStatus;
        return new InstanceOfExpr(getSourceCodeRef(ctx), type, expr);
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
        if (ctx.type() != null) {
            type = (Type) visit(ctx.type());
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
        Expr expr = (Expr) visit(ctx.expr());
        return new ArrayItemIndexExpr(getSourceCodeRef(ctx.expr()), expr);
    }

    @Override
    public AstNode visitNotExpr(KobuParser.NotExprContext ctx) {
        boolean exprStatus = topLevelExpression;
        if (topLevelExpression) {
            context.getErrorScope().addError(new InvalidStatementError(getSourceCodeRef(ctx)));
            topLevelExpression = false;
        }

        if (ctx.expr() == null) {
            context.getErrorScope().addError(new MissingExpressionError(getSourceCodeRef(ctx)));
            return null;
        }

        var exprNode = visit(ctx.expr());

        topLevelExpression = exprStatus;
        return new NotExpr(getSourceCodeRef(ctx), (Expr) exprNode);
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
    public AstNode visitFieldAccessExpr(KobuParser.FieldAccessExprContext ctx) {
        boolean exprStatus = topLevelExpression;
        topLevelExpression = false;

        boolean typeArgsStatus = allowTypeArgs;
        allowTypeArgs = false;
        AstNode leftExprNode = visit(ctx.expr());
        allowTypeArgs = typeArgsStatus;
        var exprRight = ctx.ID();
        TypeArgs typeArgs = null;
        if (ctx.typeArgs() != null) {
            if (!allowTypeArgs) {
                context.getErrorScope().addError(new InvalidStatementError(getSourceCodeRef(ctx)));
            }

            List<Type> types = new ArrayList<>();
            var typeArgCtx = ctx.typeArgs().typeArg();
            while (typeArgCtx != null) {
                Type type = (Type) visit(typeArgCtx.type());
                types.add(type);
                typeArgCtx = typeArgCtx.typeArg();
            }
            typeArgs = new TypeArgs(getSourceCodeRef(ctx.typeArgs()), types);
        }
        AstNode rightExprNode = exprRight != null ? new RefExpr(moduleScope, getSourceCodeRef(ctx.ID()), ctx.ID().getText(), typeArgs) : null;

        if (exprStatus) {
            context.getErrorScope().addError(new InvalidStatementError(getSourceCodeRef(ctx)));
        }
        topLevelExpression = exprStatus;

        if (rightExprNode == null) {
            SourceCodeRef sourceCodeRef = getSourceCodeRef(ctx.DOT());
            rightExprNode = new RefExpr(moduleScope, sourceCodeRef, "", null);
        }

        return new FieldAccessExpr(getSourceCodeRef(ctx),
                (Expr) leftExprNode, (Expr) rightExprNode);
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

        TypeArgs typeArgs = null;
        if (ctx.typeArgs() != null) {
            if (!allowTypeArgs) {
                context.getErrorScope().addError(new InvalidStatementError(getSourceCodeRef(ctx)));
            }

            List<Type> types = new ArrayList<>();
            var typeArgCtx = ctx.typeArgs().typeArg();
            while (typeArgCtx != null) {
                Type type = (Type) visit(typeArgCtx.type());
                types.add(type);
                typeArgCtx = typeArgCtx.typeArg();
            }
            typeArgs = new TypeArgs(getSourceCodeRef(ctx.typeArgs()), types);
        }
        return new RefExpr(moduleScope, getSourceCodeRef(ctx.ID()), ctx.ID().getText(), typeArgs);
    }

    @Override
    public AstNode visitStringExpr(KobuParser.StringExprContext ctx) {
        if (topLevelExpression) {
            context.getErrorScope().addError(new InvalidStatementError(getSourceCodeRef(ctx)));
        }

        if (ctx.stringLiteral().stringLiteralContent() != null) {
            var badEscapeList = ctx.stringLiteral().stringLiteralContent().STRING_BAD_ESC();
            if (badEscapeList != null && !badEscapeList.isEmpty()) {
                for (TerminalNode badEscape : badEscapeList) {
                    context.getErrorScope().addError(new IllegalEscapeCharacterError(getSourceCodeRef(badEscape)));
                }
                return new StringValueExpr("");
            }
        }

        String source = ctx.stringLiteral().getText();
        String str = StringFunctions.parseLiteralString(source);

        var sourceCodeRef = getSourceCodeRef(ctx.stringLiteral());
        if (moduleScope.getEvalMode() == EvalModeEnum.ANALYZER_SERVICE) {
            moduleScope.registerAutoCompletionSource(sourceCodeRef.getEndOffset(), new AutoCompletionSource() {
                @Override
                public List<SymbolDescriptor> requestSuggestions(List<ModuleScope> externalModules) {
                    return new ArrayList<>();
                }

                @Override
                public boolean hasOwnCompletionScope() {
                    return false;
                }
            });
        }

        return new StringValueExpr(sourceCodeRef, str);
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
        return NumberValueFactory.parse(getSourceCodeRef(ctx.NUMBER()), numberText);
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
    public AstNode visitThrowStat(KobuParser.ThrowStatContext ctx) {
        topLevelExpression = false;
        Expr expr = (Expr) visit(ctx.expr());
        topLevelExpression = true;
        return new ThrowStatement(getSourceCodeRef(ctx), expr);
    }

    @Override
    public AstNode visitTryCatchStat(KobuParser.TryCatchStatContext ctx) {
        CatchBlockStatement catchBlock = null;
        if (ctx.catchStat() != null) {
            catchBlock = (CatchBlockStatement) visit(ctx.catchStat());
        }

        List<Evaluable> block = new ArrayList<>();
        if (ctx.execStat() != null) {
            for (KobuParser.ExecStatContext execStatContext : ctx.execStat()) {
                block.add((Evaluable) visit(execStatContext));
            }
        }

        return new TryCatchStatement(getSourceCodeRef(ctx), block, catchBlock);
    }

    @Override
    public AstNode visitCatchStat(KobuParser.CatchStatContext ctx) {
        if (ctx.ID() != null && ctx.type() != null) {
            Type errorType = (Type) visit(ctx.type());
            List<Evaluable> block = new ArrayList<>();
            if (ctx.execStat() != null) {
                for (KobuParser.ExecStatContext execStatContext : ctx.execStat()) {
                    block.add((Evaluable) visit(execStatContext));
                }
            }
            var catchBlock = new CatchBlockStatement(getSourceCodeRef(ctx), getSourceCodeRef(ctx.ID()),
                    moduleScope, ctx.ID().getText(), errorType, block);

            if (ctx.catchStat() != null) {
                catchBlock.setNextCatch((CatchBlockStatement) visit(ctx.catchStat()));
            }

            return catchBlock;
        }
        return null;
    }

    @Override
    public AstNode visitQueryExpr(KobuParser.QueryExprContext ctx) {
        String bind = null;
        SourceCodeRef bindSourceCodeRef = null;
        if (ctx.queryExprAlias() != null && ctx.queryExprAlias().ID() != null) {
            bind = ctx.queryExprAlias().ID().getText();
            bindSourceCodeRef = getSourceCodeRef(ctx.queryExprAlias().ID());
        }
        Type type;
        if (ctx.type() != null) {
            type = (Type) visit(ctx.type());
        } else {
            type = BuiltinScope.ANY_TYPE;
        }
        QueryTypeClause queryTypeClause = new QueryTypeClause(moduleScope, getSourceCodeRef(ctx), bindSourceCodeRef, type,
                ctx.ANY() != null, bind);

        if (ctx.queryExprSegment() != null) {
            QueryClause queryClause = (QueryClause) visit(ctx.queryExprSegment());
            queryTypeClause.setQueryClause(queryClause);
        }

        return new Query(getSourceCodeRef(ctx), queryTypeClause);
    }

    @Override
    public AstNode visitQueryExprSegment(KobuParser.QueryExprSegmentContext ctx) {
        String alias = null;
        if (ctx.queryExprAlias() != null && ctx.queryExprAlias().ID() != null) {
            alias = ctx.queryExprAlias().ID().getText();
        }

        if (ctx.queryFieldExpr() == null && ctx.queryStarTypeExpr() == null) {
            return null;
        }

        QueryClause clause = (QueryClause) visit(ctx.queryFieldExpr() != null ? ctx.queryFieldExpr() : ctx.queryStarTypeExpr());
        if (alias != null) {
            clause.setBind(alias);
            clause.setAliasSourceCodeRef(getSourceCodeRef(ctx.queryExprAlias().ID()));
        }

        if (ctx.queryExprSegment() != null) {
            QueryFieldClause next = (QueryFieldClause) visit(ctx.queryExprSegment());
            clause.setNext(next);
        }
        return clause;
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
    public AstNode visitQueryStarTypeExpr(KobuParser.QueryStarTypeExprContext ctx) {
        return new QueryStarTypeClause(getSourceCodeRef(ctx), (Type) visit(ctx.type()), ctx.ANY() != null);
    }

    @Override
    public AstNode visitQueryExprArraySelect(KobuParser.QueryExprArraySelectContext ctx) {
        return visit(ctx.queryExprArrayItem());
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
    public AstNode visitExtractExpr(KobuParser.ExtractExprContext ctx) {
        QueryClause clause = (QueryClause) visit(ctx.queryExprSegment());
        return new QueryExtractor(moduleScope, getSourceCodeRef(ctx), clause);
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
        return ArrayTypeFactory.getArrayTypeFor((Type) visit(ctx.type()));
    }

    @Override
    public AstNode visitTypeName(KobuParser.TypeNameContext ctx) {
        if (ctx.ID() == null || ctx.ID().isEmpty()) {
            return UnknownType.INSTANCE;
        }
        if (ctx.ID().size() == 1) {
            if (moduleLoader.getEvalMode() == EvalModeEnum.ANALYZER_SERVICE) {
                moduleScope.registerAutoCompletionSource(ctx.ID(0).getSymbol().getStartIndex(), new AutoCompletionSource() {
                    @Override
                    public List<SymbolDescriptor> requestSuggestions(List<ModuleScope> externalModules) {
                        List<SymbolDescriptor> symbols = new ArrayList<>();
                        symbols.addAll(getGlobalSymbols(moduleScope, SymbolTypeEnum.RECORD_TYPE,
                                SymbolTypeEnum.TEMPLATE_TYPE, SymbolTypeEnum.BUILTIN_TYPE, SymbolTypeEnum.RULE,
                                SymbolTypeEnum.TEMPLATE, SymbolTypeEnum.ACTION, SymbolTypeEnum.MODULE_REF));
                        symbols.addAll(getExternalSymbols(moduleScope, externalModules, SymbolTypeEnum.RECORD_TYPE,
                                SymbolTypeEnum.TEMPLATE_TYPE, SymbolTypeEnum.BUILTIN_TYPE,
                                SymbolTypeEnum.RULE, SymbolTypeEnum.TEMPLATE, SymbolTypeEnum.ACTION));
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

            var typeName = ctx.ID(0).getText();

            if (typeParameterContext != null) {
                var typeAlias = typeParameterContext.get(typeName);
                if (typeAlias != null) {
                    return typeAlias;
                }
            }

            var symbol = moduleScope.resolve(typeName);

            if (!ruleTypeScope) {
                if (!(symbol instanceof Type)) {
                    context.getErrorScope().addError(new UndefinedTypeError(getSourceCodeRef(ctx.ID(0)),
                            typeName, scopeEndOffset));
                    return UnknownType.INSTANCE;
                }

                return getType(ctx, (Type) symbol);
            } else {
                if (!(symbol instanceof RuleSymbol)) {
                    return UnknownType.INSTANCE;
                }
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

            if (!ruleTypeScope) {
                if (!(symbol instanceof Type)) {
                    context.getErrorScope().addError(new UndefinedTypeError(getSourceCodeRef(ctx),
                            moduleAlias + "." + typeName, scopeEndOffset));
                    return UnknownType.INSTANCE;
                }

                return getType(ctx, (Type) symbol);
            } else {
                if (!(symbol instanceof RuleSymbol)) {
                    return UnknownType.INSTANCE;
                }
            }
            return (AstNode) symbol;
        }
    }

    private Type getType(KobuParser.TypeNameContext ctx, Type symbol) {
        Type nodeType = symbol;

        if (ctx.typeArgs() != null) {
            List<Type> typeList = getTypesFrom(ctx.typeArgs());
            if (nodeType instanceof RecordTypeSymbol) {
                RecordTypeSymbol recordType = (RecordTypeSymbol) nodeType;
                if (recordType.getTypeParameters() == null && !typeList.isEmpty()) {
                    context.getErrorScope().addError(new InvalidTypeArgsError(getSourceCodeRef(ctx.typeArgs()),
                            0, typeList.size()));
                } else if (recordType.getTypeParameters().size() != typeList.size()) {
                    context.getErrorScope().addError(new InvalidTypeArgsError(getSourceCodeRef(ctx.typeArgs()),
                            recordType.getTypeParameters().size(), typeList.size()));
                } else if (!typeList.isEmpty()) {
                    nodeType = new RecordTypeSymbol(recordType, typeList);
                }
            } else if (nodeType instanceof RecordTypeRefTypeSymbol) {
                if (typeList.size() != 1) {
                    context.getErrorScope().addError(new InvalidTypeArgsError(getSourceCodeRef(ctx.typeArgs()),
                            1, typeList.size()));
                } else {
                    Type typeArg = typeList.get(0);
                    if (!(typeArg instanceof AnyRecordTypeSymbol) && !(typeArg instanceof RecordTypeSymbol) &&
                            !(typeArg instanceof TypeAlias)) {
                        context.getErrorScope().addError(new InvalidTypeError(nodeType.getSourceCodeRef(),
                                BuiltinScope.ANY_RECORD_TYPE, typeArg));
                    }
                    nodeType = new ParameterizedRecordTypeRef(nodeType.getSourceCodeRef(), typeArg);
                }
            } else {
                context.getErrorScope().addError(new InvalidTypeArgsError(getSourceCodeRef(ctx),
                        0, typeList.size()));
            }
            return nodeType;
        } else {
            if (nodeType instanceof RecordTypeSymbol) {
                RecordTypeSymbol recordType = (RecordTypeSymbol) nodeType;
                if (recordType.getTypeParameters() != null && !recordType.getTypeParameters().isEmpty()) {
                    context.getErrorScope().addError(new InvalidTypeArgsError(getSourceCodeRef(ctx),
                            recordType.getTypeParameters().size(), 0));
                }
            } else if (nodeType instanceof RecordTypeRefTypeSymbol) {
                context.getErrorScope().addError(new InvalidTypeArgsError(getSourceCodeRef(ctx),
                        1, 0));
            }
            return nodeType;
        }
    }

    private List<Type> getTypesFrom(KobuParser.TypeArgsContext typeArgsCtx) {
        List<Type> types = new ArrayList<>();
        var typeArgCtx = typeArgsCtx.typeArg();
        while (typeArgCtx != null) {
            types.add((Type) visit(typeArgCtx.type()));
            typeArgCtx = typeArgCtx.typeArg();
        }
        return types;
    }

    @Override
    public AstNode visitTupleType(KobuParser.TupleTypeContext ctx) {
        if (ctx.type() != null) {
            TupleTypeElement tupleTypeElement = null;
            TupleTypeElement it = null;
            for (KobuParser.TypeContext typeContext : ctx.type()) {
                Type type = (Type) visit(typeContext);
                if (tupleTypeElement == null) {
                    tupleTypeElement = new TupleTypeElement(type);
                    it = tupleTypeElement;
                } else {
                    TupleTypeElement next = new TupleTypeElement(type);
                    it.setNext(next);
                    it = next;
                }
            }
            return TupleTypeFactory.getTupleTypeFor(tupleTypeElement);
        }
        return UnknownType.INSTANCE;
    }

    @Override
    public AstNode visitFunctionType(KobuParser.FunctionTypeContext ctx) {
        if (ctx.type() == null) {
            return UnknownType.INSTANCE;
        }

        List<FunctionTypeParameter> parameters = new ArrayList<>();
        var paramCtx = ctx.functionTypeParameter();
        while (paramCtx != null) {
            var param = new FunctionTypeParameter((Type) visit(paramCtx.type()), paramCtx.QM() != null);
            parameters.add(param);
            paramCtx = paramCtx.functionTypeParameter();
        }
        Type returnType = (Type) visit(ctx.type());

        return new FunctionType(parameters, returnType);
    }

    @Override
    public AstNode visitParenthesizedFunctionTypeExpr(KobuParser.ParenthesizedFunctionTypeExprContext ctx) {
        return visit(ctx.functionType());
    }

    public ModuleScope getModuleScope() {
        return moduleScope;
    }

    private List<FunctionParameter> getFunctionParameters(KobuParser.FunctionDeclParamContext paramCtx) {
        List<FunctionParameter> parameters;
        parameters = new ArrayList<>();

        while (paramCtx != null) {
            if (paramCtx.ID() != null) {
                var type = paramCtx.type() != null ? (Type) visit(paramCtx.type()) : UnknownType.INSTANCE;
                FunctionParameter param = new FunctionParameter(getSourceCodeRef(paramCtx.ID()), paramCtx.ID().getText(), type,
                        paramCtx.QM() != null);
                parameters.add(param);
            }

            paramCtx = paramCtx.functionDeclParam();
        }
        return parameters;
    }

}
