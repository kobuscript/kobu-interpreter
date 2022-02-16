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

import dev.cgrscript.interpreter.ast.eval.FieldDescriptor;
import dev.cgrscript.interpreter.ast.eval.ValueExpr;
import dev.cgrscript.interpreter.ast.eval.function.pair.PairLeftMethodImpl;
import dev.cgrscript.interpreter.ast.eval.function.pair.PairRightMethodImpl;

import java.util.*;

public class PairType implements Type {

    private final Type leftType;

    private final Type rightType;

    private final Map<String, FunctionType> methods = new HashMap<>();

    public PairType(Type leftType, Type rightType) {
        this.leftType = leftType;
        this.rightType = rightType;
        buildMethods();
    }

    public Type getLeftType() {
        return leftType;
    }

    public Type getRightType() {
        return rightType;
    }

    @Override
    public SourceCodeRef getSourceCodeRef() {
        return null;
    }

    @Override
    public String getName() {
        return "( " + leftType.getName() + ", " + rightType.getName() + " )";
    }

    @Override
    public String getIdentifier() {
        return "PairOf" + leftType.getIdentifier() + "And" + rightType.getIdentifier();
    }

    @Override
    public List<FieldDescriptor> getFields() {
        return new ArrayList<>();
    }

    @Override
    public List<FunctionType> getMethods() {
        return new ArrayList<>(methods.values());
    }

    @Override
    public Type resolveField(String name) {
        return null;
    }

    @Override
    public SourceCodeRef getFieldRef(String name) {
        return null;
    }

    @Override
    public FunctionType resolveMethod(String name) {
        return methods.get(name);
    }

    @Override
    public boolean isAssignableFrom(Type type) {
        if (type instanceof PairType) {
            PairType other = (PairType) type;
            return leftType.getName().equals(other.getLeftType().getName()) &&
                    rightType.getName().equals(other.getRightType().getName());
        }
        return false;
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
        methods.put("left", new BuiltinFunctionSymbol("left", new PairLeftMethodImpl(), leftType));
        methods.put("right", new BuiltinFunctionSymbol("right", new PairRightMethodImpl(), rightType));
    }

}
