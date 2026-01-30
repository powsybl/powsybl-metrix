/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping;

import com.google.common.collect.Range;
import com.powsybl.metrix.mapping.config.TimeSeriesMappingConfigStats;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * @author Nicolas Rol {@literal <nicolas.rol at rte-france.com>}
 */
class TimeSeriesMappingConfigStatsTest {

    @ParameterizedTest
    @MethodSource("provideArguments")
    void filterByRangesTest(double[] input, List<Range<Integer>> ranges, double[] expected) {
        assertArrayEquals(expected, TimeSeriesMappingConfigStats.filterByRanges(input, ranges));
    }

    private static Stream<Arguments> provideArguments() {
        return Stream.of(
            Arguments.of(
                new double[]{}, // Empty input array
                List.of(Range.closed(0, 2)),
                new double[]{}
            ),
            Arguments.of(
                new double[]{1, 2, 3, 4, 5, 6},
                List.of(), // Empty ranges
                new double[]{}
            ),
            Arguments.of(
                new double[]{1, 2, 3, 4, 5, 6},
                List.of(Range.closed(1, 2)),    // selects indices 1 and 2 (inclusive)
                new double[]{2, 3}
            ),
            Arguments.of(
                new double[]{1, 2, 3, 4, 5, 6},
                List.of(Range.closed(-5, 1)),  // normalized to [0,2)
                new double[]{1, 2}
            ),
            Arguments.of(
                new double[]{1, 2, 3, 4, 5, 6},
                List.of(// Non overlapping ranges
                    Range.closed(0, 1),
                    Range.closed(3, 4)
                ),
                new double[]{1, 2, 4, 5}
            ),
            Arguments.of(
                new double[]{1, 2, 3, 4, 5, 6},
                List.of(// Overlapping ranges
                    Range.closed(1, 3),
                    Range.closed(2, 5)
                ),
                new double[]{2, 3, 4, 5, 6}
            ),
            Arguments.of(
                new double[]{1, 2, 3, 4, 5, 6},
                List.of(Range.closed(8, 10)),  // completely outside
                new double[]{}
            )
        );
    }
}
