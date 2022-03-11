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

import dev.cgrscript.antlr.cgrscript.CgrScriptLexer;
import dev.cgrscript.antlr.cgrscript.CgrScriptParser;
import dev.cgrscript.interpreter.ast.eval.AutoCompletionSource;
import dev.cgrscript.interpreter.ast.eval.context.EvalContextProvider;
import dev.cgrscript.interpreter.ast.eval.context.EvalModeEnum;
import dev.cgrscript.interpreter.ast.eval.SymbolDescriptor;
import dev.cgrscript.interpreter.ast.eval.function.NativeFunctionId;
import dev.cgrscript.interpreter.ast.symbol.*;
import dev.cgrscript.interpreter.ast.utils.DocumentationUtils;
import dev.cgrscript.interpreter.ast.utils.SymbolDescriptorUtils;
import dev.cgrscript.interpreter.error.AnalyzerError;
import dev.cgrscript.interpreter.error.analyzer.CyclicModuleReferenceError;
import dev.cgrscript.interpreter.error.analyzer.DuplicatedModuleReferenceError;
import dev.cgrscript.interpreter.error.analyzer.InvalidModuleDeclarationError;
import dev.cgrscript.interpreter.error.analyzer.NativeFunctionNotFoundError;
import dev.cgrscript.interpreter.file_system.ScriptRef;
import dev.cgrscript.interpreter.module.ModuleIndexNode;
import dev.cgrscript.interpreter.module.ModuleLoader;
import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.List;
import java.util.stream.Collectors;

public class ModuleParserVisitor extends CgrScriptParserVisitor<Void> {

    private final EvalContextProvider evalContextProvider;

    private final AnalyzerContext context;

    private final ScriptRef script;

    private final BufferedTokenStream tokens;

    public ModuleParserVisitor(ModuleLoader moduleLoader, ModuleScope moduleScope,
                               EvalContextProvider evalContextProvider, AnalyzerContext context, ScriptRef script,
                               BufferedTokenStream tokens) {
        super(moduleLoader);
        this.evalContextProvider = evalContextProvider;
        this.tokens = tokens;
        this.moduleScope = moduleScope;
        this.context = context;
        this.script = script;
    }

    @Override
    public Void visitModule(CgrScriptParser.ModuleContext ctx) {
        if (ctx.moduleId() == null) {
            return null;
        }
        List<String> segments = ctx.moduleId().MODULE_ID()
                .stream()
                .map(ParseTree::getText)
                .collect(Collectors.toList());
        String declaredModuleId = String.join(".", segments);
        String fileModuleId = script.extractModuleId();
        if (!declaredModuleId.equals(fileModuleId)) {
            context.getErrorScope().addError(new InvalidModuleDeclarationError(getSourceCodeRef(ctx), declaredModuleId));
        }
        moduleScope.setNewImportOffset(ctx.MODULE_ID_BREAK().getSymbol().getStopIndex() + 1);
        return null;
    }

    @Override
    public Void visitImportExpr(CgrScriptParser.ImportExprContext ctx) {
        if (ctx.moduleId() == null) {
            if (moduleLoader.getEvalMode() == EvalModeEnum.ANALYZER_SERVICE) {
                addImportAutoCompletionSource(ctx.IMPORT().getSymbol().getStopIndex() + 2, "");
            }
            return null;
        }
        try {
            String dependencyModuleId = ctx.moduleId().getText();
            String alias = null;
            SourceCodeRef sourceCodeRef;
            if (ctx.moduleScope() != null) {
                alias = ctx.moduleScope().MODULE_ID().getText();
                sourceCodeRef = getSourceCodeRef(ctx.moduleScope().MODULE_ID());
            } else {
                sourceCodeRef = getSourceCodeRef(ctx.moduleId());
            }

            if (moduleLoader.getEvalMode() == EvalModeEnum.ANALYZER_SERVICE) {
                addImportAutoCompletionSource(ctx.moduleId().start.getStartIndex(), dependencyModuleId);
            }

            ModuleScope dependency = moduleLoader.loadScope(context, dependencyModuleId, getSourceCodeRef(ctx));
            if (!moduleScope.addModule(dependency)) {
                context.getErrorScope().addError(new DuplicatedModuleReferenceError(getSourceCodeRef(ctx), dependencyModuleId));
                return null;
            } else if (moduleLoader.getEvalMode() == EvalModeEnum.ANALYZER_SERVICE) {
                var path = moduleScope.findCyclicPath();
                if (path != null) {
                    this.context.getErrorScope().addError(new CyclicModuleReferenceError(getSourceCodeRef(ctx.moduleId()), path));
                }
            }
            if (!moduleScope.getModuleId().equals(dependencyModuleId)) {
                moduleScope.merge(context, dependency, alias, sourceCodeRef);
            }
        } catch (AnalyzerError error) {
            context.getErrorScope().addError(error);
        }
        moduleScope.setNewImportOffset(ctx.MODULE_ID_BREAK().getSymbol().getStopIndex() + 1);
        moduleScope.setHasImports(true);
        return null;
    }

    private void addImportAutoCompletionSource(int offset, String moduleId) {
        int currentOffset = offset;
        String[] segments;
        if (moduleId.endsWith(".")) {
            segments = (moduleId + " ").split("\\.");
        } else {
            segments = moduleId.split("\\.");
        }
        var index = moduleScope.getModuleIndex();
        ModuleIndexNode node = null;

        StringBuilder prefix = new StringBuilder();
        for (String segment : segments) {
            var currentNode = node;
            if (prefix.length() > 0) {
                prefix.append(".");
            }
            String prefixLevel = prefix.toString();
            moduleScope.registerAutoCompletionSource(currentOffset, new AutoCompletionSource() {
                @Override
                public List<SymbolDescriptor> requestSuggestions(List<ModuleScope> externalModules) {
                    if (currentNode != null) {
                        return SymbolDescriptorUtils.getSymbolsForImportSegments(prefixLevel, currentNode.getSegments());
                    } else {
                        return SymbolDescriptorUtils.getSymbolsForImportSegments(prefixLevel, index.getRootSegments());
                    }
                }

                @Override
                public boolean hasOwnCompletionScope() {
                    return false;
                }
            });
            String segPath = segment.trim();
            node = currentNode != null ? currentNode.getChild(segPath) : index.getChild(segPath);
            prefix.append(segPath);
            currentOffset += segPath.length() + 1;
        }
    }

    @Override
    public Void visitDeftype(CgrScriptParser.DeftypeContext ctx) {
        if (ctx.ID() != null) {
            String docText = null;
            if (moduleLoader.getEvalMode() == EvalModeEnum.ANALYZER_SERVICE) {
                var docChannel = tokens.getHiddenTokensToLeft(ctx.start.getTokenIndex(), CgrScriptLexer.BLOCKCOMMENTCHANNEL);
                if (docChannel != null) {
                    docText = DocumentationUtils.removeCommentDelimiters(docChannel.get(0).getText());
                }
            }

            var recordType = new RecordTypeSymbol(getSourceCodeRef(ctx.ID()), ctx.ID().getText(), moduleScope, docText);
            moduleScope.define(context, recordType);
        }
        return null;
    }

    @Override
    public Void visitFunctionDecl(CgrScriptParser.FunctionDeclContext ctx) {
        if (ctx.ID() != null) {

            String docText = null;
            if (moduleLoader.getEvalMode() == EvalModeEnum.ANALYZER_SERVICE) {
                var docChannel = tokens.getHiddenTokensToLeft(ctx.start.getTokenIndex(), CgrScriptLexer.BLOCKCOMMENTCHANNEL);
                if (docChannel != null) {
                    docText = DocumentationUtils.removeCommentDelimiters(docChannel.get(0).getText());
                }
            }

            var function = new FunctionSymbol(evalContextProvider, getSourceCodeRef(ctx.ID()), getSourceCodeRef(ctx.RCB()),
                    moduleScope, ctx.ID().getText(), docText);
            moduleScope.define(context, function);
        }
        return null;
    }

    @Override
    public Void visitNativeDecl(CgrScriptParser.NativeDeclContext ctx) {
        if (ctx.ID() == null) {
            return null;
        }

        String docText = null;
        if (moduleLoader.getEvalMode() == EvalModeEnum.ANALYZER_SERVICE) {
            var docChannel = tokens.getHiddenTokensToLeft(ctx.start.getTokenIndex(), CgrScriptLexer.BLOCKCOMMENTCHANNEL);
            if (docChannel != null) {
                docText = DocumentationUtils.removeCommentDelimiters(docChannel.get(0).getText());
            }
        }

        var nativeFunctionId = new NativeFunctionId(moduleScope.getModuleId(), ctx.ID().getText());
        var nativeFunction = moduleScope.getNativeFunction(nativeFunctionId);
        if (nativeFunction == null) {
            context.getErrorScope().addError(new NativeFunctionNotFoundError(getSourceCodeRef(ctx.ID()), nativeFunctionId));
            return null;
        }

        var function = new NativeFunctionSymbol(getSourceCodeRef(ctx.ID()), moduleScope, ctx.ID().getText(), nativeFunction, docText);
        moduleScope.define(context, function);

        return null;
    }

    @Override
    public Void visitDeftemplate(CgrScriptParser.DeftemplateContext ctx) {
        if (ctx.ID() == null) {
            return null;
        }
        String parentRuleModuleAlias = null;
        String parentRule = null;
        if (ctx.ruleExtends() != null && ctx.ruleExtends().typeName() != null) {
            var typeNameExpr = ctx.ruleExtends().typeName();
            if (typeNameExpr.ID().size() == 2) {
                parentRuleModuleAlias = typeNameExpr.ID(0).getText();
                parentRule = typeNameExpr.ID(1).getText();
            } else {
                parentRule = typeNameExpr.ID(0).getText();
            }
        }

        String docText = null;
        if (moduleLoader.getEvalMode() == EvalModeEnum.ANALYZER_SERVICE) {
            var docChannel = tokens.getHiddenTokensToLeft(ctx.start.getTokenIndex(), CgrScriptLexer.BLOCKCOMMENTCHANNEL);
            if (docChannel != null) {
                docText = DocumentationUtils.removeCommentDelimiters(docChannel.get(0).getText());
            }
        }

        var template = new RuleSymbol(getSourceCodeRef(ctx.ID()), ctx.ID().getText(), evalContextProvider, getSourceCodeRef(ctx.TEMPLATE_END()),
                moduleScope, RuleTypeEnum.TEMPLATE, parentRuleModuleAlias, parentRule, docText);
        moduleScope.define(context, template);
        return null;
    }

    @Override
    public Void visitDefrule(CgrScriptParser.DefruleContext ctx) {
        if (ctx.ID() == null) {
            return null;
        }
        String parentRuleModuleAlias = null;
        String parentRule = null;
        if (ctx.ruleExtends() != null && ctx.ruleExtends().typeName() != null) {
            var typeNameExpr = ctx.ruleExtends().typeName();
            if (typeNameExpr.ID().size() == 2) {
                parentRuleModuleAlias = typeNameExpr.ID(0).getText();
                parentRule = typeNameExpr.ID(1).getText();
            } else {
                parentRule = typeNameExpr.ID(0).getText();
            }
        }

        String docText = null;
        if (moduleLoader.getEvalMode() == EvalModeEnum.ANALYZER_SERVICE) {
            var docChannel = tokens.getHiddenTokensToLeft(ctx.start.getTokenIndex(), CgrScriptLexer.BLOCKCOMMENTCHANNEL);
            if (docChannel != null) {
                docText = DocumentationUtils.removeCommentDelimiters(docChannel.get(0).getText());
            }
        }

        var rule = new RuleSymbol(getSourceCodeRef(ctx.ID()), ctx.ID().getText(), evalContextProvider, getSourceCodeRef(ctx.RCB()),
                moduleScope, RuleTypeEnum.RULE, parentRuleModuleAlias, parentRule, docText);
        moduleScope.define(context, rule);
        return null;
    }

    @Override
    public Void visitDeffile(CgrScriptParser.DeffileContext ctx) {
        if (ctx.ID() == null) {
            return null;
        }
        String parentRuleModuleAlias = null;
        String parentRule = null;
        if (ctx.ruleExtends() != null && ctx.ruleExtends().typeName() != null) {
            var typeNameExpr = ctx.ruleExtends().typeName();
            if (typeNameExpr.ID().size() == 2) {
                parentRuleModuleAlias = typeNameExpr.ID(0).getText();
                parentRule = typeNameExpr.ID(1).getText();
            } else {
                parentRule = typeNameExpr.ID(0).getText();
            }
        }

        String docText = null;
        if (moduleLoader.getEvalMode() == EvalModeEnum.ANALYZER_SERVICE) {
            var docChannel = tokens.getHiddenTokensToLeft(ctx.start.getTokenIndex(), CgrScriptLexer.BLOCKCOMMENTCHANNEL);
            if (docChannel != null) {
                docText = DocumentationUtils.removeCommentDelimiters(docChannel.get(0).getText());
            }
        }

        var fileRule = new RuleSymbol(getSourceCodeRef(ctx.ID()), ctx.ID().getText(), evalContextProvider, getSourceCodeRef(ctx.PATH_END()),
                moduleScope, RuleTypeEnum.FILE, parentRuleModuleAlias, parentRule, docText);
        moduleScope.define(context, fileRule);
        return null;
    }


}
