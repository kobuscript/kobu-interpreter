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

package dev.cgrscript.database.index;

import dev.cgrscript.database.match.Match;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class TwoInputsIndexNode implements IndexNode {

    private final List<Slot> children = new ArrayList<>();

    private final Slot leftSlot = new Slot() {
        @Override
        public void receive(Match match) {
            receiveLeft(match);
        }

        @Override
        public void clear() {
            TwoInputsIndexNode.this.clear();
        }

        @Override
        public void clearMatchGroup(int matchGroupId) {
            if (leftGroups.remove(matchGroupId)) {
                leftEntries.removeIf(m -> m.getMatchGroupId() != matchGroupId);
            }
            children.forEach(slot -> slot.clearMatchGroup(matchGroupId));
        }
    };

    private final Slot rightSlot = new Slot() {
        @Override
        public void receive(Match match) {
            receiveRight(match);
        }

        @Override
        public void clear() {
            TwoInputsIndexNode.this.clear();
        }

        @Override
        public void clearMatchGroup(int matchGroupId) {
            if (rightGroups.remove(matchGroupId)) {
                rightEntries.removeIf(m -> m.getMatchGroupId() != matchGroupId);
            }
            children.forEach(slot -> slot.clearMatchGroup(matchGroupId));
        }
    };

    private final Set<Integer> leftGroups = new HashSet<>();

    private final Set<Integer> rightGroups = new HashSet<>();

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
        if (match.getMatchGroupId() != 0) {
            leftGroups.add(match.getMatchGroupId());
        }
        leftEntries.add(match);
        receive(match, null);
        rightEntries.forEach(right -> {
            receive(match, right);
        });
    }

    private void receiveRight(Match match) {
        if (match.getMatchGroupId() != 0) {
            rightGroups.add(match.getMatchGroupId());
        }
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
    }
}
