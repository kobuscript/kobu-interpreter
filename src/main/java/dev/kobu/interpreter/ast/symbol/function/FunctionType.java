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

import dev.kobu.interpreter.ast.eval.FieldDescriptor;
import dev.kobu.interpreter.ast.eval.ValueExpr;
import dev.kobu.interpreter.ast.symbol.BuiltinScope;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.ast.symbol.Type;
import dev.kobu.interpreter.ast.symbol.generics.TypeAlias;
import dev.kobu.interpreter.ast.symbol.generics.TypeParameter;

import java.util.*;
import java.util.stream.Collectors;

public class FunctionType implements Type {

    private final List<FunctionTypeParameter> parameters;

    private final Type returnType;

    public FunctionType(List<FunctionTypeParameter> parameters, Type returnType) {
        this.parameters = parameters;
        this.returnType = returnType;
    }

    public FunctionType(Type returnType, FunctionTypeParameter... parameters) {
        this.parameters = Arrays.asList(parameters);
        this.returnType = returnType;
    }

    public FunctionType(FunctionTypeParameter... parameters) {
        this.parameters = Arrays.asList(parameters);
        this.returnType = null;
    }

    public List<FunctionTypeParameter> getParameters() {
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
        return "(" + parameters.stream().map(FunctionTypeParameter::getDescription).collect(Collectors.joining(", ")) + ") => " +
                (returnType != null ? returnType.getName() : "void");
    }

    @Override
    public List<FieldDescriptor> getFields() {
        return new ArrayList<>();
    }

    @Override
    public List<NamedFunction> getMethods() {
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
    public NamedFunction resolveMethod(String name) {
        return null;
    }

    @Override
    public boolean isAssignableFrom(Type type) {
        if (type instanceof FunctionType) {
            FunctionType other = (FunctionType) type;
            if (getParameters().size() >= other.getParameters().size()) {
                for (int i = 0; i < getParameters().size(); i++) {
                    FunctionTypeParameter thisParameter = getParameters().get(i);
                    if (i >= other.getParameters().size()) {
                        if (!thisParameter.isOptional()) {
                            return false;
                        }
                        continue;
                    }
                    FunctionTypeParameter otherParameter = other.getParameters().get(i);
                    if (thisParameter.isOptional() && !otherParameter.isOptional()) {
                        return false;
                    }
                    if (!thisParameter.getType().isAssignableFrom(otherParameter.getType())) {
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
        }
        return BuiltinScope.ANY_TYPE;
    }

    @Override
    public Collection<TypeAlias> aliases() {
        Set<TypeAlias> aliases = new HashSet<>();
        for (FunctionTypeParameter parameter : parameters) {
            aliases.addAll(parameter.getType().aliases());
        }
        if (returnType != null) {
            aliases.addAll(returnType.aliases());
        }
        return aliases;
    }

    @Override
    public Type constructFor(Map<String, Type> typeArgs) {
        Type retType = null;
        if (returnType != null) {
            retType = returnType.constructFor(typeArgs);
        }
        return new FunctionType(
                parameters.stream().map(p -> p.constructFor(typeArgs)).collect(Collectors.toList()),
                retType);
    }

    @Override
    public void resolveAliases(Map<String, Type> typeArgs, Type targetType) {
        if (targetType instanceof FunctionType) {
            FunctionType targetFunction = (FunctionType) targetType;
            for (int i = 0; i < parameters.size() && i < targetFunction.parameters.size(); i++) {
                FunctionTypeParameter param = parameters.get(i);
                param.getType().resolveAliases(typeArgs, targetFunction.parameters.get(i).getType());
            }
            if (returnType != null && targetFunction.returnType != null) {
                returnType.resolveAliases(typeArgs, returnType);
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FunctionType that = (FunctionType) o;
        return Objects.equals(parameters, that.parameters) && Objects.equals(returnType, that.returnType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parameters, returnType);
    }

}
