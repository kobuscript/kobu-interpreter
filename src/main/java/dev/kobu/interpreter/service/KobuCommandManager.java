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

package dev.kobu.interpreter.service;

import dev.kobu.config.Project;
import dev.kobu.config.ProjectCommand;
import dev.kobu.config.ProjectReader;
import dev.kobu.config.error.ProjectError;
import dev.kobu.interpreter.file_system.local.LocalKobuFile;
import dev.kobu.interpreter.file_system.local.LocalKobuFileSystem;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

public class KobuCommandManager {

    private final BlockingQueue<KobuCommandEvent> eventQueue = new LinkedBlockingQueue<>();

    private List<KobuCommandGroup> commandGroups;

    private List<String> projectDefinitions;

    private Thread worker;

    public void start() {
        if (worker != null) {
            return;
        }

        worker = new Thread(new Task(), "kobu-command-manager");
        worker.setDaemon(true);
        worker.start();
    }

    public void putEvent(KobuCommandEvent event) {
        eventQueue.add(event);
    }

    public synchronized List<KobuCommandGroup> getCommands(String filePath) {
        ArrayList<KobuCommandGroup> filteredGroups = new ArrayList<>();
        if (commandGroups == null) {
            return new ArrayList<>();
        }
        for (KobuCommandGroup commandGroup : commandGroups) {
            var commands = commandGroup.getCommands().stream()
                    .filter(c -> matchCommand(c, filePath))
                    .collect(Collectors.toList());
            if (!commands.isEmpty()) {
                filteredGroups.add(new KobuCommandGroup(commandGroup.getGroupName(),
                        commandGroup.getProjectDir(), commands));
            }
        }

        return filteredGroups;
    }

    private boolean matchCommand(ProjectCommand cmd, String filePath) {
        if (cmd.getTargetPattern() == null) {
            return true;
        }
        Path path = Path.of(filePath).getFileName();
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + cmd.getTargetPattern());
        return matcher.matches(path);
    }

    private synchronized void updateProjectDefinitions(List<String> projectDefinitions) {
        this.projectDefinitions = projectDefinitions.stream()
                .filter(p -> !p.contains("/kobu_modules/"))
                .collect(Collectors.toList());

        updateProjectDefinitions();
    }

    private synchronized void updateProjectDefinitions() {
        if (projectDefinitions == null) {
            return;
        }
        commandGroups = new ArrayList<>();
        var fs = new LocalKobuFileSystem();
        var projectReader = new ProjectReader(fs);
        var groupSet = new HashSet<String>();
        for (String path : projectDefinitions) {
            Project project;
            try {
                project = projectReader.load(new LocalKobuFile(new File(path)));
            } catch (ProjectError e) {
                continue;
            }
            if (groupSet.add(project.getName()) && project.getCommands() != null && !project.getCommands().isEmpty()) {
                var group = new KobuCommandGroup(project.getName(), project.getProjectDirectory().getAbsolutePath(),
                        project.getCommands());
                commandGroups.add(group);
            }
        }
    }

    private synchronized void addProjectDefinitions(List<String> projectDefinitions) {
        projectDefinitions.stream()
                .filter(p -> !p.contains("/kobu_modules/"))
                .forEach(p -> {
                    if (!this.projectDefinitions.contains(p)) {
                        this.projectDefinitions.add(p);
                    }
                });
        updateProjectDefinitions();
    }

    private synchronized void removeProjectDefinitions(List<String> projectDefinitions) {
        this.projectDefinitions.removeAll(projectDefinitions);
        updateProjectDefinitions();
    }

    public interface KobuCommandEvent {

    }

    public static class KobuUpdateProjectDefinitionsEvent implements KobuCommandEvent {

        private final List<String> projectDefinitions;

        public KobuUpdateProjectDefinitionsEvent() {
            projectDefinitions = null;
        }

        public KobuUpdateProjectDefinitionsEvent(List<String> projectDefinitions) {
            this.projectDefinitions = projectDefinitions;
        }

        public List<String> getProjectDefinitions() {
            return projectDefinitions;
        }

    }

    public static class KobuAddProjectDefinitionsEvent implements KobuCommandEvent {

        private final List<String> projectDefinitions;

        public KobuAddProjectDefinitionsEvent(List<String> projectDefinitions) {
            this.projectDefinitions = projectDefinitions;
        }

        public List<String> getProjectDefinitions() {
            return projectDefinitions;
        }

    }

    public static class KobuRemoveProjectDefinitionsEvent implements KobuCommandEvent {

        private final List<String> projectDefinitions;

        public KobuRemoveProjectDefinitionsEvent(List<String> projectDefinitions) {
            this.projectDefinitions = projectDefinitions;
        }

        public List<String> getProjectDefinitions() {
            return projectDefinitions;
        }

    }

    private class Task implements Runnable {

        @Override
        public void run() {
            while (true) {
                try {
                    KobuCommandEvent event = eventQueue.take();
                    if (event instanceof KobuUpdateProjectDefinitionsEvent) {
                        KobuUpdateProjectDefinitionsEvent updtEvent = (KobuUpdateProjectDefinitionsEvent) event;
                        if (updtEvent.projectDefinitions == null) {
                            updateProjectDefinitions();
                        } else {
                            updateProjectDefinitions(updtEvent.projectDefinitions);
                        }
                    } else if (event instanceof KobuAddProjectDefinitionsEvent) {
                        addProjectDefinitions(((KobuAddProjectDefinitionsEvent) event).getProjectDefinitions());
                    } else if (event instanceof KobuRemoveProjectDefinitionsEvent) {
                        removeProjectDefinitions(((KobuRemoveProjectDefinitionsEvent) event).getProjectDefinitions());
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

    }

}
