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
import com.powsybl.iidm.serde.NetworkSerDe;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Paul Bui-Quang {@literal <paul.buiquang at rte-france.com>}
 */
class TimeSeriesMapperCheckerTest extends AbstractTimeSeriesMapperCheckerTest {

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
