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

package dev.kobu.interpreter.file_system;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;

public interface KobuFileSystem {

    String SCRIPT_FILE_EXT = ".kobu";

    String PROJECT_CFG = "kobu.xml";

    KobuFile findProjectDefinition(KobuFileSystemEntry entry);

    KobuFileSystemEntry loadEntry(KobuDirectory parent, String subPath);

    KobuScriptFile loadScript(List<KobuDirectory> srcDirs, String moduleId);

    KobuScriptFile loadScript(List<KobuDirectory> srcDirs, KobuFile file);

    KobuDirectory getParent(KobuFileSystemEntry entry);

    void walkFileTree(KobuDirectory dir, KobuFileVisitor fileWalker);

    boolean isBuiltinFile(KobuFile file);

    InputStream getInputStream(Path filePath) throws IOException;

    void writeFileContent(Path filePath, String content, Charset charset) throws IOException;

}
