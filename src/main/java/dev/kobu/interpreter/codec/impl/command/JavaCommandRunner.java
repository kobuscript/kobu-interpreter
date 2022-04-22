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

package dev.kobu.interpreter.codec.impl.command;

import dev.kobu.interpreter.ast.eval.ValueExpr;
import dev.kobu.interpreter.ast.eval.context.EvalContext;
import dev.kobu.interpreter.ast.eval.expr.value.RecordValueExpr;
import dev.kobu.interpreter.ast.symbol.ModuleScope;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.ast.symbol.Type;
import dev.kobu.interpreter.codec.command.TextFileCommand;
import dev.kobu.interpreter.codec.command.TextFileCommandRunner;
import dev.kobu.interpreter.error.eval.IllegalArgumentError;
import dev.kobu.interpreter.file_system.KobuFileSystem;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class JavaCommandRunner implements TextFileCommandRunner {

    private static final String JAVA_ADD_IMPORT = "JavaAddImport";

    private static final String JAVA_ADD_OR_REPLACE_METHOD = "JavaAddOrReplaceMethod";

    private static final String JAVA_ADD_OR_REPLACE_CONSTRUCTOR = "JavaAddOrReplaceConstructor";

    private static final String JAVA_ADD_OR_REPLACE_FIELD = "JavaAddOrReplaceField";

    private static final String JAVA_ADD_OR_REPLACE_INNER_DEFINITION = "JavaAddOrReplaceInnerDefinition";

    @Override
    public void runCommand(ModuleScope moduleScope, EvalContext context, String filePath, InputStream in,
                           RecordValueExpr commandRec, Map<String, ValueExpr> args, SourceCodeRef sourceCodeRef) throws IOException {

        List<TextFileCommand> commands;

        if (commandRec.getType().isAssignableFrom((Type) moduleScope.resolve(JAVA_ADD_IMPORT))) {
            commands = new JavaAddImportCommandProducer().produce(in, commandRec);
        } else if (commandRec.getType().isAssignableFrom((Type) moduleScope.resolve(JAVA_ADD_OR_REPLACE_METHOD))) {
            commands = new JavaAddOrReplaceMethodCommandProducer().produce(in, commandRec);
        } else if (commandRec.getType().isAssignableFrom((Type) moduleScope.resolve(JAVA_ADD_OR_REPLACE_CONSTRUCTOR))) {
            commands = new JavaAddOrReplaceConstructorCommandProducer().produce(in, commandRec);
        } else if (commandRec.getType().isAssignableFrom((Type) moduleScope.resolve(JAVA_ADD_OR_REPLACE_FIELD))) {
            commands = new JavaAddOrReplaceFieldCommandProducer().produce(in, commandRec);
        } else if (commandRec.getType().isAssignableFrom((Type) moduleScope.resolve(JAVA_ADD_OR_REPLACE_INNER_DEFINITION))) {
            commands = new JavaAddOrReplaceInnerDefinitionCommandProducer().produce(in, commandRec);
        } else {
            throw new IllegalArgumentError("invalid command type: " + commandRec.getType().getName(), sourceCodeRef);
        }

        KobuFileSystem fileSystem = context.getFileSystem();;
        for (TextFileCommand command : commands) {
            command.run(fileSystem);
        }

    }
}
