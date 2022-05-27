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

import dev.kobu.interpreter.file_system.*;
import dev.kobu.interpreter.file_system.local.LocalKobuDirectory;
import dev.kobu.interpreter.file_system.local.LocalKobuFile;
import dev.kobu.interpreter.file_system.local.LocalKobuScriptFile;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;

public class IntegrationTestFileSystem implements KobuFileSystem {

    @Override
    public KobuFile findProjectDefinition(KobuFileSystemEntry entry) {
        return findProjectRoot(entry);
    }

    @Override
    public KobuFileSystemEntry loadEntry(KobuDirectory parent, String subPath) {
        Path path = Path.of(parent.getAbsolutePath(), subPath);
        //we consider that if a path has an extension, it's a file, otherwise it's a directory.
        if (path.getFileName().toString().contains(".")) {
            return new IntegrationTestFile(path.toAbsolutePath().toString());
        }
        return new IntegrationTestDirectory(path.toAbsolutePath().toString());
    }

    @Override
    public KobuScriptFile loadScript(List<KobuDirectory> srcDirs, String moduleId) {
        String path = moduleId.replace('.', '/') + SCRIPT_FILE_EXT;
        for (KobuDirectory srcDir : srcDirs) {
            String dirPath = srcDir.getAbsolutePath();
            Path scriptPath = Path.of(dirPath, path).toAbsolutePath();
            if (getClass().getClassLoader().getResource(Path.of("/").relativize(scriptPath).toString()) != null) {
                return new IntegrationTestScriptFile(dirPath, scriptPath.toString());
            }
        }
        return null;
    }

    @Override
    public KobuScriptFile loadScript(List<KobuDirectory> srcDirs, KobuFile file) {
        String filePath = file.getAbsolutePath();
        for (KobuDirectory srcDir : srcDirs) {
            String dirPath = srcDir.getAbsolutePath();
            if (filePath.startsWith(dirPath)) {
                return new IntegrationTestScriptFile(dirPath, filePath);
            }
        }
        return null;
    }

    @Override
    public KobuDirectory getParent(KobuFileSystemEntry entry) {
        String path = entry.getAbsolutePath();
        Path parent = Path.of(path).getParent();
        if (parent == null) {
            return null;
        }
        return new IntegrationTestDirectory(parent.toString());
    }

    @Override
    public void walkFileTree(KobuDirectory dir, KobuFileVisitor fileWalker) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isBuiltinFile(KobuFile file) {
        return false;
    }

    @Override
    public InputStream getInputStream(Path filePath) throws IOException {
        return getClass().getClassLoader().getResourceAsStream(Path.of("/").relativize(filePath).toString());
    }

    @Override
    public String loadFileContent(Path filePath, Charset charset) throws IOException {
        try (InputStream in = getInputStream(filePath)) {
            return new String(in.readAllBytes(), charset);
        }
    }

    @Override
    public void writeFileContent(Path filePath, String content, Charset charset) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void appendFileContent(Path filePath, String content, Charset charset) throws IOException {
        throw new UnsupportedOperationException();
    }

    private KobuFile findProjectRoot(KobuFileSystemEntry entry) {
        if (entry instanceof KobuFile) {
            if (entry.getName().equals(PROJECT_CFG)) {
                return (KobuFile) entry;
            }
            return findProjectRoot(getParent(entry));
        } else if (entry instanceof KobuDirectory) {
            var directory = (IntegrationTestDirectory)entry;
            try (ScanResult scanResult = new ClassGraph().acceptPathsNonRecursive(directory.getAbsolutePath()).scan()) {
                var resources = scanResult.getResourcesWithLeafName(PROJECT_CFG);
                if (resources.size() > 0) {
                    return new IntegrationTestFile(Path.of("/", resources.get(0).getPath()).toAbsolutePath().toString());
                }
            }
            return findProjectRoot(getParent(entry));
        }
        return null;
    }


}
