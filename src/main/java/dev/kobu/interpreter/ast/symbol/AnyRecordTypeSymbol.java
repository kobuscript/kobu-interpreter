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

import dev.kobu.interpreter.ast.eval.function.record.*;
import dev.kobu.interpreter.ast.symbol.array.ArrayTypeFactory;
import dev.kobu.interpreter.ast.symbol.function.FunctionParameter;
import dev.kobu.interpreter.ast.symbol.generics.TypeAlias;
import dev.kobu.interpreter.ast.symbol.generics.TypeParameter;
import dev.kobu.interpreter.ast.symbol.tuple.TupleTypeElement;
import dev.kobu.interpreter.ast.symbol.tuple.TupleTypeFactory;

import java.util.HashMap;
import java.util.List;

public class AnyRecordTypeSymbol extends BuiltinTypeSymbol {

    private static final String ANY_RECORD_TYPE = "AnyRecord";

    private final static TypeParameter TYPE_PARAMETER_A = new TypeParameter("A");
    private final static TypeAlias TYPE_ALIAS_A = new TypeAlias(TYPE_PARAMETER_A);

    public AnyRecordTypeSymbol() {
        super(ANY_RECORD_TYPE);
    }

    @Override
    public boolean isAssignableFrom(Type type) {
        return type instanceof RecordTypeSymbol || type instanceof AnyRecordTypeSymbol;
    }

    @Override
    public Type getCommonSuperType(Type type) {
        return isAssignableFrom(type) ? this : BuiltinScope.ANY_TYPE;
    }

    public void buildMethods() {
        addMethod(new BuiltinFunctionSymbol(this, "put", new RecordPutMethodImpl(),
                new FunctionParameter("attr", BuiltinScope.STRING_TYPE, false),
                new FunctionParameter("value", BuiltinScope.ANY_TYPE, false)));
        addMethod(new BuiltinFunctionSymbol(this, "get", new RecordGetMethodImpl(),
                BuiltinScope.ANY_TYPE,
                new FunctionParameter("attr", BuiltinScope.STRING_TYPE, false)));
        addMethod(new BuiltinFunctionSymbol(this, "hasAttribute", new RecordHasAttributeMethodImpl(),
                BuiltinScope.BOOLEAN_TYPE,
                new FunctionParameter("attr", BuiltinScope.STRING_TYPE, false)));
        addMethod(new BuiltinFunctionSymbol(this, "getAttributes", new RecordGetAttributesMethodImpl(),
                ArrayTypeFactory.getArrayTypeFor(BuiltinScope.STRING_TYPE)));
        addMethod(new BuiltinFunctionSymbol(this, "values", new RecordValuesMethodImpl(),
                List.of(TYPE_PARAMETER_A),
                new HashMap<>(),
                ArrayTypeFactory.getArrayTypeFor(TYPE_ALIAS_A),
                new FunctionParameter("valueType", new ParameterizedRecordTypeRef(TYPE_ALIAS_A), true)));

        TupleTypeElement tupleTypeElement = new TupleTypeElement(BuiltinScope.STRING_TYPE);
        tupleTypeElement.setNext(new TupleTypeElement(BuiltinScope.ANY_TYPE));
        var entryType = TupleTypeFactory.getTupleTypeFor(tupleTypeElement);
        var entriesType = ArrayTypeFactory.getArrayTypeFor(entryType);
        addMethod(new BuiltinFunctionSymbol(this, "getEntries", new RecordEntriesMethodImpl(), entriesType));
    }
}
