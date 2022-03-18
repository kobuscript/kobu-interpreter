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

package dev.kobu.interpreter.error.analyzer;

import dev.kobu.interpreter.ast.symbol.RecordTypeAttribute;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.error.AnalyzerError;

import java.util.ArrayList;
import java.util.List;

public class RecordTypeAttributeConflictError extends AnalyzerError {

    private final RecordTypeAttribute attributeOrig;

    private final RecordTypeAttribute attributeDup;

    public RecordTypeAttributeConflictError(RecordTypeAttribute attributeOrig,
                                            RecordTypeAttribute attributeDup) {
        super(getRefs(attributeOrig, attributeDup));
        this.attributeOrig = attributeOrig;
        this.attributeDup = attributeDup;
    }

    @Override
    public String getDescription() {
        return "'" + attributeOrig.getRecordType().getName() + "' incorrectly extends '"
                + attributeDup.getRecordType().getName() + "'. Types of '"
                + attributeOrig.getRecordType().getName() + "." + attributeOrig.getName() + "' and '"
                + attributeDup.getRecordType().getName() + "." + attributeDup.getName()
                + "' are incompatible";
    }

    private static List<SourceCodeRef> getRefs(RecordTypeAttribute attributeOrig, RecordTypeAttribute attributeDup) {
        List<SourceCodeRef> refs = new ArrayList<>();
        if (attributeOrig.getRecordType().getSourceCodeRef() != null) {
            refs.add(attributeOrig.getRecordType().getSourceCodeRef());
        }
        if (attributeDup.getRecordType().getSourceCodeRef() != null) {
            refs.add(attributeDup.getRecordType().getSourceCodeRef());
        }
        return refs;
    }
}
