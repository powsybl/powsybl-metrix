/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration;

import com.google.common.collect.ImmutableList;
import com.powsybl.contingency.*;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.serde.NetworkSerDe;
import com.powsybl.metrix.integration.metrix.MetrixInputAnalysis;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.ResourceBundle;

import static com.powsybl.metrix.mapping.LogDslLoader.LogType.ERROR;
import static com.powsybl.metrix.mapping.LogDslLoader.LogType.WARNING;

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
public class MetrixRemedialAnalysisTest {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.MetrixAnalysis");

    private Network network;

    @BeforeEach
    public void setUp() {
        network = NetworkSerDe.read(getClass().getResourceAsStream("/simpleNetwork.xml"));
    }

    private ContingenciesProvider getBranchContingenciesProvider() {
        ContingencyElement ctyElt = new BranchContingency("FP.AND1  FVERGE1  1");
        Contingency cty = new Contingency("ctyId", Collections.singletonList(ctyElt));
        return network -> ImmutableList.of(cty);
    }

    private MetrixDslData getBranchMonitoringMetrixDslData() {
        MetrixDslData metrixDslData = new MetrixDslData();
        metrixDslData.addBranchMonitoringNk("FS.BIS1  FVALDI1  1");
        return metrixDslData;
    }

    private String getErrorInvalidRemedialFile(int line) {
        String message = ERROR + ";";
        message += RESOURCE_BUNDLE.getString("remedialsSection") + ";";
        message += String.format(RESOURCE_BUNDLE.getString("invalidRemedialFile"), line) + " ";
        return message;
    }

    private String getWarningInvalidRemedial(int line) {
        String message = WARNING + ";";
        message += RESOURCE_BUNDLE.getString("remedialsSection") + ";";
        message += String.format(RESOURCE_BUNDLE.getString("invalidRemedial"), line) + " ";
        return message;
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
            MetrixInputAnalysis metrixInputAnalysis = new MetrixInputAnalysis(
                new StringReader(remedialScript),
                contingenciesProvider,
                network,
                new MetrixParameters(),
                metrixDslData,
                null,
                bufferedWriter);
            metrixInputAnalysis.runAnalysis();
            bufferedWriter.flush();
            String actual = writer.toString();
            Assertions.assertThat(actual).isEqualTo(
                expected.isEmpty() ? expected : String.join(System.lineSeparator(), expected, "")
            );
        }
    }

    @Test
    void emptyRemedialFileTest() throws IOException {
        remedialTest(
            "",
            ""
        );
    }

    @Test
    void invalidRemedialHeaderCommentLineTest() throws IOException {
        remedialTest(
            "// comment",
            getErrorInvalidRemedialFile(1) + RESOURCE_BUNDLE.getString("invalidRemedialFileComment")
        );
    }

    @Test
    void invalidRemedialHeaderCommentTest() throws IOException {
        remedialTest(
            "/* comment",
            getErrorInvalidRemedialFile(1) + RESOURCE_BUNDLE.getString("invalidRemedialFileComment")
        );
    }

    @Test
    void invalidRemedialLineCommentLineTest() throws IOException {
        remedialTest(
            String.join(System.lineSeparator(), "NB;1;", "// comment"),
            getErrorInvalidRemedialFile(2) + RESOURCE_BUNDLE.getString("invalidRemedialFileComment")
        );
    }

    @Test
    void invalidRemedialLineCommentTest() throws IOException {
        remedialTest(
            String.join(System.lineSeparator(), "NB;1;", "/* comment"),
            getErrorInvalidRemedialFile(2) + RESOURCE_BUNDLE.getString("invalidRemedialFileComment")
        );
    }

    @Test
    void invalidRemedialHeaderEndLineTest() throws IOException {
        remedialTest(
            "line",
            getErrorInvalidRemedialFile(1) + RESOURCE_BUNDLE.getString("invalidRemedialFileEndLine")
        );
    }

    @Test
    void invalidRemedialLineEndLineTest() throws IOException {
        remedialTest(
            String.join(System.lineSeparator(), "NB;1;", "line"),
            getErrorInvalidRemedialFile(2) + RESOURCE_BUNDLE.getString("invalidRemedialFileEndLine")
        );
    }

    @Test
    void invalidRemedialHeaderNbColumnTest() throws IOException {
        remedialTest(
            "column1;column2;column3;",
             getErrorInvalidRemedialFile(1) + RESOURCE_BUNDLE.getString("invalidRemedialFileHeader")
        );
    }

    @Test
    void invalidRemedialHeaderNbColumnEmptyTest() throws IOException {
        remedialTest(
            ";column;",
            getErrorInvalidRemedialFile(1) + RESOURCE_BUNDLE.getString("invalidRemedialFileHeader")
        );
    }

    @Test
    void invalidRemedialHeaderNbColumnNumberEmptyTest() throws IOException {
        remedialTest(
            "NB;;",
            getErrorInvalidRemedialFile(1) + RESOURCE_BUNDLE.getString("invalidRemedialFileHeader")
        );
    }

    @Test
    void invalidRemedialHeaderNbMissingTest() throws IOException {
        remedialTest(
            "notNB;column;",
            getErrorInvalidRemedialFile(1) + RESOURCE_BUNDLE.getString("invalidRemedialFileHeader")
        );
    }

    @Test
    void invalidRemedialHeaderNbColumnNumberMissingTest() throws IOException {
        remedialTest(
            "NB;column;",
            getErrorInvalidRemedialFile(1) + RESOURCE_BUNDLE.getString("invalidRemedialFileHeader")
        );
    }

    @Test
    void invalidRemedialHeaderNbColumnNumberWrongIntegerTest() throws IOException {
        remedialTest(
            "NB;-1;",
            getErrorInvalidRemedialFile(1) + RESOURCE_BUNDLE.getString("invalidRemedialFileHeader")
        );
    }

    @Test
    void invalidRemedialLineNbColumnTest() throws IOException {
        remedialTest(
            String.join(System.lineSeparator(), "NB;1;", "column;"),
            getErrorInvalidRemedialFile(2) + RESOURCE_BUNDLE.getString("invalidRemedialFileLine")
        );
    }

    @Test
    void invalidRemedialLineNbColumnEmptyTest() throws IOException {
        remedialTest(
            String.join(System.lineSeparator(), "NB;1;", ";;"),
            getErrorInvalidRemedialFile(2) + RESOURCE_BUNDLE.getString("invalidRemedialFileLine")
        );
    }

    @Test
    void invalidRemedialLineNbActionMissingTest() throws IOException {
        remedialTest(
            String.join(System.lineSeparator(), "NB;1;", "ctyId;column;"),
            getErrorInvalidRemedialFile(2) + RESOURCE_BUNDLE.getString("invalidRemedialFileAction")
        );
    }

    @Test
    void invalidRemediaLineNbActionWrongIntegerTest() throws IOException {
        remedialTest(
            String.join(System.lineSeparator(), "NB;1;", "ctyId;-1;"),
            getErrorInvalidRemedialFile(2) + RESOURCE_BUNDLE.getString("invalidRemedialFileAction")
        );
    }

    @Test
    void invalidRemedialLineContingencyEmptyTest() throws IOException {
        remedialTest(
                getBranchContingenciesProvider(),
                String.join(System.lineSeparator(), "NB;1;", ";1;FP.AND1  FVERGE1  2;"),
                getErrorInvalidRemedialFile(2) + RESOURCE_BUNDLE.getString("invalidRemedialFileEmptyElement")
        );
    }

    @Test
    void invalidRemedialLineNbActionEmptyTest() throws IOException {
        remedialTest(
                getBranchContingenciesProvider(),
                String.join(System.lineSeparator(), "NB;1;", "ctyId;;FP.AND1  FVERGE1  2;"),
                getErrorInvalidRemedialFile(2) + RESOURCE_BUNDLE.getString("invalidRemedialFileAction")
        );
    }

    @Test
    void invalidRemedialLineNoActionTest() throws IOException {
        remedialTest(
                getBranchContingenciesProvider(),
                String.join(System.lineSeparator(), "NB;1;", "ctyId;1;;"),
                getErrorInvalidRemedialFile(2) + RESOURCE_BUNDLE.getString("invalidRemedialFileLine")
        );
    }

    @Test
    void invalidRemedialLineActionEmptyTest() throws IOException {
        remedialTest(
                getBranchContingenciesProvider(),
                String.join(System.lineSeparator(), "NB;1;", "ctyId;2;;FP.AND1  FVERGE1  2;"),
                getErrorInvalidRemedialFile(2) + RESOURCE_BUNDLE.getString("invalidRemedialFileEmptyElement")
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

    @Test
    void invalidRemedialLineBranchToCloseActionTest() throws IOException {
        remedialTest(
                getBranchContingenciesProvider(),
                String.join(System.lineSeparator(), "NB;1;", "ctyId;2;+action;FP.AND1  FVERGE1  2;"),
                getWarningInvalidRemedial(2) + String.format(RESOURCE_BUNDLE.getString("invalidRemedialNetwork"), "action")
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
    void invalidRemedialLineConstraintTest() throws IOException {
        remedialTest(
                getBranchContingenciesProvider(),
                String.join(System.lineSeparator(), "NB;1;", "ctyId|equipment;1;FP.AND1  FVERGE1  2;"),
                getWarningInvalidRemedial(2) + String.format(RESOURCE_BUNDLE.getString("invalidRemedialNetwork"), "equipment")
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
    void invalidRemedialLineEmptyConstraintTest() throws IOException {
        remedialTest(
                getBranchContingenciesProvider(),
                getBranchMonitoringMetrixDslData(),
                String.join(System.lineSeparator(), "NB;1;", "ctyId||FS.BIS1  FVALDI1  1;1;FP.AND1  FVERGE1  2;"),
                getErrorInvalidRemedialFile(2) + RESOURCE_BUNDLE.getString("invalidRemedialFileEmptyElement")
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
    void invalidNbRemedialMoreLinesTest() throws IOException {
        remedialTest(
                getBranchContingenciesProvider(),
                String.join(System.lineSeparator(), "NB;2;", "ctyId;1;FP.AND1  FVERGE1  2;"),
                getErrorInvalidRemedialFile(2) + String.format(RESOURCE_BUNDLE.getString("invalidRemedialFileNbRemedialMoreLines"), 2, 1)
        );
    }

    @Test
    void invalidNbRemedialLessLinesTest() throws IOException {
        remedialTest(
                getBranchContingenciesProvider(),
                String.join(System.lineSeparator(), "NB;1;", "ctyId;1;FP.AND1  FVERGE1  2;", "ctyId;1;FS.BIS1  FVALDI1  1;"),
                getWarningInvalidRemedial(3) + String.format(RESOURCE_BUNDLE.getString("invalidRemedialNbRemedialLessLines"), 1)
        );
    }

    @Test
    void validRemedialFileTest() throws IOException {
        remedialTest(
                getBranchContingenciesProvider(),
                getBranchMonitoringMetrixDslData(),
                String.join(System.lineSeparator(), "NB;1;", "ctyId|FS.BIS1  FVALDI1  1;1;FP.AND1  FVERGE1  2;"),
                "");
    }

    @Test
    void validRemedialFileTrimTest() throws IOException {
        remedialTest(
                getBranchContingenciesProvider(),
                getBranchMonitoringMetrixDslData(),
                String.join(System.lineSeparator(), "NB;1;", "ctyId  |FS.BIS1  FVALDI1  1    ;1;FP.AND1  FVERGE1  2        ;"),
                "");
    }

    @Test
    void validRemedialFileNoActionTest() throws IOException {
        remedialTest(
                getBranchContingenciesProvider(),
                getBranchMonitoringMetrixDslData(),
                String.join(System.lineSeparator(), "NB;1;", "ctyId;0;"),
                "");
    }
}
