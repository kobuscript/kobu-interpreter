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

package dev.kobu.integration.file_system;

import dev.kobu.interpreter.file_system.KobuFileSystem;
import dev.kobu.interpreter.file_system.KobuScriptFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class IntegrationTestScriptFile implements KobuScriptFile {

    private final String srcDir;

    private final String absolutePath;

    public IntegrationTestScriptFile(String srcDir, String absolutePath) {
        this.srcDir = srcDir;
        this.absolutePath = absolutePath;
    }

    @Override
    public InputStream newInputStream() throws IOException {
        return getClass().getClassLoader().getResourceAsStream(Path.of("/").relativize(Path.of(absolutePath)).toString());
    }

    @Override
    public String getAbsolutePath() {
        return absolutePath;
    }

    @Override
    public String getName() {
        return Path.of(absolutePath).getFileName().toString();
    }

    @Override
    public String extractModuleId() {
        Path modulePath = null;
        if (srcDir != null) {
            var rootPath = Path.of(srcDir);
            var filePath = Path.of(absolutePath);
            modulePath = rootPath.relativize(filePath);
        } else {
            modulePath = Path.of(absolutePath);
        }

        List<String> segments = new ArrayList<>();
        for (int i = 0; i < modulePath.getNameCount() - 1; i++) {
            segments.add(modulePath.getName(i).toString());
        }
        segments.add(modulePath.getFileName().toString().replace(KobuFileSystem.SCRIPT_FILE_EXT, ""));

        return String.join(".", segments);
    }

}
