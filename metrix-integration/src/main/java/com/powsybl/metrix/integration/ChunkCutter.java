/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.integration;

import com.google.common.collect.Range;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian@rte-france.com>
 */
public class ChunkCutter {

    private final int firstVariant;

    private final int lastVariant;

    private final int chunkSize;

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
        this.firstVariant = firstVariant;
        this.lastVariant = lastVariant;
        this.chunkSize = chunkSize;
    }

    public int getChunkCount() {
        return (int) Math.ceil((float) (lastVariant - firstVariant + 1) / chunkSize);
    }

    public Range<Integer> getChunkRange(int chunk) {
        if (chunk == -1) {
            return Range.closed(-1, -1);
        }
        if (chunk < -1 || chunk >= getChunkCount()) {
            throw new IllegalArgumentException("Chunk " + chunk + " is out of range [0, " + getChunkCount() + "]");
        }
        return Range.closed(firstVariant + chunk * chunkSize,
                            Math.min(lastVariant, firstVariant + (chunk + 1) * chunkSize - 1));
    }
}
