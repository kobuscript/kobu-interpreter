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

package dev.kobu.interpreter.file_system.local;

import dev.kobu.interpreter.file_system.*;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

public class LocalKobuFileSystem implements KobuFileSystem {

    @Override
    public KobuFile findProjectDefinition(KobuFile file) {
        return findProjectRoot(file);
    }

    @Override
    public KobuFileSystemEntry loadEntry(KobuDirectory parent, String subPath) {
        File dir = ((LocalKobuDirectory)parent).getDir();
        File file = new File(dir, subPath);
        if (file.isDirectory()) {
            return new LocalKobuDirectory(file);
        }
        return new LocalKobuFile(file);
    }

    @Override
    public KobuScriptFile loadScript(List<KobuDirectory> srcDirs, String moduleId) {
        String path = moduleId.replace('.', '/') + SCRIPT_FILE_EXT;
        for (KobuDirectory srcDir : srcDirs) {
            File dir = ((LocalKobuDirectory)srcDir).getDir();
            File scriptFile = new File(dir, path);
            if (scriptFile.isFile()) {
                return new LocalKobuScriptFile(dir, scriptFile);
            }
        }
        return null;
    }

    @Override
    public KobuScriptFile loadScript(List<KobuDirectory> srcDirs, KobuFile file) {
        File localFile = ((LocalKobuFile) file).getFile();
        Path path = localFile.toPath();
        for (KobuDirectory srcDir : srcDirs) {
            File localDir = ((LocalKobuDirectory) srcDir).getDir();
            if (path.startsWith(localDir.toPath())) {
                return new LocalKobuScriptFile(localDir, localFile);
            }
        }
        return null;
    }

    @Override
    public KobuDirectory getParent(KobuFileSystemEntry entry) {
        File file;
        if (entry instanceof LocalKobuFile) {
            file = ((LocalKobuFile)entry).getFile();
        } else {
            file = ((LocalKobuDirectory)entry).getDir();
        }
        File dir = file.getParentFile();
        if (dir == null) {
            return null;
        }
        return new LocalKobuDirectory(dir);
    }

    @Override
    public void walkFileTree(KobuDirectory dir, KobuFileVisitor visitor) {
        try {
            Files.walkFileTree(((LocalKobuDirectory)dir).getDir().toPath(), new SimpleFileVisitor<>() {

                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
                    var file = path.toFile();
                    if (file.isFile()) {
                        if (file.getName().endsWith(SCRIPT_FILE_EXT)) {
                            visitor.visit(new LocalKobuScriptFile(((LocalKobuDirectory)dir).getDir(), file));
                        } else {
                            visitor.visit(new LocalKobuFile(file));
                        }
                    } else if (file.isDirectory()) {
                        visitor.visit(new LocalKobuDirectory(file));
                    }
                    return FileVisitResult.CONTINUE;
                }

            });
        } catch (IOException e) {
            throw new KobuFileSystemException(e);
        }
    }

    @Override
    public boolean isBuiltinFile(KobuFile file) {
        return false;
    }

    @Override
    public InputStream getInputStream(Path filePath) throws IOException {
        return new FileInputStream(filePath.toFile());
    }

    @Override
    public String loadFileContent(Path filePath, Charset charset) throws IOException {
        return Files.readString(filePath, charset);
    }

    @Override
    public void writeFileContent(Path filePath, String content, Charset charset) throws IOException {
        File file = filePath.toFile();
        File parentFile = file.getParentFile();
        parentFile.mkdirs();
        try (FileWriter fileWriter = new FileWriter(file, charset)) {
            fileWriter.write(content);
        }
    }

    @Override
    public void appendFileContent(Path filePath, String content, Charset charset) throws IOException {
        try (FileWriter fileWriter = new FileWriter(filePath.toFile(), charset)) {
            fileWriter.append(content);
        }
    }

    private KobuFile findProjectRoot(KobuFileSystemEntry entry) {
        if (entry instanceof KobuFile) {
            if (entry.getName().equals(PROJECT_CFG)) {
                return (KobuFile) entry;
            }
            return findProjectRoot(getParent(entry));
        } else if (entry instanceof KobuDirectory) {
            File[] files = ((LocalKobuDirectory) entry).getDir().listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().equals(PROJECT_CFG)) {
                        return new LocalKobuFile(file);
                    }
                }
            }
            return findProjectRoot(getParent(entry));
        }
        return null;
    }

}
