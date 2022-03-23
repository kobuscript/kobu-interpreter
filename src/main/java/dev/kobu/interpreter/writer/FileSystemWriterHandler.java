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

package dev.kobu.interpreter.writer;

import dev.kobu.interpreter.ast.file.FileOutput;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class FileSystemWriterHandler implements WriterHandler {

    private final String projectDir;

    public FileSystemWriterHandler() {
        projectDir = "";
    }

    public FileSystemWriterHandler(String projectDir) {
        this.projectDir = projectDir;
    }

    @Override
    public String getAbsolutePath(FileOutput file) {
        return String.join(File.separator, file.getPath());
    }

    @Override
    public void write(FileOutput fileOut) {
        String filePath = getAbsolutePath(fileOut);
        File file = new File(filePath);
        if (!file.isAbsolute()) {
            file = new File(projectDir, filePath);
        }
        if (!file.isFile()) {
            file.getParentFile().mkdirs();
        }
        try (FileWriter fileWriter = new FileWriter(file)) {
            fileWriter.write(fileOut.getContent().getValue());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void conflictFileError(List<FileOutput> files) {
        System.out.println("conflict!");
    }

}