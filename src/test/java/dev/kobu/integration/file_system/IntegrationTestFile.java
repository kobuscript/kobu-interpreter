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

import dev.kobu.interpreter.file_system.KobuFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public class IntegrationTestFile implements KobuFile {

    private final String absolutePath;

    public IntegrationTestFile(String absolutePath) {
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

}
