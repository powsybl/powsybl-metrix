/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.serde.NetworkSerDe;
import com.powsybl.timeseries.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.threeten.extra.Interval;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.time.Duration;
import java.util.*;

import static com.powsybl.metrix.mapping.EquipmentGroupTimeSeriesMapperObserver.GROUP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class EquipmentGroupTimeSeriesMapperObserverTest {

    private final int chunkSize = 2;
    private final MappingParameters mappingParameters = MappingParameters.load();

    private FileSystem fileSystem;
    private Network network;
    private ReadOnlyTimeSeriesStore store;
    private TimeSeriesIndex regularIndex;

    @BeforeEach
    public void setUp() {
        this.fileSystem = Jimfs.newFileSystem(Configuration.unix());
        network = NetworkSerDe.read(Objects.requireNonNull(getClass().getResourceAsStream("/simpleNetwork.xml")));
        regularIndex = RegularTimeSeriesIndex.create(Interval.parse("2015-01-01T01:00:00Z/2015-01-01T02:00:00Z"), Duration.ofHours(1));
        store = new ReadOnlyTimeSeriesStoreCache(
                TimeSeries.createDouble("ts_10", regularIndex, 10d, 11d),
                TimeSeries.createDouble("ts_20", regularIndex, 20d, 21d)
        );
    }

    @AfterEach
    public void tearDown() throws IOException {
        this.fileSystem.close();
    }

    private final String mapToGeneratorsScript =
            String.join(System.lineSeparator(),
                    "mapToGenerators {",
                    "    timeSeriesName 'ts_10'",
                    "    filter { generator.id == 'FSSV.O11_G' }",
                    "}");

    final String provideGroupTsGenerators = String.join(System.lineSeparator(),
            "provideGroupTsGenerators {",
            "    filter { generator.terminal.voltageLevel.id == 'FSSV.O1' }",
            "    group %s",
            "}");

    final String provideGroupWithNameTsGenerators = String.join(System.lineSeparator(),
            "provideGroupTsGenerators {",
            "    filter { generator.terminal.voltageLevel.id == 'FSSV.O1' }",
            "    group VOLTAGE_LEVEL",
            "    withName 'userGivenName'",
            "}");

    final String mapToLoadsScript = String.join(System.lineSeparator(),
            "mapToLoads {",
            "    timeSeriesName %s",
            "    variable %s",
            "    filter { load.id == %s }",
            "}");

    final String provideGroupTsLoadsVoltageLevel = String.join(System.lineSeparator(),
            "provideGroupTsLoads {",
            "    filter { load.terminal.voltageLevel.id == 'FVALDI1' }",
            "    group VOLTAGE_LEVEL",
            "}");

    final String provideGroupWithNameTsLoadsVoltageLevel = String.join(System.lineSeparator(),
            "provideGroupTsLoads {",
            "    filter { load.terminal.voltageLevel.id == 'FVALDI1' }",
            "    group VOLTAGE_LEVEL",
            "    withName 'userGivenName'",
            "}");

    private TimeSeriesMappingConfig loadMappingConfig(String script) {
        return new TimeSeriesDslLoader(script).load(network, mappingParameters, store, new DataTableStore(), null);
    }

    private void runMapping(TimeSeriesMappingConfig mappingConfig, TimeSeriesMapperObserver observer) {
        TimeSeriesMapper mapper = new TimeSeriesMapper(mappingConfig, network, new TimeSeriesMappingLogger());
        TimeSeriesMapperParameters parameters = new TimeSeriesMapperParameters(new TreeSet<>(Collections.singleton(1)),
                Range.closed(0, 1), true, false, false, mappingParameters.getToleranceThreshold());
        mapper.mapToNetwork(store, parameters, ImmutableList.of(observer));
    }

    private void generatorTest(TimeSeriesMappingConfig mappingConfig, String expectedTimeSeriesName) {
        // Create observer
        TimeSeriesMapperObserver observer = new EquipmentGroupTimeSeriesMapperObserver(network, mappingConfig, chunkSize, Range.closed(0, chunkSize)) {
            @Override
            public void addTimeSeries(String timeSeriesName, int version, Range<Integer> pointRange, double[] values, Map<String, String> tags, TimeSeriesIndex index) {
                assertThat(timeSeriesName).isEqualTo(expectedTimeSeriesName);
                assertThat(version).isEqualTo(1);
                assertThat(index).isEqualTo(regularIndex);
                assertThat(pointRange).isEqualTo(Range.closed(0, chunkSize - 1));
                assertThat(values).isEqualTo(new double[]{10 + 480, 11 + 480});
                assertThat(tags).containsEntry("equipment", GROUP);
            }
        };

        // Run mapping
        runMapping(mappingConfig, observer);
    }

    private void checkLoadTest(Set<String> expectedTimeSeriesNames, String timeSeriesName, int version, Range<Integer> pointRange, Map<String, String> tags, TimeSeriesIndex index) {
        assertTrue(expectedTimeSeriesNames.contains(timeSeriesName));
        assertThat(version).isEqualTo(1);
        assertThat(index).isEqualTo(regularIndex);
        assertThat(pointRange).isEqualTo(Range.closed(0, chunkSize - 1));
        assertThat(tags).containsEntry("equipment", GROUP);
    }

    private void loadTest(TimeSeriesMappingConfig mappingConfig, double[] expectedVariableActivePowerValues, double[] expectedFixedActivePowerValues) {
        // Expected time series names
        final String variableActivePowerTimeSeriesName = "FVALDI1_variableActivePower";
        final String fixedActivePowerTimeSeriesName = "FVALDI1_fixedActivePower";
        final Set<String> expectedTimeSeriesNames = ImmutableSet.of(variableActivePowerTimeSeriesName, fixedActivePowerTimeSeriesName);

        // Create observer
        TimeSeriesMapperObserver observer = new EquipmentGroupTimeSeriesMapperObserver(network, mappingConfig, chunkSize, Range.closed(0, chunkSize)) {
            boolean isVariableActivePowerTimeSeriesOk = false;
            boolean isFixedActivePowerTimeSeriesOk = false;

            @Override
            public void addTimeSeries(String timeSeriesName, int version, Range<Integer> pointRange, double[] values, Map<String, String> tags, TimeSeriesIndex index) {
                checkLoadTest(expectedTimeSeriesNames, timeSeriesName, version, pointRange, tags, index);
                switch (timeSeriesName) {
                    case variableActivePowerTimeSeriesName:
                        isVariableActivePowerTimeSeriesOk = true;
                        assertThat(values).isEqualTo(expectedVariableActivePowerValues);
                        break;
                    case fixedActivePowerTimeSeriesName:
                        isFixedActivePowerTimeSeriesOk = true;
                        assertThat(values).isEqualTo(expectedFixedActivePowerValues);
                        break;
                    default:
                        fail();
                        break;
                }
            }

            @Override
            public void end() {
                // All expected time series are provided
                assertThat(isVariableActivePowerTimeSeriesOk).isTrue();
                assertThat(isFixedActivePowerTimeSeriesOk).isTrue();

            }
        };

        // Run mapping
        runMapping(mappingConfig, observer);
    }

    /*
     * GENERATOR TEST
     * 2 generators in FSSV.O1 voltageLevel :
     * FSSV.O11_G is mapped
     * FSSV.O12_G is not mapped
     */

    @Test
    void generatorVoltageLevelTest() {
        String script = String.join(System.lineSeparator(),
                mapToGeneratorsScript,
                String.format(provideGroupTsGenerators, "VOLTAGE_LEVEL"));
        TimeSeriesMappingConfig mappingConfig = loadMappingConfig(script);
        final String expectedTimeSeriesName = "FSSV.O1_" + EquipmentVariable.targetP.getVariableName();
        generatorTest(mappingConfig, expectedTimeSeriesName);
    }

    @Test
    void generatorSubstationTest() {
        String script = String.join(System.lineSeparator(),
                mapToGeneratorsScript,
                String.format(provideGroupTsGenerators, "SUBSTATION"));
        TimeSeriesMappingConfig mappingConfig = loadMappingConfig(script);
        final String expectedTimeSeriesName = "FSSV._" + EquipmentVariable.targetP.getVariableName();
        generatorTest(mappingConfig, expectedTimeSeriesName);
    }

    @Test
    void generatorWithNameTest() {
        String script = String.join(System.lineSeparator(),
                mapToGeneratorsScript,
                provideGroupWithNameTsGenerators);
        TimeSeriesMappingConfig mappingConfig = loadMappingConfig(script);
        final String expectedTimeSeriesName = "FSSV.O1_userGivenName_" + EquipmentVariable.targetP.getVariableName();
        generatorTest(mappingConfig, expectedTimeSeriesName);
    }

    /*
     * LOAD TEST
     * 2 loads in FVALDI1 voltageLevel :
     * FVALDI11_L  p0 = 470 with LoadDetail extension variableActivePower = 70 fixedActivePower = 400
     * FVALDI11_L2 p0 = 10
     */

    /*
     * FVALDI11_L mapped
     * FVALDI11_L2 not mapped
     */

    @Test
    void loadMapToP0OnLoadWithDetailTest() {
        String script = String.join(System.lineSeparator(),
                String.format(mapToLoadsScript, "\"ts_10\"", "p0", "\"FVALDI11_L\""),
                provideGroupTsLoadsVoltageLevel);
        TimeSeriesMappingConfig mappingConfig = loadMappingConfig(script);
        loadTest(mappingConfig, new double[]{10 + 10, 11 + 10}, new double[]{0, 0});
    }

    @Test
    void loadMapToVariableActivePowerOnLoadWithDetailTest() {
        String script = String.join(System.lineSeparator(),
                String.format(mapToLoadsScript, "\"ts_10\"", "variableActivePower", "\"FVALDI11_L\""),
                provideGroupTsLoadsVoltageLevel);
        TimeSeriesMappingConfig mappingConfig = loadMappingConfig(script);
        loadTest(mappingConfig, new double[]{10 + 10, 11 + 10}, new double[]{400, 400});
    }

    @Test
    void loadMapToFixedActivePowerOnLoadWithDetailTest() {
        String script = String.join(System.lineSeparator(),
                String.format(mapToLoadsScript, "\"ts_10\"", "fixedActivePower", "\"FVALDI11_L\""),
                provideGroupTsLoadsVoltageLevel);
        TimeSeriesMappingConfig mappingConfig = loadMappingConfig(script);
        loadTest(mappingConfig, new double[]{70 + 10, 70 + 10}, new double[]{10, 11});
    }

    @Test
    void loadMapToAllActivePowerOnLoadWithDetailTest() {
        String script = String.join(System.lineSeparator(),
                String.format(mapToLoadsScript, "\"ts_10\"", "variableActivePower", "\"FVALDI11_L\""),
                String.format(mapToLoadsScript, "\"ts_20\"", "fixedActivePower", "\"FVALDI11_L\""),
                provideGroupTsLoadsVoltageLevel);
        TimeSeriesMappingConfig mappingConfig = loadMappingConfig(script);
        loadTest(mappingConfig, new double[]{10 + 10, 11 + 10}, new double[]{20, 21});
    }

    /*
     * LOAD TEST
     * FVALDI11_L2 mapped
     * FVALDI11_L not mapped
     */

    @Test
    void loadMapToP0Test() {
        String script = String.join(System.lineSeparator(),
                String.format(mapToLoadsScript, "\"ts_10\"", "p0", "\"FVALDI11_L2\""),
                provideGroupTsLoadsVoltageLevel);
        TimeSeriesMappingConfig mappingConfig = loadMappingConfig(script);
        loadTest(mappingConfig, new double[]{70 + 10, 70 + 11}, new double[]{400, 400});
    }

    @Test
    void loadMapToVariableActivePowerTest() {
        String script = String.join(System.lineSeparator(),
                String.format(mapToLoadsScript, "\"ts_10\"", "variableActivePower", "\"FVALDI11_L2\""),
                provideGroupTsLoadsVoltageLevel);
        TimeSeriesMappingConfig mappingConfig = loadMappingConfig(script);
        loadTest(mappingConfig, new double[]{70 + 10, 70 + 11}, new double[]{400, 400});
    }

    @Test
    void loadMapToFixedActivePowerTest() {
        String script = String.join(System.lineSeparator(),
                String.format(mapToLoadsScript, "\"ts_10\"", "fixedActivePower", "\"FVALDI11_L2\""),
                provideGroupTsLoadsVoltageLevel);
        TimeSeriesMappingConfig mappingConfig = loadMappingConfig(script);
        loadTest(mappingConfig, new double[]{70, 70}, new double[]{400 + 10, 400 + 11});
    }

    @Test
    void loadMapToAllActivePowerTest() {
        String script = String.join(System.lineSeparator(),
                String.format(mapToLoadsScript, "\"ts_10\"", "variableActivePower", "\"FVALDI11_L2\""),
                String.format(mapToLoadsScript, "\"ts_20\"", "fixedActivePower", "\"FVALDI11_L2\""),
                provideGroupTsLoadsVoltageLevel);
        TimeSeriesMappingConfig mappingConfig = loadMappingConfig(script);
        loadTest(mappingConfig, new double[]{70 + 10, 70 + 11}, new double[]{400 + 20, 400 + 21});
    }

    @Test
    void loadMapToP0WithNameTest() {
        String script = String.join(System.lineSeparator(),
                String.format(mapToLoadsScript, "\"ts_10\"", "p0", "\"FVALDI11_L2\""),
                provideGroupWithNameTsLoadsVoltageLevel);
        TimeSeriesMappingConfig mappingConfig = loadMappingConfig(script);
        Set<String> actualTimeSeriesNames = new HashSet<>();
        // Create observer
        TimeSeriesMapperObserver observer = new EquipmentGroupTimeSeriesMapperObserver(network, mappingConfig, chunkSize, Range.closed(0, chunkSize)) {
            @Override
            public void addTimeSeries(String timeSeriesName, int version, Range<Integer> pointRange, double[] values, Map<String, String> tags, TimeSeriesIndex index) {
                actualTimeSeriesNames.add(timeSeriesName);
            }
        };

        // Run mapping
        runMapping(mappingConfig, observer);
        assertThat(actualTimeSeriesNames).hasSize(2);
        assertTrue(actualTimeSeriesNames.contains("FVALDI1_userGivenName_variableActivePower"));
        assertTrue(actualTimeSeriesNames.contains("FVALDI1_userGivenName_fixedActivePower"));
    }
}
