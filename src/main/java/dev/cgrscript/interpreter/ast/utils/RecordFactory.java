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

import dev.cgrscript.interpreter.ast.eval.context.EvalContext;
import dev.cgrscript.interpreter.ast.eval.expr.value.RecordValueExpr;
import dev.cgrscript.interpreter.ast.symbol.RecordTypeSymbol;
import dev.cgrscript.interpreter.ast.symbol.SourceCodeRef;
import dev.cgrscript.interpreter.error.eval.InvalidCallError;

import java.util.HashMap;

public class RecordFactory {

    public static RecordValueExpr create(EvalContext context, String typeName) {
        return create(context, typeName, null);
    }

    public static RecordValueExpr create(EvalContext context, String typeName, SourceCodeRef sourceCodeRef) {
        var typeSymbol = context.getModuleScope().resolve(typeName);
        if (!(typeSymbol instanceof RecordTypeSymbol)) {
            throw new InvalidCallError("Invalid record type: " + typeName, sourceCodeRef);
        }
        return create(context, (RecordTypeSymbol) typeSymbol, sourceCodeRef);
    }

    public static RecordValueExpr create(EvalContext context, RecordTypeSymbol typeSymbol, SourceCodeRef sourceCodeRef) {
        return new RecordValueExpr(typeSymbol, new HashMap<>(), context.getDatabase().generateRecordId());
    }

}
