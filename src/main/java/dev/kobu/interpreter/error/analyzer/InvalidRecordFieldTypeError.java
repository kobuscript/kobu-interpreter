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

import dev.kobu.interpreter.ast.symbol.RecordTypeSymbol;
import dev.kobu.interpreter.ast.symbol.SourceCodeRef;
import dev.kobu.interpreter.ast.symbol.Type;
import dev.kobu.interpreter.error.AnalyzerError;

import java.util.Objects;

public class InvalidRecordFieldTypeError extends AnalyzerError {

    private final RecordTypeSymbol recordType;

    private final String fieldName;

    private final Type valueType;

    public InvalidRecordFieldTypeError(SourceCodeRef sourceCodeRef, RecordTypeSymbol recordType, String fieldName,
                                       Type valueType) {
        super(sourceCodeRef);
        this.recordType = recordType;
        this.fieldName = fieldName;
        this.valueType = valueType;
    }

    public RecordTypeSymbol getRecordType() {
        return recordType;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Type getValueType() {
        return valueType;
    }

    @Override
    public String getDescription() {
        Type fieldType = recordType.resolveField(fieldName);
        String vType = valueType != null ? valueType.getName() : "void";
        return "Type '" + vType + "' is not assignable to type '" + fieldType.getName() + "'";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        InvalidRecordFieldTypeError that = (InvalidRecordFieldTypeError) o;
        return Objects.equals(recordType, that.recordType) && Objects.equals(fieldName, that.fieldName) && Objects.equals(valueType, that.valueType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), recordType, fieldName, valueType);
    }
}
