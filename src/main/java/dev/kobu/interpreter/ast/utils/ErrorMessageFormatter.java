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

package dev.kobu.interpreter.ast.utils;

import dev.kobu.config.Project;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.error.AnalyzerError;
import dev.kobu.interpreter.error.EvalError;
import dev.kobu.interpreter.error.ParserError;
import dev.kobu.interpreter.file_system.ClasspathScriptRef;
import dev.kobu.interpreter.file_system.KobuScriptFile;
import dev.kobu.interpreter.file_system.ResourceRef;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;

public class ErrorMessageFormatter {

    public static String getSource(SourceCodeRef sourceCodeRef) throws IOException {
        if (sourceCodeRef == null || !sourceCodeRef.hasPosition()) {
            return "";
        }

        StringBuilder source = new StringBuilder();
        int pad = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(sourceCodeRef.getFile().newInputStream()))) {
            int lineIdx = 0;
            String line = "";
            while (lineIdx < sourceCodeRef.getLineStart()) {
                line = reader.readLine();
                lineIdx++;
            }

            if (lineIdx == sourceCodeRef.getLineEnd()) {
                source.append(line).append('\n');
                int charIdx = 0;
                boolean lineInit = false;
                while (charIdx < sourceCodeRef.getCharStart()) {
                    source.append(' ');
                    if (!lineInit) {
                        if (StringFunctions.isSpaceChar(line, charIdx)) {
                            pad++;
                        } else {
                            lineInit = true;
                        }
                    }
                    charIdx++;
                }
                while (charIdx < sourceCodeRef.getCharEnd()) {
                    source.append('^');
                    charIdx++;
                }
                source.append('\n');
            } else {
                int linePad = 0;
                while (lineIdx <= sourceCodeRef.getLineEnd()) {
                    source.append(line).append('\n');
                    line = reader.readLine();
                    for (int i = 0; i < line.length(); i++) {
                        if (StringFunctions.isSpaceChar(line, i)) {
                            linePad++;
                        } else {
                            break;
                        }
                    }
                    if (lineIdx == sourceCodeRef.getLineStart()) {
                        pad = linePad;
                    } else {
                        pad = Math.min(pad, linePad);
                    }
                    lineIdx++;
                }
                source.append('\n');
            }
        }

        String out = source.toString();
        if (pad > 0) {
            out = out.replaceAll("^[ \t\b\f]{" + pad + "}", "  ");
            out = out.replaceAll("\n[ \t\b\f]{" + pad + "}", "\n  ");
        } else {
            out = "  " + out.replaceAll("\n", "\n  ").replaceAll("[ \t\b\f]+$", "");
        }
        return out;
    }

    public static String getText(SourceCodeRef sourceCodeRef) {
        if (sourceCodeRef == null) {
            return "";
        }

        StringBuilder source = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(sourceCodeRef.getFile().newInputStream()))) {
            int lineIdx = 0;
            String line = "";
            while (lineIdx < sourceCodeRef.getLineStart()) {
                line = reader.readLine();
                lineIdx++;
            }

            if (lineIdx == sourceCodeRef.getLineEnd()) {
                if (sourceCodeRef.getLineStart() == sourceCodeRef.getLineEnd()) {
                    source.append(line, sourceCodeRef.getCharStart(), sourceCodeRef.getCharEnd() + 1);
                    return source.toString();
                } else {
                    source.append(line.substring(sourceCodeRef.getCharStart()));
                }
                lineIdx++;
            }

            while (lineIdx < sourceCodeRef.getLineEnd()) {
                source.append('\n');
                source.append(line);
                lineIdx++;
            }
            source.append('\n');
            source.append(line, 0, sourceCodeRef.getCharEnd() + 1);
        } catch (IOException e) {
            return "";
        }

        return source.toString();
    }

    public static StringBuilder getMessage(AnalyzerError analyzerError, Project project) {
        StringBuilder message = new StringBuilder();

        if (analyzerError.getSourceCodeRefs().size() == 1) {
            SourceCodeRef sourceCodeRef = analyzerError.getSourceCodeRefs().get(0);
            message.append("ERROR: ").append(getPathOf(sourceCodeRef.getFile(), project)).append('\n');
            if (sourceCodeRef.hasPosition()) {
                message.append(' ').append(sourceCodeRef.getLineStart()).append(':').append(sourceCodeRef.getCharStart());
                message.append(' ');
            }
            message.append(analyzerError.getDescription());
            message.append('\n');
            appendSource(message, sourceCodeRef);
        } else {
            message.append("ERROR: ").append(analyzerError.getDescription());
            message.append("\n\n");

            for (SourceCodeRef sourceCodeRef : analyzerError.getSourceCodeRefs()) {
                message.append("File: ").append(getPathOf(sourceCodeRef.getFile(), project)).append('\n');
                if (sourceCodeRef.hasPosition()) {
                    message.append(sourceCodeRef.getLineStart()).append(':').append(sourceCodeRef.getCharStart());
                    message.append('\n');
                }
                appendSource(message, sourceCodeRef);
            }
        }

        return message;
    }

    public static StringBuilder getMessage(EvalError evalError, Project project) {
        StringBuilder message = new StringBuilder();
        SourceCodeRef sourceCodeRef = evalError.getSourceCodeRef();
        message.append("ERROR: ").append(getPathOf(sourceCodeRef.getFile(), project)).append('\n');
        message.append(' ').append(sourceCodeRef.getLineStart()).append(':').append(sourceCodeRef.getCharStart());
        message.append(' ');
        message.append(evalError.getDescription());
        message.append('\n');
        appendSource(message, sourceCodeRef);

        return message;
    }

    public static StringBuilder getMessage(ParserError parserError, Project project) {
        StringBuilder message = new StringBuilder();
        SourceCodeRef sourceCodeRef = parserError.getSourceCodeRef();
        message.append("ERROR: ").append(getPathOf(sourceCodeRef.getFile(), project)).append('\n');
        message.append(' ').append(sourceCodeRef.getLineStart()).append(':').append(sourceCodeRef.getCharStart());
        message.append(' ');
        message.append(parserError.getMessage());
        message.append('\n');
        appendSource(message, sourceCodeRef);

        return message;
    }

    private static void appendSource(StringBuilder message, SourceCodeRef sourceCodeRef) {
        try {
            message.append(getSource(sourceCodeRef));
        } catch (IOException ex) {
            message.append("<Failed to read data from: ")
                    .append(sourceCodeRef.getFile().getAbsolutePath())
                    .append(">");
        }
    }

    private static String getPathOf(ResourceRef file, Project project) {
        if (project != null && project.getProjectDirectory() != null) {
            var relPath = Path.of(project.getProjectDirectory().getAbsolutePath())
                    .relativize(Path.of(file.getAbsolutePath())).toString();
            if (!relPath.startsWith("..")) {
                return relPath;
            } else if (file instanceof ClasspathScriptRef) {
                return ((ClasspathScriptRef) file).extractModuleId();
            }
        }
        return file.getAbsolutePath();
    }

}
