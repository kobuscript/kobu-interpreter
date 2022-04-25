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

package dev.kobu.interpreter.ast.symbol.function;

import dev.kobu.interpreter.ast.eval.SymbolDocumentation;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.ast.symbol.Type;
import dev.kobu.interpreter.ast.symbol.generics.TypeAlias;
import dev.kobu.interpreter.ast.symbol.generics.TypeParameter;

import java.util.Map;
import java.util.stream.Collectors;

public interface NamedFunction extends KobuFunction {

    String getName();

    SourceCodeRef getSourceCodeRef();

    SymbolDocumentation getDocumentation(Map<String, Type> typeArgs);

    default String getDescription(Map<String, Type> typeArgs) {
        StringBuilder str = new StringBuilder();
        if (getTypeParameters() != null && !getTypeParameters().isEmpty()) {
            str.append("<");
            str.append(getTypeParameters().stream().map(param -> {
                return typeArgs.containsKey(param.getAlias()) ?
                        typeArgs.get(param.getAlias()).getName() :
                        param.getAlias();
            }).collect(Collectors.joining(", ")));
            str.append(">");
        }
        str.append('(');
        if (getParameters() != null) {
            str.append(getParameters().stream().map(p -> p.getDescription(typeArgs))
                    .collect(Collectors.joining(", ")));
        }
        str.append(')');
        if (getReturnType() != null) {
            str.append(": ").append(getReturnType().constructFor(typeArgs).getName());
        } else {
            str.append(": void");
        }

        return str.toString();
    }

}
