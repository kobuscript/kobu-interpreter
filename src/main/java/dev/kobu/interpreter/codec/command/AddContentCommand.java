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

package dev.kobu.interpreter.codec.command;

import dev.kobu.interpreter.ast.eval.context.EvalContext;
import dev.kobu.interpreter.file_system.KobuFileSystem;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class AddContentCommand implements TextFileCommand {

    private final String filePath;

    private final int startIndex;

    private final String content;

    public AddContentCommand(String filePath, int startIndex, String content) {
        this.filePath = filePath;
        this.startIndex = startIndex;
        this.content = content;
    }

    public String getFilePath() {
        return filePath;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public String getContent() {
        return content;
    }

    @Override
    public void run(EvalContext evalContext, KobuFileSystem fileSystem) throws IOException {
        String newContent;
        Path path = Path.of(filePath);
        path = path.toAbsolutePath();
        try (InputStream in = fileSystem.getInputStream(path)) {
            String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            newContent = text.substring(0, startIndex);
            newContent += content;
            newContent += text.substring(startIndex);
        }

        if (evalContext.getCommandOutDir() != null) {
            Path outPath = Path.of(evalContext.getCommandOutDir());
            path = outPath.resolve(path.getRoot().relativize(path));
        }
        fileSystem.writeFileContent(path, newContent, StandardCharsets.UTF_8);
    }
}
