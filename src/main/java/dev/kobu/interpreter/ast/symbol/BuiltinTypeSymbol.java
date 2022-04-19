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
import dev.kobu.interpreter.ast.symbol.function.NamedFunction;
import dev.kobu.interpreter.ast.symbol.generics.TypeAlias;

import java.util.*;

public abstract class BuiltinTypeSymbol extends Symbol implements Type {

    private final Map<String, NamedFunction> methods = new HashMap<>();

    private final Map<String, Type> fields = new HashMap<>();

    public BuiltinTypeSymbol(String name) {
        super(null, null, name, false);
    }

    @Override
    public Type resolveField(String name) {
        return fields.get(name);
    }

    @Override
    public SourceCodeRef getFieldRef(String name) {
        return null;
    }

    @Override
    public List<FieldDescriptor> getFields() {
        List<FieldDescriptor> fields = new ArrayList<>();
        this.fields.forEach((k, v) -> fields.add(new FieldDescriptor(k, v.getName())));
        return fields;
    }

    @Override
    public List<NamedFunction> getMethods() {
        var methodList = new ArrayList<>(methods.values());
        if (!(this instanceof AnyTypeSymbol)) {
            methodList.addAll(BuiltinScope.ANY_TYPE.getMethods());
        }
        return methodList;
    }

    @Override
    public NamedFunction resolveMethod(String name) {
        var method = methods.get(name);
        if (method == null && !(this instanceof AnyTypeSymbol)) {
            method = BuiltinScope.ANY_TYPE.resolveMethod(name);
        }
        return method;
    }

    @Override
    public Collection<TypeAlias> aliases() {
        return List.of();
    }

    @Override
    public Type constructFor(Map<String, Type> typeArgs) {
        return this;
    }

    @Override
    public void resolveAliases(Map<String, Type> typeArgs, Type targetType) {

    }

    protected void addMethod(BuiltinFunctionSymbol method) {
        methods.put(method.getName(), method);
    }

    protected void addField(String fieldName, Type type) {
        fields.put(fieldName, type);
    }

}
