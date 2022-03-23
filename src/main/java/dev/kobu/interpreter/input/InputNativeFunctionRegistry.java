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

package dev.kobu.interpreter.input;

import dev.kobu.interpreter.ast.eval.function.NativeFunctionId;
import dev.kobu.interpreter.input.function.FetchFromUrlFunctionImpl;
import dev.kobu.interpreter.input.function.ReadFromFileFunctionImpl;
import dev.kobu.interpreter.module.ModuleLoader;

public class InputNativeFunctionRegistry {

    public static void register(ModuleLoader moduleLoader) {
        //csv
        moduleLoader.addNativeFunction(new NativeFunctionId("dev.kobu.core.types.Csv", "readCsv"),
                new ReadFromFileFunctionImpl(InputReader::parseCsv, InputReader::getCsvType));
        moduleLoader.addNativeFunction(new NativeFunctionId("dev.kobu.core.types.Csv", "fetchCsv"),
                new FetchFromUrlFunctionImpl(InputReader::parseCsv));

        //json
        moduleLoader.addNativeFunction(new NativeFunctionId("dev.kobu.core.types.Json", "readJson"),
                new ReadFromFileFunctionImpl(InputReader::parseJson, InputReader::getJsonType));
        moduleLoader.addNativeFunction(new NativeFunctionId("dev.kobu.core.types.Json", "fetchJson"),
                new FetchFromUrlFunctionImpl(InputReader::parseJson));
    }

}