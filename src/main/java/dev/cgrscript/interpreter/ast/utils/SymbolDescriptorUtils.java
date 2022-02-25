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

package dev.cgrscript.interpreter.ast.utils;

import dev.cgrscript.interpreter.ast.eval.SymbolDescriptor;
import dev.cgrscript.interpreter.ast.eval.SymbolTypeEnum;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SymbolDescriptorUtils {

    private static final List<SymbolDescriptor> globalKeywords = List.of(
            new SymbolDescriptor(SymbolTypeEnum.KEYWORD, "def", "", "define a new type or rule"),
            new SymbolDescriptor(SymbolTypeEnum.KEYWORD, "fun", "", "define a new function")
    );

    private static final List<SymbolDescriptor> defKeywords = List.of(
            new SymbolDescriptor(SymbolTypeEnum.KEYWORD, "type", "", "define a new record type"),
            new SymbolDescriptor(SymbolTypeEnum.KEYWORD, "rule", "", "define a new rule"),
            new SymbolDescriptor(SymbolTypeEnum.KEYWORD, "template", "", " define a new template rule"),
            new SymbolDescriptor(SymbolTypeEnum.KEYWORD, "file", "", "define a new file rule")
    );

    private static final List<SymbolDescriptor> statKeywords = List.of(
            new SymbolDescriptor(SymbolTypeEnum.KEYWORD, "if", "", ""),
            new SymbolDescriptor(SymbolTypeEnum.KEYWORD, "for", "", ""),
            new SymbolDescriptor(SymbolTypeEnum.KEYWORD, "while", "", ""),
            new SymbolDescriptor(SymbolTypeEnum.KEYWORD, "var", "", "")
    );

    public static final SymbolDescriptor voidKeyword = new SymbolDescriptor(SymbolTypeEnum.KEYWORD, "void", "", "");

    public static List<SymbolDescriptor> getStatKeywords() {
        return statKeywords;
    }

    public static List<SymbolDescriptor> getDefKeywords() {
        return defKeywords;
    }

    public static List<SymbolDescriptor> getGlobalKeywords() {
        return globalKeywords;
    }

    public static List<SymbolDescriptor> getSymbolsForImportSegments(String prefix, List<String> segments) {
        return segments.stream()
                .map(segment -> new SymbolDescriptor(SymbolTypeEnum.MODULE_SEGMENT, prefix + segment, segment,
                        "", ""))
                .collect(Collectors.toList());
    }
}
