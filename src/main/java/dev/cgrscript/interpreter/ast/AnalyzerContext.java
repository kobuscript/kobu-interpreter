package dev.cgrscript.interpreter.ast;

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

import dev.cgrscript.interpreter.ast.symbol.SourceCodeRef;
import dev.cgrscript.interpreter.error.AnalyzerError;
import dev.cgrscript.interpreter.error.ParserErrorListener;
import dev.cgrscript.interpreter.error.analyzer.CyclicModuleReferenceError;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class AnalyzerContext {

    private AnalyzerErrorScope errorScope;

    private final LinkedHashSet<String> moduleVisitStack = new LinkedHashSet<>();

    private final ParserErrorListener parserErrorListener = new ParserErrorListener();

    public AnalyzerErrorScope getErrorScope() {
        return errorScope;
    }

    public void addModule(SourceCodeRef sourceCodeRef, String moduleId) {
        boolean inserted = moduleVisitStack.add(moduleId);
        if (!inserted && sourceCodeRef != null) {
            errorScope.addError(new CyclicModuleReferenceError(sourceCodeRef, getCyclicPath(moduleId)));
        }
        pushErrorScope();
    }

    public void removeModule(String moduleId) {
        moduleVisitStack.remove(moduleId);
        popErrorScope();
    }

    public ParserErrorListener getParserErrorListener() {
        return parserErrorListener;
    }

    public void pushErrorScope() {
        if (errorScope == null) {
            errorScope = new AnalyzerErrorScope();
        } else {
            errorScope = errorScope.pushScope();
        }
    }

    public void popErrorScope() {
        if (errorScope != null) {
            errorScope = errorScope.popScope();
        }
    }

    public List<AnalyzerError> getModuleErrors() {
        return errorScope.getErrors();
    }

    public List<AnalyzerError> getAllErrors() {
        List<AnalyzerError> errors = new ArrayList<>();
        if (errorScope.getErrors() != null) {
            errors.addAll(errorScope.getErrors());
        }
        if (errorScope.getDependenciesErrors() != null) {
            errors.addAll(errorScope.getDependenciesErrors());
        }
        return errors;
    }

    private List<String> getCyclicPath(String moduleId) {
        List<String> path = new ArrayList<>();
        boolean started = false;
        for (String item : moduleVisitStack) {
            if (!started) {
                if (item.equals(moduleId)) {
                    path.add(item);
                    started = true;
                }
            } else {
                path.add(item);
            }
        }
        path.add(moduleId);
        return path;
    }


}
