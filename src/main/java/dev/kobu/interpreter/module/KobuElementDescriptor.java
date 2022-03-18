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

package dev.kobu.interpreter.module;

public class KobuElementDescriptor {

    public static KobuElementDescriptor builtinElement(String name) {
        return new KobuElementDescriptor(true, null, name);
    }

    public static KobuElementDescriptor element(String moduleId, String name) {
        return new KobuElementDescriptor(false, moduleId, name);
    }

    private final boolean builtin;

    private final String moduleId;

    private final String name;

    private KobuElementDescriptor(boolean builtin, String moduleId, String name) {
        this.builtin = builtin;
        this.moduleId = moduleId;
        this.name = name;
    }

    public boolean isBuiltin() {
        return builtin;
    }

    public String getModuleId() {
        return moduleId;
    }

    public String getName() {
        return name;
    }

}
