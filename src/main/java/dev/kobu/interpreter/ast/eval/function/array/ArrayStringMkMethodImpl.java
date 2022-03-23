package dev.kobu.interpreter.ast.eval.function.array;

import dev.kobu.interpreter.ast.eval.context.EvalContext;
import dev.kobu.interpreter.ast.eval.ValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.ArrayValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.StringValueExpr;
import dev.kobu.interpreter.ast.eval.expr.value.TemplateValueExpr;
import dev.kobu.interpreter.ast.eval.function.BuiltinMethod;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ArrayStringMkMethodImpl extends BuiltinMethod {

    @Override
    protected ValueExpr run(EvalContext context, ValueExpr object, Map<String, ValueExpr> args, SourceCodeRef sourceCodeRef) {
        ArrayValueExpr arrayValueExpr = (ArrayValueExpr) object;
        StringValueExpr prefixExpr = (StringValueExpr) args.get("prefix");
        StringValueExpr delimiterExpr = (StringValueExpr) args.get("delimiter");
        StringValueExpr suffixExpr = (StringValueExpr) args.get("suffix");

        String prefix = prefixExpr.getValue();
        String delimiter = delimiterExpr.getValue();
        String suffix = suffixExpr.getValue();

        if (arrayValueExpr.getValue().isEmpty()) {
            return new StringValueExpr("");
        }

        List<String> values = arrayValueExpr.getValue()
                .stream()
                .map(expr -> {
                    if (expr instanceof StringValueExpr) {
                        return ((StringValueExpr) expr).getValue();
                    }
                    return ((TemplateValueExpr)expr).getValue();
                })
                .collect(Collectors.toList());

        return new StringValueExpr(prefix + String.join(delimiter, values) + suffix);

    }

    @Override
    public String getDocumentation() {
        return "";
    }

}