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

import dev.kobu.interpreter.ast.eval.FieldDescriptor;
import dev.kobu.interpreter.ast.eval.ValueExpr;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class FunctionType implements Type {

    private final List<Type> parameters;

    private final Type returnType;

    public FunctionType(List<Type> parameters, Type returnType) {
        this.parameters = parameters;
        this.returnType = returnType;
    }

    public List<Type> getParameters() {
        return parameters;
    }

    public Type getReturnType() {
        return returnType;
    }

    @Override
    public SourceCodeRef getSourceCodeRef() {
        return null;
    }

    @Override
    public String getName() {
        return "(" + parameters.stream().map(Type::getName).collect(Collectors.joining(", ")) + ") => " +
                (returnType != null ? returnType.getName() : "void");
    }

    @Override
    public String getIdentifier() {
        return null;
    }

    @Override
    public List<FieldDescriptor> getFields() {
        return new ArrayList<>();
    }

    @Override
    public List<FunctionDefinition> getMethods() {
        return new ArrayList<>();
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
    public FunctionDefinition resolveMethod(String name) {
        return null;
    }

    @Override
    public boolean isAssignableFrom(Type type) {
        if (type instanceof FunctionType) {
            FunctionType other = (FunctionType) type;
            if (other.getParameters().size() == getParameters().size()) {
                for (int i = 0; i < other.getParameters().size(); i++) {
                    if (!getParameters().get(i).isAssignableFrom(other.getParameters().get(i))) {
                        return false;
                    }
                }
                if (getReturnType() == null) {
                    return other.getReturnType() == null;
                }
                if (other.getReturnType() == null) {
                    return false;
                }
                return getReturnType().isAssignableFrom(other.getReturnType());
            }
        }
        return false;
    }

    @Override
    public Type getCommonSuperType(Type type) {
        if (isAssignableFrom(type)) {
            return this;
        } else if (type instanceof FunctionType) {
            FunctionType other = (FunctionType) type;
            if (getParameters().size() == other.getParameters().size()) {
                List<Type> params = new ArrayList<>();
                for (int i = 0; i < getParameters().size(); i++) {
                    params.add(getParameters().get(i).getCommonSuperType(other.getParameters().get(i)));
                }

                if ((getReturnType() != null && other.getReturnType() == null)
                        || (getReturnType() == null && other.getReturnType() != null)) {
                    return BuiltinScope.ANY_TYPE;
                } else if (getReturnType() == null && other.getReturnType() == null) {
                    return new FunctionType(params, null);
                }
                return new FunctionType(params, getReturnType().getCommonSuperType(other.getReturnType()));
            }
        }

        return BuiltinScope.ANY_TYPE;
    }

    @Override
    public Comparator<ValueExpr> getComparator() {
        return null;
    }
}
