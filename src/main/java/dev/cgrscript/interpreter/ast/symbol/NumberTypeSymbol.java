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

package dev.cgrscript.interpreter.ast.symbol;

import dev.cgrscript.interpreter.ast.eval.expr.value.NumberValueExpr;
import dev.cgrscript.interpreter.ast.eval.ValueExpr;
import dev.cgrscript.interpreter.ast.eval.function.number.*;

import java.util.Comparator;

public class NumberTypeSymbol extends BuiltinTypeSymbol {

    private static final String TYPE_NAME = "number";

    public NumberTypeSymbol() {
        super(TYPE_NAME);
        buildMethods();
    }

    @Override
    public String getIdentifier() {
        return "Number";
    }

    @Override
    public boolean isAssignableFrom(Type type) {
        return type instanceof NumberTypeSymbol;
    }

    @Override
    public Type getCommonSuperType(Type type) {
        return isAssignableFrom(type) ? this : BuiltinScope.ANY_TYPE;
    }

    @Override
    public Comparator<ValueExpr> getComparator() {
        return Comparator.comparingDouble(o -> ((NumberValueExpr) o).toDouble());
    }

    private void buildMethods() {
        addMethod(new BuiltinFunctionSymbol(this,"abs", new AbsMethodImpl(), this));
        addMethod(new BuiltinFunctionSymbol(this,"round", new RoundMethodImpl(), this));
        addMethod(new BuiltinFunctionSymbol(this,"floor", new FloorMethodImpl(), this));
        addMethod(new BuiltinFunctionSymbol(this,"ceil", new CeilMethodImpl(), this));
        addMethod(new BuiltinFunctionSymbol(this,"pow", new PowMethodImpl(), this,
                new FunctionParameter("exp", this, false)));
    }
}
