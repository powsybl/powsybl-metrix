/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.timeseries.*;
import com.powsybl.timeseries.ast.FloatNodeCalc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.threeten.extra.Interval;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TimeSeriesMapperTest {

    private Network network;

    private final MappingParameters mappingParameters = MappingParameters.load();

    @BeforeEach
    public void setUp() {
        // create test network
        network = MappingTestNetwork.create();
    }

    @Test
    void mappingTest() {

        List<String> equipmentTimeSeriesNames = new ArrayList<>();
        List<Identifiable<?>> equipmentIds = new ArrayList<>();
        List<Double> equipmentValues = new ArrayList<>();
        List<String> equipmentVariables = new ArrayList<>();

        List<String> otherTimeSeriesNames = new ArrayList<>();
        List<Identifiable<?>> otherIds = new ArrayList<>();
        List<Double> otherValues = new ArrayList<>();
        List<String> otherVariables = new ArrayList<>();

        // create time series space mock
        TimeSeriesIndex index = RegularTimeSeriesIndex.create(Interval.parse("2015-01-01T00:00:00Z/2015-07-20T00:00:00Z"), Duration.ofDays(200));
        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache(
                TimeSeries.createDouble("equipment_ts", index, 1d, 2d),
                TimeSeries.createDouble("other_ts", index, 2d, 2d)
            );

        // create mapping config
        TimeSeriesMappingConfig mappingConfig = new TimeSeriesMappingConfig(network);
        TimeSeriesMappingConfigLoader loader = new TimeSeriesMappingConfigLoader(mappingConfig, store.getTimeSeriesNames(new TimeSeriesFilter()));
        loader.addEquipmentMapping(MappableEquipmentType.LOAD, "equipment_ts", "LD2", NumberDistributionKey.ONE, EquipmentVariable.p0);
        loader.addEquipmentTimeSeries("other_ts", OtherVariable.OTHER_VARIABLE, "L1");

        // create mapper
        TimeSeriesMappingLogger logger = new TimeSeriesMappingLogger();
        TimeSeriesMapper mapper = new TimeSeriesMapper(mappingConfig, network, logger);
        TimeSeriesMapperParameters parameters = new TimeSeriesMapperParameters(new TreeSet<>(Collections.singleton(1)),
                Range.closed(0, 0), false, false, true, mappingParameters.getToleranceThreshold());

        // launch TimeSeriesMapper test
        TimeSeriesMapperObserver observer = new DefaultTimeSeriesMapperObserver() {
            @Override
            public void timeSeriesMappedToEquipment(int point, String timeSeriesName, Identifiable<?> identifiable, MappingVariable variable, double equipmentValue) {
                if (variable instanceof EquipmentVariable) {
                    equipmentTimeSeriesNames.add(timeSeriesName);
                    equipmentIds.add(identifiable);
                    equipmentValues.add(equipmentValue);
                    equipmentVariables.add(variable.getVariableName());
                } else if (variable instanceof OtherVariable) {
                    otherTimeSeriesNames.add(timeSeriesName);
                    otherIds.add(identifiable);
                    otherValues.add(equipmentValue);
                    otherVariables.add(variable.getVariableName());
                }
            }
        };
        mapper.mapToNetwork(store, parameters, ImmutableList.of(observer));

        assertEquals(1, equipmentTimeSeriesNames.size());
        assertEquals(ImmutableList.of("equipment_ts"), equipmentTimeSeriesNames);
        assertEquals(ImmutableList.of(network.getIdentifiable("LD2")), equipmentIds);
        assertEquals(ImmutableList.of(1.0), equipmentValues);
        assertEquals(ImmutableList.of("p0"), equipmentVariables);

        assertEquals(1, otherTimeSeriesNames.size());
        assertEquals(ImmutableList.of("other_ts"), otherTimeSeriesNames);
        assertEquals(ImmutableList.of(network.getIdentifiable("L1")), otherIds);
        assertEquals(ImmutableList.of(2.0), otherValues);
        assertEquals(ImmutableList.of("otherVariable"), otherVariables);
    }

    @Test
    void goodIndexTest() {
        // create time series space mock
        TimeSeriesIndex index = RegularTimeSeriesIndex.create(Interval.parse("1970-01-01T00:00:00Z/1970-01-02T00:00:00Z"), Duration.ofDays(1));

        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache(
                TimeSeries.createDouble("foo", index, 1d, 1d),
                TimeSeries.createDouble("bar", index, 1d, 1d)
        );

        // create mapping config
        TimeSeriesMappingConfig mappingConfig = new TimeSeriesMappingConfig(network);

        mappingConfig.getTimeSeriesNodes().put("calculated", new FloatNodeCalc(10f));
        TimeSeriesMappingConfigLoader loader = new TimeSeriesMappingConfigLoader(mappingConfig, store.getTimeSeriesNames(new TimeSeriesFilter()));
        loader.addEquipmentMapping(MappableEquipmentType.LOAD, "calculated", "l1", NumberDistributionKey.ONE, EquipmentVariable.p0);
        loader.addEquipmentMapping(MappableEquipmentType.LOAD, "foo", "l2", NumberDistributionKey.ONE, EquipmentVariable.p0);
        loader.addEquipmentMapping(MappableEquipmentType.GENERATOR, "bar", "g1", NumberDistributionKey.ONE, EquipmentVariable.targetP);

        new TimeSeriesMappingConfigTableLoader(mappingConfig, store).checkIndexUnicity();
    }

    @Test
    void wrongIndexTest() {
        // create time series space mock
        TimeSeriesIndex index1 = RegularTimeSeriesIndex.create(Interval.parse("1970-01-01T00:00:00Z/1970-01-02T00:00:00Z"), Duration.ofDays(1));
        TimeSeriesIndex index2 = RegularTimeSeriesIndex.create(Interval.parse("1970-01-01T00:00:00Z/1970-01-03T00:00:00Z"), Duration.ofDays(2));

        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache(
            TimeSeries.createDouble("foo", index1, 1d, 1d),
            TimeSeries.createDouble("bar", index2, 1d, 1d)
        );

        // create mapping config
        TimeSeriesMappingConfig mappingConfig = new TimeSeriesMappingConfig(network);

        mappingConfig.getTimeSeriesNodes().put("calculated", new FloatNodeCalc(10f));
        TimeSeriesMappingConfigLoader loader = new TimeSeriesMappingConfigLoader(mappingConfig, store.getTimeSeriesNames(new TimeSeriesFilter()));
        loader.addEquipmentMapping(MappableEquipmentType.LOAD, "calculated", "l1", NumberDistributionKey.ONE, EquipmentVariable.p0);
        loader.addEquipmentMapping(MappableEquipmentType.LOAD, "foo", "l2", NumberDistributionKey.ONE, EquipmentVariable.p0);
        loader.addEquipmentMapping(MappableEquipmentType.GENERATOR, "bar", "g1", NumberDistributionKey.ONE, EquipmentVariable.targetP);

        assertThrows(TimeSeriesMappingException.class,
            () -> new TimeSeriesMappingConfigTableLoader(mappingConfig, store).checkIndexUnicity(),
            "Time series involved in the mapping must have the same index");
    }
}
