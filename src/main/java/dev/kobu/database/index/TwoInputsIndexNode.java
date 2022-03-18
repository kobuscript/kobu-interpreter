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

package dev.kobu.database.index;

import java.util.ArrayList;
import java.util.List;

public abstract class TwoInputsIndexNode implements IndexNode {

    private final List<Slot> children = new ArrayList<>();

    private final Slot leftSlot = new LeftSlot();

    private final Slot rightSlot = new RightSlot();

    private final List<Match> leftEntries = new ArrayList<>();

    private final List<Match> rightEntries = new ArrayList<>();

    protected abstract void receive(Match left, Match right);

    public Slot getLeftSlot() {
        return leftSlot;
    }

    public Slot getRightSlot() {
        return rightSlot;
    }

    private void receiveLeft(Match match) {
        leftEntries.removeIf(match::overrides);
        leftEntries.add(match);
        rightEntries.forEach(right -> {
            receive(match, right);
        });
    }

    private void receiveRight(Match match) {
        rightEntries.add(match);
        leftEntries.forEach(left -> {
            receive(left, match);
        });
    }

    @Override
    public void dispatch(Match match) {
        children.forEach(slot -> slot.receive(match));
    }

    @Override
    public void addChild(Slot child) {
        children.add(child);
    }

    @Override
    public void clear() {
        leftEntries.clear();
        rightEntries.clear();
        children.forEach(Slot::clear);
    }

    private class LeftSlot implements Slot {

        @Override
        public void receive(Match match) {
            receiveLeft(match);
        }

        @Override
        public void clear() {
            TwoInputsIndexNode.this.clear();
        }

    }

    private class RightSlot implements Slot {

        @Override
        public void receive(Match match) {
            receiveRight(match);
        }

        @Override
        public void clear() {
            rightEntries.clear();
        }

    }
}
