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

import dev.cgrscript.interpreter.ast.eval.SymbolDocumentation;
import dev.cgrscript.interpreter.ast.eval.SymbolTypeEnum;
import dev.cgrscript.interpreter.ast.eval.function.NativeFunction;

import java.util.ArrayList;
import java.util.List;

public class NativeFunctionSymbol extends Symbol implements FunctionType {

    private final NativeFunction functionImpl;

    private List<FunctionParameter> parameters = new ArrayList<>();

    private Type returnType;

    private final SymbolDocumentation documentation;

    public NativeFunctionSymbol(SourceCodeRef sourceCodeRef, ModuleScope moduleScope, String name,
                                NativeFunction functionImpl, String docText) {
        super(moduleScope, sourceCodeRef, name);
        this.functionImpl = functionImpl;
        this.functionImpl.setFuncDef(this);
        this.documentation = new SymbolDocumentation(moduleScope.getModuleId(), SymbolTypeEnum.FUNCTION,
                getName() + getDescription(), docText);
    }

    @Override
    public List<FunctionParameter> getParameters() {
        return parameters;
    }

    @Override
    public Type getReturnType() {
        return returnType;
    }

    public void setParameters(List<FunctionParameter> parameters) {
        this.parameters = parameters;
    }

    public void setReturnType(Type returnType) {
        this.returnType = returnType;
    }

    public NativeFunction getFunctionImpl() {
        return functionImpl;
    }

    @Override
    public SymbolDocumentation getDocumentation() {
        return documentation;
    }
}
