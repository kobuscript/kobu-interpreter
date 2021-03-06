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

package dev.kobu.config;

import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "new", description = "Create a new project")
public class NewCliCommand implements Callable<Integer> {

    @CommandLine.Parameters(index = "0", paramLabel = "PROJECT_NAME", description = "name of the project")
    String projectName;

    @CommandLine.Parameters(index = "1", paramLabel = "VERSION", description = "version of the project", defaultValue = "0.1")
    String version;

    @Override
    public Integer call() throws Exception {

        File dir = new File(projectName);
        if (dir.isFile()) {
            System.err.println("ERROR: " + dir.getAbsolutePath() + " already exists");
            return 1;
        }
        if (dir.isDirectory()) {
            var list = dir.list();
            if (list == null || list.length > 0) {
                System.err.println("ERROR: " + dir.getAbsolutePath() + " already exists");
                return 1;
            }
        }

        if (!dir.mkdirs()) {
            System.err.println("ERROR: Failed to create " + dir.getAbsolutePath());
            return 1;
        }

        String projectFileContents = getProjectFileContents();
        File projectFile = new File(dir, "kobu.xml");
        Files.writeString(projectFile.toPath(), projectFileContents);

        File srcDir = new File(dir, "src");

        if (!srcDir.mkdirs()) {
            System.err.println("ERROR: Failed to create " + srcDir.getAbsolutePath());
            return 1;
        }

        String defaultEntryPointContents = getDefaultEntryPointContents();
        File defaultEntryPointFile = new File(srcDir, "Main.kobu");
        Files.writeString(defaultEntryPointFile.toPath(), defaultEntryPointContents);

        System.out.println("\n Project '" + projectName + "' created\n");

        return 0;
    }

    private String getProjectFileContents() throws IOException {
        try (InputStream in = NewCliCommand.class.getResourceAsStream("/kobu-default.xml")) {
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return content
                    .replaceAll("\\$NAME\\$", projectName)
                    .replaceAll("\\$VERSION\\$", version);
        }
    }

    private String getDefaultEntryPointContents() throws IOException {
        try (InputStream in = NewCliCommand.class.getResourceAsStream("/kobu-default-entry-point")) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
