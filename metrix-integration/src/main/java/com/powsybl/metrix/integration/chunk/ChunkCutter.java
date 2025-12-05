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
import java.util.List;

import static com.powsybl.metrix.commons.ComputationRange.checkRange;
import static com.powsybl.metrix.commons.ComputationRange.checkRanges;

/**
 * @author Paul Bui-Quang {@literal <paul.buiquang at rte-france.com>}
 */
public class ChunkCutter {

    private final int chunkSize;

    private final List<Range<Integer>> ranges = new ArrayList<>();

    public ChunkCutter(int firstVariant, int lastVariant, int chunkSize) {
        checkRange(firstVariant, lastVariant);
        this.chunkSize = chunkSize;
        this.ranges.addAll(splitRange(Range.closed(firstVariant, lastVariant), chunkSize));
    }

    public ChunkCutter(List<Range<Integer>> ranges, int chunkSize) {
        checkRanges(ranges);
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("Chunk size (" + chunkSize + ") has to be greater or equals to one");
        }
        this.chunkSize = chunkSize;
        ranges.forEach(range -> this.ranges.addAll(splitRange(range, chunkSize)));
    }

    public static List<Range<Integer>> splitRange(Range<Integer> rangeToSplit, int chunkSize) {
        List<Range<Integer>> rangeList = new ArrayList<>();
        int firstVariant = rangeToSplit.lowerEndpoint();
        int lastVariant = rangeToSplit.upperEndpoint();
        for (int lower = firstVariant; lower <= lastVariant; lower += chunkSize) {
            rangeList.add(Range.closed(lower, Math.min(lower + chunkSize - 1, lastVariant)));
        }
        return rangeList;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public int getChunkOffset() {
        return 0;
    }

    public int getChunkCount() {
        return ranges.size();
    }

    public Range<Integer> getChunkRange(int chunk) {
        if (chunk == -1) {
            return Range.closed(-1, -1);
        }
        if (chunk < -1 || chunk >= getChunkCount()) {
            throw new IllegalArgumentException("Chunk " + chunk + " is out of range [0, " + getChunkCount() + "]");
        }
        return ranges.get(chunk);
    }
}
