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

import dev.kobu.interpreter.file_system.KobuDirectory;

import java.util.List;

public class Project {

    private String name;

    private String version;

    private List<ProjectProperty> properties;

    private String sourcePath;

    private List<ProjectDependency> dependencies;

    private KobuDirectory projectDirectory;

    private List<KobuDirectory> srcDirs;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<ProjectProperty> getProperties() {
        return properties;
    }

    public void setProperties(List<ProjectProperty> properties) {
        this.properties = properties;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    public List<ProjectDependency> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<ProjectDependency> dependencies) {
        this.dependencies = dependencies;
    }

    public KobuDirectory getProjectDirectory() {
        return projectDirectory;
    }

    public void setProjectDirectory(KobuDirectory projectDirectory) {
        this.projectDirectory = projectDirectory;
    }

    public List<KobuDirectory> getSrcDirs() {
        return srcDirs;
    }

    public void setSrcDirs(List<KobuDirectory> srcDirs) {
        this.srcDirs = srcDirs;
    }

}
