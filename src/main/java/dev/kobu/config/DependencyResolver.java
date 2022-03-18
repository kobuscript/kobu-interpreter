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

import dev.kobu.config.error.ProjectError;
import dev.kobu.interpreter.file_system.KobuScriptFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DependencyResolver {

    private static final String DEFAULT_REPOSITORY_PATH = ".kobu";
    private static final String LIB_PATH = "libs";

    private final Project project;

    private final Pattern gitUrlRegex = Pattern.compile("([a-z0-9+.-]+)://(?:(?:[^@]+?@)?([^/]+?)(?::[0-9]*)?)?(/[^:]+)");
    private final Pattern gitScpRegex = Pattern.compile("(?:[^@]+?@)?(.+?):([^:]+)");

    public DependencyResolver(Project project) {
        this.project = project;
    }

    public void fetchDependencies() {

    }

    public KobuScriptFile loadModuleScript(String moduleId) {

        return null;
    }

    private List<File> getDependenciesSrcDirs() throws ProjectError {
        List<File> srcDirs = new ArrayList<>();
        Set<String> libs;

        if (project.getDependencies() != null) {

            libs = project.getDependencies()
                    .stream()
                    .map(dep -> cleanUrl(dep.getUrl()))
                    .collect(Collectors.toSet());

            for (ProjectDependency dependency : project.getDependencies()) {
                srcDirs.addAll(getDependenciesSrcDirs(dependency, libs));
            }

        }

        return srcDirs;
    }

    private File getRepositoryDir() {
        if (project.getRepositoryPath() != null) {
            return new File(project.getRepositoryPath());
        }
        return new File(System.getProperty("user.home"), DEFAULT_REPOSITORY_PATH);
    }

    private File getLibDir() {
        return new File(getRepositoryDir(), LIB_PATH);
    }

    private List<File> getDependenciesSrcDirs(ProjectDependency dependency, Set<String> libs) throws ProjectError {
//        var cleanUrl = cleanUrl(dependency.getUrl());
//        var dependencyDir = getDependencyDir(dependency, cleanUrl);
//        var dependencyProjectFile = new File(dependencyDir, KobuFileSystem.PROJECT_CFG);
//
//        Project dependencyProject;
//        try {
//            dependencyProject = projectReader.load(new LocalKobuFile(dependencyProjectFile));
//        } catch (ProjectError error) {
//            throw new ProjectDependencyError(new SourceCodeRef(new LocalKobuFile(dependencyProjectFile)), dependency, error);
//        }

        List<File> srcDirs = new ArrayList<>();
//        if (dependencyProject.getSourcePaths() != null) {
//
//            for (ProjectSourcePath sourcePath : dependencyProject.getSourcePaths()) {
//                srcDirs.add(new File(dependencyDir, sourcePath.getPath()));
//            }
//
//        }
//
//        if (dependencyProject.getDependencies() != null) {
//
//            for (ProjectDependency transitiveDependency: dependencyProject.getDependencies()) {
//                if (libs.add(cleanUrl(transitiveDependency.getUrl()))) {
//                    srcDirs.addAll(getDependenciesSrcDirs(transitiveDependency, libs));
//                }
//            }
//
//        }

        return srcDirs;
    }

    private File getDependencyDir(ProjectDependency dependency, String cleanUrl) {
        var libDir = getLibDir();
        var dependencyDir = new File(libDir, cleanUrl);
        return new File(dependencyDir, dependency.getSha());
    }

    //https://github.com/clojure/tools.gitlibs/blob/9f98af7631e34983d5b0886e1ab6eadc3856290b/src/main/clojure/clojure/tools/gitlibs/impl.clj#L54
    private String cleanUrl(String url) {
        String scheme = "";
        String host = "";
        String path = "";

        if (url.startsWith("file://")) {
            scheme = "file";
            path = url.substring(7).replaceAll("^([^/])", "REL/$1");
        } else if (url.contains("://")) {
            var matcher = gitUrlRegex.matcher(url);
            scheme = matcher.group(1);
            host = matcher.group(2);
            path = matcher.group(3);
        } else if (url.contains(":")) {
            var matcher = gitScpRegex.matcher(url);
            scheme = "ssh";
            host = matcher.group(1);
            path = matcher.group(2);
        }

        String cleanPath = path
                .replaceAll("\\.git/?$", "")
                .replace("~", "_TILDE_");

        StringBuilder cleanUrl = new StringBuilder();
        cleanUrl.append(scheme);
        cleanUrl.append("/").append(host);
        for (String part : cleanPath.split("/")) {
            if (part.isBlank()) {
                continue;
            }
            cleanUrl.append("/");
            if (part.equals(".")) {
                cleanUrl.append("_DOT_");
            } else if (part.equals("..")) {
                cleanUrl.append("_DOTDOT_");
            } else {
                cleanUrl.append(part);
            }
        }

        return cleanUrl.toString();
    }
}
