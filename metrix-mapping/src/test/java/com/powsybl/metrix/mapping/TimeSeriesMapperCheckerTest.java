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
import com.powsybl.commons.TestUtil;
import com.powsybl.commons.datasource.MemDataSource;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.extensions.HvdcAngleDroopActivePowerControl;
import com.powsybl.iidm.network.extensions.HvdcOperatorActivePowerRange;
import com.powsybl.iidm.xml.NetworkXml;
import com.powsybl.timeseries.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.threeten.extra.Interval;

import java.io.*;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class TimeSeriesMapperCheckerTest {

    private static final String INFO = "INFO";

    private static final String WARNING = "WARNING";

    private static final String VARIANT_ALL = "all";

    private static final String VARIANT_1 = "1";

    private static final String VARIANT_EMPTY = "";

    private static final String LABEL_SEPARATOR = " / ";

    private static final String BASE_CASE_RANGE_PROBLEM = "base case range problem" + LABEL_SEPARATOR;

    private static final String MAPPING_RANGE_PROBLEM = "mapping range problem" + LABEL_SEPARATOR;

    private static final String SCALING_DOWN_PROBLEM = "scaling down" + LABEL_SEPARATOR;

    private static final String LIMIT_CHANGE = "limit change" + LABEL_SEPARATOR;

    private static final String IL_DISABLED = LABEL_SEPARATOR + "IL disabled";

    private ReadOnlyTimeSeriesStore store;

    private final MappingParameters mappingParameters = MappingParameters.load();

    private final String emptyScript = String.join(System.lineSeparator(),
            "mapToLoads {",
            "    timeSeriesName 'chronique_nulle'",
            "}");

    private final String pmax2Script = String.join(System.lineSeparator(),
            "mapToGenerators {",
            "    timeSeriesName 'chronique_2000'",
            "    filter { generator.id == 'N_G' }",
            "}");

    private final String pmax3Script = String.join(System.lineSeparator(),
            "mapToGenerators {",
            "    timeSeriesName 'chronique_100'",
            "    filter { generator.id == 'N_G' }",
            "    variable maxP",
            "}");

    private final String pmax4Script = String.join(System.lineSeparator(),
            "mapToGenerators {",
            "    timeSeriesName 'chronique_100'",
            "    filter { generator.id == 'N_G' }",
            "    variable maxP",
            "}",
           "mapToGenerators {",
            "    timeSeriesName 'chronique_200'",
            "    filter { generator.id == 'N_G' }",
            "    variable targetP",
            "}");

    private final String pmax5Script = String.join(System.lineSeparator(),
            "mapToGenerators {",
            "    timeSeriesName 'chronique_10000'",
            "    filter { generator.id == 'N_G' }",
            "    variable maxP",
            "}",
            "mapToGenerators {",
            "    timeSeriesName 'chronique_1000'",
            "    filter { generator.id == 'N_G' }",
            "    variable targetP",
            "}");

    private final String pmin2aScript = String.join(System.lineSeparator(),
            "mapToGenerators {",
            "    timeSeriesName 'chronique_m2000'",
            "    filter { generator.id == 'N_G' }",
            "}");

    private final String pmin2bScript = String.join(System.lineSeparator(),
            "mapToGenerators {",
            "    timeSeriesName 'chronique_m2000'",
            "    filter { generator.id == 'N_G' }",
            "}");

    private final String pmin2cScript = String.join(System.lineSeparator(),
            "mapToGenerators {",
            "    timeSeriesName 'chronique_250'",
            "    filter { generator.id == 'N_G' }",
            "}");

    private final String pmin3aScript = String.join(System.lineSeparator(),
            "mapToGenerators {",
            "    timeSeriesName 'chronique_m100'",
            "    filter { generator.id == 'N_G' }",
            "    variable minP",
            "}");

    private final String pmin3bScript = String.join(System.lineSeparator(),
            "mapToGenerators {",
            "    timeSeriesName 'chronique_100'",
            "    filter { generator.id == 'N_G' }",
            "    variable minP",
            "}");

    private final String pmin3cScript = String.join(System.lineSeparator(),
            "mapToGenerators {",
            "    timeSeriesName 'chronique_500'",
            "    filter { generator.id == 'N_G' }",
            "    variable minP",
            "}");

    private final String pmin4aScript = String.join(System.lineSeparator(),
            "mapToGenerators {",
            "    timeSeriesName 'chronique_m100'",
            "    filter { generator.id == 'N_G' }",
            "    variable minP",
            "}",
            "mapToGenerators {",
            "    timeSeriesName 'chronique_m200'",
            "    filter { generator.id == 'N_G' }",
            "    variable targetP",
            "}");

    private final String pmin4bScript = String.join(System.lineSeparator(),
            "mapToGenerators {",
            "    timeSeriesName 'chronique_50'",
            "    filter { generator.id == 'N_G' }",
            "    variable minP",
            "}",
            "mapToGenerators {",
            "    timeSeriesName 'chronique_m200'",
            "    filter { generator.id == 'N_G' }",
            "    variable targetP",
            "}");

    private final String pmin4cScript = String.join(System.lineSeparator(),
            "mapToGenerators {",
            "    timeSeriesName 'chronique_500'",
            "    filter { generator.id == 'N_G' }",
            "    variable minP",
            "}",
            "mapToGenerators {",
            "    timeSeriesName 'chronique_100'",
            "    filter { generator.id == 'N_G' }",
            "    variable targetP",
            "}");

    private final String pmax2HvdcLineScript = String.join(System.lineSeparator(),
            "mapToHvdcLines {",
            "    timeSeriesName 'chronique_2000'",
            "    filter { hvdcLine.id == 'HVDC2' }",
            "}");

    private final String pmax3HvdcLineScript = String.join(System.lineSeparator(),
            "mapToHvdcLines {",
            "    timeSeriesName 'chronique_100'",
            "    variable maxP",
            "    filter { hvdcLine.id == 'HVDC2' }",
            "}");

    private final String pmax4HvdcLineScript = String.join(System.lineSeparator(),
            "mapToHvdcLines {",
            "    timeSeriesName 'chronique_100'",
            "    variable maxP",
            "    filter { hvdcLine.id == 'HVDC2' }",
            "}",
            "mapToHvdcLines {",
            "    timeSeriesName 'chronique_2000'",
            "    filter { hvdcLine.id == 'HVDC2' }",
            "}");

    private final String pmin2HvdcLineScript = String.join(System.lineSeparator(),
            "mapToHvdcLines {",
            "    timeSeriesName 'chronique_m2000'",
            "    filter { hvdcLine.id == 'HVDC2' }",
            "}");

    private final String pmin3HvdcLineScript = String.join(System.lineSeparator(),
            "mapToHvdcLines {",
            "    timeSeriesName 'chronique_m100'",
            "    filter { hvdcLine.id == 'HVDC2' }",
            "    variable minP",
            "}");

    private final String pmin4HvdcLineScript = String.join(System.lineSeparator(),
            "mapToHvdcLines {",
            "    timeSeriesName 'chronique_m100'",
            "    variable minP",
            "    filter { hvdcLine.id == 'HVDC2' }",
            "}",
            "mapToHvdcLines {",
            "    timeSeriesName 'chronique_m2000'",
            "    filter { hvdcLine.id == 'HVDC2' }",
            "}");

    private Network createNetwork() {
        Network network = NetworkXml.read(getClass().getResourceAsStream("/simpleNetwork.xiidm"));
        List<String> generators = ImmutableList.of("SO_G1", "SO_G2", "SE_G", "N_G");
        generators.forEach(id -> network.getGenerator(id).setTargetP(0));
        List<String> loads = ImmutableList.of("SO_L", "SE_L1", "SE_L2");
        loads.forEach(id -> network.getLoad(id).setP0(0));
        return network;
    }

    private void setHvdcLine(Network network, String id,
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

    private void compareNetworkPointGenerator(String id, MemDataSource dataSource, double expectedMinP, double expectedP, double expectedMaxP) {
        try (InputStream inputStream = dataSource.newInputStream("", "xiidm")) {
            Network networkPoint = NetworkXml.read(inputStream);
            Generator generator = networkPoint.getGenerator(id);
            assertEquals(expectedMinP, generator.getMinP(), 0);
            assertEquals(expectedP, generator.getTargetP(), 0);
            assertEquals(expectedMaxP, generator.getMaxP(), 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void compareNetworkPointHvdcLine(String id, MemDataSource dataSource, double expectedSetPoint, double expectedMaxP, Double expectedCS2toCS1, Double expectedCS1toCS2) {
        try (InputStream inputStream = dataSource.newInputStream("", "xiidm")) {
            Network networkPoint = NetworkXml.read(inputStream);
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

    private void compareBalance(BalanceSummary balanceSummary, double expectedBalance) {
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

    private void compareLogger(TimeSeriesMappingLogger logger, String expectedType, String expectedLabel, String expectedSynthesisLabel, String expectedVariant, String expectedMessage, String expectedSynthesisMessage) {
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

    private void testGenerator(Network network, String script, boolean ignoreLimits, String generator,
                      double expectedBalanceValue, double expectedMinP, double expectedP, double expectedMaxP) {
        testGenerator(network, script, ignoreLimits, generator, expectedBalanceValue, expectedMinP, expectedP, expectedMaxP, "", "", "", "", null, null);
    }

    private void testGenerator(Network network, String script, boolean ignoreLimits, String generator,
                      double expectedBalanceValue, double expectedMinP, double expectedP, double expectedMaxP,
                      String expectedType, String expectedLabel, String expectedSynthesisLabel, String expectedVariant, String expectedMessage, String expectedSynthesisMessage) {
        TimeSeriesMappingLogger logger = new TimeSeriesMappingLogger();
        TimeSeriesDslLoader dsl = new TimeSeriesDslLoader(script);
        TimeSeriesMappingConfig mappingConfig = dsl.load(network, mappingParameters, store, null);
        TimeSeriesMapper mapper = new TimeSeriesMapper(mappingConfig, network, logger);
        TimeSeriesMapperParameters parameters = new TimeSeriesMapperParameters(new TreeSet<>(Collections.singleton(1)),
                Range.closed(0, 0), ignoreLimits, false, true, mappingParameters.getToleranceThreshold());

        BalanceSummary balanceSummary = new BalanceSummary();
        MemDataSource dataSource = new MemDataSource();
        NetworkPointWriter networkPointWriter = new NetworkPointWriter(network, dataSource);
        mapper.mapToNetwork(store, parameters, ImmutableList.of(balanceSummary, networkPointWriter));

        compareNetworkPointGenerator(generator, dataSource, expectedMinP, expectedP, expectedMaxP);
        compareBalance(balanceSummary, expectedBalanceValue);
        compareLogger(logger, expectedType, expectedLabel, expectedSynthesisLabel, expectedVariant, expectedMessage, expectedSynthesisMessage);
    }

    private void testHvdcLine(double baseCaseSetpoint, boolean withExtensionHopr,
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

    private void testHvdcLine(Network network, String script, boolean ignoreLimits, String hvdcLine,
                              double expectedSetPoint, double expectedMaxP, double expectedCS2toCS1, double expectedCS1toCS2,
                              String expectedType, String expectedVariant, String expectedLabel, String expectedSynthesisLabel, String expectedMessage, String expectedSynthesisMessage) {
        TimeSeriesMappingLogger logger = new TimeSeriesMappingLogger();
        TimeSeriesDslLoader dsl = new TimeSeriesDslLoader(script);
        TimeSeriesMappingConfig mappingConfig = dsl.load(network, mappingParameters, store, null);
        TimeSeriesMapper mapper = new TimeSeriesMapper(mappingConfig, network, logger);
        TimeSeriesMapperParameters parameters = new TimeSeriesMapperParameters(new TreeSet<>(Collections.singleton(1)),
                Range.closed(0, 0), ignoreLimits, false, true, mappingParameters.getToleranceThreshold());

        MemDataSource dataSource = new MemDataSource();
        NetworkPointWriter networkPointWriter = new NetworkPointWriter(network, dataSource);
        mapper.mapToNetwork(store, parameters, ImmutableList.of(networkPointWriter));

        compareNetworkPointHvdcLine(hvdcLine, dataSource, expectedSetPoint, expectedMaxP, expectedCS2toCS1, expectedCS1toCS2);
        compareLogger(logger, expectedType, expectedLabel, expectedSynthesisLabel, expectedVariant, expectedMessage, expectedSynthesisMessage);
    }

    @BeforeEach
    public void setUp() {
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

    /*
     * GENERATOR TEST
     */

    @Test
    void pmax1Test() {
        Network network = createNetwork();
        network.getGenerator("N_G").setTargetP(2000);
        network.getGenerator("N_G").setMaxP(1000);

        // targetP not mapped
        // maxP not mapped
        // targetP > maxP

        String expectedLabelTargetP = BASE_CASE_RANGE_PROBLEM + "targetP changed to base case maxP";
        String expectedLabelMaxP = BASE_CASE_RANGE_PROBLEM + "maxP changed to base case targetP";

        // without ignore limits
        // -> targetP reduced to maxP = 1000
        testGenerator(NetworkXml.copy(network), emptyScript, false, "N_G", 1000, 0, 1000, 1000,
                WARNING, expectedLabelTargetP, expectedLabelTargetP, VARIANT_ALL,
                "targetP 2000 of N_G not included in 0 to 1000, targetP changed to 1000",
                null);

        // with ignore limits
        // -> maxP changed to targetP = 2000
        testGenerator(NetworkXml.copy(network), emptyScript, true, "N_G", 2000, 0, 2000, 2000,
                INFO, expectedLabelMaxP, expectedLabelMaxP, VARIANT_ALL,
                "targetP 2000 of N_G not included in 0 to 1000, maxP changed to 2000",
                null);
    }

    @Test
    void pmax2Test() {
        Network network = createNetwork();
        network.getGenerator("N_G").setMaxP(1000);

        // targetP mapped
        // maxP not mapped
        // mapped targetP > maxP

        String expectedLabel = SCALING_DOWN_PROBLEM + "at least one targetP changed to base case maxP";

        // without ignore limits
        // -> targetP reduced to maxP = 1000
        testGenerator(NetworkXml.copy(network), pmax2Script, false, "N_G", 1000, 0, 1000, 1000,
                WARNING, expectedLabel, expectedLabel, VARIANT_1,
                "Impossible to scale down 2000 of ts chronique_2000, targetP 1000 has been applied",
                "Impossible to scale down at least one value of ts chronique_2000, modified targetP has been applied");

        // with ignore limits
        // -> maxP changed to targetP = 2000
        testGenerator(NetworkXml.copy(network), pmax2Script, true, "N_G", 2000, 0, 2000, 2001,
                INFO, LIMIT_CHANGE + "maxP", SCALING_DOWN_PROBLEM + "at least one maxP increased", VARIANT_EMPTY,
                "maxP of N_G lower than targetP for 1 variants, maxP increased from 1000 to 2000",
                "maxP violated by targetP in scaling down of at least one value of ts chronique_2000, maxP has been increased for equipments");
    }

    @Test
    void pmax3Test() {
        Network network = createNetwork();
        network.getGenerator("N_G").setTargetP(200);
        network.getGenerator("N_G").setMaxP(300);

        // targetP not mapped
        // maxP mapped
        // targetP > mapped maxP

        String expectedLabel = MAPPING_RANGE_PROBLEM + "targetP changed to mapped maxP";
        String expectedLabelIL = expectedLabel + IL_DISABLED;
        String expectedMessage = "targetP 200 of N_G not included in 0 to 100, targetP changed to 100";

        // without ignore limits
        // -> targetP reduced to maxP = 100
        testGenerator(NetworkXml.copy(network), pmax3Script, false, "N_G", 100, 0, 100, 100,
                WARNING, expectedLabel, expectedLabel, VARIANT_1, expectedMessage, null);

        // with ignore limits
        // -> same as without ignore limits (ignore limits disabled)
        testGenerator(NetworkXml.copy(network), pmax3Script, true, "N_G", 100, 0, 100, 100,
                WARNING, expectedLabelIL, expectedLabelIL, VARIANT_1, expectedMessage, null);
    }

    @Test
    void pmax4Test() {
        Network network = createNetwork();
        network.getGenerator("N_G").setMaxP(300);

        // targetP mapped
        // maxP mapped
        // mapped targetP > mapped maxP

        String expectedLabel = SCALING_DOWN_PROBLEM + "at least one targetP changed to mapped maxP";
        String expectedLabelIL = expectedLabel + IL_DISABLED;
        String expectedMessage = "Impossible to scale down 200 of ts chronique_200, targetP 100 has been applied";
        String expectedSynthesisMessage = "Impossible to scale down at least one value of ts chronique_200, modified targetP has been applied";

        // without ignore limits
        // -> targetP reduced to maxP = 100
        testGenerator(NetworkXml.copy(network), pmax4Script, false, "N_G", 100, 0, 100, 100,
                WARNING, expectedLabel, expectedLabel, VARIANT_1, expectedMessage, expectedSynthesisMessage);

        // with ignore limits
        testGenerator(NetworkXml.copy(network), pmax4Script, true, "N_G", 100, 0, 100, 100,
                WARNING, expectedLabelIL, expectedLabelIL, VARIANT_1, expectedMessage, expectedSynthesisMessage);
    }

    @Test
    void pmax5Test() {
        Network network = createNetwork();
        network.getGenerator("N_G").setMaxP(100);

        // without ignore limits
        testGenerator(NetworkXml.copy(network), pmax5Script, false, "N_G", 1000, 0, 1000, 10000);

        // with ignore limits
        testGenerator(NetworkXml.copy(network), pmax5Script, true, "N_G", 1000, 0, 1000, 10000);
    }

    @Test
    void pmin1aTest() {
        Network network = createNetwork();
        network.getGenerator("N_G").setTargetP(-2000);
        network.getGenerator("N_G").setMinP(-1000);

        String expectedLabelTargetP = BASE_CASE_RANGE_PROBLEM + "targetP changed to base case minP";
        String expectedLabelMinP = BASE_CASE_RANGE_PROBLEM + "minP changed to base case targetP";

        // without ignore limits
        testGenerator(NetworkXml.copy(network), emptyScript, false, "N_G", -1000, -1000, -1000, 600,
                WARNING, expectedLabelTargetP, expectedLabelTargetP, VARIANT_ALL,
                "targetP -2000 of N_G not included in -1000 to 600, targetP changed to -1000",
                null);

        // with ignore limits
        testGenerator(NetworkXml.copy(network), emptyScript, true, "N_G", -2000, -2000, -2000, 600,
                INFO, expectedLabelMinP, expectedLabelMinP, VARIANT_ALL,
                "targetP -2000 of N_G not included in -1000 to 600, minP changed to -2000",
                null);
    }

    @Test
    void pmin1bTest() {
        Network network = createNetwork();
        network.getGenerator("N_G").setTargetP(-2000);
        network.getGenerator("N_G").setMaxP(2000);
        network.getGenerator("N_G").setMinP(1000);

        String expectedLabel = BASE_CASE_RANGE_PROBLEM + "targetP changed to 0";
        String expectedLabelIL = expectedLabel + IL_DISABLED;
        String expectedMessage = "targetP -2000 of N_G not included in 1000 to 2000, targetP changed to 0";

        // without ignore limits
        testGenerator(NetworkXml.copy(network), emptyScript, false, "N_G", 0, 1000, 0, 2000,
                WARNING, expectedLabel, expectedLabel, VARIANT_ALL, expectedMessage, null);

        // with ignore limits
        testGenerator(NetworkXml.copy(network), emptyScript, true, "N_G", 0, 1000, 0, 2000,
                WARNING, expectedLabelIL, expectedLabelIL, VARIANT_ALL, expectedMessage, null);
    }

    @Test
    void pmin1cTest() {
        Network network = createNetwork();
        network.getGenerator("N_G").setTargetP(500);
        network.getGenerator("N_G").setMaxP(2000);
        network.getGenerator("N_G").setMinP(1000);

        String expectedLabel = BASE_CASE_RANGE_PROBLEM + "base case minP violated by base case targetP";
        String expectedMessage = "targetP 500 of N_G not included in 1000 to 2000, but targetP has not been changed";

        // without ignore limits
        testGenerator(NetworkXml.copy(network), emptyScript, false, "N_G", 500, 1000, 500, 2000,
                INFO, expectedLabel, expectedLabel, VARIANT_ALL, expectedMessage, null);

        // with ignore limits
        testGenerator(NetworkXml.copy(network), emptyScript, true, "N_G", 500, 1000, 500, 2000,
                INFO, expectedLabel, expectedLabel, VARIANT_ALL, expectedMessage, null);
    }

    @Test
    void pmin2aTest() {
        Network network = createNetwork();
        network.getGenerator("N_G").setMinP(-1000);

        String expectedLabel = SCALING_DOWN_PROBLEM + "at least one targetP changed to base case minP";

        // without ignore limits
        testGenerator(NetworkXml.copy(network), pmin2aScript, false, "N_G", -1000, -1000, -1000, 600,
                WARNING, expectedLabel, expectedLabel, VARIANT_1,
                "Impossible to scale down -2000 of ts chronique_m2000, targetP -1000 has been applied",
                "Impossible to scale down at least one value of ts chronique_m2000, modified targetP has been applied");

        // with ignore limits
        testGenerator(NetworkXml.copy(network), pmin2aScript, true, "N_G", -2000, -2001, -2000, 600,
                INFO, LIMIT_CHANGE + "minP", SCALING_DOWN_PROBLEM + "at least one minP decreased", VARIANT_EMPTY,
                "minP of N_G higher than targetP for 1 variants, minP decreased from -1000 to -2000",
                "minP violated by targetP in scaling down of at least one value of ts chronique_m2000, minP has been decreased for equipments");
    }

    @Test
    void pmin2bTest() {
        Network network = createNetwork();
        network.getGenerator("N_G").setMaxP(1000);
        network.getGenerator("N_G").setMinP(500);
        network.getGenerator("N_G").setTargetP(600);

        String expectedLabel = SCALING_DOWN_PROBLEM + "at least one targetP changed to 0";
        String expectedLabelIL = expectedLabel + IL_DISABLED;
        String expectedMessage = "Impossible to scale down -2000 of ts chronique_m2000, targetP 0 has been applied";
        String expectedSynthesisMessage = "Impossible to scale down at least one value of ts chronique_m2000, modified targetP has been applied";

        // without ignore limits
        testGenerator(NetworkXml.copy(network), pmin2bScript, false, "N_G", 0, 500, 0, 1000,
                WARNING, expectedLabel, expectedLabel, VARIANT_1, expectedMessage, expectedSynthesisMessage);

        // with ignore limits
        testGenerator(NetworkXml.copy(network), pmin2bScript, true, "N_G", 0, 500, 0, 1000,
                WARNING, expectedLabelIL, expectedLabelIL, VARIANT_1, expectedMessage, expectedSynthesisMessage);
    }

    @Test
    void pmin2cTest() {
        Network network = createNetwork();
        network.getGenerator("N_G").setTargetP(750);
        network.getGenerator("N_G").setMaxP(1000);
        network.getGenerator("N_G").setMinP(500);

        String expectedLabel = SCALING_DOWN_PROBLEM + "base case minP violated by mapped targetP";
        String expectedSynthesisMessage = "Impossible to scale down at least one value of ts chronique_250, but aimed targetP of equipments have been applied";

        // without ignore limits
        testGenerator(NetworkXml.copy(network), pmin2cScript, false, "N_G", 250, 500, 250, 1000,
                INFO, expectedLabel, expectedLabel, VARIANT_1, null, expectedSynthesisMessage);

        // with ignore limits
        testGenerator(NetworkXml.copy(network), pmin2cScript, true, "N_G", 250, 500, 250, 1000,
                INFO, expectedLabel, expectedLabel, VARIANT_1, null, expectedSynthesisMessage);
    }

    @Test
    void pmin3aTest() {
        Network network = createNetwork();
        network.getGenerator("N_G").setMaxP(1000);
        network.getGenerator("N_G").setTargetP(-200);
        network.getGenerator("N_G").setMinP(-1000);

        String expectedLabel = MAPPING_RANGE_PROBLEM + "targetP changed to mapped minP";
        String expectedLabelIL = expectedLabel + IL_DISABLED;
        String expectedMessage = "targetP -200 of N_G not included in -100 to 1000, targetP changed to -100";

        // without ignore limits
        testGenerator(NetworkXml.copy(network), pmin3aScript, false, "N_G", -100, -100, -100, 1000,
                WARNING, expectedLabel, expectedLabel, VARIANT_1, expectedMessage, null);

        // with ignore limits
        testGenerator(NetworkXml.copy(network), pmin3aScript, true, "N_G", -100, -100, -100, 1000,
                WARNING, expectedLabelIL, expectedLabelIL, VARIANT_1, expectedMessage, null);
    }

    @Test
    void pmin3bTest() {
        Network network = createNetwork();
        network.getGenerator("N_G").setMaxP(1000);
        network.getGenerator("N_G").setTargetP(-200);
        network.getGenerator("N_G").setMinP(-1000);

        String expectedLabel = MAPPING_RANGE_PROBLEM + "targetP changed to 0";
        String expectedLabelIL = expectedLabel + IL_DISABLED;
        String expectedMessage = "targetP -200 of N_G not included in 100 to 1000, targetP changed to 0";

        // without ignore limits
        testGenerator(NetworkXml.copy(network), pmin3bScript, false, "N_G", 0, 100, 0, 1000,
                WARNING, expectedLabel, expectedLabel, VARIANT_1, expectedMessage, null);

        // with ignore limits
        testGenerator(NetworkXml.copy(network), pmin3bScript, true, "N_G", 0, 100, 0, 1000,
                WARNING, expectedLabelIL, expectedLabelIL, VARIANT_1, expectedMessage, null);
    }

    @Test
    void pmin3cTest() {
        Network network = createNetwork();
        network.getGenerator("N_G").setMaxP(1000);
        network.getGenerator("N_G").setTargetP(100);

        String expectedLabel = MAPPING_RANGE_PROBLEM + "mapped minP violated by mapped targetP";
        String expectedMessage = "targetP 100 of N_G not included in 500 to 1000, but targetP has not been changed";

        // without ignore limits
        testGenerator(NetworkXml.copy(network), pmin3cScript, false, "N_G", 100, 500, 100, 1000,
                INFO, expectedLabel, expectedLabel, VARIANT_1, expectedMessage, null);

        // with ignore limits
        testGenerator(NetworkXml.copy(network), pmin3cScript, true, "N_G", 100, 500, 100, 1000,
                INFO, expectedLabel, expectedLabel, VARIANT_1, expectedMessage, null);
    }

    @Test
    void pmin4aTest() {
        Network network = createNetwork();
        network.getGenerator("N_G").setMaxP(300);
        network.getGenerator("N_G").setMinP(-10);
        network.getGenerator("N_G").setTargetP(50);

        String expectedLabel = SCALING_DOWN_PROBLEM + "at least one targetP changed to mapped minP";
        String expectedLabelIL = expectedLabel + IL_DISABLED;
        String expectedMessage = "Impossible to scale down -200 of ts chronique_m200, targetP -100 has been applied";
        String expectedSynthesisMessage = "Impossible to scale down at least one value of ts chronique_m200, modified targetP has been applied";

        // without ignore limits
        testGenerator(NetworkXml.copy(network), pmin4aScript, false, "N_G", -100, -100, -100, 300,
                WARNING, expectedLabel, expectedLabel, VARIANT_1, expectedMessage, expectedSynthesisMessage);

        // with ignore limits
        testGenerator(NetworkXml.copy(network), pmin4aScript, true, "N_G", -100, -100, -100, 300,
                WARNING, expectedLabelIL, expectedLabelIL, VARIANT_1, expectedMessage, expectedSynthesisMessage);
    }

    @Test
    void pmin4bTest() {
        Network network = createNetwork();
        network.getGenerator("N_G").setMaxP(1000);
        network.getGenerator("N_G").setMinP(-300);
        network.getGenerator("N_G").setTargetP(10);

        String expectedLabel = SCALING_DOWN_PROBLEM + "at least one targetP changed to 0";
        String expectedLabelIL = expectedLabel + IL_DISABLED;
        String expectedMessage = "Impossible to scale down -200 of ts chronique_m200, targetP 0 has been applied";
        String expectedSynthesisMessage = "Impossible to scale down at least one value of ts chronique_m200, modified targetP has been applied";

        // without ignore limits
        testGenerator(NetworkXml.copy(network), pmin4bScript, false, "N_G", 0, 50, 0, 1000,
                WARNING, expectedLabel, expectedLabel, VARIANT_1, expectedMessage, expectedSynthesisMessage);

        // with ignore limits
        testGenerator(NetworkXml.copy(network), pmin4bScript, true, "N_G", 0, 50, 0, 1000,
                WARNING, expectedLabelIL, expectedLabelIL, VARIANT_1, expectedMessage, expectedSynthesisMessage);
    }

    @Test
    void pmin4cTest() {
        Network network = createNetwork();
        network.getGenerator("N_G").setMaxP(1000);
        network.getGenerator("N_G").setMinP(0);
        network.getGenerator("N_G").setTargetP(100);

        String expectedLabel = SCALING_DOWN_PROBLEM + "mapped minP violated by mapped targetP";
        String expectedSynthesisMessage = "Impossible to scale down at least one value of ts chronique_100, but aimed targetP of equipments have been applied";

        // without ignore limits
        testGenerator(NetworkXml.copy(network), pmin4cScript, false, "N_G", 100, 500, 100, 1000,
                INFO, expectedLabel, expectedLabel, VARIANT_1, null, expectedSynthesisMessage);

        // with ignore limits
        testGenerator(NetworkXml.copy(network), pmin4cScript, true, "N_G", 100, 500, 100, 1000,
                INFO, expectedLabel, expectedLabel, VARIANT_1, null, expectedSynthesisMessage);
    }

    /*
     * HVDC TEST
     * base case values HVDC2
     *       activePowerSetPoint = 0 maxP = 1011
     *       HvdcOperatorActivePowerRange fromCS1toCS2 = 1000 fromCS2toCS1 = 900
     *       HvdcAngleDroopActivePowerControl p0 = 100 enabled = false
     * 4 cases to test :
     *    HvdcOperatorActivePowerRange | HvdcAngleDroopActivePowerControl
     * 1) no                           | no
     * 2) yes                          | no
     * 3) no                           | yes (enabled)
     * 4) yes                          | yes (enabled)
     */

    @Test
    void pmax0HvdcLineTest() {
        Network network = createNetwork();
        setHvdcLine(network, "HVDC2", true, false, 0);
        HvdcOperatorActivePowerRange hvdcRange = network.getHvdcLine("HVDC2").getExtension(HvdcOperatorActivePowerRange.class);
        hvdcRange.setOprFromCS1toCS2(2000);

        // activePowerSetpoint not mapped
        // maxP not mapped
        // CS1toCS2 > maxP

        String expectedLabel = BASE_CASE_RANGE_PROBLEM + "CS1toCS2 changed to base case maxP";
        String expectedLabelIL = BASE_CASE_RANGE_PROBLEM + "maxP changed to base case CS1toCS2";

        // without ignore limits
        // -> CS1toCS2 reduced to maxP
        testHvdcLine(NetworkXml.copy(network), emptyScript, false, "HVDC2", 0, 1011, 900, 1011,
                WARNING,
                VARIANT_ALL,
                expectedLabel,
                expectedLabel,
                "CS1toCS2 2000 of HVDC2 not included in 0 to 1011, CS1toCS2 changed to 1011",
                null);

        // with ignore limits
        // -> maxP changed to CS1toCS2
        testHvdcLine(NetworkXml.copy(network), emptyScript, true, "HVDC2", 0, 2000, 900, 2000,
                INFO,
                VARIANT_ALL,
                expectedLabelIL,
                expectedLabelIL,
                "CS1toCS2 2000 of HVDC2 not included in 0 to 1011, maxP changed to 2000",
                null);
    }

    @Test
    void pmax1HvdcLineTest() {
        double baseCaseSetpoint = 2000;

        // activePowerSetpoint not mapped
        // maxP not mapped
        // activePowerSetpoint/p0 > maxP

        String expectedLabelSetpointToMaxP = BASE_CASE_RANGE_PROBLEM + "activePowerSetpoint changed to base case maxP";
        String expectedLabelSetpointToCS1toCS2 = BASE_CASE_RANGE_PROBLEM + "activePowerSetpoint changed to base case CS1toCS2";
        String expectedLabelMaxPToSetpoint = BASE_CASE_RANGE_PROBLEM + "maxP changed to base case activePowerSetpoint";
        String expectedLabelCS1toCS2ToSetpoint = BASE_CASE_RANGE_PROBLEM + "CS1toCS2 changed to base case activePowerSetpoint";

        // without ignore limits
        // -> activePowerSetpoint/p0 reduced to maxP/CS1toCS2
        testHvdcLine(baseCaseSetpoint, false,
                emptyScript, false, "HVDC2", 1011, 1011, 1011, 1011,
                WARNING,
                VARIANT_ALL,
                expectedLabelSetpointToMaxP,
                expectedLabelSetpointToMaxP,
                "activePowerSetpoint 2000 of HVDC2 not included in -1011 to 1011, activePowerSetpoint changed to 1011",
                null);

        testHvdcLine(baseCaseSetpoint, true,
                emptyScript, false, "HVDC2", 1000, 1011, 900, 1000,
                WARNING,
                VARIANT_ALL,
                expectedLabelSetpointToCS1toCS2,
                expectedLabelSetpointToCS1toCS2,
                "activePowerSetpoint 2000 of HVDC2 not included in -900 to 1000, activePowerSetpoint changed to 1000",
                null);

        // with ignore limits
        // -> maxP/CS1toCS2 changed to activePowerSetpoint/p0 = 2000
        testHvdcLine(baseCaseSetpoint, false,
                emptyScript, true, "HVDC2", 2000, 2000, 1011, 2000,
                INFO,
                VARIANT_ALL,
                expectedLabelMaxPToSetpoint,
                expectedLabelMaxPToSetpoint,
                "activePowerSetpoint 2000 of HVDC2 not included in -1011 to 1011, maxP changed to 2000",
                null);

        testHvdcLine(baseCaseSetpoint, true,
                emptyScript, true, "HVDC2", 2000, 2000, 900, 2000,
                INFO,
                VARIANT_ALL,
                expectedLabelCS1toCS2ToSetpoint,
                expectedLabelCS1toCS2ToSetpoint,
                "activePowerSetpoint 2000 of HVDC2 not included in -900 to 1000, CS1toCS2 changed to 2000",
                null);
    }

    @Test
    void pmax2HvdcLineTest() {
        double baseCaseSetpoint = 0;

        final String expectedSynthesisMessage = "Impossible to scale down at least one value of ts chronique_2000, modified activePowerSetpoint has been applied";

        // activePowerSetpoint mapped
        // maxP not mapped
        // mapped activePowerSetpoint > maxP/CS1toCS2

        String expectedLabelSetpointToMaxP = SCALING_DOWN_PROBLEM + "at least one activePowerSetpoint changed to base case maxP";
        String expectedLabelSetpointToCS1toCS2 = SCALING_DOWN_PROBLEM + "at least one activePowerSetpoint changed to base case CS1toCS2";

        // without ignore limits
        // -> mapped activePowerSetpoint reduced to maxP/CS1toCS2
        testHvdcLine(baseCaseSetpoint, false,
                pmax2HvdcLineScript, false, "HVDC2", 1011, 1011, 1011, 1011,
                WARNING,
                VARIANT_1,
                expectedLabelSetpointToMaxP,
                expectedLabelSetpointToMaxP,
                "Impossible to scale down 2000 of ts chronique_2000, activePowerSetpoint 1011 has been applied",
                expectedSynthesisMessage);

        testHvdcLine(baseCaseSetpoint, true,
                pmax2HvdcLineScript, false, "HVDC2", 1000, 1011, 900, 1000,
                WARNING,
                VARIANT_1,
                expectedLabelSetpointToCS1toCS2,
                expectedLabelSetpointToCS1toCS2,
                "Impossible to scale down 2000 of ts chronique_2000, activePowerSetpoint 1000 has been applied",
                expectedSynthesisMessage);

        // with ignore limits
        // -> maxP/CS1toCS2 changed to mapped activePowerSetpoint = 2000
        testHvdcLine(baseCaseSetpoint, false,
                pmax2HvdcLineScript, true, "HVDC2", 2000, 2001, 1011, 2001,
                INFO,
                VARIANT_EMPTY,
                LIMIT_CHANGE + "maxP",
                SCALING_DOWN_PROBLEM + "at least one maxP increased",
                "maxP of HVDC2 lower than activePowerSetpoint for 1 variants, maxP increased from 1011 to 2000",
                "maxP violated by activePowerSetpoint in scaling down of at least one value of ts chronique_2000, maxP has been increased for equipments");

        testHvdcLine(baseCaseSetpoint, true,
                pmax2HvdcLineScript, true, "HVDC2", 2000, 2001, 900, 2001,
                INFO,
                VARIANT_EMPTY,
                LIMIT_CHANGE + "CS1toCS2",
                SCALING_DOWN_PROBLEM + "at least one CS1toCS2 increased",
                "CS1toCS2 of HVDC2 lower than activePowerSetpoint for 1 variants, CS1toCS2 increased from 1000 to 2000",
                "CS1toCS2 violated by activePowerSetpoint in scaling down of at least one value of ts chronique_2000, CS1toCS2 has been increased for equipments");
    }

    @Test
    void pmax3HvdcLineTest() {
        double baseCaseSetpoint = 500;

        String expectedLabel = MAPPING_RANGE_PROBLEM + "activePowerSetpoint changed to mapped maxP";
        String expectedLabelIL = expectedLabel + IL_DISABLED;
        final String expectedMessage = "activePowerSetpoint 500 of HVDC2 not included in -1011 to 100, activePowerSetpoint changed to 100";
        final String expectedMessageHopr = "activePowerSetpoint 500 of HVDC2 not included in -900 to 100, activePowerSetpoint changed to 100";

        // activePowerSetpoint not mapped
        // maxP mapped
        // activePowerSetpoint/p0 > mapped maxP

        // without ignore limits
        // -> activePowerSetpoint/p0 reduced to mapped maxP
        testHvdcLine(baseCaseSetpoint, false,
                pmax3HvdcLineScript, false, "HVDC2", 100, 1011, 1011, 100,
                WARNING,
                VARIANT_1,
                expectedLabel,
                expectedLabel,
                expectedMessage,
                null);

        testHvdcLine(baseCaseSetpoint, true,
                pmax3HvdcLineScript, false, "HVDC2", 100, 1011, 900, 100,
                WARNING,
                VARIANT_1,
                expectedLabel,
                expectedLabel,
                expectedMessageHopr,
                null);

        // with ignore limits
        // -> same as without ignore limits (ignore limits disabled)
        testHvdcLine(baseCaseSetpoint, false,
                pmax3HvdcLineScript, true, "HVDC2", 100, 1011, 1011, 100,
                WARNING,
                VARIANT_1,
                expectedLabelIL,
                expectedLabelIL,
                expectedMessage,
                null);

        testHvdcLine(baseCaseSetpoint, true,
                pmax3HvdcLineScript, true, "HVDC2", 100, 1011, 900, 100,
                WARNING,
                VARIANT_1,
                expectedLabelIL,
                expectedLabelIL,
                expectedMessageHopr,
                null);
    }

    @Test
    void pmax4HvdcLineTest() {
        double baseCaseSetpoint = 0;

        final String expectedSynthesisMessage = "Impossible to scale down at least one value of ts chronique_2000, modified activePowerSetpoint has been applied";
        String expectedLabel = SCALING_DOWN_PROBLEM + "at least one activePowerSetpoint changed to mapped maxP";
        String expectedLabelIL = expectedLabel + IL_DISABLED;
        final String expectedMessage = "Impossible to scale down 2000 of ts chronique_2000, activePowerSetpoint 100 has been applied";

        // activePowerSetpoint mapped
        // maxP mapped
        // mapped activePowerSetpoint > mapped maxP

        // without ignore limits
        // -> mapped activePowerSetpoint reduced to mapped maxP
        testHvdcLine(baseCaseSetpoint, false,
                pmax4HvdcLineScript, false, "HVDC2", 100, 1011, 1011, 100,
                WARNING,
                VARIANT_1,
                expectedLabel,
                expectedLabel,
                expectedMessage,
                expectedSynthesisMessage);

        testHvdcLine(baseCaseSetpoint, true,
                pmax4HvdcLineScript, false, "HVDC2", 100, 1011, 900, 100,
                WARNING,
                VARIANT_1,
                expectedLabel,
                expectedLabel,
                expectedMessage,
                expectedSynthesisMessage);

        // with ignore limits
        // -> same as without ignore limits (ignore limits disabled)
        testHvdcLine(baseCaseSetpoint, false,
                pmax4HvdcLineScript, true, "HVDC2", 100, 1011, 1011, 100,
                WARNING,
                VARIANT_1,
                expectedLabelIL,
                expectedLabelIL,
                expectedMessage,
                expectedSynthesisMessage);

        testHvdcLine(baseCaseSetpoint, true,
                pmax4HvdcLineScript, true, "HVDC2", 100, 1011, 900, 100,
                WARNING,
                VARIANT_1,
                expectedLabelIL,
                expectedLabelIL,
                expectedMessage,
                expectedSynthesisMessage);
    }

    @Test
    void pmin0HvdcLineTest() {
        Network network = createNetwork();
        setHvdcLine(network, "HVDC2", true, false, 0);
        HvdcOperatorActivePowerRange hvdcRange = network.getHvdcLine("HVDC2").getExtension(HvdcOperatorActivePowerRange.class);
        hvdcRange.setOprFromCS2toCS1(2000);

        // activePowerSetpoint not mapped
        // minP not mapped
        // CS2toCS1 > maxP

        String expectedLabel = BASE_CASE_RANGE_PROBLEM + "-CS2toCS1 changed to base case -maxP";
        String expectedLabelIL = BASE_CASE_RANGE_PROBLEM + "-maxP changed to base case -CS2toCS1";

        // without ignore limits
        // -> CS2toCS1 reduced to minP
        testHvdcLine(NetworkXml.copy(network), emptyScript, false, "HVDC2", 0, 1011, 1011, 1000,
                WARNING,
                VARIANT_ALL,
                expectedLabel,
                expectedLabel,
                "-CS2toCS1 -2000 of HVDC2 not included in -1011 to 0, -CS2toCS1 changed to -1011",
                null);

        // with ignore limits
        // -> minP changed to CS2toCS1
        testHvdcLine(NetworkXml.copy(network), emptyScript, true, "HVDC2", 0, 2000, 2000, 1000,
                INFO,
                VARIANT_ALL,
                expectedLabelIL,
                expectedLabelIL,
                "-CS2toCS1 -2000 of HVDC2 not included in -1011 to 0, -maxP changed to -2000",
                null);
    }

    @Test
    void pmin1HvdcLineTest() {
        double baseCaseSetpoint = -2000;

        // activePowerSetpoint not mapped
        // minP not mapped
        // activePowerSetpoint/p0 < minP

        String expectedLabelSetpointToMaxP = BASE_CASE_RANGE_PROBLEM + "activePowerSetpoint changed to base case -maxP";
        String expectedLabelSetpointToCS2toCS1 = BASE_CASE_RANGE_PROBLEM + "activePowerSetpoint changed to base case -CS2toCS1";
        String expectedLabelMaxPToSetpoint = BASE_CASE_RANGE_PROBLEM + "-maxP changed to base case activePowerSetpoint";
        String expectedLabelCS2toCS1ToSetpoint = BASE_CASE_RANGE_PROBLEM + "-CS2toCS1 changed to base case activePowerSetpoint";

        // without ignore limits
        // -> activePowerSetpoint/p0 changed to minP/CS2toCS1
        testHvdcLine(baseCaseSetpoint, false,
                emptyScript, false, "HVDC2", -1011, 1011, 1011, 1011,
                WARNING,
                VARIANT_ALL,
                expectedLabelSetpointToMaxP,
                expectedLabelSetpointToMaxP,
                "activePowerSetpoint -2000 of HVDC2 not included in -1011 to 1011, activePowerSetpoint changed to -1011",
                null);

        testHvdcLine(baseCaseSetpoint, true,
                emptyScript, false, "HVDC2", -900, 1011, 900, 1000,
                WARNING,
                VARIANT_ALL,
                expectedLabelSetpointToCS2toCS1,
                expectedLabelSetpointToCS2toCS1,
                "activePowerSetpoint -2000 of HVDC2 not included in -900 to 1000, activePowerSetpoint changed to -900",
                null);

        // with ignore limits
        // -> minP/CS2toCS1 changed to activePowerSetpoint/p0 = 2000
        testHvdcLine(baseCaseSetpoint, false,
                emptyScript, true, "HVDC2", -2000, 2000, 2000, 1011,
                INFO,
                VARIANT_ALL,
                expectedLabelMaxPToSetpoint,
                expectedLabelMaxPToSetpoint,
                "activePowerSetpoint -2000 of HVDC2 not included in -1011 to 1011, -maxP changed to -2000",
                null);

        testHvdcLine(baseCaseSetpoint, true,
                emptyScript, true, "HVDC2", -2000, 2000, 2000, 1000,
                INFO,
                VARIANT_ALL,
                expectedLabelCS2toCS1ToSetpoint,
                expectedLabelCS2toCS1ToSetpoint,
                "activePowerSetpoint -2000 of HVDC2 not included in -900 to 1000, -CS2toCS1 changed to -2000",
                null);
    }

    @Test
    void pmin2HvdcLineTest() {
        double baseCaseSetpoint = 0;

        // activePowerSetpoint mapped
        // minP not mapped
        // mapped activePowerSetpoint/p0 < minP

        String expectedLabelSetpointToMinP = SCALING_DOWN_PROBLEM + "at least one activePowerSetpoint changed to base case -maxP";
        String expectedLabelSetpointToCS2toCS1 = SCALING_DOWN_PROBLEM + "at least one activePowerSetpoint changed to base case -CS2toCS1";

        // without ignore limits
        // -> activePowerSetpoint/p0 changed to minP/CS2toCS1
        testHvdcLine(baseCaseSetpoint, false,
                pmin2HvdcLineScript, false, "HVDC2", -1011, 1011, 1011, 1011,
                WARNING,
                VARIANT_1,
                expectedLabelSetpointToMinP,
                expectedLabelSetpointToMinP,
                "Impossible to scale down -2000 of ts chronique_m2000, activePowerSetpoint -1011 has been applied",
                "Impossible to scale down at least one value of ts chronique_m2000, modified activePowerSetpoint has been applied");

        testHvdcLine(baseCaseSetpoint, true,
                pmin2HvdcLineScript, false, "HVDC2", -900, 1011, 900, 1000,
                WARNING,
                VARIANT_1,
                expectedLabelSetpointToCS2toCS1,
                expectedLabelSetpointToCS2toCS1,
                "Impossible to scale down -2000 of ts chronique_m2000, activePowerSetpoint -900 has been applied",
                "Impossible to scale down at least one value of ts chronique_m2000, modified activePowerSetpoint has been applied");

        // with ignore limits
        // -> minP/CS2toCS1 changed to activePowerSetpoint/p0
        testHvdcLine(baseCaseSetpoint, false,
                pmin2HvdcLineScript, true, "HVDC2", -2000, 2001, 2001, 1011,
                INFO,
                VARIANT_EMPTY,
                LIMIT_CHANGE + "-maxP",
                SCALING_DOWN_PROBLEM + "at least one -maxP decreased",
                "-maxP of HVDC2 higher than activePowerSetpoint for 1 variants, -maxP decreased from -1011 to -2000",
                "-maxP violated by activePowerSetpoint in scaling down of at least one value of ts chronique_m2000, -maxP has been decreased for equipments");

        testHvdcLine(baseCaseSetpoint, true,
                pmin2HvdcLineScript, true, "HVDC2", -2000, 2001, 2001, 1000,
                INFO,
                VARIANT_EMPTY,
                LIMIT_CHANGE + "-CS2toCS1",
                SCALING_DOWN_PROBLEM + "at least one -CS2toCS1 decreased",
                "-CS2toCS1 of HVDC2 higher than activePowerSetpoint for 1 variants, -CS2toCS1 decreased from -900 to -2000",
                "-CS2toCS1 violated by activePowerSetpoint in scaling down of at least one value of ts chronique_m2000, -CS2toCS1 has been decreased for equipments");
    }

    @Test
    void pmin3HvdcLineTest() {
        double baseCaseSetpoint = -500;

        String expectedLabel = MAPPING_RANGE_PROBLEM + "activePowerSetpoint changed to mapped minP";
        String expectedLabelIL = expectedLabel + IL_DISABLED;
        final String expectedMessage = "activePowerSetpoint -500 of HVDC2 not included in -100 to 1011, activePowerSetpoint changed to -100";
        final String expectedMessageHopr = "activePowerSetpoint -500 of HVDC2 not included in -100 to 1000, activePowerSetpoint changed to -100";

        // activePowerSetpoint not mapped
        // minP mapped
        // activePowerSetpoint/p0 < mapped minP

        // without ignore limits
        // -> activePowerSetpoint/p0 changed to minP/CS2toCS1
        testHvdcLine(baseCaseSetpoint, false,
                pmin3HvdcLineScript, false, "HVDC2", -100, 1011, 100, 1011,
                WARNING,
                VARIANT_1,
                expectedLabel,
                expectedLabel,
                expectedMessage,
                null);

        testHvdcLine(baseCaseSetpoint, true,
                pmin3HvdcLineScript, false, "HVDC2", -100, 1011, 100, 1000,
                WARNING,
                VARIANT_1,
                expectedLabel,
                expectedLabel,
                expectedMessageHopr,
                null);

        // with ignore limits
        // -> minP/CS2toCS1 changed to activePowerSetpoint/p0
        testHvdcLine(baseCaseSetpoint, false,
                pmin3HvdcLineScript, true, "HVDC2", -100, 1011, 100, 1011,
                WARNING,
                VARIANT_1,
                expectedLabelIL,
                expectedLabelIL,
                expectedMessage,
                null);

        testHvdcLine(baseCaseSetpoint, true,
                pmin3HvdcLineScript, true, "HVDC2", -100, 1011, 100, 1000,
                WARNING,
                VARIANT_1,
                expectedLabelIL,
                expectedLabelIL,
                expectedMessageHopr,
                null);
    }

    @Test
    void pmin4HvdcLineTest() {
        double baseCaseSetpoint = 0;

        final String expectedSynthesisMessage = "Impossible to scale down at least one value of ts chronique_m2000, modified activePowerSetpoint has been applied";
        String expectedLabel = SCALING_DOWN_PROBLEM + "at least one activePowerSetpoint changed to mapped minP";
        String expectedLabelIL = expectedLabel + IL_DISABLED;
        final String expectedMessage = "Impossible to scale down -2000 of ts chronique_m2000, activePowerSetpoint -100 has been applied";

        // activePowerSetpoint mapped
        // minP mapped
        // mapped activePowerSetpoint/p0 < mapped minP

        // without ignore limits
        // -> activePowerSetpoint/p0 changed to mapped minP/CS2toCS1
        testHvdcLine(baseCaseSetpoint, false,
                pmin4HvdcLineScript, false, "HVDC2", -100, 1011, 100, 1011,
                WARNING,
                VARIANT_1,
                expectedLabel,
                expectedLabel,
                expectedMessage,
                expectedSynthesisMessage);

        testHvdcLine(baseCaseSetpoint, true,
                pmin4HvdcLineScript, false, "HVDC2", -100, 1011, 100, 1000,
                WARNING,
                VARIANT_1,
                expectedLabel,
                expectedLabel,
                expectedMessage,
                expectedSynthesisMessage);

        // with ignore limits
        // -> mapped minP/CS2toCS1 changed to activePowerSetpoint/p0
        testHvdcLine(baseCaseSetpoint, false,
                pmin4HvdcLineScript, true, "HVDC2", -100, 1011, 100, 1011,
                WARNING,
                VARIANT_1,
                expectedLabelIL,
                expectedLabelIL,
                expectedMessage,
                expectedSynthesisMessage);

        testHvdcLine(baseCaseSetpoint, true,
                pmin4HvdcLineScript, true, "HVDC2", -100, 1011, 100, 1000,
                WARNING,
                VARIANT_1,
                expectedLabelIL,
                expectedLabelIL,
                expectedMessage,
                expectedSynthesisMessage);
    }
}
