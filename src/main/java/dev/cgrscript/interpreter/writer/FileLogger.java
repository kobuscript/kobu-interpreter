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

package dev.cgrscript.interpreter.writer;

import dev.cgrscript.interpreter.ast.file.FileOutput;

import java.util.ArrayList;
import java.util.List;

public class FileLogger {

    private final List<FileOutput> files = new ArrayList<>();

    public void addFile(FileOutput fileOutput) {
        files.add(fileOutput);
    }

    public void log(OutputWriterLogTypeEnum logTypeEnum, WriterHandler writerHandler) {
        System.out.println("--------------------");
        System.out.println("Total files: " + files.size());
        System.out.println("--------------------");
        for (FileOutput file : files) {
            System.out.println(writerHandler.getAbsolutePath(file));
            if (logTypeEnum == OutputWriterLogTypeEnum.VERBOSE) {
                System.out.println();
                System.out.println(file.getContent().getValue());
                System.out.println("--------------------");
                System.out.println();
            }
        }
    }

}
