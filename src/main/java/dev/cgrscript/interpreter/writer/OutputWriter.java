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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OutputWriter {

    private final OutputWriterModeEnum mode;

    private final OutputWriterLogTypeEnum logType;

    private final FileLogger fileLogger = new FileLogger();

    private final WriterHandler writerHandler;

    private final Map<String, List<FileOutput>> filesMap = new HashMap<>();

    public OutputWriter(OutputWriterModeEnum mode, OutputWriterLogTypeEnum logType, WriterHandler writerHandler) {
        this.mode = mode;
        this.logType = logType;
        this.writerHandler = writerHandler;
    }

    public void addFile(FileOutput file) {
        String absolutePath = writerHandler.getAbsolutePath(file);
        List<FileOutput> files = filesMap.computeIfAbsent(absolutePath, k -> new ArrayList<>());
        files.add(file);
    }

    public void write() {
        filesMap.values().stream().filter(fs -> fs.size() > 1).forEach(writerHandler::conflictFileError);
        filesMap.values().stream().filter(fs -> fs.size() == 1).map(fs -> fs.get(0)).forEach(this::write);
        fileLogger.log(logType, writerHandler);
    }

    private void write(FileOutput fileOutput) {
        fileLogger.addFile(fileOutput);

        if (mode == OutputWriterModeEnum.WRITE_TO_DISK) {
            writerHandler.write(fileOutput);
        }
    }

}
