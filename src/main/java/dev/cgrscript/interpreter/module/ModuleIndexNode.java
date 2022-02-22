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

package dev.cgrscript.interpreter.module;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ModuleIndexNode {

    private final String segment;

    private final Map<String, ModuleIndexNode> children = new LinkedHashMap<>();

    public ModuleIndexNode(String segment) {
        this.segment = segment;
    }

    public String getSegment() {
        return segment;
    }

    public ModuleIndexNode getOrCreateChild(String segment) {
        return children.computeIfAbsent(segment, ModuleIndexNode::new);
    }

    public ModuleIndexNode getChild(String segment) {
        return children.get(segment);
    }

    public List<String> getChildren() {
        return new ArrayList<>(children.keySet());
    }

    protected void addModule(String[] segments, int idx) {
        String segment = segments[idx];
        ModuleIndexNode node = getOrCreateChild(segment);
        if (idx < segments.length - 1) {
            node.addModule(segments, idx + 1);
        }
    }

}
