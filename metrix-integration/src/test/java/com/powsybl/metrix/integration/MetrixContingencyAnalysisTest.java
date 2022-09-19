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
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.ResourceBundle;

import static com.powsybl.metrix.integration.AbstractCompareTxt.compareStreamTxt;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class MetrixContingencyAnalysisTest {

    @Test
    void loadContingenciesTest() throws IOException {
        Network n = NetworkXml.read(getClass().getResourceAsStream("/simpleNetwork.xml"));

        ContingencyElement contingencyWithWrongId = new BranchContingency("wrongId");
        Contingency ctyWithWrongId = new Contingency("ctyWithWrongId", Collections.singletonList(contingencyWithWrongId));

        ContingencyElement contingencyWithWrongType = new BusbarSectionContingency("FP.AND1_1");
        Contingency ctyWithWrongType = new Contingency("ctyWithWrongType", Collections.singletonList(contingencyWithWrongType));

        ContingencyElement contingencyWithWrongTypeAndId = new BusbarSectionContingency("wrongId");
        Contingency ctyWithWrongTypeAndId = new Contingency("ctyWithWrongTypeAndId", Collections.singletonList(contingencyWithWrongTypeAndId));

        ContingencyElement contingencyWithWrongTypeForIdentifiable = new BranchContingency("FSSV.O11_G");
        Contingency ctyWithWrongTypeForIdentifiable = new Contingency("ctyWithWrongTypeForIdentifiable", Collections.singletonList(contingencyWithWrongTypeForIdentifiable));

        ContingenciesProvider provider = network -> ImmutableList.of(ctyWithWrongId, ctyWithWrongType, ctyWithWrongTypeAndId, ctyWithWrongTypeForIdentifiable);

        StringWriter writer = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(writer)) {
            MetrixInputAnalysis metrixInputAnalysis = new MetrixInputAnalysis(new StringReader(""), provider, n, new MetrixDslData(), bufferedWriter);
            metrixInputAnalysis.runAnalysis();
            bufferedWriter.flush();
            String actual = writer.toString();
            String expectedFilePath = ResourceBundle.getBundle("expected.contents").getString("metrix_contingency_analysis.file");
            assertNotNull(compareStreamTxt(getClass().getResourceAsStream(expectedFilePath),
                new ByteArrayInputStream(actual.getBytes(StandardCharsets.UTF_8))));
        }
    }

    @Test
    void metrixDslDataContingencyAnalysisTest() throws IOException {
        Network n = NetworkXml.read(getClass().getResourceAsStream("/simpleNetwork.xml"));

        MetrixDslData metrixDslData = new MetrixDslData();
        metrixDslData.addCurativeLoad("loadId", 10, ImmutableList.of("ctyForLoadId"));
        metrixDslData.addPtc("phaseTapChangerId", MetrixPtcControlType.OPTIMIZED_ANGLE_CONTROL, ImmutableList.of("ctyForPhaseTapChangerId"));
        metrixDslData.addGeneratorForRedispatching("generatorId", ImmutableList.of("ctyForGeneratorId"));
        metrixDslData.addHvdc("hvdcLineId", MetrixHvdcControlType.OPTIMIZED, ImmutableList.of("ctyForHvdcLineId"));

        StringWriter writer = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(writer)) {
            MetrixInputAnalysis metrixInputAnalysis = new MetrixInputAnalysis(new StringReader(""), new EmptyContingencyListProvider(), n, metrixDslData, bufferedWriter);
            metrixInputAnalysis.runAnalysis();
            bufferedWriter.flush();
            String actual = writer.toString();
            String expectedFilePath = ResourceBundle.getBundle("expected.contents").getString("metrix_dsl_data_contingency_analysis.file");
            assertNotNull(compareStreamTxt(getClass().getResourceAsStream(expectedFilePath),
                    new ByteArrayInputStream(actual.getBytes(StandardCharsets.UTF_8))));
        }
    }

    @Test
    void metrixRemedialContingencyAnalysisTest() throws IOException {
        Network n = NetworkXml.read(getClass().getResourceAsStream("/simpleNetwork.xml"));

        StringWriter writer = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(writer)) {
            MetrixInputAnalysis metrixInputAnalysis = new MetrixInputAnalysis(new StringReader(
                    String.join(System.lineSeparator(),
                            "NB;1;",
                            "ctyId;1;FP.AND1  FVERGE1  1;")
            ), new EmptyContingencyListProvider(), n, new MetrixDslData(), bufferedWriter);
            metrixInputAnalysis.runAnalysis();
            bufferedWriter.flush();
            String actual = writer.toString();
            String expectedFilePath = ResourceBundle.getBundle("expected.contents").getString("metrix_remedial_contingency_analysis.file");
            assertNotNull(compareStreamTxt(getClass().getResourceAsStream(expectedFilePath),
                    new ByteArrayInputStream(actual.getBytes(StandardCharsets.UTF_8))));
        }
    }
}
