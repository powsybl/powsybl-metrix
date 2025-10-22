/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration.chunk;

import com.google.common.collect.Range;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * @author Paul Bui-Quang {@literal <paul.buiquang at rte-france.com>}
 */
public class ChunkCutter {

    private final int chunkSize;

    private final List<Range<Integer>> msaList;

    public ChunkCutter(List<Range<Integer>> msaList, int chunkSize) {
        this.chunkSize = chunkSize;
        assertNoIntersection(msaList);
        this.msaList = msa(msaList.stream().sorted(Comparator.comparing(Range::lowerEndpoint)).toList());
    }

    private void assertNoIntersection(List<Range<Integer>> msaList) {
        for (Range<Integer> range : msaList) {
            for (Range<Integer> rangeTotest : msaList) {
                if (range != rangeTotest) {
                    if (range.isConnected(rangeTotest)) {
                        throw new IllegalStateException(range + " has intersection overlaps with range " + rangeTotest);
                    }
                }
            }
        }
    }

    public List<Range<Integer>> msa(List<Range<Integer>> msaList) {
        List<Range<Integer>> newMsaList = new ArrayList<>();
        for (Range<Integer> range : msaList) {
            newMsaList.addAll(splitRange(range, this.chunkSize));
        }
        return newMsaList;
    }

    public List<Range<Integer>> getMsaList() {
        return msaList;
    }

    private List<Range<Integer>> splitRange(Range<Integer> range, int chunkSize) {
        List<Range<Integer>> newMsaList = new ArrayList<>();
        int increment = chunkSize - 1;
        int lowerEndpoint = range.lowerEndpoint();
        int upperEndpoint = range.lowerEndpoint() + increment;
        while (upperEndpoint < range.upperEndpoint()) {
            Range<Integer> newRange = Range.closed(lowerEndpoint, upperEndpoint);
            newMsaList.add(newRange);
            lowerEndpoint = upperEndpoint + 1;
            upperEndpoint = lowerEndpoint + increment;
        }
        Range<Integer> newRange = Range.closed(lowerEndpoint, range.upperEndpoint());
        newMsaList.add(newRange);
        return newMsaList;
    }

    public ChunkCutter(int firstVariant, int lastVariant, int chunkSize) {
        if (firstVariant < 0) {
            throw new IllegalArgumentException("First variant (" + firstVariant + ") has to be positive");
        }
        if (lastVariant < firstVariant) {
            throw new IllegalArgumentException("Last variant (" + lastVariant +
                ") has to be greater or equals to first variant (" + firstVariant + ")");
        }
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("Chunk size (" + chunkSize + ") has to be greater or equals to one");
        }
        this.chunkSize = chunkSize;
        this.msaList = msa(List.of(Range.closed(firstVariant, lastVariant)));
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public int getChunkOffset() {
        return 0;
    }

    public int getChunkCount() {
        return msaList.size();
    }

    public Range<Integer> getChunkRange(int chunk) {
        if (chunk == -1) {
            return Range.closed(-1, -1);
        }
        if (chunk < -1 || chunk >= getChunkCount()) {
            throw new IllegalArgumentException("Chunk " + chunk + " is out of range [0, " + getChunkCount() + "]");
        }
        return this.msaList.get(chunk);
    }

    public int getChunkFromIndex(int index) {
        for (int i = 0; i < msaList.size(); i++) {
            if (msaList.get(i).contains(index)) {
                return i;
            }
        }
        return 0;
    }
}
