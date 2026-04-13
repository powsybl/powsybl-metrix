/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration.postprocessing;

import com.powsybl.contingency.Contingency;
import com.powsybl.metrix.integration.contingency.Probability;
import com.powsybl.timeseries.ast.DoubleNodeCalc;
import com.powsybl.timeseries.ast.NodeCalc;
import com.powsybl.timeseries.ast.TimeSeriesNameNodeCalc;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.powsybl.metrix.integration.postprocessing.MetrixPostProcessingTimeSeries.findIdsToProcess;
import static com.powsybl.metrix.integration.postprocessing.MetrixPostProcessingTimeSeries.getContingencyIdFromTsName;
import static com.powsybl.metrix.integration.postprocessing.MetrixPostProcessingTimeSeries.getProbabilityNodeCalc;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
class MetrixPostProcessingTimeSeriesTest {

    @Test
    void findIdsToProcessSimpleTest() {
        List<String> actual = findIdsToProcess(Set.of("id1", "id2"), Set.of("PREFIX_id1"), "PREFIX_");
        Assertions.assertThat(actual).containsExactly("id1");
    }

    @Test
    void findIdsToProcessSimpleWithSuffixTest() {
        List<String> actual = findIdsToProcess(Set.of("id1", "id1_suffix"), Set.of("PREFIX_id1"), "PREFIX_");
        Assertions.assertThat(actual).containsExactly("id1");
    }

    @Test
    void getContingencyIdFromTsNameTest() {
        final String notNull = "Time series name and/or prefix cannot be null";
        final String notEmpty = "Time series name and/or prefix cannot be empty";
        final String notStartWithPrefix = "Time series name 'other' does not start with prefix 'prefix'";
        final String notContainContingency = "Time series name 'prefix' does not contain a contingency id after prefix 'prefix'";
        String ctyId = getContingencyIdFromTsName("GEN_CUR_GROUP_ID_CONTINGENCY_ID", "GEN_CUR_GROUP_ID_");
        assertEquals("CONTINGENCY_ID", ctyId);
        assertEquals(notNull, assertThrows(IllegalArgumentException.class, () -> getContingencyIdFromTsName("ts", null)).getMessage());
        assertEquals(notNull, assertThrows(IllegalArgumentException.class, () -> getContingencyIdFromTsName(null, "prefix")).getMessage());
        assertEquals(notEmpty, assertThrows(IllegalArgumentException.class, () -> getContingencyIdFromTsName("ts", "")).getMessage());
        assertEquals(notEmpty, assertThrows(IllegalArgumentException.class, () -> getContingencyIdFromTsName("", "prefix")).getMessage());
        assertEquals(notStartWithPrefix, assertThrows(IllegalArgumentException.class, () -> getContingencyIdFromTsName("other", "prefix")).getMessage());
        assertEquals(notContainContingency, assertThrows(IllegalArgumentException.class, () -> getContingencyIdFromTsName("prefix", "prefix")).getMessage());
    }

    @Test
    void shouldReturnTimeSeriesNodeCalcWhenProbabilityTimeSeriesRefDefined() {
        // GIVEN
        Map<String, NodeCalc> calculatedTimeSeries = new HashMap<>();
        Contingency contingency = mock(Contingency.class);
        Probability probability = mock(Probability.class);
        when(probability.getProbabilityTimeSeriesRef()).thenReturn("ts");
        when(probability.getProbabilityBase()).thenReturn(null);
        when(contingency.getExtension(Probability.class)).thenReturn(probability);
        // WHEN
        NodeCalc result = getProbabilityNodeCalc(contingency, calculatedTimeSeries);
        // THEN
        assertNotNull(result);
        assertInstanceOf(TimeSeriesNameNodeCalc.class, result);
        assertEquals(1, calculatedTimeSeries.size());
        assertTrue(calculatedTimeSeries.containsKey("ts"));
    }

    @Test
    void shouldReturnDoubleNodeCalcWhenProbabilityBaseDefined() {
        // GIVEN
        Map<String, NodeCalc> calculatedTimeSeries = new HashMap<>();
        Contingency contingency = mock(Contingency.class);
        Probability probability = mock(Probability.class);
        when(probability.getProbabilityTimeSeriesRef()).thenReturn(null);
        when(probability.getProbabilityBase()).thenReturn(0.25);
        when(contingency.getExtension(Probability.class)).thenReturn(probability);
        // WHEN
        NodeCalc result = getProbabilityNodeCalc(contingency, calculatedTimeSeries);
        // THEN
        assertNotNull(result);
        assertInstanceOf(DoubleNodeCalc.class, result);
        assertEquals(1, calculatedTimeSeries.size());
        assertTrue(calculatedTimeSeries.containsKey("0.25"));
    }

    @Test
    void shouldReturnDefaultValueWhenNoProbabilityDefined() {
        // GIVEN
        Map<String, NodeCalc> calculatedTimeSeries = new HashMap<>();
        Contingency contingency = mock(Contingency.class);
        when(contingency.getExtension(Probability.class)).thenReturn(null);
        // WHEN
        NodeCalc result = getProbabilityNodeCalc(contingency, calculatedTimeSeries);
        // THEN
        assertNotNull(result);
        assertInstanceOf(DoubleNodeCalc.class, result);
        assertEquals(1, calculatedTimeSeries.size());
        assertTrue(calculatedTimeSeries.containsKey("defaultProbability"));
    }

    @Test
    void shouldReuseExistingNodeCalcFromCache() {
        // GIVEN
        Map<String, NodeCalc> calculatedTimeSeries = new HashMap<>();
        Contingency contingency = mock(Contingency.class);
        Probability probability = mock(Probability.class);
        when(contingency.getExtension(Probability.class)).thenReturn(null);
        when(probability.getProbabilityTimeSeriesRef()).thenReturn("ts");
        when(probability.getProbabilityBase()).thenReturn(null);
        when(contingency.getExtension(Probability.class)).thenReturn(probability);
        // WHEN
        NodeCalc first = getProbabilityNodeCalc(contingency, calculatedTimeSeries);
        NodeCalc second = getProbabilityNodeCalc(contingency, calculatedTimeSeries);
        // THEN
        assertSame(first, second);
        assertEquals(1, calculatedTimeSeries.size());
    }
}
