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

package dev.cgrscript.interpreter.file_system.local;

import dev.cgrscript.interpreter.file_system.CgrFileSystem;
import dev.cgrscript.interpreter.file_system.CgrScriptFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class LocalCgrScriptFile implements CgrScriptFile {

    private final File srcDir;

    private final File file;

    protected LocalCgrScriptFile(File srcDir, File file) {
        this.srcDir = srcDir;
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    public File getSrcDir() {
        return srcDir;
    }

    @Override
    public String getAbsolutePath() {
        return file.getAbsolutePath();
    }

    @Override
    public String getName() {
        return file.getName();
    }

    @Override
    public String extractModuleId() {
        Path modulePath = null;
        if (srcDir != null) {
            var rootPath = srcDir.toPath();
            var filePath = file.toPath();
            modulePath = rootPath.relativize(filePath);
        } else {
            modulePath = file.toPath();
        }

        List<String> segments = new ArrayList<>();
        for (int i = 0; i < modulePath.getNameCount() - 1; i++) {
            segments.add(modulePath.getName(i).toString());
        }
        segments.add(modulePath.getFileName().toString().replace(CgrFileSystem.SCRIPT_FILE_EXT, ""));

        return String.join(".", segments);
    }

    @Override
    public InputStream newInputStream() throws IOException {
        return new FileInputStream(file);
    }
}
