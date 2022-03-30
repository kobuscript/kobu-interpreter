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

package dev.kobu.interpreter.ast.eval;

import java.util.ArrayList;
import java.util.List;

public class Branch {

    private boolean hasReturnStatement;

    private boolean hasThrowStatement;

    private boolean hasUnreachableCode;

    private boolean canInterrupt;

    private InterruptTypeEnum interrupt;

    private final Branch parent;

    private final List<Branch> children = new ArrayList<>();

    public Branch(Branch parent) {
        this.parent = parent;
        if (this.parent != null) {
            this.parent.children.add(this);
        }
    }

    public boolean hasReturnStatement() {
        return hasReturnStatement;
    }

    public void setHasReturnStatement(boolean hasReturnStatement) {
        this.hasReturnStatement = hasReturnStatement;
    }

    public boolean hasThrowStatement() {
        return hasThrowStatement;
    }

    public void setHasThrowStatement(boolean hasThrowStatement) {
        this.hasThrowStatement = hasThrowStatement;
    }

    public boolean hasTerminalStatement() {
        return hasReturnStatement || hasThrowStatement;
    }

    public boolean hasUnreachableCode() {
        return hasUnreachableCode;
    }

    public void setHasUnreachableCode(boolean hasUnreachableCode) {
        this.hasUnreachableCode = hasUnreachableCode;
    }

    public void setCanInterrupt(boolean canInterrupt) {
        this.canInterrupt = canInterrupt;
    }

    public Branch getParent() {
        return parent;
    }

    public boolean canInterrupt() {
        if (canInterrupt) {
            return true;
        }
        if (parent != null) {
            return parent.canInterrupt();
        }
        return false;
    }

    public InterruptTypeEnum getInterrupt() {
        return interrupt;
    }

    public void setInterrupt(InterruptTypeEnum interrupt) {
        this.interrupt = interrupt;
    }

    public void updateReturnStatement() {
        hasReturnStatement = children.stream().allMatch(Branch::hasTerminalStatement);
        hasUnreachableCode = false;
    }

}
