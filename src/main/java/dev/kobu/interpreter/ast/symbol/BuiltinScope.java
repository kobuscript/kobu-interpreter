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

import dev.kobu.interpreter.ast.AnalyzerContext;
import dev.kobu.interpreter.ast.eval.context.ContextSnapshot;
import dev.kobu.interpreter.ast.eval.function.global.NewRecordFunctionImpl;
import dev.kobu.interpreter.ast.eval.function.global.PrintFunctionImpl;
import dev.kobu.interpreter.ast.eval.function.global.conf.*;
import dev.kobu.interpreter.ast.eval.function.global.rules.AddRulesFunctionImpl;
import dev.kobu.interpreter.ast.eval.function.global.rules.FireRulesFunctionImpl;
import dev.kobu.interpreter.ast.eval.function.global.rules.InsertFunctionImpl;
import dev.kobu.interpreter.ast.eval.function.global.rules.UpdateFunctionImpl;
import dev.kobu.interpreter.ast.symbol.array.ArrayTypeFactory;
import dev.kobu.interpreter.ast.symbol.function.FunctionParameter;
import dev.kobu.interpreter.ast.symbol.generics.TypeAlias;
import dev.kobu.interpreter.ast.symbol.generics.TypeParameter;
import dev.kobu.interpreter.ast.symbol.value.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BuiltinScope implements Scope {

    public static final String MODULE_ID = "dev.kobu.lang";

    public static final AnyTypeSymbol ANY_TYPE = new AnyTypeSymbol();

    public static final AnyValTypeSymbol ANY_VAL_TYPE = new AnyValTypeSymbol();

    public static final NumberTypeSymbol NUMBER_TYPE = new NumberTypeSymbol();

    public static final StringTypeSymbol STRING_TYPE = new StringTypeSymbol();

    public static final BooleanTypeSymbol BOOLEAN_TYPE = new BooleanTypeSymbol();

    public static final DateTypeSymbol DATE_TYPE = new DateTypeSymbol();

    public static final DateFormatterTypeSymbol DATE_FORMATTER_TYPE = new DateFormatterTypeSymbol();

    public static final AnyRecordTypeSymbol ANY_RECORD_TYPE = new AnyRecordTypeSymbol();

    public static final RuleRefTypeSymbol RULE_REF_TYPE = new RuleRefTypeSymbol();

    public static final RecordTypeRefTypeSymbol RECORD_TYPE_REF_TYPE = new RecordTypeRefTypeSymbol();

    public static final AnyTemplateTypeSymbol ANY_TEMPLATE_TYPE = new AnyTemplateTypeSymbol();

    public static final PathTypeSymbol PATH_TYPE = new PathTypeSymbol();

    public static final FileTypeSymbol FILE_TYPE = new FileTypeSymbol();

    private final Map<String, Symbol> symbols = new HashMap<>();

    private final static TypeParameter TYPE_PARAMETER_A = new TypeParameter("A");
    private final static TypeAlias TYPE_ALIAS_A = new TypeAlias(TYPE_PARAMETER_A);

    public BuiltinScope() {
        buildScope();
    }

    @Override
    public Scope getEnclosingScope() {
        return null;
    }

    @Override
    public void define(AnalyzerContext context, Symbol symbol) {
        throw new UnsupportedOperationException("Can't change builtin scope.");
    }

    @Override
    public Symbol resolve(String name) {
        return symbols.get(name);
    }

    @Override
    public Collection<Symbol> getSymbols() {
        return symbols.values();
    }

    @Override
    public void getSnapshot(ContextSnapshot snapshot) {

    }

    private void buildScope() {

        ANY_TYPE.buildMethods();
        NUMBER_TYPE.buildMethods();
        STRING_TYPE.buildMethods();
        ANY_RECORD_TYPE.buildMethods();
        ANY_TEMPLATE_TYPE.buildMethods();
        DATE_TYPE.buildMethods();
        DATE_FORMATTER_TYPE.buildMethods();
        PATH_TYPE.buildMethods();
        FILE_TYPE.buildMethods();

        symbols.put(ANY_TYPE.getName(), ANY_TYPE);
        symbols.put(ANY_VAL_TYPE.getName(), ANY_VAL_TYPE);
        symbols.put(NUMBER_TYPE.getName(), NUMBER_TYPE);
        symbols.put(STRING_TYPE.getName(), STRING_TYPE);
        symbols.put(BOOLEAN_TYPE.getName(), BOOLEAN_TYPE);
        symbols.put(DATE_TYPE.getName(), DATE_TYPE);
        symbols.put(ANY_RECORD_TYPE.getName(), ANY_RECORD_TYPE);
        symbols.put(RULE_REF_TYPE.getName(), RULE_REF_TYPE);
        symbols.put(RECORD_TYPE_REF_TYPE.getName(), RECORD_TYPE_REF_TYPE);
        symbols.put(ANY_TEMPLATE_TYPE.getName(), ANY_TEMPLATE_TYPE);
        symbols.put(DATE_FORMATTER_TYPE.getName(), DATE_FORMATTER_TYPE);
        symbols.put(PATH_TYPE.getName(), PATH_TYPE);
        symbols.put(FILE_TYPE.getName(), FILE_TYPE);

        var recordArrayType = ArrayTypeFactory.getArrayTypeFor(BuiltinScope.ANY_RECORD_TYPE);

        var envVarFunc = new BuiltinFunctionSymbol("env", new EnvFunctionImpl(), STRING_TYPE,
                new FunctionParameter("var", STRING_TYPE, false),
                new FunctionParameter("default", STRING_TYPE, true));
        var propVarFunc = new BuiltinFunctionSymbol("property", new PropertyFunctionImpl(), STRING_TYPE,
                new FunctionParameter("name", STRING_TYPE, false),
                new FunctionParameter("default", STRING_TYPE, true));
        var confVarFunc = new BuiltinFunctionSymbol("conf", new ConfFunctionImpl(), STRING_TYPE,
                new FunctionParameter("name", STRING_TYPE, false),
                new FunctionParameter("default", STRING_TYPE, true));
        var projectRootDirFunc = new BuiltinFunctionSymbol("projectRootDir", new ProjectRootDirFunctionImpl(),
                PATH_TYPE);
        var mainScriptRootDirFunc = new BuiltinFunctionSymbol("mainScriptDir", new MainScriptDirFunctionImpl(),
                PATH_TYPE);

        var newRecordFunc = new BuiltinFunctionSymbol("newRecord", new NewRecordFunctionImpl(),
                List.of(TYPE_PARAMETER_A), new HashMap<>(),
                TYPE_ALIAS_A,
                new FunctionParameter("type", new ParameterizedRecordTypeRef(TYPE_ALIAS_A), false));

        var addRulesFunc = new BuiltinFunctionSymbol("addRules", new AddRulesFunctionImpl(),
                new FunctionParameter("rules", ArrayTypeFactory.getArrayTypeFor(BuiltinScope.RULE_REF_TYPE), false));
        var insertFunc = new BuiltinFunctionSymbol("insert", new InsertFunctionImpl(),
                new FunctionParameter("value", ANY_RECORD_TYPE, false));
        var updateFunc = new BuiltinFunctionSymbol("update", new UpdateFunctionImpl(),
                new FunctionParameter("value", ANY_RECORD_TYPE, false));
        var fireRulesFunc = new BuiltinFunctionSymbol("fireRules", new FireRulesFunctionImpl(),
                new FunctionParameter("records", recordArrayType, false));

        var printFunc = new BuiltinFunctionSymbol("print", new PrintFunctionImpl(),
                new FunctionParameter("obj", ANY_TYPE, false));

        symbols.put("env", envVarFunc);
        symbols.put("property", propVarFunc);
        symbols.put("conf", confVarFunc);
        symbols.put("projectRootDir", projectRootDirFunc);
        symbols.put("mainScriptRootDir", mainScriptRootDirFunc);
        symbols.put("newRecord", newRecordFunc);
        symbols.put("addRules", addRulesFunc);
        symbols.put("insert", insertFunc);
        symbols.put("update", updateFunc);
        symbols.put("fireRules", fireRulesFunc);
        symbols.put("print", printFunc);

    }
}
