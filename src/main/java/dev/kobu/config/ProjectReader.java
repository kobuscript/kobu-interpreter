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

import dev.kobu.config.error.*;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.file_system.KobuDirectory;
import dev.kobu.interpreter.file_system.KobuFile;
import dev.kobu.interpreter.file_system.KobuFileSystem;
import dev.kobu.interpreter.file_system.KobuFileSystemEntry;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProjectReader {

    private final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

    private final KobuFileSystem fileSystem;

    public ProjectReader(KobuFileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }

    public Project loadDefaultProject(KobuFile scriptFile) {
        var project = new Project();
        project.setProjectDirectory(fileSystem.getParent(scriptFile));
        var srcDirs = new ArrayList<KobuDirectory>();
        srcDirs.add(project.getProjectDirectory());
        project.setSrcDirs(srcDirs);
        return project;
    }

    public Project load(KobuFile projectFile) throws ProjectError {

        DocumentBuilder db;
        SourceCodeRef sourceCodeRef = new SourceCodeRef(projectFile);
        try {
            db = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException ex) {
            throw new ProjectSetupError(ex);
        }

        Document doc;
        try (InputStream in = projectFile.newInputStream()) {
            doc = db.parse(in);
        } catch (Exception ex) {
            throw new InvalidProjectFileError(sourceCodeRef, ex);
        }

        doc.getDocumentElement().normalize();

        Project project = new Project();
        project.setName(getRequiredField(doc.getDocumentElement(), "name", sourceCodeRef));
        project.setVersion(getRequiredField(doc.getDocumentElement(), "version", sourceCodeRef));
        project.setSourcePath(getField(doc.getDocumentElement(), "sourcePath", sourceCodeRef));

        NodeList propertyNodes = getGroup(doc.getDocumentElement(), "properties", "property", sourceCodeRef);
        if (propertyNodes != null) {
            List<ProjectProperty> properties = new ArrayList<>();
            for (int i = 0; i < propertyNodes.getLength(); i++) {
                Element element = (Element) propertyNodes.item(i);
                ProjectProperty property = new ProjectProperty();
                property.setName(getRequiredField(element, "name", sourceCodeRef));
                property.setValue(getRequiredField(element, "value", sourceCodeRef));
                properties.add(property);
            }
            project.setProperties(properties);
        }

        NodeList dependencyNodes = getGroup(doc.getDocumentElement(), "dependencies", "dependency", sourceCodeRef);
        if (dependencyNodes != null) {
            List<ProjectDependency> dependencies = new ArrayList<>();
            Set<String> repos = new HashSet<>();
            for (int i = 0; i < dependencyNodes.getLength(); i++) {
                Element element = (Element) dependencyNodes.item(i);
                ProjectDependency dependency = new ProjectDependency();
                dependency.setUrl(getRequiredField(element, "url", sourceCodeRef));
                dependency.setSha(getRequiredField(element, "sha", sourceCodeRef));
                dependency.setTag(getField(element, "tag", sourceCodeRef));
                if (!repos.add(dependency.getUrl())) {
                    throw new ProjectDuplicatedDependencyError(sourceCodeRef, dependency);
                }
                dependencies.add(dependency);
            }
            project.setDependencies(dependencies);
        }

        NodeList commandNodes = getGroup(doc.getDocumentElement(), "commands", "command", sourceCodeRef);
        if (commandNodes != null) {
            List<ProjectCommand> commands = new ArrayList<>();
            Set<String> commandSet = new HashSet<>();
            for (int i = 0; i < commandNodes.getLength(); i++) {
                Element element = (Element) commandNodes.item(i);
                ProjectCommand command = new ProjectCommand();
                command.setName(getRequiredField(element, "name", sourceCodeRef));
                command.setScriptPath(getRequiredField(element, "script", sourceCodeRef));
                command.setDescription(getRequiredField(element, "description", sourceCodeRef));
                command.setTargetPattern(getField(element, "pattern", sourceCodeRef));
                if (!commandSet.add(command.getName())) {
                    throw new ProjectDuplicatedCommandError(sourceCodeRef, command);
                }
                commands.add(command);
            }
            project.setCommands(commands);
        }

        var projectDir = fileSystem.getParent(projectFile);
        var srcDirs = new ArrayList<KobuDirectory>();
        if (project.getSourcePath() == null || project.getSourcePath().isEmpty()) {
           srcDirs.add(projectDir);
        } else {
            KobuFileSystemEntry srcEntry = fileSystem.loadEntry(projectDir, project.getSourcePath());
            if (!(srcEntry instanceof KobuDirectory)) {
                throw new ProjectInvalidSourcePathError(sourceCodeRef, project.getSourcePath());
            }
            srcDirs.add((KobuDirectory) srcEntry);
        }
        project.setProjectDirectory(projectDir);
        project.setSrcDirs(srcDirs);

        return project;
    }

    private String getRequiredField(Element element, String fieldName, SourceCodeRef sourceCodeRef) throws ProjectError {
        var nodes = getChildrenWithName(element, fieldName);
        if (nodes.size() == 0) {
            throw new ProjectMissingFieldError(sourceCodeRef, new String[]{element.getTagName(), fieldName});
        } else if (nodes.size() > 1) {
            throw new ProjectDuplicatedFieldError(sourceCodeRef, new String[]{element.getTagName(), fieldName});
        }
        return nodes.get(0).getFirstChild().getNodeValue();
    }

    private Integer getRequiredIntField(Element element, String fieldName, SourceCodeRef sourceCodeRef) throws ProjectError {
        String value = getRequiredField(element, fieldName, sourceCodeRef);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new ProjectInvalidIntFieldError(sourceCodeRef, new String[]{element.getTagName(), fieldName}, value);
        }
    }

    private String getField(Element element, String fieldName, SourceCodeRef sourceCodeRef) throws ProjectError {
        var nodes = element.getElementsByTagName(fieldName);
        if (nodes.getLength() > 1) {
            throw new ProjectDuplicatedFieldError(sourceCodeRef, new String[]{element.getTagName(), fieldName});
        } else if (nodes.getLength() == 1) {
            return nodes.item(0).getFirstChild().getNodeValue();
        }
        return null;
    }

    private NodeList getGroup(Element element, String groupName, String groupItemName, SourceCodeRef sourceCodeRef) throws ProjectError {
        var groups = element.getElementsByTagName(groupName);

        if (groups.getLength() == 0) {
            return null;
        }

        if (groups.getLength() > 1) {
            throw new ProjectDuplicatedFieldError(sourceCodeRef, new String[]{element.getTagName(), groupName});
        }

        var groupItems = ((Element)groups.item(0)).getElementsByTagName(groupItemName);

        if (groupItems.getLength() == 0) {
            throw new ProjectMissingFieldError(sourceCodeRef, new String[]{groupName, groupItemName});
        }

        return groupItems;
    }

    private List<Node> getChildrenWithName(Element element, String name) {
        List<Node> nodes = new ArrayList<>();
        for (int i = 0; i < element.getChildNodes().getLength(); i++) {
            Node node = element.getChildNodes().item(i);
            if (node instanceof Element && ((Element) node).getTagName().equals(name)) {
                nodes.add(node);
            }
        }
        return nodes;
    }
}
