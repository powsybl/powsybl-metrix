/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.integration;

import com.google.common.collect.ImmutableList;
import com.powsybl.contingency.*;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.xml.NetworkXml;
import com.powsybl.metrix.integration.metrix.MetrixInputAnalysis;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.Collections;
import java.util.ResourceBundle;

import static com.powsybl.metrix.mapping.LogDslLoader.LogType.WARNING;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MetrixContingencyAnalysisTest {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.MetrixAnalysis");

    private Network network;

    @BeforeEach
    public void setUp() {
        network = NetworkXml.read(getClass().getResourceAsStream("/simpleNetwork.xml"));
    }

    private String getWarningInvalidContingency(String contingency, String equipment) {
        String message = WARNING + ";";
        message += RESOURCE_BUNDLE.getString("contingenciesSection") + ";";
        message += String.format(RESOURCE_BUNDLE.getString("invalidContingency"), contingency, equipment) + " ";
        return message;
    }

    private String getWarningInvalidRemedial(int line) {
        String message = WARNING + ";";
        message += RESOURCE_BUNDLE.getString("remedialsSection") + ";";
        message += String.format(RESOURCE_BUNDLE.getString("invalidRemedial"), line) + " ";
        return message;
    }

    private String getWarningInvalidMetrixDslDataContingency(String section, String equipmentType, String contingency) {
        String message = WARNING + ";";
        message += RESOURCE_BUNDLE.getString("contingenciesSection") + section + ";";
        message += String.format(RESOURCE_BUNDLE.getString("invalidMetrixDslDataContingency"), RESOURCE_BUNDLE.getString(equipmentType), contingency);
        return message;
    }

    private void metrixDslDataContingencyAnalysisTest(MetrixDslData metrixDslData, String expected) throws IOException {
        StringWriter writer = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(writer)) {
            MetrixInputAnalysis metrixInputAnalysis = new MetrixInputAnalysis(new StringReader(""), new EmptyContingencyListProvider(), network, metrixDslData, bufferedWriter);
            metrixInputAnalysis.runAnalysis();
            bufferedWriter.flush();
            String actual = writer.toString();
            assertEquals(String.join(System.lineSeparator(),
                    expected,
                    ""), actual);
        }
    }

    private void loadContingencyTest(Contingency contingency, String expected) throws IOException {
        ContingenciesProvider provider = network -> ImmutableList.of(contingency);

        StringWriter writer = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(writer)) {
            MetrixInputAnalysis metrixInputAnalysis = new MetrixInputAnalysis(new StringReader(""), provider, network, new MetrixDslData(), bufferedWriter);
            metrixInputAnalysis.runAnalysis();
            bufferedWriter.flush();
            String actual = writer.toString();
            assertEquals(String.join(System.lineSeparator(),
                expected,
                ""), actual);
        }
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
    void metrixDslDataLoadContingencyAnalysisTest() throws IOException {
        MetrixDslData metrixDslData = new MetrixDslData();
        metrixDslData.addCurativeLoad("loadId", 10, ImmutableList.of("ctyForLoadId"));
        String expected = getWarningInvalidMetrixDslDataContingency(" - loadId", "load", "ctyForLoadId");
        metrixDslDataContingencyAnalysisTest(metrixDslData, expected);
    }

    @Test
    void metrixDslDataPhaseTapChangerContingencyAnalysisTest() throws IOException {
        MetrixDslData metrixDslData = new MetrixDslData();
        metrixDslData.addPtc("phaseTapChangerId", MetrixPtcControlType.OPTIMIZED_ANGLE_CONTROL, ImmutableList.of("ctyForPhaseTapChangerId"));
        String expected = getWarningInvalidMetrixDslDataContingency(" - phaseTapChangerId", "phaseTapChanger", "ctyForPhaseTapChangerId");
        metrixDslDataContingencyAnalysisTest(metrixDslData, expected);
    }

    @Test
    void metrixDslDataGeneratorContingencyAnalysisTest() throws IOException {
        MetrixDslData metrixDslData = new MetrixDslData();
        metrixDslData.addGeneratorForRedispatching("generatorId", ImmutableList.of("ctyForGeneratorId"));
        String expected = getWarningInvalidMetrixDslDataContingency(" - generatorId", "generator", "ctyForGeneratorId");
        metrixDslDataContingencyAnalysisTest(metrixDslData, expected);
    }

    @Test
    void metrixDslDataHvdcLineContingencyAnalysisTest() throws IOException {
        MetrixDslData metrixDslData = new MetrixDslData();
        metrixDslData.addHvdc("hvdcLineId", MetrixHvdcControlType.OPTIMIZED, ImmutableList.of("ctyForHvdcLineId"));
        String expected = getWarningInvalidMetrixDslDataContingency(" - hvdcLineId", "hvdcLine", "ctyForHvdcLineId");
        metrixDslDataContingencyAnalysisTest(metrixDslData, expected);
    }

    @Test
    void metrixRemedialContingencyAnalysisTest() throws IOException {
        StringWriter writer = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(writer)) {
            MetrixInputAnalysis metrixInputAnalysis = new MetrixInputAnalysis(new StringReader(
                    String.join(System.lineSeparator(),
                            "NB;1;",
                            "ctyId;1;FP.AND1  FVERGE1  1;")
            ), new EmptyContingencyListProvider(), network, new MetrixDslData(), bufferedWriter);
            metrixInputAnalysis.runAnalysis();
            bufferedWriter.flush();
            String actual = writer.toString();
            String expectedMessage = getWarningInvalidRemedial(2);
            expectedMessage += String.format(RESOURCE_BUNDLE.getString("invalidMetrixRemedialContingency"), "ctyId");
            assertEquals(String.join(System.lineSeparator(), expectedMessage, ""), actual);
        }
    }
}
