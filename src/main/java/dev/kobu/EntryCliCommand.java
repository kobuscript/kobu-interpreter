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

package dev.kobu;

import dev.kobu.config.NewCliCommand;
import dev.kobu.interpreter.FormatCliCommand;
import dev.kobu.interpreter.RunCliCommand;
import picocli.CommandLine;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@CommandLine.Command(mixinStandardHelpOptions = true,
        subcommands = {RunCliCommand.class, NewCliCommand.class, FormatCliCommand.class},
        versionProvider = EntryCliCommand.KobuVersionProvider.class)
public class EntryCliCommand {

    public static class KobuVersionProvider implements CommandLine.IVersionProvider {

        @Override
        public String[] getVersion() throws Exception {
            try (InputStream in = EntryCliCommand.class.getResourceAsStream("/version.txt")) {
                return new String[]{"kobu-interpreter " + new String(in.readAllBytes(), StandardCharsets.UTF_8)};
            }
        }

    }

}
