/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration.analysis;

import com.powsybl.contingency.BranchContingency;
import com.powsybl.contingency.BusbarSectionContingency;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyElement;
import com.powsybl.contingency.GeneratorContingency;
import com.powsybl.contingency.HvdcLineContingency;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.serde.NetworkSerDe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

import static java.lang.System.Logger.Level.WARNING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
class MetrixContingencyAnalysisTest {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.MetrixAnalysis");

    private Network network;

    @BeforeEach
    void setUp() {
        network = NetworkSerDe.read(Objects.requireNonNull(getClass().getResourceAsStream("/simpleNetwork.xml")));
    }

    @Test
    void loadContingencyWithWrongIdTest() throws IOException {
        ContingencyElement contingencyWithWrongId = new BranchContingency("wrongId");
        Contingency ctyWithWrongId = new Contingency("ctyWithWrongId", Collections.singletonList(contingencyWithWrongId));
        String invalidContingency = getWarningInvalidContingency("ctyWithWrongId", "wrongId");
        String expected = invalidContingency + RESOURCE_BUNDLE.getString("invalidContingencyNetwork");
        loadContingencyTest(ctyWithWrongId, expected);
    }

    @Test
    void loadContingencyWithWrongTypeTest() throws IOException {
        ContingencyElement contingencyWithWrongType = new BusbarSectionContingency("FP.AND1_1");
        Contingency ctyWithWrongType = new Contingency("ctyWithWrongType", Collections.singletonList(contingencyWithWrongType));
        String invalidContingency = getWarningInvalidContingency("ctyWithWrongType", "FP.AND1_1");
        String expected = invalidContingency + String.format(RESOURCE_BUNDLE.getString("invalidContingencyType"), "BUSBAR_SECTION");
        loadContingencyTest(ctyWithWrongType, expected);
    }

    @Test
    void loadContingencyWithWrongTypeAndIdTest() throws IOException {
        ContingencyElement contingencyWithWrongTypeAndId = new BusbarSectionContingency("wrongId");
        Contingency ctyWithWrongTypeAndId = new Contingency("ctyWithWrongTypeAndId", Collections.singletonList(contingencyWithWrongTypeAndId));
        String invalidContingency = getWarningInvalidContingency("ctyWithWrongTypeAndId", "wrongId");
        String expected = String.join(System.lineSeparator(),
                invalidContingency + RESOURCE_BUNDLE.getString("invalidContingencyNetwork"),
                invalidContingency + String.format(RESOURCE_BUNDLE.getString("invalidContingencyType"), "BUSBAR_SECTION"));
        loadContingencyTest(ctyWithWrongTypeAndId, expected);
    }

    @Test
    void loadContingencyWithWrongTypeForIdentifiableTest() throws IOException {
        ContingencyElement contingencyWithWrongTypeForIdentifiable = new BranchContingency("FSSV.O11_G");
        Contingency ctyWithWrongTypeForIdentifiable = new Contingency("ctyWithWrongTypeForIdentifiable", Collections.singletonList(contingencyWithWrongTypeForIdentifiable));
        String invalidContingency = getWarningInvalidContingency("ctyWithWrongTypeForIdentifiable", "FSSV.O11_G");
        String expected = invalidContingency + String.format(RESOURCE_BUNDLE.getString("invalidContingencyNetworkType"), "BRANCH", "GENERATOR");
        loadContingencyTest(ctyWithWrongTypeForIdentifiable, expected);
    }

    @Test
    void loadContingencyWithInvalidNumberOfElementsTest() throws IOException {
        Contingency ctyWithInvalidNumberOfElements = new Contingency("ctyWithInvalidNumberOfElements", List.of());
        String message = WARNING + ";";
        message += RESOURCE_BUNDLE.getString("contingenciesSection") + ";";
        String expected = message + String.format(RESOURCE_BUNDLE.getString("invalidNumberOfElements"), "ctyWithInvalidNumberOfElements");
        loadContingencyTest(ctyWithInvalidNumberOfElements, expected);
    }

    @Test
    void propagateTrippingTest() {
        ContingencyElement l = new BranchContingency("FTDPRA1  FVERGE1  1");
        Contingency cty = new Contingency("cty", l);

        ContingencyElement l2 = new BranchContingency("FTDPRA1  FVERGE1  2");
        ContingencyElement l3 = new BranchContingency("FVALDI1  FTDPRA1  1");
        ContingencyElement l4 = new BranchContingency("FVALDI1  FTDPRA1  2");
        ContingencyElement l5 = new BranchContingency("FP.AND1  FTDPRA1  1");

        propagateTest(cty, true, List.of(l, l2, l3, l4, l5));
        propagateTest(cty, false, List.of(l));

        ContingencyElement h = new HvdcLineContingency("HVDC1");
        cty = new Contingency("cty", l4, h);
        propagateTest(cty, true, List.of(l4, h));
        propagateTest(cty, false, List.of(l4, h));

        ContingencyElement g = new GeneratorContingency("FSSV.O11_G");
        cty = new Contingency("cty", g, l3);
        propagateTest(cty, true, List.of(g, l3));
        propagateTest(cty, false, List.of(g, l3));
    }

    private String getWarningInvalidContingency(String contingency, String equipment) {
        String message = WARNING + ";";
        message += RESOURCE_BUNDLE.getString("contingenciesSection") + ";";
        message += String.format(RESOURCE_BUNDLE.getString("invalidContingency"), contingency, equipment) + " ";
        return message;
    }

    private void loadContingencyTest(Contingency contingency, String expected) throws IOException {
        ContingenciesProvider provider = n -> List.of(contingency);

        StringWriter writer = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(writer)) {
            AnalysisLogger logger = new AnalysisLogger(bufferedWriter);
            ContingencyLoader contingencyLoader = new ContingencyLoader(provider, network, false, null, null, logger);
            contingencyLoader.load();
            bufferedWriter.flush();
            String actual = writer.toString();
            assertEquals(String.join(System.lineSeparator(),
                expected,
                ""), actual);
        }
    }

    private void propagateTest(Contingency contingency, boolean propagateBranchTripping, List<ContingencyElement> expected) {
        ContingenciesProvider provider = n -> List.of(contingency);
        ContingencyLoader contingencyLoader = new ContingencyLoader(provider, network, propagateBranchTripping, null, null, null);
        List<Contingency> contingencies = contingencyLoader.load();
        assertThat(contingencies.getFirst().getElements()).containsExactlyInAnyOrderElementsOf(expected);
    }
}
