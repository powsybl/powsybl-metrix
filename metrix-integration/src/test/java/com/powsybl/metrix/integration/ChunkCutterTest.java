/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.integration;

import com.google.common.collect.Range;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class ChunkCutterTest {

    @Test
    public void test() {
        ChunkCutter cutter = new ChunkCutter(0, 8735, 92);
        assertEquals(95, cutter.getChunkCount());
        assertEquals(Range.closed(-1, -1), cutter.getChunkRange(-1));
        assertEquals(Range.closed(0, 91), cutter.getChunkRange(0));
        assertEquals(Range.closed(92, 183), cutter.getChunkRange(1));
        assertEquals(Range.closed(8648, 8735), cutter.getChunkRange(94));
    }

    @Test
    public void test2() {
        ChunkCutter cutter = new ChunkCutter(0, 8735, 10000);
        assertEquals(1, cutter.getChunkCount());
        assertEquals(Range.closed(0, 8735), cutter.getChunkRange(0));
    }

    @Test
    public void test3() {
        ChunkCutter cutter = new ChunkCutter(0, 8735, 1);
        assertEquals(8736, cutter.getChunkCount());
        assertEquals(Range.closed(0, 0), cutter.getChunkRange(0));
        assertEquals(Range.closed(1, 1), cutter.getChunkRange(1));
        assertEquals(Range.closed(8735, 8735), cutter.getChunkRange(8735));
    }

    @Test
    public void test4() {
        ChunkCutter cutter = new ChunkCutter(100, 200, 92);
        assertEquals(2, cutter.getChunkCount());
        assertEquals(Range.closed(100, 191), cutter.getChunkRange(0));
        assertEquals(Range.closed(192, 200), cutter.getChunkRange(1));
    }
}
