/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping;

import com.powsybl.commons.test.TestUtil;
import com.powsybl.iidm.network.Network;
import com.powsybl.metrix.commons.data.datatable.DataTableStore;
import com.powsybl.metrix.mapping.config.ScriptLogConfig;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStore;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStoreCache;
import com.powsybl.timeseries.RegularTimeSeriesIndex;
import com.powsybl.timeseries.TimeSeries;
import com.powsybl.timeseries.TimeSeriesIndex;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.threeten.extra.Interval;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.time.Duration;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
class ScriptTimeSeriesDslLoaderTest {

    private void launchTest(String script, String expectedTsNames) throws IOException {
        Network network = Mockito.mock(Network.class);
        MappingParameters parameters = Mockito.mock(MappingParameters.class);

        TimeSeriesIndex index = RegularTimeSeriesIndex.create(Interval.parse("2015-01-01T00:00:00Z/2015-07-20T00:00:00Z"), Duration.ofDays(50));
        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache(
            TimeSeries.createDouble("ts", index, 1d, 2d, 3d, 4d, 5d)
        );

        // load mapping script
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (Writer out = new BufferedWriter(new OutputStreamWriter(outputStream))) {
            new TimeSeriesDslLoader(script).load(network, parameters, store, new DataTableStore(), new ScriptLogConfig(out), null);
        }

        String output = TestUtil.normalizeLineSeparator(outputStream.toString());
        assertEquals(expectedTsNames, output);
    }

    @Test
    void testTsNames() throws IOException {
        final String expectedTsNames = "inputTsNames = [ts]\ncalculatedTsNames = []\ncalculatedTsNames = [one]\ncalculatedTsNames = [one, two]\ninputTsNames = [ts]\n";

        // mapping script
        String script = """
            println("inputTsNames = " + inputTsNames())
            println("calculatedTsNames = " + calculatedTsNames())
            ts['one'] = 1
            println("calculatedTsNames = " + calculatedTsNames())
            ts['two'] = 2
            println("calculatedTsNames = " + calculatedTsNames())
            println("inputTsNames = " + inputTsNames())
            """;

        launchTest(script, expectedTsNames);
    }

    @Test
    void testTsExists() throws IOException {
        final String expectedTsExists = "ts = true\none = false\none = false\none = true\n";

        // mapping script
        String script = """
            println("ts = " + inputTsExists('ts'))
            println("one = " + inputTsExists('one'))
            println("one = " + calculatedTsExists('one'))
            ts['one'] = 1
            println("one = " + calculatedTsExists('one'))
            """;

        launchTest(script, expectedTsExists);
    }

    @Test
    void testAllTsNames() throws IOException {
        final String expectedTsNames = "ts = true\none = false\ntsNames = [ts]\none = true\ntsNames = [ts, one]\n";

        // mapping script
        String script = """
            println("ts = " + tsExists('ts'))
            println("one = " + tsExists('one'))
            println("tsNames = " + tsNames())
            ts['one'] = 1
            println("one = " + tsExists('one'))
            println("tsNames = " + tsNames())
            """;

        launchTest(script, expectedTsNames);
    }

    @ParameterizedTest
    @MethodSource("provideForbiddenModificationScript")
    void testAddingNameInUnmodifiableInputTsNames(String script) {
        assertThrows(UnsupportedOperationException.class, () -> launchTest(script, ""));
    }

    private static Stream<Arguments> provideForbiddenModificationScript() {
        return Stream.of(
            Arguments.of("""
            timeSeriesNames = inputTsNames()
            timeSeriesNames.add("otherTimeSeriesName")
            """),
            Arguments.of("""
            timeSeriesNames = calculatedTsNames()
            timeSeriesNames.add("otherTimeSeriesName")
            """),
            Arguments.of("""
            timeSeriesNames = tsNames()
            timeSeriesNames.add("otherTimeSeriesName")
            """)
        );
    }
}
