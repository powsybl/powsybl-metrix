/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration.network;

import com.google.common.collect.Range;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.impl.VariantManagerHolder;
import com.powsybl.metrix.integration.MetrixSubset;
import com.powsybl.timeseries.RegularTimeSeriesIndex;
import com.powsybl.timeseries.TimeSeriesIndex;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.threeten.extra.Interval;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Paul Bui-Quang {@literal <paul.buiquang at rte-france.com>}
 */
class MetrixVariantsWriterTest {

    @Test
    void baseCaseTest() throws IOException {
        StringWriter writer = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(writer)) {
            new MetrixVariantsWriter(null, null)
                    .write(null, bufferedWriter, null);
        }
        assertEquals(String.join(System.lineSeparator(),
                "NT;1;",
                "0;") + System.lineSeparator(),
                writer.toString());
    }

    @Test
    void variantsTest() throws IOException {
        TimeSeriesIndex index = RegularTimeSeriesIndex.create(
                Interval.parse("2015-01-01T00:00:00Z/2015-01-01T01:00:00Z"),
                Duration.ofMinutes(15));
        StringWriter writer = new StringWriter();

        MetrixNetwork network = mock(MetrixNetwork.class);
        when(network.getIndex(MetrixSubset.LOAD, "l1")).thenReturn(2);
        when(network.getIndex(MetrixSubset.LOAD, "l2")).thenReturn(1);
        when(network.getIndex(MetrixSubset.LOAD, "l3")).thenReturn(1);
        when(network.getIndex(MetrixSubset.LOAD, "l4")).thenReturn(1);
        when(network.getIndex(MetrixSubset.LOAD, "l5")).thenReturn(1);
        when(network.getIndex(MetrixSubset.LOAD, "l6")).thenReturn(1);
        when(network.getIndex(MetrixSubset.LOAD, "l7")).thenReturn(1);
        when(network.getIndex(MetrixSubset.LOAD, "l8")).thenReturn(1);
        when(network.getIndex(MetrixSubset.LOAD, "l9")).thenReturn(1);
        when(network.getIndex(MetrixSubset.LOAD, "l10")).thenReturn(1);
        when(network.getIndex(MetrixSubset.LOAD, "l13")).thenThrow(new IllegalStateException());

        try (BufferedWriter bufferedWriter = new BufferedWriter(writer)) {
            new MetrixVariantsWriter(new MetrixVariantProviderMock(network, index), network)
                .write(Range.closed(0, 3), bufferedWriter, null);
        }

        assertEquals(
            String.join(System.lineSeparator(), IOUtils.readLines(Objects.requireNonNull(MetrixVariantsWriterTest.class.getResourceAsStream("/expected/variantsOutput.txt")), StandardCharsets.UTF_8)),
            writer.toString().trim()
        );
    }

    abstract static class AbstractNetworkImplTest implements Network, VariantManagerHolder {

    }
}
