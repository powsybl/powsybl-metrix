/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping;

import com.google.common.collect.Range;
import com.powsybl.metrix.commons.ComputationRange;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
class ComputationRangeTest {

    @Test
    void testComputationRange() {
        // GIVEN
        ComputationRange computationRange = new ComputationRange(Set.of(1), Range.closed(1, 10));
        // WHEN
        List<Range<Integer>> ranges = computationRange.getRanges();
        // THEN
        assertThat(ranges).isNotNull();
    }

    @Test
    void testComputationRangeList() {
        // GIVEN
        ComputationRange computationRange = new ComputationRange(Set.of(1), List.of(Range.closed(1, 10)));
        // WHEN
        List<Range<Integer>> ranges = computationRange.getRanges();
        // THEN
        assertThat(ranges).isNotNull();
    }

    @Test
    void testComputationFirstVariantAndVariantCount() {
        // GIVEN
        ComputationRange computationRange = new ComputationRange(Set.of(1), 1, 10);
        // WHEN
        List<Range<Integer>> ranges = computationRange.getRanges();
        // THEN
        assertThat(ranges).isNotNull();
    }

    @Test
    void testComputationRangeException() {
        // GIVEN
        Set<Integer> versions = Set.of(1);
        List<Range<Integer>> ranges = List.of(Range.closed(-1, 10));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new ComputationRange(versions, ranges));
        assertTrue(e.getMessage().contains("First variant (-1) has to be positive"));
    }

    @Test
    void testComputationRangeOverlap() {
        // GIVEN
        Set<Integer> versions = Set.of(1);
        List<Range<Integer>> ranges = List.of(Range.closed(1, 2), Range.closed(5, 15), Range.closed(10, 30));
        // WHEN
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new ComputationRange(versions, ranges));
        // THEN
        assertTrue(e.getMessage().contains("[5..15] overlaps with range [10..30]"));
    }
}
