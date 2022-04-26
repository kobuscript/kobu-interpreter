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

package dev.kobu.interpreter.ast.symbol;

import dev.kobu.interpreter.ast.eval.DocumentationSource;
import dev.kobu.interpreter.ast.eval.SymbolDocumentation;
import dev.kobu.interpreter.ast.eval.SymbolTypeEnum;
import dev.kobu.interpreter.ast.eval.context.EvalModeEnum;

import java.util.Map;

public class RecordTypeStarAttribute implements DocumentationSource {

    private final ModuleScope moduleScope;

    private final SourceCodeRef sourceCodeRef;

    private final Type type;

    private final RecordTypeSymbol recordType;

    private SymbolDocumentation documentation;

    public RecordTypeStarAttribute(ModuleScope moduleScope, SourceCodeRef sourceCodeRef, Type type, RecordTypeSymbol recordType) {
        this.moduleScope = moduleScope;
        this.sourceCodeRef = sourceCodeRef;
        this.type = type;
        this.recordType = recordType;

        if (moduleScope.getEvalMode() == EvalModeEnum.ANALYZER_SERVICE) {
            moduleScope.registerDocumentationSource(sourceCodeRef.getStartOffset(), this);
        }
    }

    public RecordTypeStarAttribute(RecordTypeStarAttribute starAttr, Map<String, Type> typeAliasMap) {
        this.moduleScope = starAttr.moduleScope;
        this.sourceCodeRef = starAttr.sourceCodeRef;
        this.recordType = starAttr.recordType;
        this.type = starAttr.type.constructFor(typeAliasMap);
    }

    public SourceCodeRef getSourceCodeRef() {
        return sourceCodeRef;
    }

    public Type getType() {
        return type;
    }

    public RecordTypeSymbol getRecordType() {
        return recordType;
    }

    @Override
    public SymbolDocumentation getDocumentation() {
        if (documentation == null) {
            String description = "*: " + (type != null ? type.getName() : "Any");
            documentation = new SymbolDocumentation(moduleScope.getModuleId(), SymbolTypeEnum.ATTRIBUTE, description,
                    null, recordType.getName());
        }
        return documentation;
    }
}
