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

import java.util.regex.Pattern;

public class TemplateIndentation {

    public static int getInsertionIndex(String str, boolean fromInit) {
        Pattern pattern;
        if (fromInit) {
            pattern = Pattern.compile("(\\n[\\s]*).*$");
        } else {
            pattern = Pattern.compile("(\\n.*)$");
        }
        var matcher = pattern.matcher(str);
        if (matcher.find()) {
            return matcher.group(1).length() - 1;
        }
        return 0;
    }

    public static String indent(String str, int margin, boolean trim) {
        String marginStr = "\n" + " ".repeat(margin);

        if (trim) {
            return str
                    .replaceAll("\\r\\n", "\n")
                    .replaceAll("^\\n", "")
                    .replaceAll("\\n$", "")
                    .replaceAll("\\n", marginStr);
        }
        return str
                .replaceAll("\\n", marginStr);
    }

}
