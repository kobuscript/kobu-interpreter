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

package dev.cgrscript.interpreter.ast.eval.function.array;

import dev.cgrscript.interpreter.ast.eval.context.EvalContext;
import dev.cgrscript.interpreter.ast.eval.ValueExpr;
import dev.cgrscript.interpreter.ast.eval.expr.value.ArrayValueExpr;
import dev.cgrscript.interpreter.ast.eval.expr.value.PairValueExpr;
import dev.cgrscript.interpreter.ast.eval.expr.value.RecordValueExpr;
import dev.cgrscript.interpreter.ast.eval.function.BuiltinMethod;
import dev.cgrscript.interpreter.ast.symbol.PairType;
import dev.cgrscript.interpreter.ast.symbol.RecordTypeSymbol;
import dev.cgrscript.interpreter.ast.symbol.SourceCodeRef;
import dev.cgrscript.interpreter.ast.symbol.Type;

import java.util.Map;

public class ArraySortMethodImpl extends BuiltinMethod {

    private String field;

    public ArraySortMethodImpl() {
    }

    public ArraySortMethodImpl(String field) {
        this.field = field;
    }

    @Override
    protected ValueExpr run(EvalContext context, ValueExpr object, Map<String, ValueExpr> args, SourceCodeRef sourceCodeRef) {

        ArrayValueExpr arrayExpr = (ArrayValueExpr) object;

        Type elementType = arrayExpr.getType().getElementType();
        if (field == null) {
            arrayExpr.getValue().sort(elementType.getComparator());
        } else if (elementType instanceof RecordTypeSymbol) {
            arrayExpr.getValue().sort((o1, o2) -> {
                RecordValueExpr r1 = (RecordValueExpr) o1;
                RecordValueExpr r2 = (RecordValueExpr) o2;
                ValueExpr v1 = r1.resolveField(field);
                ValueExpr v2 = r2.resolveField(field);
                var fieldType = r1.getType().resolveField(field);

                if (v1 == null && v2 == null) {
                    return 0;
                }
                if (v1 == null) {
                    return 1;
                }
                if (v2 == null) {
                    return -1;
                }

                return fieldType.getComparator().compare(v1, v2);
            });
        } else if (elementType instanceof PairType) {
            arrayExpr.getValue().sort((o1, o2) -> {
                PairValueExpr p1 = (PairValueExpr) o1;
                PairValueExpr p2 = (PairValueExpr) o2;

                Type type;
                ValueExpr v1;
                ValueExpr v2;

                if ("left".equals(field)) {
                    type = ((PairType)p1.getType()).getLeftType();
                    v1 = p1.getLeftValue();
                    v2 = p2.getLeftValue();
                } else {
                    type = ((PairType)p1.getType()).getRightType();
                    v1 = p1.getRightValue();
                    v2 = p2.getRightValue();
                }

                return type.getComparator().compare(v1, v2);

            });
        }

        return null;
    }

    @Override
    public String getDocumentation() {
        return "";
    }

}
