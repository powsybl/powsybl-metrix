/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.serde.NetworkSerDe;
import org.junit.jupiter.api.Test;

/**
 * @author Paul Bui-Quang {@literal <paul.buiquang at rte-france.com>}
 */
class TimeSeriesMapperCheckerBatteryTest extends AbstractTimeSeriesMapperCheckerTest {

    /*
     * GENERATOR TEST
     */

    @Test
    void pmax1Test() {
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
    void pmax2Test() {
        Network network = createNetwork();
        network.getBattery(BATTERY_ID).setMaxP(1000);

        String pmax2Script = """
            mapToBatteries {
                timeSeriesName 'chronique_2000'
                filter { battery.id == 'NO_BATTERY' }
            }
            """;
        // targetP mapped
        // maxP not mapped
        // mapped targetP > maxP

        String expectedLabel = SCALING_DOWN_PROBLEM + "at least one targetP changed to base case maxP";

        // without ignore limits
        // -> targetP reduced to maxP = 1000
        testBattery(NetworkSerDe.copy(network), pmax2Script, false, BATTERY_ID, 0, 0, 1000, 1000,
                WARNING, expectedLabel, expectedLabel, VARIANT_1,
                "Impossible to scale down 2000 of ts chronique_2000, targetP 1000 has been applied",
                "Impossible to scale down at least one value of ts chronique_2000, modified targetP has been applied");

        // with ignore limits
        // -> maxP changed to targetP = 2000
        testBattery(NetworkSerDe.copy(network), pmax2Script, true, BATTERY_ID, 0, 0, 2000, 2001,
                INFO, LIMIT_CHANGE + "maxP", SCALING_DOWN_PROBLEM + "at least one maxP increased", VARIANT_EMPTY,
                "maxP of NO_BATTERY lower than targetP for 1 variants, maxP increased from 1000 to 2000",
                "maxP violated by targetP in scaling down of at least one value of ts chronique_2000, maxP has been increased for equipments");
    }

    @Test
    void pmax3Test() {
        Network network = createNetwork();
        network.getBattery(BATTERY_ID).setTargetP(200);
        network.getBattery(BATTERY_ID).setMaxP(300);

        String pmax3Script = """
            mapToBatteries {
                timeSeriesName 'chronique_100'
                filter { battery.id == 'NO_BATTERY' }
                variable maxP
            }
            """;
        // targetP not mapped
        // maxP mapped
        // targetP > mapped maxP

        String expectedLabel = MAPPING_RANGE_PROBLEM + "targetP changed to mapped maxP";
        String expectedLabelIL = expectedLabel + IL_DISABLED;
        String expectedMessage = "targetP 200 of NO_BATTERY not included in 0 to 100, targetP changed to 100";

        // without ignore limits
        // -> targetP reduced to maxP = 100
        testBattery(NetworkSerDe.copy(network), pmax3Script, false, BATTERY_ID, 0, 0, 100, 100,
                WARNING, expectedLabel, expectedLabel, VARIANT_1, expectedMessage, null);

        // with ignore limits
        // -> same as without ignore limits (ignore limits disabled)
        testBattery(NetworkSerDe.copy(network), pmax3Script, true, BATTERY_ID, 0, 0, 100, 100,
                WARNING, expectedLabelIL, expectedLabelIL, VARIANT_1, expectedMessage, null);
    }

    @Test
    void pmax4Test() {
        Network network = createNetwork();
        network.getBattery(BATTERY_ID).setMaxP(300);

        // targetP mapped
        // maxP mapped
        // mapped targetP > mapped maxP

        String expectedLabel = SCALING_DOWN_PROBLEM + "at least one targetP changed to mapped maxP";
        String expectedLabelIL = expectedLabel + IL_DISABLED;
        String expectedMessage = "Impossible to scale down 200 of ts chronique_200, targetP 100 has been applied";
        String expectedSynthesisMessage = "Impossible to scale down at least one value of ts chronique_200, modified targetP has been applied";

        String pmax4Script = """
            mapToBatteries {
                timeSeriesName 'chronique_100'
                filter { battery.id == 'NO_BATTERY' }
                variable maxP
            }
            mapToBatteries {
                timeSeriesName 'chronique_200'
                filter { battery.id == 'NO_BATTERY' }
                variable targetP
            }
            """;
        // without ignore limits
        // -> targetP reduced to maxP = 100
        testBattery(NetworkSerDe.copy(network), pmax4Script, false, BATTERY_ID, 0, 0, 100, 100,
                WARNING, expectedLabel, expectedLabel, VARIANT_1, expectedMessage, expectedSynthesisMessage);

        // with ignore limits
        testBattery(NetworkSerDe.copy(network), pmax4Script, true, BATTERY_ID, 0, 0, 100, 100,
                WARNING, expectedLabelIL, expectedLabelIL, VARIANT_1, expectedMessage, expectedSynthesisMessage);
    }

    @Test
    void pmax5Test() {
        Network network = createNetwork();
        network.getBattery(BATTERY_ID).setMaxP(100);
        String pmax5Script = """
            mapToBatteries {
                timeSeriesName 'chronique_10000'
                filter { battery.id == 'NO_BATTERY' }
                variable maxP
            }
            mapToBatteries {
                timeSeriesName 'chronique_1000'
                filter { battery.id == 'NO_BATTERY' }
                variable targetP
            }
            """;

        // without ignore limits
        testBattery(NetworkSerDe.copy(network), pmax5Script, false, BATTERY_ID, 0, 0, 1000, 10000);

        // with ignore limits
        testBattery(NetworkSerDe.copy(network), pmax5Script, true, BATTERY_ID, 0, 0, 1000, 10000);
    }

    @Test
    void pmin1aTest() {
        Network network = createNetwork();
        network.getBattery(BATTERY_ID).setTargetP(-2000);
        network.getBattery(BATTERY_ID).setMinP(-1000);

        String expectedLabelTargetP = BASE_CASE_RANGE_PROBLEM + "targetP changed to base case minP";
        String expectedLabelMinP = BASE_CASE_RANGE_PROBLEM + "minP changed to base case targetP";

        // without ignore limits
        testBattery(NetworkSerDe.copy(network), emptyScript, false, BATTERY_ID, 0, -1000, -1000, 600,
                WARNING, expectedLabelTargetP, expectedLabelTargetP, VARIANT_ALL,
                "targetP -2000 of NO_BATTERY not included in -1000 to 600, targetP changed to -1000",
                null);

        // with ignore limits
        testBattery(NetworkSerDe.copy(network), emptyScript, true, BATTERY_ID, 0, -2000, -2000, 600,
                INFO, expectedLabelMinP, expectedLabelMinP, VARIANT_ALL,
                "targetP -2000 of NO_BATTERY not included in -1000 to 600, minP changed to -2000",
                null);
    }

    @Test
    void pmin1bTest() {
        Network network = createNetwork();
        network.getBattery(BATTERY_ID).setTargetP(-2000);
        network.getBattery(BATTERY_ID).setMaxP(2000);
        network.getBattery(BATTERY_ID).setMinP(1000);

        String expectedLabel = BASE_CASE_RANGE_PROBLEM + "targetP changed to 0";
        String expectedLabelIL = expectedLabel + IL_DISABLED;
        String expectedMessage = "targetP -2000 of NO_BATTERY not included in 1000 to 2000, targetP changed to 0";

        // without ignore limits
        testBattery(NetworkSerDe.copy(network), emptyScript, false, BATTERY_ID, 0, 1000, 0, 2000,
                WARNING, expectedLabel, expectedLabel, VARIANT_ALL, expectedMessage, null);

        // with ignore limits
        testBattery(NetworkSerDe.copy(network), emptyScript, true, BATTERY_ID, 0, 1000, 0, 2000,
                WARNING, expectedLabelIL, expectedLabelIL, VARIANT_ALL, expectedMessage, null);
    }

    @Test
    void pmin1cTest() {
        Network network = createNetwork();
        network.getBattery(BATTERY_ID).setTargetP(500);
        network.getBattery(BATTERY_ID).setMaxP(2000);
        network.getBattery(BATTERY_ID).setMinP(1000);

        String expectedLabel = BASE_CASE_RANGE_PROBLEM + "base case minP violated by base case targetP";
        String expectedMessage = "targetP 500 of NO_BATTERY not included in 1000 to 2000, but targetP has not been changed";

        // without ignore limits
        testBattery(NetworkSerDe.copy(network), emptyScript, false, BATTERY_ID, 0, 1000, 500, 2000,
                INFO, expectedLabel, expectedLabel, VARIANT_ALL, expectedMessage, null);

        // with ignore limits
        testBattery(NetworkSerDe.copy(network), emptyScript, true, BATTERY_ID, 0, 1000, 500, 2000,
                INFO, expectedLabel, expectedLabel, VARIANT_ALL, expectedMessage, null);
    }

    @Test
    void pmin2aTest() {
        Network network = createNetwork();
        network.getBattery(BATTERY_ID).setMinP(-1000);

        String expectedLabel = SCALING_DOWN_PROBLEM + "at least one targetP changed to base case minP";

        // without ignore limits
        String pmin2aScript = """
            mapToBatteries {
                timeSeriesName 'chronique_m2000'
                filter { battery.id == 'NO_BATTERY' }
            }
            """;
        testBattery(NetworkSerDe.copy(network), pmin2aScript, false, BATTERY_ID, 0, -1000, -1000, 600,
                WARNING, expectedLabel, expectedLabel, VARIANT_1,
                "Impossible to scale down -2000 of ts chronique_m2000, targetP -1000 has been applied",
                "Impossible to scale down at least one value of ts chronique_m2000, modified targetP has been applied");

        // with ignore limits
        testBattery(NetworkSerDe.copy(network), pmin2aScript, true, BATTERY_ID, 0, -2001, -2000, 600,
                INFO, LIMIT_CHANGE + "minP", SCALING_DOWN_PROBLEM + "at least one minP decreased", VARIANT_EMPTY,
                "minP of NO_BATTERY higher than targetP for 1 variants, minP decreased from -1000 to -2000",
                "minP violated by targetP in scaling down of at least one value of ts chronique_m2000, minP has been decreased for equipments");
    }

    @Test
    void pmin2bTest() {
        Network network = createNetwork();
        network.getBattery(BATTERY_ID).setMaxP(1000);
        network.getBattery(BATTERY_ID).setMinP(500);
        network.getBattery(BATTERY_ID).setTargetP(600);

        String expectedLabel = SCALING_DOWN_PROBLEM + "at least one targetP changed to 0";
        String expectedLabelIL = expectedLabel + IL_DISABLED;
        String expectedMessage = "Impossible to scale down -2000 of ts chronique_m2000, targetP 0 has been applied";
        String expectedSynthesisMessage = "Impossible to scale down at least one value of ts chronique_m2000, modified targetP has been applied";

        // without ignore limits
        String pmin2bScript = """
            mapToBatteries {
                timeSeriesName 'chronique_m2000'
                filter { battery.id == 'NO_BATTERY' }
            }
            """;
        testBattery(NetworkSerDe.copy(network), pmin2bScript, false, BATTERY_ID, 0, 500, 0, 1000,
                WARNING, expectedLabel, expectedLabel, VARIANT_1, expectedMessage, expectedSynthesisMessage);

        // with ignore limits
        testBattery(NetworkSerDe.copy(network), pmin2bScript, true, BATTERY_ID, 0, 500, 0, 1000,
                WARNING, expectedLabelIL, expectedLabelIL, VARIANT_1, expectedMessage, expectedSynthesisMessage);
    }

    @Test
    void pmin2cTest() {
        Network network = createNetwork();
        network.getBattery(BATTERY_ID).setTargetP(750);
        network.getBattery(BATTERY_ID).setMaxP(1000);
        network.getBattery(BATTERY_ID).setMinP(500);

        String expectedLabel = SCALING_DOWN_PROBLEM + "base case minP violated by mapped targetP";
        String expectedSynthesisMessage = "Impossible to scale down at least one value of ts chronique_250, but aimed targetP of equipments have been applied";

        // without ignore limits
        String pmin2cScript = """
            mapToBatteries {
                timeSeriesName 'chronique_250'
                filter { battery.id == 'NO_BATTERY' }
            }
            """;
        testBattery(NetworkSerDe.copy(network), pmin2cScript, false, BATTERY_ID, 0, 500, 250, 1000,
                INFO, expectedLabel, expectedLabel, VARIANT_1, null, expectedSynthesisMessage);

        // with ignore limits
        testBattery(NetworkSerDe.copy(network), pmin2cScript, true, BATTERY_ID, 0, 500, 250, 1000,
                INFO, expectedLabel, expectedLabel, VARIANT_1, null, expectedSynthesisMessage);
    }

    @Test
    void pmin3aTest() {
        Network network = createNetwork();
        network.getBattery(BATTERY_ID).setMaxP(1000);
        network.getBattery(BATTERY_ID).setTargetP(-200);
        network.getBattery(BATTERY_ID).setMinP(-1000);

        String expectedLabel = MAPPING_RANGE_PROBLEM + "targetP changed to mapped minP";
        String expectedLabelIL = expectedLabel + IL_DISABLED;
        String expectedMessage = "targetP -200 of NO_BATTERY not included in -100 to 1000, targetP changed to -100";

        // without ignore limits
        String pmin3aScript = """
            mapToBatteries {
                timeSeriesName 'chronique_m100'
                filter { battery.id == 'NO_BATTERY' }
                variable minP
            }
            """;
        testBattery(NetworkSerDe.copy(network), pmin3aScript, false, BATTERY_ID, 0, -100, -100, 1000,
                WARNING, expectedLabel, expectedLabel, VARIANT_1, expectedMessage, null);

        // with ignore limits
        testBattery(NetworkSerDe.copy(network), pmin3aScript, true, BATTERY_ID, 0, -100, -100, 1000,
                WARNING, expectedLabelIL, expectedLabelIL, VARIANT_1, expectedMessage, null);
    }

    @Test
    void pmin3bTest() {
        Network network = createNetwork();
        network.getBattery(BATTERY_ID).setMaxP(1000);
        network.getBattery(BATTERY_ID).setTargetP(-200);
        network.getBattery(BATTERY_ID).setMinP(-1000);

        String expectedLabel = MAPPING_RANGE_PROBLEM + "targetP changed to 0";
        String expectedLabelIL = expectedLabel + IL_DISABLED;
        String expectedMessage = "targetP -200 of NO_BATTERY not included in 100 to 1000, targetP changed to 0";

        // without ignore limits
        String pmin3bScript = """
            mapToBatteries {
                timeSeriesName 'chronique_100'
                filter { battery.id == 'NO_BATTERY' }
                variable minP
            }
            """;
        testBattery(NetworkSerDe.copy(network), pmin3bScript, false, BATTERY_ID, 0, 100, 0, 1000,
                WARNING, expectedLabel, expectedLabel, VARIANT_1, expectedMessage, null);

        // with ignore limits
        testBattery(NetworkSerDe.copy(network), pmin3bScript, true, BATTERY_ID, 0, 100, 0, 1000,
                WARNING, expectedLabelIL, expectedLabelIL, VARIANT_1, expectedMessage, null);
    }

    @Test
    void pmin3cTest() {
        Network network = createNetwork();
        network.getBattery(BATTERY_ID).setMaxP(1000);
        network.getBattery(BATTERY_ID).setTargetP(100);

        String expectedLabel = MAPPING_RANGE_PROBLEM + "mapped minP violated by mapped targetP";
        String expectedMessage = "targetP 100 of NO_BATTERY not included in 500 to 1000, but targetP has not been changed";

        // without ignore limits
        String pmin3cScript = """
            mapToBatteries {
                timeSeriesName 'chronique_500'
                filter { battery.id == 'NO_BATTERY' }
                variable minP
            }
            """;
        testBattery(NetworkSerDe.copy(network), pmin3cScript, false, BATTERY_ID, 0, 500, 100, 1000,
                INFO, expectedLabel, expectedLabel, VARIANT_1, expectedMessage, null);

        // with ignore limits
        testBattery(NetworkSerDe.copy(network), pmin3cScript, true, BATTERY_ID, 0, 500, 100, 1000,
                INFO, expectedLabel, expectedLabel, VARIANT_1, expectedMessage, null);
    }

    @Test
    void pmin4aTest() {
        Network network = createNetwork();
        network.getBattery(BATTERY_ID).setMaxP(300);
        network.getBattery(BATTERY_ID).setMinP(-10);
        network.getBattery(BATTERY_ID).setTargetP(50);

        String expectedLabel = SCALING_DOWN_PROBLEM + "at least one targetP changed to mapped minP";
        String expectedLabelIL = expectedLabel + IL_DISABLED;
        String expectedMessage = "Impossible to scale down -200 of ts chronique_m200, targetP -100 has been applied";
        String expectedSynthesisMessage = "Impossible to scale down at least one value of ts chronique_m200, modified targetP has been applied";

        // without ignore limits
        String pmin4aScript = """
            mapToBatteries {
                timeSeriesName 'chronique_m100'
                filter { battery.id == 'NO_BATTERY' }
                variable minP
            }
            mapToBatteries {
                timeSeriesName 'chronique_m200'
                filter { battery.id == 'NO_BATTERY' }
                variable targetP
            }
            """;
        testBattery(NetworkSerDe.copy(network), pmin4aScript, false, BATTERY_ID, 0, -100, -100, 300,
                WARNING, expectedLabel, expectedLabel, VARIANT_1, expectedMessage, expectedSynthesisMessage);

        // with ignore limits
        testBattery(NetworkSerDe.copy(network), pmin4aScript, true, BATTERY_ID, 0, -100, -100, 300,
                WARNING, expectedLabelIL, expectedLabelIL, VARIANT_1, expectedMessage, expectedSynthesisMessage);
    }

    @Test
    void pmin4bTest() {
        Network network = createNetwork();
        network.getBattery(BATTERY_ID).setMaxP(1000);
        network.getBattery(BATTERY_ID).setMinP(-300);
        network.getBattery(BATTERY_ID).setTargetP(10);

        String expectedLabel = SCALING_DOWN_PROBLEM + "at least one targetP changed to 0";
        String expectedLabelIL = expectedLabel + IL_DISABLED;
        String expectedMessage = "Impossible to scale down -200 of ts chronique_m200, targetP 0 has been applied";
        String expectedSynthesisMessage = "Impossible to scale down at least one value of ts chronique_m200, modified targetP has been applied";

        // without ignore limits
        String pmin4bScript = """
            mapToBatteries {
                timeSeriesName 'chronique_50'
                filter { battery.id == 'NO_BATTERY' }
                variable minP
            }
            mapToBatteries {
                timeSeriesName 'chronique_m200'
                filter { battery.id == 'NO_BATTERY' }
                variable targetP
            }
            """;
        testBattery(NetworkSerDe.copy(network), pmin4bScript, false, BATTERY_ID, 0, 50, 0, 1000,
                WARNING, expectedLabel, expectedLabel, VARIANT_1, expectedMessage, expectedSynthesisMessage);

        // with ignore limits
        testBattery(NetworkSerDe.copy(network), pmin4bScript, true, BATTERY_ID, 0, 50, 0, 1000,
                WARNING, expectedLabelIL, expectedLabelIL, VARIANT_1, expectedMessage, expectedSynthesisMessage);
    }

    @Test
    void pmin4cTest() {
        Network network = createNetwork();
        network.getBattery(BATTERY_ID).setMaxP(1000);
        network.getBattery(BATTERY_ID).setMinP(0);
        network.getBattery(BATTERY_ID).setTargetP(100);

        String expectedLabel = SCALING_DOWN_PROBLEM + "mapped minP violated by mapped targetP";
        String expectedSynthesisMessage = "Impossible to scale down at least one value of ts chronique_100, but aimed targetP of equipments have been applied";

        // without ignore limits
        String pmin4cScript = """
            mapToBatteries {
                timeSeriesName 'chronique_500'
                filter { battery.id == 'NO_BATTERY' }
                variable minP
            }
            mapToBatteries {
                timeSeriesName 'chronique_100'
                filter { battery.id == 'NO_BATTERY' }
                variable targetP
            }
            """;
        testBattery(NetworkSerDe.copy(network), pmin4cScript, false, BATTERY_ID, 0, 500, 100, 1000,
                INFO, expectedLabel, expectedLabel, VARIANT_1, null, expectedSynthesisMessage);

        // with ignore limits
        testBattery(NetworkSerDe.copy(network), pmin4cScript, true, BATTERY_ID, 0, 500, 100, 1000,
                INFO, expectedLabel, expectedLabel, VARIANT_1, null, expectedSynthesisMessage);
    }
}
