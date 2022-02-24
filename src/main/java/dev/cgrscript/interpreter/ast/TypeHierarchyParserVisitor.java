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

import dev.cgrscript.interpreter.error.AnalyzerError;
import dev.cgrscript.interpreter.error.ParserErrorListener;
import dev.cgrscript.interpreter.error.analyzer.RecordInvalidSuperTypeError;
import dev.cgrscript.interpreter.module.ModuleLoader;
import dev.cgrscript.interpreter.ast.symbol.*;
import dev.cgrscript.antlr.cgrscript.CgrScriptParser;
import org.antlr.v4.runtime.tree.TerminalNode;

public class TypeHierarchyParserVisitor extends CgrScriptParserVisitor<Void> {

    private final AnalyzerContext context;

    public TypeHierarchyParserVisitor(ModuleLoader moduleLoader, ModuleScope moduleScope, AnalyzerContext context) {
        super(moduleLoader);
        this.context = context;
        this.moduleScope = moduleScope;
    }

    @Override
    public Void visitDeftype(CgrScriptParser.DeftypeContext ctx) {

        if (ctx.inheritance() != null) {
            RecordTypeSymbol recordType = (RecordTypeSymbol) moduleScope.resolve(ctx.ID().getText());

            if (ctx.inheritance() != null) {
                TerminalNode typeSymbol = ctx.inheritance().ID();
                var type = moduleScope.resolve(typeSymbol.getText());
                if (type instanceof AnyRecordTypeSymbol) {
                    return null;
                }
                if (!(type instanceof RecordTypeSymbol)) {
                    context.getErrorScope().addError(new RecordInvalidSuperTypeError(getSourceCodeRef(typeSymbol), recordType,
                            typeSymbol.getText()));
                } else {
                    recordType.setSuperType(new RecordSuperType(getSourceCodeRef(typeSymbol), (RecordTypeSymbol) type));
                }
            }

        }

        return null;
    }

}
