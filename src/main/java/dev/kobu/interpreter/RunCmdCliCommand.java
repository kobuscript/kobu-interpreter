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

package dev.kobu.interpreter;

import dev.kobu.KobuScriptRunner;
import dev.kobu.config.Project;
import dev.kobu.config.ProjectCommand;
import dev.kobu.config.ProjectReader;
import dev.kobu.interpreter.file_system.KobuFile;
import dev.kobu.interpreter.file_system.local.LocalKobuDirectory;
import dev.kobu.interpreter.file_system.local.LocalKobuFile;
import dev.kobu.interpreter.file_system.local.LocalKobuFileSystem;
import picocli.CommandLine;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "cmd", description = "Run a Kobu command")
public class RunCmdCliCommand implements Callable<Integer> {

    @CommandLine.Option(names = {"-o", "--outDir"}, paramLabel = "OUT_DIR", description = "Write the updated files to OUT_DIR instead of overwriting")
    String outDir;

    @CommandLine.Parameters(index = "0", paramLabel = "CMD", description = "Command to run")
    String cmd;

    @CommandLine.Parameters(index = "1..*", paramLabel = "ARG", description = "Script arguments")
    List<String> scriptArgs;

    @Override
    public Integer call() throws Exception {
        LocalKobuFileSystem fileSystem = new LocalKobuFileSystem();
        LocalKobuDirectory currentDir = new LocalKobuDirectory(Path.of("").toAbsolutePath().toFile());
        KobuFile projectFile = fileSystem.findProjectDefinition(currentDir);
        if (projectFile == null) {
            System.err.println("ERROR: Project not found for the current dir");
            return 1;
        }
        ProjectReader projectReader = new ProjectReader(fileSystem);

        Project project = projectReader.load(projectFile);

        if (project.getCommands() == null) {
            System.err.println("Command not found: " + cmd);
            return 1;
        }

        ProjectCommand projectCommand = project.getCommands().stream()
                .filter(c -> c.getId().equals(cmd))
                .findFirst().orElse(null);

        if (projectCommand == null) {
            System.err.println("Command not found: " + cmd);
            return 1;
        }

        Path rootPath = Path.of(project.getProjectDirectory().getAbsolutePath()).toAbsolutePath();
        if (project.getSourcePath() != null) {
            rootPath = rootPath.resolve(project.getSourcePath());
        }

        File file = rootPath.resolve(projectCommand.getScriptPath()).toFile();

        if (!file.isFile()) {
            System.err.println("ERROR: File not found: " + file.getAbsolutePath());
            return 1;
        }

        var scriptFile = new LocalKobuFile(file.getAbsoluteFile());

        int status = new KobuScriptRunner(fileSystem, scriptFile, scriptArgs, project, outDir).run(System.out, System.err);

        if (status == 0) {
            System.out.println("\nCommand executed\n");
        }

        return status;
    }

}
