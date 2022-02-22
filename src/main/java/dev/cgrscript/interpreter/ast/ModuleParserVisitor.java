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

import dev.cgrscript.interpreter.ast.eval.function.NativeFunction;
import dev.cgrscript.interpreter.ast.eval.function.NativeFunctionId;
import dev.cgrscript.interpreter.error.AnalyzerError;
import dev.cgrscript.interpreter.error.ParserErrorListener;
import dev.cgrscript.interpreter.error.analyzer.DuplicatedModuleReferenceError;
import dev.cgrscript.interpreter.error.analyzer.InvalidModuleDeclarationError;
import dev.cgrscript.interpreter.error.analyzer.NativeFunctionNotFoundError;
import dev.cgrscript.interpreter.file_system.ScriptRef;
import dev.cgrscript.interpreter.module.ModuleLoader;
import dev.cgrscript.interpreter.ast.symbol.*;
import dev.cgrscript.antlr.cgrscript.CgrScriptParser;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ModuleParserVisitor extends CgrScriptParserVisitor<Void> {

    private final ParserErrorListener parserErrorListener;

    private final ScriptRef script;

    private final Map<NativeFunctionId, NativeFunction> nativeFunctions;

    public ModuleParserVisitor(ModuleLoader moduleLoader, ParserErrorListener parserErrorListener, ScriptRef script,
                               Map<NativeFunctionId, NativeFunction> nativeFunctions) {
        super(moduleLoader);
        this.parserErrorListener = parserErrorListener;
        this.script = script;
        this.nativeFunctions = nativeFunctions;
    }

    @Override
    public Void visitModule(CgrScriptParser.ModuleContext ctx) {
        if (ctx.moduleId() == null) {
            moduleScope = new ModuleScope(script.extractModuleId(), script, moduleLoader.getProject().getProjectDirectory().getAbsolutePath(),
                    moduleLoader.getProject().getProperties(), nativeFunctions, moduleLoader.getModuleIndex());
            moduleScope.addError(new InvalidModuleDeclarationError(getSourceCodeRef(ctx), "null"));
            return null;
        }
        List<String> segments = ctx.moduleId().ID()
                .stream()
                .map(ParseTree::getText)
                .collect(Collectors.toList());
        String declaredModuleId = String.join(".", segments);
        String fileModuleId = script.extractModuleId();
        moduleScope = new ModuleScope(declaredModuleId, script, moduleLoader.getProject().getProjectDirectory().getAbsolutePath(),
                moduleLoader.getProject().getProperties(), nativeFunctions, moduleLoader.getModuleIndex());
        if (!declaredModuleId.equals(fileModuleId)) {
            moduleScope.addError(new InvalidModuleDeclarationError(getSourceCodeRef(ctx), declaredModuleId));
        }
        moduleScope.setNewImportOffset(ctx.SEMI() != null ?
                ctx.SEMI().getSymbol().getStopIndex() + 1 :
                ctx.moduleId().stop.getStopIndex() + 1);
        return null;
    }

    @Override
    public Void visitImportExpr(CgrScriptParser.ImportExprContext ctx) {
        int endOffset = 0;
        try {
            String dependencyModuleId = ctx.moduleId().getText();
            String alias = null;
            SourceCodeRef sourceCodeRef = null;
            if (ctx.moduleScope() != null) {
                alias = ctx.moduleScope().ID().getText();
                sourceCodeRef = getSourceCodeRef(ctx.moduleScope().ID());
                endOffset = ctx.moduleScope().ID().getSymbol().getStopIndex();
            } else {
                sourceCodeRef = getSourceCodeRef(ctx.moduleId());
                endOffset = ctx.moduleId().stop.getStopIndex();
            }
            ModuleScope dependency = moduleLoader.loadScope(parserErrorListener,
                    dependencyModuleId, getSourceCodeRef(ctx));
            if (!moduleScope.addModule(dependency)) {
                moduleScope.addError(new DuplicatedModuleReferenceError(getSourceCodeRef(ctx), dependencyModuleId));
                return null;
            }
            moduleScope.merge(dependency, alias, sourceCodeRef);
        } catch (AnalyzerError error) {
            moduleScope.addError(error);
        }
        moduleScope.setNewImportOffset(ctx.SEMI() != null ?
                ctx.SEMI().getSymbol().getStopIndex() + 1 :
                endOffset + 1);
        moduleScope.setHasImports(true);
        return null;
    }

    @Override
    public Void visitDeftype(CgrScriptParser.DeftypeContext ctx) {
        if (ctx.ID() != null) {
            var recordType = new RecordTypeSymbol(getSourceCodeRef(ctx), ctx.ID().getText(), moduleScope);
            moduleScope.define(recordType);
        }
        return null;
    }

    @Override
    public Void visitFunctionDecl(CgrScriptParser.FunctionDeclContext ctx) {
        if (ctx.ID() != null && ctx.RCB() != null) {
            var function = new FunctionSymbol(getSourceCodeRef(ctx.ID()), getSourceCodeRef(ctx.RCB()),
                    moduleScope, ctx.ID().getText());
            moduleScope.define(function);
        }
        return null;
    }

    @Override
    public Void visitNativeDecl(CgrScriptParser.NativeDeclContext ctx) {
        var nativeFunctionId = new NativeFunctionId(moduleScope.getModuleId(), ctx.ID().getText());
        var nativeFunction = moduleScope.getNativeFunction(nativeFunctionId);
        if (nativeFunction == null) {
            moduleScope.addError(new NativeFunctionNotFoundError(getSourceCodeRef(ctx.ID()), nativeFunctionId));
            return null;
        }

        var function = new NativeFunctionSymbol(getSourceCodeRef(ctx.ID()), moduleScope, ctx.ID().getText(), nativeFunction);
        moduleScope.define(function);

        return null;
    }

    @Override
    public Void visitDeftemplate(CgrScriptParser.DeftemplateContext ctx) {
        if (ctx.ID() == null) {
            return null;
        }
        String parentRule = null;
        if (ctx.ruleExtends() != null) {
            parentRule = ctx.ruleExtends().ID().getText();
        }
        var template = new RuleSymbol(getSourceCodeRef(ctx.ID()), ctx.ID().getText(), getSourceCodeRef(ctx.TEMPLATE_END()),
                moduleScope, RuleTypeEnum.TEMPLATE, parentRule);
        moduleScope.define(template);
        return null;
    }

    @Override
    public Void visitDefrule(CgrScriptParser.DefruleContext ctx) {
        if (ctx.ID() == null) {
            return null;
        }
        String parentRule = null;
        if (ctx.ruleExtends() != null) {
            parentRule = ctx.ruleExtends().ID().getText();
        }
        var rule = new RuleSymbol(getSourceCodeRef(ctx.ID()), ctx.ID().getText(), getSourceCodeRef(ctx.RCB()),
                moduleScope, RuleTypeEnum.RULE, parentRule);
        moduleScope.define(rule);
        return null;
    }

    @Override
    public Void visitDeffile(CgrScriptParser.DeffileContext ctx) {
        if (ctx.ID() == null) {
            return null;
        }
        String parentRule = null;
        if (ctx.ruleExtends() != null) {
            parentRule = ctx.ruleExtends().ID().getText();
        }
        var fileRule = new RuleSymbol(getSourceCodeRef(ctx.ID()), ctx.ID().getText(), getSourceCodeRef(ctx.PATH_END()),
                moduleScope, RuleTypeEnum.FILE, parentRule);
        moduleScope.define(fileRule);
        return null;
    }

    public ModuleScope getModuleScope() {
        return moduleScope;
    }


}
