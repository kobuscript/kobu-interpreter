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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

@DisplayName("Integration test - database")
public class DatabaseIntegrationTest extends IntegrationTestBase {

    @Test
    void htmlPage() throws IOException {
        runTest("database/src/HtmlPage.kobu", "database/out/HtmlPage.out");
    }

    @Test
    void csvToHtml() throws IOException {
        runTest("database/src/CsvToHtml.kobu", "database/out/CsvToHtml.out");
    }

    @Test
    void javaToString() throws IOException {
        runTest("database/src/JavaToString.kobu", "database/out/JavaToString.out");
    }

    @Test
    void javaBuilder() throws IOException {
        runTest("database/src/JavaBuilder.kobu", "database/out/JavaBuilder.out");
    }

}
