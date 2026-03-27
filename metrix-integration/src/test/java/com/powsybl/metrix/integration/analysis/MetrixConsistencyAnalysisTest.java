/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration.analysis;

import com.powsybl.contingency.BranchContingency;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyElement;
import com.powsybl.contingency.EmptyContingencyListProvider;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.serde.NetworkSerDe;
import com.powsybl.metrix.integration.MetrixDslData;
import com.powsybl.metrix.integration.type.MetrixHvdcControlType;
import com.powsybl.metrix.integration.type.MetrixPtcControlType;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

import static java.lang.System.Logger.Level.WARNING;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
class MetrixConsistencyAnalysisTest {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.MetrixAnalysis");

    private Network network;

    @BeforeEach
    void setUp() {
        network = NetworkSerDe.read(Objects.requireNonNull(getClass().getResourceAsStream("/simpleNetwork.xml")));
    }

    @Test
    void metrixDslDataLoadContingencyAnalysisTest() throws IOException {
        MetrixDslData metrixDslData = new MetrixDslData();
        metrixDslData.addCurativeLoad("loadId", 10, List.of("ctyForLoadId"));
        String expected = getWarningInvalidMetrixDslDataContingency(" - loadId", "LOAD", "ctyForLoadId");
        metrixDslDataContingencyAnalysisTest(metrixDslData, expected);
    }

    @Test
    void metrixDslDataPhaseTapChangerContingencyAnalysisTest() throws IOException {
        MetrixDslData metrixDslData = new MetrixDslData();
        metrixDslData.addPtc("phaseTapChangerId", MetrixPtcControlType.OPTIMIZED_ANGLE_CONTROL, List.of("ctyForPhaseTapChangerId"));
        String expected = getWarningInvalidMetrixDslDataContingency(" - phaseTapChangerId", "TWO_WINDINGS_TRANSFORMER", "ctyForPhaseTapChangerId");
        metrixDslDataContingencyAnalysisTest(metrixDslData, expected);
    }

    @Test
    void metrixDslDataGeneratorContingencyAnalysisTest() throws IOException {
        MetrixDslData metrixDslData = new MetrixDslData();
        metrixDslData.addGeneratorForRedispatching("generatorId", List.of("ctyForGeneratorId"));
        String expected = getWarningInvalidMetrixDslDataContingency(" - generatorId", "GENERATOR", "ctyForGeneratorId");
        metrixDslDataContingencyAnalysisTest(metrixDslData, expected);
    }

    @Test
    void metrixDslDataHvdcLineContingencyAnalysisTest() throws IOException {
        MetrixDslData metrixDslData = new MetrixDslData();
        metrixDslData.addHvdc("hvdcLineId", MetrixHvdcControlType.OPTIMIZED, List.of("ctyForHvdcLineId"));
        String expected = getWarningInvalidMetrixDslDataContingency(" - hvdcLineId", "HVDC_LINE", "ctyForHvdcLineId");
        metrixDslDataContingencyAnalysisTest(metrixDslData, expected);
    }

    @Test
    void metrixRemedialContingencyAnalysisTest() throws IOException {
        remedialTest(
            String.join(System.lineSeparator(), "NB;1;", "ctyId;1;FP.AND1  FVERGE1  1;"),
            getWarningInvalidRemedial(2) + String.format(RESOURCE_BUNDLE.getString("invalidMetrixRemedialContingency"), "ctyId")
        );
    }

    @Test
    void invalidRemedialLineBranchToOpenTypeTest() throws IOException {
        remedialTest(
            getBranchContingenciesProvider(),
            String.join(System.lineSeparator(), "NB;1;", "ctyId;2;FSSV.O11_G;FP.AND1  FVERGE1  2;"),
            getWarningInvalidRemedial(2) + String.format(RESOURCE_BUNDLE.getString("invalidRemedialActionType"), "FSSV.O11_G")
        );
    }

    @Test
    void invalidRemedialLineBranchToCloseTypeTest() throws IOException {
        remedialTest(
            getBranchContingenciesProvider(),
            String.join(System.lineSeparator(), "NB;1;", "ctyId;2;+FSSV.O11_G;FP.AND1  FVERGE1  2;"),
            getWarningInvalidRemedial(2) + String.format(RESOURCE_BUNDLE.getString("invalidRemedialActionType"), "FSSV.O11_G")
        );
    }

    @Test
    void invalidRemedialLineBranchToCloseActionTest() throws IOException {
        remedialTest(
            getBranchContingenciesProvider(),
            String.join(System.lineSeparator(), "NB;1;", "ctyId;2;+action;FP.AND1  FVERGE1  2;"),
            getWarningInvalidRemedial(2) + String.format(RESOURCE_BUNDLE.getString("invalidRemedialNetwork"), "action")
        );
    }

    @Test
    void invalidRemedialLineConstraintMonitoredTest() throws IOException {
        remedialTest(
            getBranchContingenciesProvider(),
            String.join(System.lineSeparator(), "NB;1;", "ctyId|FS.BIS1  FVALDI1  1;1;FP.AND1  FVERGE1  2;"),
            getWarningInvalidRemedial(2) + String.format(RESOURCE_BUNDLE.getString("invalidMetrixRemedialConstraint"), "FS.BIS1  FVALDI1  1")
        );
    }

    @Test
    void invalidRemedialLineConstraintTypeTest() throws IOException {
        remedialTest(
            getBranchContingenciesProvider(),
            String.join(System.lineSeparator(), "NB;1;", "ctyId|FSSV.O11_G;1;FP.AND1  FVERGE1  2;"),
            getWarningInvalidRemedial(2) + String.format(RESOURCE_BUNDLE.getString("invalidRemedialConstraintType"), "FSSV.O11_G")
        );
    }

    @Test
    void invalidRemedialLineConstraintTest() throws IOException {
        remedialTest(
            getBranchContingenciesProvider(),
            String.join(System.lineSeparator(), "NB;1;", "ctyId|equipment;1;FP.AND1  FVERGE1  2;"),
            getWarningInvalidRemedial(2) + String.format(RESOURCE_BUNDLE.getString("invalidRemedialNetwork"), "equipment")
        );
    }

    @Test
    void invalidRemedialLineBranchToOpenActionTest() throws IOException {
        remedialTest(
            getBranchContingenciesProvider(),
            String.join(System.lineSeparator(), "NB;1;", "ctyId;2;action;+FP.AND1  FVERGE1  2;"),
            getWarningInvalidRemedial(2) + String.format(RESOURCE_BUNDLE.getString("invalidRemedialNetwork"), "action")
        );
    }

    private void remedialTest(String remedialScript, String expected) throws IOException {
        remedialTest(new EmptyContingencyListProvider(), new MetrixDslData(), remedialScript, expected);
    }

    private void remedialTest(ContingenciesProvider contingenciesProvider, String remedialScript, String expected) throws IOException {
        remedialTest(contingenciesProvider, new MetrixDslData(), remedialScript, expected);
    }

    private void remedialTest(ContingenciesProvider contingenciesProvider, MetrixDslData metrixDslData, String remedialScript, String expected) throws IOException {
        StringWriter writer = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(writer)) {
            AnalysisLogger logger = new AnalysisLogger(bufferedWriter);
            RemedialLoader remedialLoader = new RemedialLoader(new StringReader(remedialScript), logger);
            ConsistencyChecker consistencyChecker = new ConsistencyChecker(network, metrixDslData, logger);
            consistencyChecker.run(remedialLoader.load(), contingenciesProvider.getContingencies(network));
            bufferedWriter.flush();
            String actual = writer.toString();
            Assertions.assertThat(actual).isEqualTo(
                expected.isEmpty() ? expected : String.join(System.lineSeparator(), expected, "")
            );
        }
    }

    private ContingenciesProvider getBranchContingenciesProvider() {
        ContingencyElement ctyElt = new BranchContingency("FP.AND1  FVERGE1  2");
        Contingency cty = new Contingency("ctyId", Collections.singletonList(ctyElt));
        return n -> List.of(cty);
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
        message += String.format(RESOURCE_BUNDLE.getString("invalidMetrixDslDataContingency"), equipmentType, contingency);
        return message;
    }

    private void metrixDslDataContingencyAnalysisTest(MetrixDslData metrixDslData, String expected) throws IOException {
        StringWriter writer = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(writer)) {
            AnalysisLogger logger = new AnalysisLogger(bufferedWriter);
            ContingencyLoader contingencyLoader = new ContingencyLoader(new EmptyContingencyListProvider(), network, false, null, null, logger);
            RemedialLoader remedialLoader = new RemedialLoader(new StringReader(""), logger);
            ConsistencyChecker consistencyChecker = new ConsistencyChecker(network, metrixDslData, logger);
            MetrixInputAnalysis metrixInputAnalysis = new MetrixInputAnalysis(contingencyLoader, remedialLoader, consistencyChecker);
            metrixInputAnalysis.runAnalysis();
            bufferedWriter.flush();
            String actual = writer.toString();
            assertEquals(String.join(System.lineSeparator(),
                expected,
                ""), actual);
        }
    }
}
