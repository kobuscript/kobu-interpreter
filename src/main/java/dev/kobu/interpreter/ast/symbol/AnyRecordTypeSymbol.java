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

import dev.kobu.interpreter.ast.eval.ValueExpr;
import dev.kobu.interpreter.ast.eval.function.record.RecordGetFieldTypeNameMethodImpl;
import dev.kobu.interpreter.ast.eval.function.record.RecordGetMethodImpl;
import dev.kobu.interpreter.ast.eval.function.record.RecordHasFieldMethodImpl;
import dev.kobu.interpreter.ast.eval.function.record.RecordPutMethodImpl;
import dev.kobu.interpreter.ast.symbol.function.FunctionParameter;

import java.util.Comparator;

public class AnyRecordTypeSymbol extends BuiltinTypeSymbol {

    private static final String ANY_RECORD_TYPE = "AnyRecord";

    public AnyRecordTypeSymbol() {
        super(ANY_RECORD_TYPE);
        buildMethods();
    }

    @Override
    public boolean isAssignableFrom(Type type) {
        return type instanceof RecordTypeSymbol || type instanceof AnyRecordTypeSymbol;
    }

    @Override
    public Type getCommonSuperType(Type type) {
        return isAssignableFrom(type) ? this : BuiltinScope.ANY_TYPE;
    }

    @Override
    public Comparator<ValueExpr> getComparator() {
        return null;
    }

    private void buildMethods() {
        addMethod(new BuiltinFunctionSymbol("put", new RecordPutMethodImpl(),
                new FunctionParameter("field", BuiltinScope.STRING_TYPE, false),
                new FunctionParameter("value", BuiltinScope.ANY_TYPE, false)));
        addMethod(new BuiltinFunctionSymbol("get", new RecordGetMethodImpl(),
                BuiltinScope.ANY_TYPE,
                new FunctionParameter("field", BuiltinScope.STRING_TYPE, false)));
        addMethod(new BuiltinFunctionSymbol("hasField", new RecordHasFieldMethodImpl(),
                BuiltinScope.BOOLEAN_TYPE,
                new FunctionParameter("field", BuiltinScope.STRING_TYPE, false)));
        addMethod(new BuiltinFunctionSymbol("getFieldTypeName", new RecordGetFieldTypeNameMethodImpl(),
                BuiltinScope.STRING_TYPE,
                new FunctionParameter("field", BuiltinScope.STRING_TYPE, false)));
    }
}
