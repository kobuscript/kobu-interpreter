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

package dev.kobu.integration;

import dev.kobu.KobuScriptRunner;
import dev.kobu.interpreter.file_system.local.LocalKobuFile;
import dev.kobu.interpreter.file_system.local.LocalKobuFileSystem;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class IntegrationTestBase {

    protected void runTest(String scriptPath, String expectedResultPath, String... args) throws IOException {
        try (InputStream expectedResultIn = getInputStream(expectedResultPath)) {
            var expectedResult = new String(expectedResultIn.readAllBytes(), StandardCharsets.UTF_8);

            var fileSystem = new LocalKobuFileSystem();
            var scriptFile = new LocalKobuFile(new File(getFullPath(scriptPath)));

            var runner = new KobuScriptRunner(fileSystem, scriptFile, Arrays.asList(args));
            var out = new ByteArrayOutputStream();
            var outPrintStream = new PrintStream(out);
            runner.run(outPrintStream, outPrintStream);

            var result = out.toString(StandardCharsets.UTF_8);
            assertEquals(expectedResult, result);
        }
    }

    private InputStream getInputStream(String path) throws IOException {
        return new FileInputStream(getFullPath(path));
    }

    private String getFullPath(String path) {
        File dir = new File("src/test/resources/dev/kobu/integration");
        return new File(dir, path).getAbsolutePath();
    }
}
