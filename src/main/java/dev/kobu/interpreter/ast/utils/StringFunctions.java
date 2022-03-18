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

import java.util.Locale;

public class StringFunctions {

    public static String capitalize(String str) {
        if (str.length() == 0) {
            return "";
        }
        if (str.length() == 1) {
            return str.toUpperCase(Locale.ROOT);
        }
        return str.substring(0, 1).toUpperCase(Locale.ROOT) + str.substring(1).toLowerCase(Locale.ROOT);
    }

    public static String kebabToCamelCase(String str) {
        String[] parts = str.split("-");
        if (parts.length == 0) {
            return "";
        }
        StringBuilder camelCase = new StringBuilder(parts[0].toLowerCase(Locale.ROOT));
        for (int i = 1; i < parts.length; i++) {
            camelCase.append(StringFunctions.capitalize(parts[i]));
        }

        return camelCase.toString();
    }

    public static String kebabToSnakeCase(String str) {
        return str.replaceAll("-", "_").toLowerCase(Locale.ROOT);
    }

    public static String kebabToPascalCase(String str) {
        String[] parts = str.split("-");
        if (parts.length == 0) {
            return "";
        }
        StringBuilder pascalCase = new StringBuilder();
        for (String part : parts) {
            pascalCase.append(StringFunctions.capitalize(part));
        }
        return pascalCase.toString();
    }

    public static String camelToKebabCase(String str) {
        return str.replaceAll("([A-Z]+)", "-$1").toLowerCase(Locale.ROOT);
    }

    public static String camelToPascalCase(String str) {
        return capitalize(str);
    }

    public static String camelToSnakeCase(String str) {
        return str.replaceAll("([A-Z]+)", "_$1").toLowerCase(Locale.ROOT);
    }

    public static String pascalToKebabCase(String str) {
        return str.replaceAll("([A-Z]+)", "-$1").toLowerCase(Locale.ROOT);
    }

    public static String pascalToCamelCase(String str) {
        if (str.length() <= 1) {
            return str.toLowerCase(Locale.ROOT);
        }
        return str.substring(0, 1).toLowerCase(Locale.ROOT) + str.substring(1);
    }

    public static String pascalToSnakeCase(String str) {
        return str.replaceAll("([A-Z]+)", "_$1").toLowerCase(Locale.ROOT);
    }

    public static String snakeToKebabCase(String str) {
        return str.replace("_", "-");
    }

    public static String snakeToCamelCase(String str) {
        String[] parts = str.split("_");
        if (parts.length == 0) {
            return "";
        }
        StringBuilder camelCase = new StringBuilder(parts[0].toLowerCase(Locale.ROOT));
        for (int i = 1; i < parts.length; i++) {
            camelCase.append(StringFunctions.capitalize(parts[i]));
        }

        return camelCase.toString();
    }

    public static String snakeToPascalCase(String str) {
        String[] parts = str.split("_");
        if (parts.length == 0) {
            return "";
        }
        StringBuilder pascalCase = new StringBuilder();
        for (String part : parts) {
            pascalCase.append(StringFunctions.capitalize(part));
        }
        return pascalCase.toString();
    }

    public static boolean isSpaceChar(String str, int charIdx) {
        char c = str.charAt(charIdx);
        return c == ' ' || c == '\t' || c == '\b' || c == '\f';
    }

}
