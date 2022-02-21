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

import dev.cgrscript.interpreter.file_system.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

public class LocalCgrFileSystem implements CgrFileSystem {

    @Override
    public CgrFile findProjectDefinition(CgrFile file) {
        return findProjectRoot(file);
    }

    @Override
    public CgrFileSystemEntry loadEntry(CgrDirectory parent, String subPath) {
        File dir = ((LocalCgrDirectory)parent).getDir();
        File file = new File(dir, subPath);
        if (file.isDirectory()) {
            return new LocalCgrDirectory(file);
        }
        return new LocalCgrFile(file);
    }

    @Override
    public CgrScriptFile loadScript(List<CgrDirectory> srcDirs, String moduleId) {
        String path = moduleId.replace('.', '/') + SCRIPT_FILE_EXT;
        for (CgrDirectory srcDir : srcDirs) {
            File dir = ((LocalCgrDirectory)srcDir).getDir();
            File scriptFile = new File(dir, path);
            if (scriptFile.isFile()) {
                return new LocalCgrScriptFile(dir, scriptFile);
            }
        }
        return null;
    }

    @Override
    public CgrScriptFile loadScript(List<CgrDirectory> srcDirs, CgrFile file) {
        File localFile = ((LocalCgrFile) file).getFile();
        Path path = localFile.toPath();
        for (CgrDirectory srcDir : srcDirs) {
            File localDir = ((LocalCgrDirectory) srcDir).getDir();
            if (path.startsWith(localDir.toPath())) {
                return new LocalCgrScriptFile(localDir, localFile);
            }
        }
        return null;
    }

    @Override
    public CgrDirectory getParent(CgrFileSystemEntry entry) {
        File file;
        if (entry instanceof LocalCgrFile) {
            file = ((LocalCgrFile)entry).getFile();
        } else {
            file = ((LocalCgrDirectory)entry).getDir();
        }
        File dir = file.getParentFile();
        if (dir == null) {
            return null;
        }
        return new LocalCgrDirectory(dir);
    }

    @Override
    public void walkFileTree(CgrDirectory dir, CgrFileVisitor visitor) {
        try {
            Files.walkFileTree(((LocalCgrDirectory)dir).getDir().toPath(), new SimpleFileVisitor<>() {

                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
                    var file = path.toFile();
                    if (file.isFile()) {
                        if (file.getName().endsWith(SCRIPT_FILE_EXT)) {
                            visitor.visit(new LocalCgrScriptFile(((LocalCgrDirectory)dir).getDir(), file));
                        } else {
                            visitor.visit(new LocalCgrFile(file));
                        }
                    } else if (file.isDirectory()) {
                        visitor.visit(new LocalCgrDirectory(file));
                    }
                    return FileVisitResult.CONTINUE;
                }

            });
        } catch (IOException e) {
            throw new CgrFileSystemException(e);
        }
    }

    private CgrFile findProjectRoot(CgrFileSystemEntry entry) {
        if (entry instanceof CgrFile) {
            if (entry.getName().equals(PROJECT_CFG)) {
                return (CgrFile) entry;
            }
            return findProjectRoot(getParent(entry));
        } else if (entry instanceof CgrDirectory) {
            File[] files = ((LocalCgrDirectory) entry).getDir().listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().equals(PROJECT_CFG)) {
                        return new LocalCgrFile(file);
                    }
                }
            }
            return findProjectRoot(getParent(entry));
        }
        return null;
    }

}
