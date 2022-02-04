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

import dev.cgrscript.interpreter.file_system.CgrDirectory;
import dev.cgrscript.interpreter.file_system.CgrFileSystemEntry;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LocalCgrDirectory implements CgrDirectory {

    private final File dir;

    public LocalCgrDirectory(File dir) {
        this.dir = dir;
    }

    @Override
    public List<CgrFileSystemEntry> list() {
        List<CgrFileSystemEntry> entries = new ArrayList<>();
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    entries.add(new LocalCgrDirectory(file));
                } else {
                    entries.add(new LocalCgrFile(file));
                }
            }
        }
        return entries;
    }

    @Override
    public String getAbsolutePath() {
        return dir.getAbsolutePath();
    }

    @Override
    public String getName() {
        return dir.getName();
    }

    public File getDir() {
        return dir;
    }
}
