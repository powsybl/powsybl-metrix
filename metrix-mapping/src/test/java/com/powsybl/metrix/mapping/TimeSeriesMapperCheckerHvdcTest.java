/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.iidm.network.extensions.HvdcOperatorActivePowerRange;
import com.powsybl.iidm.serde.NetworkSerDe;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Paul Bui-Quang {@literal <paul.buiquang at rte-france.com>}
 */
class TimeSeriesMapperCheckerHvdcTest extends AbstractTimeSeriesMapperCheckerTest {

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

    private final String phaseTapChangerLowTapPositionScript = String.join(System.lineSeparator(),
            "mapToPhaseTapChangers {",
            "    timeSeriesName 'chronique_m100'",
            "    filter { twoWindingsTransformer.id == 'NE_NO_1' }",
            "}");

    private final String phaseTapChangerHighTapPositionScript = String.join(System.lineSeparator(),
            "mapToPhaseTapChangers {",
            "    timeSeriesName 'chronique_100'",
            "    filter { twoWindingsTransformer.id == 'NE_NO_1' }",
            "}");

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
        testGenerator(NetworkSerDe.copy(network), emptyScript, false, "N_G", 1000, 0, 1000, 1000,
                WARNING, expectedLabelTargetP, expectedLabelTargetP, VARIANT_ALL,
                "targetP 2000 of N_G not included in 0 to 1000, targetP changed to 1000",
                null);

        // with ignore limits
        // -> maxP changed to targetP = 2000
        testGenerator(NetworkSerDe.copy(network), emptyScript, true, "N_G", 2000, 0, 2000, 2000,
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
        testGenerator(NetworkSerDe.copy(network), pmax2Script, false, "N_G", 1000, 0, 1000, 1000,
                WARNING, expectedLabel, expectedLabel, VARIANT_1,
                "Impossible to scale down 2000 of ts chronique_2000, targetP 1000 has been applied",
                "Impossible to scale down at least one value of ts chronique_2000, modified targetP has been applied");

        // with ignore limits
        // -> maxP changed to targetP = 2000
        testGenerator(NetworkSerDe.copy(network), pmax2Script, true, "N_G", 2000, 0, 2000, 2001,
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
        testGenerator(NetworkSerDe.copy(network), pmax3Script, false, "N_G", 100, 0, 100, 100,
                WARNING, expectedLabel, expectedLabel, VARIANT_1, expectedMessage, null);

        // with ignore limits
        // -> same as without ignore limits (ignore limits disabled)
        testGenerator(NetworkSerDe.copy(network), pmax3Script, true, "N_G", 100, 0, 100, 100,
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
        testGenerator(NetworkSerDe.copy(network), pmax4Script, false, "N_G", 100, 0, 100, 100,
                WARNING, expectedLabel, expectedLabel, VARIANT_1, expectedMessage, expectedSynthesisMessage);

        // with ignore limits
        testGenerator(NetworkSerDe.copy(network), pmax4Script, true, "N_G", 100, 0, 100, 100,
                WARNING, expectedLabelIL, expectedLabelIL, VARIANT_1, expectedMessage, expectedSynthesisMessage);
    }

    @Test
    void pmax5Test() {
        Network network = createNetwork();
        network.getGenerator("N_G").setMaxP(100);

        // without ignore limits
        testGenerator(NetworkSerDe.copy(network), pmax5Script, false, "N_G", 1000, 0, 1000, 10000);

        // with ignore limits
        testGenerator(NetworkSerDe.copy(network), pmax5Script, true, "N_G", 1000, 0, 1000, 10000);
    }

    @Test
    void pmin1aTest() {
        Network network = createNetwork();
        network.getGenerator("N_G").setTargetP(-2000);
        network.getGenerator("N_G").setMinP(-1000);

        String expectedLabelTargetP = BASE_CASE_RANGE_PROBLEM + "targetP changed to base case minP";
        String expectedLabelMinP = BASE_CASE_RANGE_PROBLEM + "minP changed to base case targetP";

        // without ignore limits
        testGenerator(NetworkSerDe.copy(network), emptyScript, false, "N_G", -1000, -1000, -1000, 600,
                WARNING, expectedLabelTargetP, expectedLabelTargetP, VARIANT_ALL,
                "targetP -2000 of N_G not included in -1000 to 600, targetP changed to -1000",
                null);

        // with ignore limits
        testGenerator(NetworkSerDe.copy(network), emptyScript, true, "N_G", -2000, -2000, -2000, 600,
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
        testGenerator(NetworkSerDe.copy(network), emptyScript, false, "N_G", 0, 1000, 0, 2000,
                WARNING, expectedLabel, expectedLabel, VARIANT_ALL, expectedMessage, null);

        // with ignore limits
        testGenerator(NetworkSerDe.copy(network), emptyScript, true, "N_G", 0, 1000, 0, 2000,
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
        testGenerator(NetworkSerDe.copy(network), emptyScript, false, "N_G", 500, 1000, 500, 2000,
                INFO, expectedLabel, expectedLabel, VARIANT_ALL, expectedMessage, null);

        // with ignore limits
        testGenerator(NetworkSerDe.copy(network), emptyScript, true, "N_G", 500, 1000, 500, 2000,
                INFO, expectedLabel, expectedLabel, VARIANT_ALL, expectedMessage, null);
    }

    @Test
    void pmin2aTest() {
        Network network = createNetwork();
        network.getGenerator("N_G").setMinP(-1000);

        String expectedLabel = SCALING_DOWN_PROBLEM + "at least one targetP changed to base case minP";

        // without ignore limits
        testGenerator(NetworkSerDe.copy(network), pmin2aScript, false, "N_G", -1000, -1000, -1000, 600,
                WARNING, expectedLabel, expectedLabel, VARIANT_1,
                "Impossible to scale down -2000 of ts chronique_m2000, targetP -1000 has been applied",
                "Impossible to scale down at least one value of ts chronique_m2000, modified targetP has been applied");

        // with ignore limits
        testGenerator(NetworkSerDe.copy(network), pmin2aScript, true, "N_G", -2000, -2001, -2000, 600,
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
        testGenerator(NetworkSerDe.copy(network), pmin2bScript, false, "N_G", 0, 500, 0, 1000,
                WARNING, expectedLabel, expectedLabel, VARIANT_1, expectedMessage, expectedSynthesisMessage);

        // with ignore limits
        testGenerator(NetworkSerDe.copy(network), pmin2bScript, true, "N_G", 0, 500, 0, 1000,
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
        testGenerator(NetworkSerDe.copy(network), pmin2cScript, false, "N_G", 250, 500, 250, 1000,
                INFO, expectedLabel, expectedLabel, VARIANT_1, null, expectedSynthesisMessage);

        // with ignore limits
        testGenerator(NetworkSerDe.copy(network), pmin2cScript, true, "N_G", 250, 500, 250, 1000,
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
        testGenerator(NetworkSerDe.copy(network), pmin3aScript, false, "N_G", -100, -100, -100, 1000,
                WARNING, expectedLabel, expectedLabel, VARIANT_1, expectedMessage, null);

        // with ignore limits
        testGenerator(NetworkSerDe.copy(network), pmin3aScript, true, "N_G", -100, -100, -100, 1000,
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
        testGenerator(NetworkSerDe.copy(network), pmin3bScript, false, "N_G", 0, 100, 0, 1000,
                WARNING, expectedLabel, expectedLabel, VARIANT_1, expectedMessage, null);

        // with ignore limits
        testGenerator(NetworkSerDe.copy(network), pmin3bScript, true, "N_G", 0, 100, 0, 1000,
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
        testGenerator(NetworkSerDe.copy(network), pmin3cScript, false, "N_G", 100, 500, 100, 1000,
                INFO, expectedLabel, expectedLabel, VARIANT_1, expectedMessage, null);

        // with ignore limits
        testGenerator(NetworkSerDe.copy(network), pmin3cScript, true, "N_G", 100, 500, 100, 1000,
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
        testGenerator(NetworkSerDe.copy(network), pmin4aScript, false, "N_G", -100, -100, -100, 300,
                WARNING, expectedLabel, expectedLabel, VARIANT_1, expectedMessage, expectedSynthesisMessage);

        // with ignore limits
        testGenerator(NetworkSerDe.copy(network), pmin4aScript, true, "N_G", -100, -100, -100, 300,
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
        testGenerator(NetworkSerDe.copy(network), pmin4bScript, false, "N_G", 0, 50, 0, 1000,
                WARNING, expectedLabel, expectedLabel, VARIANT_1, expectedMessage, expectedSynthesisMessage);

        // with ignore limits
        testGenerator(NetworkSerDe.copy(network), pmin4bScript, true, "N_G", 0, 50, 0, 1000,
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
        testGenerator(NetworkSerDe.copy(network), pmin4cScript, false, "N_G", 100, 500, 100, 1000,
                INFO, expectedLabel, expectedLabel, VARIANT_1, null, expectedSynthesisMessage);

        // with ignore limits
        testGenerator(NetworkSerDe.copy(network), pmin4cScript, true, "N_G", 100, 500, 100, 1000,
                INFO, expectedLabel, expectedLabel, VARIANT_1, null, expectedSynthesisMessage);
    }

    /*
     * BATTERY TEST
     */

    @Test
    void pmax1BatteryTest() {
        Network network = createNetwork();
        network.getBattery(BATTERY_ID).setTargetP(2000);
        network.getBattery(BATTERY_ID).setMaxP(1000);

        // targetP not mapped
        // maxP not mapped
        // targetP > maxP

        String expectedLabelTargetP = BASE_CASE_RANGE_PROBLEM + "targetP changed to base case maxP";
        String expectedLabelMaxP = BASE_CASE_RANGE_PROBLEM + "maxP changed to base case targetP";

        // without ignore limits
        // -> targetP reduced to maxP = 1000
        testBattery(NetworkSerDe.copy(network), emptyScript, false, BATTERY_ID, 0, 0, 1000, 1000,
            WARNING, expectedLabelTargetP, expectedLabelTargetP, VARIANT_ALL,
            "targetP 2000 of NO_BATTERY not included in 0 to 1000, targetP changed to 1000",
            null);

        // with ignore limits
        // -> maxP changed to targetP = 2000
        testBattery(NetworkSerDe.copy(network), emptyScript, true, BATTERY_ID, 0, 0, 2000, 2000,
            INFO, expectedLabelMaxP, expectedLabelMaxP, VARIANT_ALL,
            "targetP 2000 of NO_BATTERY not included in 0 to 1000, maxP changed to 2000",
            null);
    }

    @Test
    void pmax2BatteryTest() {
        Network network = createNetwork();
        network.getBattery(BATTERY_ID).setMaxP(1000);
        String script = """
            mapToBatteries {
                timeSeriesName 'chronique_2000'
                filter { battery.id == 'NO_BATTERY' }
            }""";

        // targetP mapped
        // maxP not mapped
        // mapped targetP > maxP

        String expectedLabel = SCALING_DOWN_PROBLEM + "at least one targetP changed to base case maxP";

        // without ignore limits
        // -> targetP reduced to maxP = 1000
        testBattery(NetworkSerDe.copy(network), script, false, BATTERY_ID, 0, 0, 1000, 1000,
            WARNING, expectedLabel, expectedLabel, VARIANT_1,
            "Impossible to scale down 2000 of ts chronique_2000, targetP 1000 has been applied",
            "Impossible to scale down at least one value of ts chronique_2000, modified targetP has been applied");

        // with ignore limits
        // -> maxP changed to targetP = 2000
        testBattery(NetworkSerDe.copy(network), script, true, BATTERY_ID, 0, 0, 2000, 2001,
            INFO, LIMIT_CHANGE + "maxP", SCALING_DOWN_PROBLEM + "at least one maxP increased", VARIANT_EMPTY,
            "maxP of NO_BATTERY lower than targetP for 1 variants, maxP increased from 1000 to 2000",
            "maxP violated by targetP in scaling down of at least one value of ts chronique_2000, maxP has been increased for equipments");
    }

    @Test
    void pmax3BatteryTest() {
        Network network = createNetwork();
        network.getBattery(BATTERY_ID).setTargetP(200);
        network.getBattery(BATTERY_ID).setMaxP(300);
        String script = """
            mapToBatteries {
                timeSeriesName 'chronique_100'
                filter { battery.id == 'NO_BATTERY' }
                variable maxP
            }""";

        // targetP not mapped
        // maxP mapped
        // targetP > mapped maxP

        String expectedLabel = MAPPING_RANGE_PROBLEM + "targetP changed to mapped maxP";
        String expectedLabelIL = expectedLabel + IL_DISABLED;
        String expectedMessage = "targetP 200 of NO_BATTERY not included in 0 to 100, targetP changed to 100";

        // without ignore limits
        // -> targetP reduced to maxP = 100
        testBattery(NetworkSerDe.copy(network), script, false, BATTERY_ID, 0, 0, 100, 100,
            WARNING, expectedLabel, expectedLabel, VARIANT_1, expectedMessage, null);

        // with ignore limits
        // -> same as without ignore limits (ignore limits disabled)
        testBattery(NetworkSerDe.copy(network), script, true, BATTERY_ID, 0, 0, 100, 100,
            WARNING, expectedLabelIL, expectedLabelIL, VARIANT_1, expectedMessage, null);
    }

    @Test
    void pmax4BatteryTest() {
        Network network = createNetwork();
        network.getBattery(BATTERY_ID).setMaxP(300);
        String script = """
            mapToBatteries {
                timeSeriesName 'chronique_100'
                filter { battery.id == 'NO_BATTERY' }
                variable maxP
            }
            mapToBatteries {
                timeSeriesName 'chronique_200'
                filter { battery.id == 'NO_BATTERY' }
                variable targetP
            }""";
        // targetP mapped
        // maxP mapped
        // mapped targetP > mapped maxP

        String expectedLabel = SCALING_DOWN_PROBLEM + "at least one targetP changed to mapped maxP";
        String expectedLabelIL = expectedLabel + IL_DISABLED;
        String expectedMessage = "Impossible to scale down 200 of ts chronique_200, targetP 100 has been applied";
        String expectedSynthesisMessage = "Impossible to scale down at least one value of ts chronique_200, modified targetP has been applied";

        // without ignore limits
        // -> targetP reduced to maxP = 100
        testBattery(NetworkSerDe.copy(network), script, false, BATTERY_ID, 0, 0, 100, 100,
            WARNING, expectedLabel, expectedLabel, VARIANT_1, expectedMessage, expectedSynthesisMessage);

        // with ignore limits
        testBattery(NetworkSerDe.copy(network), script, true, BATTERY_ID, 0, 0, 100, 100,
            WARNING, expectedLabelIL, expectedLabelIL, VARIANT_1, expectedMessage, expectedSynthesisMessage);
    }

    @Test
    void pmax5BatteryTest() {
        Network network = createNetwork();
        network.getBattery(BATTERY_ID).setMaxP(100);
        String script = """
            mapToBatteries {
                timeSeriesName 'chronique_10000'
                filter { battery.id == 'NO_BATTERY' }
                variable maxP
            }
            mapToBatteries {
                timeSeriesName 'chronique_1000'
                filter { battery.id == 'NO_BATTERY' }
                variable targetP
            }""";
        // without ignore limits
        testBattery(NetworkSerDe.copy(network), script, false, BATTERY_ID, 0, 0, 1000, 10000);

        // with ignore limits
        testBattery(NetworkSerDe.copy(network), script, true, BATTERY_ID, 0, 0, 1000, 10000);
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
        testHvdcLine(NetworkSerDe.copy(network), emptyScript, false, "HVDC2", 0, 1011, 900, 1011,
                WARNING,
                VARIANT_ALL,
                expectedLabel,
                expectedLabel,
                "CS1toCS2 2000 of HVDC2 not included in 0 to 1011, CS1toCS2 changed to 1011",
                null);

        // with ignore limits
        // -> maxP changed to CS1toCS2
        testHvdcLine(NetworkSerDe.copy(network), emptyScript, true, "HVDC2", 0, 2000, 900, 2000,
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
        String expectedLabelCase1 = BASE_CASE_RANGE_PROBLEM + "maxP changed to base case CS1toCS2";

        // without ignore limits
        // -> CS2toCS1 reduced to minP
        testHvdcLine(NetworkSerDe.copy(network), emptyScript, false, "HVDC2", 0, 1011, 1011, 1000,
                WARNING,
                VARIANT_ALL,
                expectedLabel,
                expectedLabel,
                "-CS2toCS1 -2000 of HVDC2 not included in -1011 to 0, -CS2toCS1 changed to -1011",
                null);

        // with ignore limits
        // -> minP changed to CS2toCS1
        testHvdcLine(NetworkSerDe.copy(network), emptyScript, true, "HVDC2", 0, 2000, 2000, 1000,
                INFO,
                VARIANT_ALL,
                expectedLabelIL,
                expectedLabelIL,
                "-CS2toCS1 -2000 of HVDC2 not included in -1011 to 0, -maxP changed to -2000",
                null);

        // with ignore limits
        // -> maxP changed to CS1toCS2
        network.getHvdcLine("HVDC2").setMaxP(950);
        hvdcRange.setOprFromCS2toCS1(975);
        testHvdcLine(NetworkSerDe.copy(network), emptyScript, true, "HVDC2", 0, 1000, 975, 1000,
            INFO,
            VARIANT_ALL,
            expectedLabelCase1,
            expectedLabelCase1,
            "CS1toCS2 1000 of HVDC2 not included in 0 to 950, maxP changed to 1000",
            null);

        // with ignore limits
        // -> maxP changed to CS1toCS2
        hvdcRange.setOprFromCS2toCS1(925);
        testHvdcLine(NetworkSerDe.copy(network), emptyScript, true, "HVDC2", 0, 1000, 925, 1000,
            INFO,
            VARIANT_ALL,
            expectedLabelCase1,
            expectedLabelCase1,
            "CS1toCS2 1000 of HVDC2 not included in 0 to 950, maxP changed to 1000",
            null);

        // with ignore limits
        // -> no change
        network.getHvdcLine("HVDC2").setMaxP(1011);
        testHvdcLine(NetworkSerDe.copy(network), emptyScript, true, "HVDC2", 0, 1011, 925, 1000,
            INFO,
            VARIANT_ALL,
            null,
            null,
            null,
            null);

        // with ignore limits
        // -> no change
        network.getHvdcLine("HVDC2").setMaxP(950);
        hvdcRange.setOprFromCS2toCS1(2000);
        testHvdcLine(NetworkSerDe.copy(network), emptyScript, true, "HVDC2", 0, 2000, 2000, 1000,
            INFO,
            VARIANT_ALL,
            null,
            null,
            null,
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

    /*
     * PHASE TAP CHANGER TEST
     */

    @Test
    void isTwoWindingsTransformerWithOutOfBoundsPhaseTapPosition() {
        Network network = createNetwork();
        assertFalse(TimeSeriesMapperChecker.isTwoWindingsTransformerWithOutOfBoundsPhaseTapPosition(network.getIdentifiable("HVDC1"), EquipmentVariable.P0, 100));
        assertTrue(TimeSeriesMapperChecker.isTwoWindingsTransformerWithOutOfBoundsPhaseTapPosition(network.getIdentifiable("NE_NO_1"), EquipmentVariable.PHASE_TAP_POSITION, 100));

        // Add twoWindingsTransformer without phaseTapChanger
        TwoWindingsTransformer twoWindingsTransformer = network.getTwoWindingsTransformer("NE_NO_1");
        Substation substation = network.getSubstation("NO");
        substation.newTwoWindingsTransformer()
                .setId("twt")
                .setVoltageLevel1(twoWindingsTransformer.getTerminal1().getVoltageLevel().getId())
                .setVoltageLevel2(twoWindingsTransformer.getTerminal2().getVoltageLevel().getId())
                .setNode1(13)
                .setNode2(14)
                .setR(twoWindingsTransformer.getR())
                .setX(twoWindingsTransformer.getX())
                .add();
        assertFalse(TimeSeriesMapperChecker.isTwoWindingsTransformerWithOutOfBoundsPhaseTapPosition(network.getIdentifiable("twt"), EquipmentVariable.PHASE_TAP_POSITION, 10));
    }

    @Test
    void mappingRangeProblemPhaseTapChangerTest() {
        Network network = createNetwork();

        String expectedLabelLowTapPosition = MAPPING_RANGE_PROBLEM + "phaseTapPosition changed to lowTapPosition";
        String expectedLabelHighTapPosition = MAPPING_RANGE_PROBLEM + "phaseTapPosition changed to highTapPosition";

        testPhaseTapChanger(NetworkSerDe.copy(network), phaseTapChangerLowTapPositionScript, "NE_NO_1", 0,
                WARNING, VARIANT_ALL, expectedLabelLowTapPosition, null,
                "phaseTapPosition -100 of NE_NO_1 not included in 0 to 32, phaseTapPosition changed to 0",
                null);

        testPhaseTapChanger(NetworkSerDe.copy(network), phaseTapChangerHighTapPositionScript, "NE_NO_1", 32,
                WARNING, VARIANT_ALL, expectedLabelHighTapPosition, null,
                "phaseTapPosition 100 of NE_NO_1 not included in 0 to 32, phaseTapPosition changed to 32",
                null);
    }
}
