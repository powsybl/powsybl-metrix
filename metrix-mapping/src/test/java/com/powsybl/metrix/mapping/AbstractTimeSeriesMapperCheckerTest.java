/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping;

import com.google.common.collect.Range;
import com.powsybl.commons.datasource.MemDataSource;
import com.powsybl.commons.test.TestUtil;
import com.powsybl.iidm.network.Battery;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.extensions.HvdcAngleDroopActivePowerControl;
import com.powsybl.iidm.network.extensions.HvdcOperatorActivePowerRange;
import com.powsybl.iidm.serde.NetworkSerDe;
import com.powsybl.metrix.commons.data.datatable.DataTableStore;
import com.powsybl.metrix.mapping.balance.BalanceSummary;
import com.powsybl.metrix.mapping.config.TimeSeriesMappingConfig;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStore;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStoreCache;
import com.powsybl.timeseries.RegularTimeSeriesIndex;
import com.powsybl.timeseries.TimeSeries;
import com.powsybl.timeseries.TimeSeriesIndex;
import org.junit.jupiter.api.BeforeEach;
import org.threeten.extra.Interval;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * @author Paul Bui-Quang {@literal <paul.buiquang at rte-france.com>}
 */
abstract class AbstractTimeSeriesMapperCheckerTest {

    static final String BATTERY_ID = "NO_BATTERY";

    static final String INFO = "INFO";

    static final String WARNING = "WARNING";

    static final String VARIANT_ALL = "all";

    static final String VARIANT_1 = "1";

    static final String VARIANT_EMPTY = "";

    static final String LABEL_SEPARATOR = " / ";

    static final String BASE_CASE_RANGE_PROBLEM = "base case range problem" + LABEL_SEPARATOR;

    static final String MAPPING_RANGE_PROBLEM = "mapping range problem" + LABEL_SEPARATOR;

    static final String SCALING_DOWN_PROBLEM = "scaling down" + LABEL_SEPARATOR;

    static final String LIMIT_CHANGE = "limit change" + LABEL_SEPARATOR;

    static final String IL_DISABLED = LABEL_SEPARATOR + "IL disabled";

    ReadOnlyTimeSeriesStore store;

    final MappingParameters mappingParameters = MappingParameters.load();

    final String emptyScript = """
        mapToLoads {
            timeSeriesName 'chronique_nulle'
        }
        """;

    Network createNetwork() {
        Network network = NetworkSerDe.read(Objects.requireNonNull(getClass().getResourceAsStream("/simpleNetwork_with_battery.xiidm")));
        List<String> generators = List.of("SO_G1", "SO_G2", "SE_G", "N_G");
        generators.forEach(id -> network.getGenerator(id).setTargetP(0));
        List<String> loads = List.of("SO_L", "SE_L1", "SE_L2");
        loads.forEach(id -> network.getLoad(id).setP0(0));
        List<String> batteries = List.of("NO_BATTERY");
        batteries.forEach(id -> network.getBattery(id).setTargetP(0));
        return network;
    }

    void setHvdcLine(Network network, String id,
                             boolean withExtensionHopr,
                             boolean withExtensionHapcEnabled,
                             double setPoint) {

        HvdcLine hvdcLine = network.getHvdcLine(id);

        if (!withExtensionHopr) {
            hvdcLine.removeExtension(HvdcOperatorActivePowerRange.class);
        }
        if (!withExtensionHapcEnabled) {
            hvdcLine.removeExtension(HvdcAngleDroopActivePowerControl.class);
        } else {
            network.getHvdcLine(id).getExtension(HvdcAngleDroopActivePowerControl.class).setEnabled(true);
        }

        HvdcAngleDroopActivePowerControl activePowerControl = hvdcLine.getExtension(HvdcAngleDroopActivePowerControl.class);

        if (activePowerControl != null && activePowerControl.isEnabled()) {
            activePowerControl.setP0((float) setPoint);
        } else {
            hvdcLine.setActivePowerSetpoint(Math.abs(setPoint));
            hvdcLine.setConvertersMode(setPoint < 0 ? HvdcLine.ConvertersMode.SIDE_1_INVERTER_SIDE_2_RECTIFIER : HvdcLine.ConvertersMode.SIDE_1_RECTIFIER_SIDE_2_INVERTER);
        }
    }

    void compareNetworkPointGenerator(String id, MemDataSource dataSource, double expectedMinP, double expectedP, double expectedMaxP) {
        try (InputStream inputStream = dataSource.newInputStream("", "xiidm")) {
            Network networkPoint = NetworkSerDe.read(inputStream);
            Generator generator = networkPoint.getGenerator(id);
            assertEquals(expectedMinP, generator.getMinP(), 0);
            assertEquals(expectedP, generator.getTargetP(), 0);
            assertEquals(expectedMaxP, generator.getMaxP(), 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    void compareNetworkPointBattery(String id, MemDataSource dataSource, double expectedMinP, double expectedP, double expectedMaxP) {
        try (InputStream inputStream = dataSource.newInputStream("", "xiidm")) {
            Network networkPoint = NetworkSerDe.read(inputStream);
            Battery battery = networkPoint.getBattery(id);
            assertEquals(expectedMinP, battery.getMinP(), 0);
            assertEquals(expectedP, battery.getTargetP(), 0);
            assertEquals(expectedMaxP, battery.getMaxP(), 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    void compareNetworkPointHvdcLine(String id, MemDataSource dataSource, double expectedSetPoint, double expectedMaxP, Double expectedCS2toCS1, Double expectedCS1toCS2) {
        try (InputStream inputStream = dataSource.newInputStream("", "xiidm")) {
            Network networkPoint = NetworkSerDe.read(inputStream);
            HvdcLine hvdcLine = networkPoint.getHvdcLine(id);
            HvdcOperatorActivePowerRange activePowerRange = hvdcLine.getExtension(HvdcOperatorActivePowerRange.class);
            assertNotEquals(null, activePowerRange);
            assertEquals(expectedSetPoint, TimeSeriesMapper.getHvdcLineSetPoint(hvdcLine), 0);
            assertEquals(expectedCS2toCS1, activePowerRange.getOprFromCS2toCS1(), 0);
            assertEquals(expectedCS1toCS2, activePowerRange.getOprFromCS1toCS2(), 0);
            assertEquals(expectedMaxP, hvdcLine.getMaxP(), 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    void compareNetworkPointPhaseTapChanger(String id, MemDataSource dataSource, int expectedPhaseTapPosition) {
        try (InputStream inputStream = dataSource.newInputStream("", "xiidm")) {
            Network networkPoint = NetworkSerDe.read(inputStream);
            int tapPosition = networkPoint.getTwoWindingsTransformer(id).getPhaseTapChanger().getTapPosition();
            assertEquals(expectedPhaseTapPosition, tapPosition, 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    void compareBalance(BalanceSummary balanceSummary, double expectedBalance) {
        StringWriter balanceSummaryCsvOutput = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(balanceSummaryCsvOutput)) {
            balanceSummary.writeCsv(bufferedWriter);
            bufferedWriter.flush();
            String balanceSummaryCsv = balanceSummaryCsvOutput.toString();
            String[] lines = balanceSummaryCsv.split("\n");
            String[] line1 = lines[1].split(";");
            double actualBalance = Double.parseDouble(line1[1]);
            assertEquals(expectedBalance, actualBalance, 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    void compareLogger(TimeSeriesMappingLogger logger, String expectedType, String expectedLabel, String expectedSynthesisLabel, String expectedVariant, String expectedMessage, String expectedSynthesisMessage) {
        StringWriter loggerCsvOutput = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(loggerCsvOutput)) {
            logger.writeCsv(bufferedWriter);
            bufferedWriter.flush();
            String loggerCsv = TestUtil.normalizeLineSeparator(loggerCsvOutput.toString());
            String[] lines = loggerCsv.split("\n");
            int nbLines = lines.length;
            int nbExpectedLines = 1;
            nbExpectedLines += expectedMessage != null ? 1 : 0;
            nbExpectedLines += expectedSynthesisMessage != null ? 1 : 0;
            assertEquals(nbExpectedLines, nbLines, 0);
            if (expectedMessage != null) {
                String[] line = lines[1].split(";");
                String actualType = line[0];
                String actualLabel = line[1];
                String actualVariant = line[3];
                String actualMessage = line[5];
                assertEquals(expectedType, actualType);
                assertEquals(expectedLabel, actualLabel);
                assertEquals(expectedVariant, actualVariant);
                assertEquals(expectedMessage, actualMessage);
            }
            if (expectedSynthesisMessage != null) {
                int index = expectedMessage == null ? 1 : 2;
                String[] line = lines[index].split(";");
                String actualType = line[0];
                String actualLabel = line[1];
                String actualVariant = line[3];
                String actualMessage = line[5];
                assertEquals(expectedType, actualType);
                assertEquals(expectedSynthesisLabel + LABEL_SEPARATOR + "TS synthesis", actualLabel);
                assertEquals("", actualVariant);
                assertEquals(expectedSynthesisMessage, actualMessage);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    void testGenerator(Network network, String script, boolean ignoreLimits, String generator,
                      double expectedBalanceValue, double expectedMinP, double expectedP, double expectedMaxP) {
        testGenerator(network, script, ignoreLimits, generator, expectedBalanceValue, expectedMinP, expectedP, expectedMaxP, "", "", "", "", null, null);
    }

    void testGenerator(Network network, String script, boolean ignoreLimits, String generator,
                               double expectedBalanceValue, double expectedMinP, double expectedP, double expectedMaxP,
                               String expectedType, String expectedLabel, String expectedSynthesisLabel, String expectedVariant, String expectedMessage, String expectedSynthesisMessage) {
        TimeSeriesMappingLogger logger = new TimeSeriesMappingLogger();
        TimeSeriesDslLoader dsl = new TimeSeriesDslLoader(script);
        TimeSeriesMappingConfig mappingConfig = dsl.load(network, mappingParameters, store, new DataTableStore(), null);
        TimeSeriesMapperParameters parameters = new TimeSeriesMapperParameters(new TreeSet<>(Collections.singleton(1)),
            Range.closed(0, 0), ignoreLimits, false, true, mappingParameters.getToleranceThreshold());
        TimeSeriesMapper mapper = new TimeSeriesMapper(mappingConfig, parameters, network, logger);

        BalanceSummary balanceSummary = new BalanceSummary();
        MemDataSource dataSource = new MemDataSource();
        NetworkPointWriter networkPointWriter = new NetworkPointWriter(network, dataSource);
        mapper.mapToNetwork(store, List.of(balanceSummary, networkPointWriter));

        compareNetworkPointGenerator(generator, dataSource, expectedMinP, expectedP, expectedMaxP);
        compareBalance(balanceSummary, expectedBalanceValue);
        compareLogger(logger, expectedType, expectedLabel, expectedSynthesisLabel, expectedVariant, expectedMessage, expectedSynthesisMessage);
    }

    void testBattery(Network network, String script, boolean ignoreLimits, String batteryId,
                               double expectedBalanceValue, double expectedMinP, double expectedP, double expectedMaxP) {
        testBattery(network, script, ignoreLimits, batteryId, expectedBalanceValue, expectedMinP, expectedP, expectedMaxP, "", "", "", "", null, null);
    }

    void testBattery(Network network, String script, boolean ignoreLimits, String batteryId,
                               double expectedBalanceValue, double expectedMinP, double expectedP, double expectedMaxP,
                               String expectedType, String expectedLabel, String expectedSynthesisLabel, String expectedVariant, String expectedMessage, String expectedSynthesisMessage) {
        TimeSeriesMappingLogger logger = new TimeSeriesMappingLogger();
        TimeSeriesDslLoader dsl = new TimeSeriesDslLoader(script);
        TimeSeriesMappingConfig mappingConfig = dsl.load(network, mappingParameters, store, new DataTableStore(), null);
        TimeSeriesMapperParameters parameters = new TimeSeriesMapperParameters(new TreeSet<>(Collections.singleton(1)),
            Range.closed(0, 0), ignoreLimits, false, true, mappingParameters.getToleranceThreshold());
        TimeSeriesMapper mapper = new TimeSeriesMapper(mappingConfig, parameters, network, logger);

        BalanceSummary balanceSummary = new BalanceSummary();
        MemDataSource dataSource = new MemDataSource();
        NetworkPointWriter networkPointWriter = new NetworkPointWriter(network, dataSource);
        mapper.mapToNetwork(store, List.of(balanceSummary, networkPointWriter));

        compareNetworkPointBattery(batteryId, dataSource, expectedMinP, expectedP, expectedMaxP);
        compareBalance(balanceSummary, expectedBalanceValue);
        compareLogger(logger, expectedType, expectedLabel, expectedSynthesisLabel, expectedVariant, expectedMessage, expectedSynthesisMessage);
    }

    void testHvdcLine(double baseCaseSetpoint, boolean withExtensionHopr,
                              String script, boolean ignoreLimits, String hvdcLine,
                              double expectedSetPoint, double expectedMaxP, double expectedCS2toCS1, double expectedCS1toCS2,
                              String expectedType,
                              String expectedVariant,
                              String expectedLabel,
                              String expectedSynthesisLabel,
                              String expectedMessage,
                              String expectedMessageSynthesis) {

        Network networkActivePowerSetpoint = createNetwork();
        Network networkP0 = createNetwork();

        setHvdcLine(networkActivePowerSetpoint, hvdcLine, withExtensionHopr, false, baseCaseSetpoint);
        setHvdcLine(networkP0, hvdcLine, withExtensionHopr, true, baseCaseSetpoint);

        testHvdcLine(networkActivePowerSetpoint, script, ignoreLimits, hvdcLine, expectedSetPoint, expectedMaxP, expectedCS2toCS1, expectedCS1toCS2,
                expectedType,
                expectedVariant,
                expectedLabel,
                expectedSynthesisLabel,
                expectedMessage,
                expectedMessageSynthesis);

        testHvdcLine(networkP0, script, ignoreLimits, hvdcLine, expectedSetPoint, expectedMaxP, expectedCS2toCS1, expectedCS1toCS2,
                expectedType,
                expectedVariant,
                expectedLabel,
                expectedSynthesisLabel,
                expectedMessage,
                expectedMessageSynthesis);
    }

    void testHvdcLine(Network network, String script, boolean ignoreLimits, String hvdcLine,
                              double expectedSetPoint, double expectedMaxP, double expectedCS2toCS1, double expectedCS1toCS2,
                              String expectedType, String expectedVariant, String expectedLabel, String expectedSynthesisLabel, String expectedMessage, String expectedSynthesisMessage) {
        TimeSeriesMappingLogger logger = new TimeSeriesMappingLogger();
        TimeSeriesDslLoader dsl = new TimeSeriesDslLoader(script);
        TimeSeriesMappingConfig mappingConfig = dsl.load(network, mappingParameters, store, new DataTableStore(), null);
        TimeSeriesMapperParameters parameters = new TimeSeriesMapperParameters(new TreeSet<>(Collections.singleton(1)),
                Range.closed(0, 0), ignoreLimits, false, true, mappingParameters.getToleranceThreshold());
        TimeSeriesMapper mapper = new TimeSeriesMapper(mappingConfig, parameters, network, logger);

        MemDataSource dataSource = new MemDataSource();
        NetworkPointWriter networkPointWriter = new NetworkPointWriter(network, dataSource);
        mapper.mapToNetwork(store, List.of(networkPointWriter));

        compareNetworkPointHvdcLine(hvdcLine, dataSource, expectedSetPoint, expectedMaxP, expectedCS2toCS1, expectedCS1toCS2);
        compareLogger(logger, expectedType, expectedLabel, expectedSynthesisLabel, expectedVariant, expectedMessage, expectedSynthesisMessage);
    }

    void testPhaseTapChanger(Network network, String script, String twoWindingsTransformer,
                                     int expectedPhaseTapPosition,
                                     String expectedType, String expectedVariant, String expectedLabel, String expectedSynthesisLabel, String expectedMessage, String expectedSynthesisMessage) {
        TimeSeriesMappingLogger logger = new TimeSeriesMappingLogger();
        TimeSeriesDslLoader dsl = new TimeSeriesDslLoader(script);
        TimeSeriesMappingConfig mappingConfig = dsl.load(network, mappingParameters, store, new DataTableStore(), null);
        TimeSeriesMapperParameters parameters = new TimeSeriesMapperParameters(new TreeSet<>(Collections.singleton(1)),
                Range.closed(0, 0), false, false, true, mappingParameters.getToleranceThreshold());
        TimeSeriesMapper mapper = new TimeSeriesMapper(mappingConfig, parameters, network, logger);

        MemDataSource dataSource = new MemDataSource();
        NetworkPointWriter networkPointWriter = new NetworkPointWriter(network, dataSource);
        mapper.mapToNetwork(store, List.of(networkPointWriter));

        compareNetworkPointPhaseTapChanger(twoWindingsTransformer, dataSource, expectedPhaseTapPosition);
        compareLogger(logger, expectedType, expectedLabel, expectedSynthesisLabel, expectedVariant, expectedMessage, expectedSynthesisMessage);
    }

    @BeforeEach
    void setUp() {
        // create time series space mock
        TimeSeriesIndex index = RegularTimeSeriesIndex.create(Interval.parse("1970-01-01T00:00:00Z/1970-01-01T01:00:00Z"), Duration.ofHours(1));

        store = new ReadOnlyTimeSeriesStoreCache(
                TimeSeries.createDouble("chronique_m2000", index, -2000d, -2000d),
                TimeSeries.createDouble("chronique_m200", index, -200d, -200d),
                TimeSeries.createDouble("chronique_m100", index, -100d, -100d),
                TimeSeries.createDouble("chronique_nulle", index, 0d, 0d),
                TimeSeries.createDouble("chronique_10000", index, 10000d, 10000d),
                TimeSeries.createDouble("chronique_2000", index, 2000d, 2000d),
                TimeSeries.createDouble("chronique_1000", index, 1000d, 1000d),
                TimeSeries.createDouble("chronique_500", index, 500d, 500d),
                TimeSeries.createDouble("chronique_200", index, 200d, 200d),
                TimeSeries.createDouble("chronique_250", index, 250d, 250d),
                TimeSeries.createDouble("chronique_100", index, 100d, 100d),
                TimeSeries.createDouble("chronique_50", index, 50d, 50d));
    }

}
