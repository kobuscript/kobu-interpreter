package dev.cgrscript.interpreter.module;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ModuleIndex {

    private final Map<String, ModuleIndexNode> children = new LinkedHashMap<>();

    public void addModule(String moduleId) {
        addModule(moduleId.split("\\."));
    }

    private void addModule(String[] segments) {
        String segment = segments[0];
        ModuleIndexNode node = children.computeIfAbsent(segment, ModuleIndexNode::new);
        if (segments.length > 1) {
            node.addModule(segments, 1);
        }
    }

    public List<String> getModules() {
        return new ArrayList<>(children.keySet());
    }

    public ModuleIndexNode getChild(String segment) {
        return children.get(segment);
    }

}
