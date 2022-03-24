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

import dev.kobu.interpreter.ast.eval.SymbolDocumentation;
import dev.kobu.interpreter.ast.eval.SymbolTypeEnum;
import dev.kobu.interpreter.ast.eval.function.BuiltinFunction;
import dev.kobu.interpreter.ast.symbol.function.FunctionParameter;
import dev.kobu.interpreter.ast.symbol.function.FunctionType;
import dev.kobu.interpreter.ast.symbol.function.NamedFunction;
import dev.kobu.interpreter.ast.symbol.generics.TypeParameter;

import java.util.*;
import java.util.stream.Collectors;

public class BuiltinFunctionSymbol extends Symbol implements NamedFunction {

    private final Type enclosingType;

    private final BuiltinFunction functionImpl;

    private final List<FunctionParameter> parameters = new ArrayList<>();

    private Type returnType;

    private SymbolDocumentation symbolDocumentation;

    private FunctionType type;

    private final Map<String, Type> typeArgsMap = new HashMap<>();

    public BuiltinFunctionSymbol(Type enclosingType, String name, BuiltinFunction functionImpl,
                                 Type returnType, FunctionParameter... args) {
        super(null,null, name);
        this.enclosingType = enclosingType;
        this.functionImpl = functionImpl;
        this.functionImpl.setFuncDef(this);
        this.returnType = returnType;
        this.parameters.addAll(Arrays.asList(args));
        buildType();
    }

    public BuiltinFunctionSymbol(Type enclosingType, String name, BuiltinFunction functionImpl,
                                 FunctionParameter... args) {
        this(enclosingType, name, functionImpl, null, args);
    }

    public BuiltinFunctionSymbol(String name, BuiltinFunction functionImpl) {
        this(null, name, functionImpl);
    }

    public BuiltinFunctionSymbol(String name, BuiltinFunction functionImpl,
                                 Type returnType) {
        this(null, name, functionImpl, returnType);
    }

    public BuiltinFunctionSymbol(String name, BuiltinFunction functionImpl,
                                 Type returnType, FunctionParameter... args) {
        this(null, name, functionImpl, returnType, args);
    }

    public BuiltinFunctionSymbol(String name, BuiltinFunction functionImpl,
                                 FunctionParameter... args) {
        this(null, name, functionImpl, args);
    }

    @Override
    public Map<String, Type> providedTypeArguments() {
        return typeArgsMap;
    }

    @Override
    public List<FunctionParameter> getParameters() {
        return parameters;
    }

    @Override
    public Type getReturnType() {
        return returnType;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public SymbolDocumentation getDocumentation() {
        if (symbolDocumentation == null) {
            String description = getName() + functionImpl.getFuncDef().getDescription();
            if (enclosingType != null) {
                this.symbolDocumentation = new SymbolDocumentation(BuiltinScope.MODULE_ID, SymbolTypeEnum.FUNCTION,
                        description, functionImpl.getDocumentation(), enclosingType.getName());
            } else {
                this.symbolDocumentation = new SymbolDocumentation(BuiltinScope.MODULE_ID, SymbolTypeEnum.FUNCTION,
                        description, functionImpl.getDocumentation());
            }
        }
        return symbolDocumentation;
    }

    public void setProvidedTypeArg(String typeAlias, Type type) {
        typeArgsMap.put(typeAlias, type);
    }

    public void setReturnType(Type returnType) {
        this.returnType = returnType;
    }

    public BuiltinFunction getFunctionImpl() {
        return functionImpl;
    }

    private void buildType() {
        this.type = new FunctionType(
                parameters.stream().map(FunctionParameter::toFunctionTypeParameter).collect(Collectors.toList()),
                returnType);
    }

}
