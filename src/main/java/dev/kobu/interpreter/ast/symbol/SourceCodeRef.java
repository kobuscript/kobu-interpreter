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

package dev.kobu.interpreter.ast.symbol;

import dev.kobu.interpreter.file_system.ResourceRef;

import java.util.Objects;

public class SourceCodeRef {

    private final ResourceRef file;

    private String moduleId;

    private boolean hasPosition;

    private int lineStart;

    private int charStart;

    private int lineEnd;

    private int charEnd;

    private int startOffset;

    private int endOffset;

    public SourceCodeRef(ResourceRef file) {
        this.file = file;
    }

    public SourceCodeRef(ResourceRef file, String moduleId) {
        this.file = file;
        this.moduleId = moduleId;
    }

    public SourceCodeRef(ResourceRef file, String moduleId,
                         int lineStart, int charStart, int lineEnd, int charEnd,
                         int startOffset, int endOffset) {
        this.file = file;
        this.moduleId = moduleId;
        this.lineStart = lineStart;
        this.charStart = Math.max(charStart, 0);
        this.lineEnd = lineEnd;
        this.charEnd = charEnd;
        this.startOffset = Math.max(startOffset, 0);
        this.endOffset = endOffset;
        this.hasPosition = true;
    }

    public ResourceRef getFile() {
        return file;
    }

    public String getModuleId() {
        return moduleId;
    }

    public int getLineStart() {
        return lineStart;
    }

    public int getCharStart() {
        return charStart;
    }

    public int getLineEnd() {
        return lineEnd;
    }

    public int getCharEnd() {
        return charEnd;
    }

    public int getStartOffset() {
        return startOffset;
    }

    public int getEndOffset() {
        return endOffset;
    }

    public boolean hasPosition() {
        return hasPosition;
    }

    @Override
    public String toString() {
        return "SourceCodeRef: " + moduleId + " " + lineStart + ":" + charStart;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SourceCodeRef that = (SourceCodeRef) o;
        return startOffset == that.startOffset && endOffset == that.endOffset && Objects.equals(moduleId, that.moduleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(moduleId, startOffset, endOffset);
    }

}
