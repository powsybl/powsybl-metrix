/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration.chunk;

import com.google.common.collect.Range;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Paul Bui-Quang {@literal <paul.buiquang at rte-france.com>}
 */
class ChunkCutterTest {

    @Test
    void test() {
        ChunkCutter cutter = new ChunkCutter(List.of(Range.closed(0, 8735)), 92);
        assertEquals(95, cutter.getChunkCount());
        assertEquals(92, cutter.getChunkSize());
        assertEquals(Range.closed(-1, -1), cutter.getChunkRange(-1));
        assertEquals(Range.closed(0, 91), cutter.getChunkRange(0));
        assertEquals(Range.closed(92, 183), cutter.getChunkRange(1));
        assertEquals(Range.closed(8648, 8735), cutter.getChunkRange(94));
    }

    @Test
    void test2() {
        ChunkCutter cutter = new ChunkCutter(List.of(Range.closed(0, 8735)), 10000);
        assertEquals(1, cutter.getChunkCount());
        assertEquals(10000, cutter.getChunkSize());
        assertEquals(Range.closed(0, 8735), cutter.getChunkRange(0));
    }

    @Test
    void test3() {
        ChunkCutter cutter = new ChunkCutter(List.of(Range.closed(0, 8735)), 1);
        assertEquals(8736, cutter.getChunkCount());
        assertEquals(1, cutter.getChunkSize());
        assertEquals(Range.closed(0, 0), cutter.getChunkRange(0));
        assertEquals(Range.closed(1, 1), cutter.getChunkRange(1));
        assertEquals(Range.closed(8735, 8735), cutter.getChunkRange(8735));
    }

    @Test
    void test4() {
        ChunkCutter cutter = new ChunkCutter(List.of(Range.closed(100, 200)), 92);
        assertEquals(2, cutter.getChunkCount());
        assertEquals(92, cutter.getChunkSize());
        assertEquals(Range.closed(100, 191), cutter.getChunkRange(0));
        assertEquals(Range.closed(192, 200), cutter.getChunkRange(1));
    }
}
