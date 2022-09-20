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
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.Collections;
import java.util.ResourceBundle;

public class MetrixRemedialAnalysisTest {

    private Network network;

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("expected.contents");

    @BeforeEach
    public void setUp() {
        network = NetworkXml.read(getClass().getResourceAsStream("/simpleNetwork.xml"));
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
                metrixDslData,
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
            RESOURCE_BUNDLE.getString("invalid_remedial_header_comment")
        );
    }

    @Test
    void invalidRemedialHeaderCommentTest() throws IOException {
        remedialTest(
            "/* comment",
            RESOURCE_BUNDLE.getString("invalid_remedial_header_comment")
        );
    }

    @Test
    void invalidRemedialLineCommentLineTest() throws IOException {
        remedialTest(
            String.join(System.lineSeparator(), "NB;1;", "// comment"),
            RESOURCE_BUNDLE.getString("invalid_remedial_line_comment")
        );
    }

    @Test
    void invalidRemedialLineCommentTest() throws IOException {
        remedialTest(
            String.join(System.lineSeparator(), "NB;1;", "/* comment"),
            RESOURCE_BUNDLE.getString("invalid_remedial_line_comment")
        );
    }

    @Test
    void invalidRemedialHeaderEndLineTest() throws IOException {
        remedialTest(
            "line",
            RESOURCE_BUNDLE.getString("invalid_remedial_header_end_line")
        );
    }

    @Test
    void invalidRemedialLineEndLineTest() throws IOException {
        remedialTest(
            String.join(System.lineSeparator(), "NB;1;", "line"),
            RESOURCE_BUNDLE.getString("invalid_remedial_line_end_line")
        );
    }

    @Test
    void invalidRemedialHeaderNbColumnTest() throws IOException {
        remedialTest(
            "column1;column2;column3;",
            RESOURCE_BUNDLE.getString("invalid_remedial_header_malformed")
        );
    }

    @Test
    void invalidRemedialHeaderNbColumnEmptyTest() throws IOException {
        remedialTest(
            ";column;",
            RESOURCE_BUNDLE.getString("invalid_remedial_header_malformed")
        );
    }

    @Test
    void invalidRemedialHeaderNbColumnNumberEmptyTest() throws IOException {
        remedialTest(
            "NB;;",
            RESOURCE_BUNDLE.getString("invalid_remedial_header_malformed")
        );
    }

    @Test
    void invalidRemedialHeaderNbMissingTest() throws IOException {
        remedialTest(
            "notNB;column;",
            RESOURCE_BUNDLE.getString("invalid_remedial_header_malformed")
        );
    }

    @Test
    void invalidRemedialHeaderNbColumnNumberMissingTest() throws IOException {
        remedialTest(
            "NB;column;",
            RESOURCE_BUNDLE.getString("invalid_remedial_header_malformed")
        );
    }

    @Test
    void invalidRemedialHeaderNbColumnNumberWrongIntegerTest() throws IOException {
        remedialTest(
            "NB;-1;",
            RESOURCE_BUNDLE.getString("invalid_remedial_header_malformed")
        );
    }

    @Test
    void invalidRemedialLineNbColumnTest() throws IOException {
        remedialTest(
            String.join(System.lineSeparator(), "NB;1;", "column;"),
            RESOURCE_BUNDLE.getString("invalid_remedial_line_malformed")
        );
    }

    @Test
    void invalidRemedialLineNbColumnEmptyTest() throws IOException {
        remedialTest(
            String.join(System.lineSeparator(), "NB;1;", ";;"),
            RESOURCE_BUNDLE.getString("invalid_remedial_line_malformed")
        );
    }

    @Test
    void invalidRemedialLineNbActionMissingTest() throws IOException {
        remedialTest(
            String.join(System.lineSeparator(), "NB;1;", "ctyId;column;"),
            RESOURCE_BUNDLE.getString("invalid_remedial_line_malformed")
        );
    }

    @Test
    void invalidRemediaLineNbActionWrongIntegerTest() throws IOException {
        remedialTest(
            String.join(System.lineSeparator(), "NB;1;", "ctyId;-1;"),
            RESOURCE_BUNDLE.getString("invalid_remedial_line_malformed")
        );
    }

    @Test
    void invalidRemedialLineContingencyEmptyTest() throws IOException {
        ContingencyElement ctyElt = new BranchContingency("FP.AND1  FVERGE1  1");
        Contingency cty = new Contingency("ctyId", Collections.singletonList(ctyElt));
        ContingenciesProvider contingenciesProvider = network -> ImmutableList.of(cty);

        remedialTest(
            contingenciesProvider,
            String.join(System.lineSeparator(), "NB;1;", ";1;FP.AND1  FVERGE1  2;"),
            RESOURCE_BUNDLE.getString("invalid_remedial_line_empty_element")
        );
    }

    @Test
    void invalidRemedialLineNbActionEmptyTest() throws IOException {
        ContingencyElement ctyElt = new BranchContingency("FP.AND1  FVERGE1  1");
        Contingency cty = new Contingency("ctyId", Collections.singletonList(ctyElt));
        ContingenciesProvider contingenciesProvider = network -> ImmutableList.of(cty);

        remedialTest(
            contingenciesProvider,
            String.join(System.lineSeparator(), "NB;1;", "ctyId;;FP.AND1  FVERGE1  2;"),
            RESOURCE_BUNDLE.getString("invalid_remedial_line_empty_element")
        );
    }

    @Test
    void invalidRemedialLineNoActionTest() throws IOException {
        ContingencyElement ctyElt = new BranchContingency("FP.AND1  FVERGE1  1");
        Contingency cty = new Contingency("ctyId", Collections.singletonList(ctyElt));
        ContingenciesProvider contingenciesProvider = network -> ImmutableList.of(cty);

        remedialTest(
            contingenciesProvider,
            String.join(System.lineSeparator(), "NB;1;", "ctyId;1;;"),
            RESOURCE_BUNDLE.getString("invalid_remedial_line_malformed")
        );
    }

    @Test
    void invalidRemedialLineActionEmptyTest() throws IOException {
        ContingencyElement ctyElt = new BranchContingency("FP.AND1  FVERGE1  1");
        Contingency cty = new Contingency("ctyId", Collections.singletonList(ctyElt));
        ContingenciesProvider contingenciesProvider = network -> ImmutableList.of(cty);

        remedialTest(
            contingenciesProvider,
            String.join(System.lineSeparator(), "NB;1;", "ctyId;2;;FP.AND1  FVERGE1  2;"),
            RESOURCE_BUNDLE.getString("invalid_remedial_line_empty_element")
        );
    }

    @Test
    void invalidRemedialLineBranchToOpenActionTest() throws IOException {
        ContingencyElement ctyElt = new BranchContingency("FP.AND1  FVERGE1  1");
        Contingency cty = new Contingency("ctyId", Collections.singletonList(ctyElt));
        ContingenciesProvider contingenciesProvider = network -> ImmutableList.of(cty);

        remedialTest(
            contingenciesProvider,
            String.join(System.lineSeparator(), "NB;1;", "ctyId;2;action;+FP.AND1  FVERGE1  2;"),
            RESOURCE_BUNDLE.getString("invalid_remedial_line_action_does_not_exist")
        );
    }

    @Test
    void invalidRemedialLineBranchToCloseActionTest() throws IOException {
        ContingencyElement ctyElt = new BranchContingency("FP.AND1  FVERGE1  1");
        Contingency cty = new Contingency("ctyId", Collections.singletonList(ctyElt));
        ContingenciesProvider contingenciesProvider = network -> ImmutableList.of(cty);

        remedialTest(
            contingenciesProvider,
            String.join(System.lineSeparator(), "NB;1;", "ctyId;2;+action;FP.AND1  FVERGE1  2;"),
            RESOURCE_BUNDLE.getString("invalid_remedial_line_action_does_not_exist")
        );
    }

    @Test
    void invalidRemedialLineBranchToOpenTypeTest() throws IOException {
        ContingencyElement ctyElt = new BranchContingency("FP.AND1  FVERGE1  1");
        Contingency cty = new Contingency("ctyId", Collections.singletonList(ctyElt));
        ContingenciesProvider contingenciesProvider = network -> ImmutableList.of(cty);

        remedialTest(
            contingenciesProvider,
            String.join(System.lineSeparator(), "NB;1;", "ctyId;2;FSSV.O11_G;FP.AND1  FVERGE1  2;"),
            RESOURCE_BUNDLE.getString("invalid_remedial_line_equipment_is_not_a_branch")
        );
    }

    @Test
    void invalidRemedialLineBranchToCloseTypeTest() throws IOException {
        ContingencyElement ctyElt = new BranchContingency("FP.AND1  FVERGE1  1");
        Contingency cty = new Contingency("ctyId", Collections.singletonList(ctyElt));
        ContingenciesProvider contingenciesProvider = network -> ImmutableList.of(cty);

        remedialTest(
            contingenciesProvider,
            String.join(System.lineSeparator(), "NB;1;", "ctyId;2;+FSSV.O11_G;FP.AND1  FVERGE1  2;"),
            RESOURCE_BUNDLE.getString("invalid_remedial_line_equipment_is_not_a_branch")
        );
    }

    @Test
    void invalidRemedialLineConstraintTest() throws IOException {
        ContingencyElement ctyElt = new BranchContingency("FP.AND1  FVERGE1  1");
        Contingency cty = new Contingency("ctyId", Collections.singletonList(ctyElt));
        ContingenciesProvider contingenciesProvider = network -> ImmutableList.of(cty);

        remedialTest(
            contingenciesProvider,
            String.join(System.lineSeparator(), "NB;1;", "ctyId|equipment;1;FP.AND1  FVERGE1  2;"),
            RESOURCE_BUNDLE.getString("invalid_remedial_line_constraint")
        );
    }

    @Test
    void invalidRemedialLineConstraintTypeTest() throws IOException {
        ContingencyElement ctyElt = new BranchContingency("FP.AND1  FVERGE1  1");
        Contingency cty = new Contingency("ctyId", Collections.singletonList(ctyElt));
        ContingenciesProvider contingenciesProvider = network -> ImmutableList.of(cty);

        remedialTest(
            contingenciesProvider,
            String.join(System.lineSeparator(), "NB;1;", "ctyId|FSSV.O11_G;1;FP.AND1  FVERGE1  2;"),
            RESOURCE_BUNDLE.getString("invalid_remedial_line_constraint_type")
        );
    }

    @Test
    void invalidRemedialLineEmptyConstraintTest() throws IOException {
        ContingencyElement ctyElt = new BranchContingency("FP.AND1  FVERGE1  1");
        Contingency cty = new Contingency("ctyId", Collections.singletonList(ctyElt));
        ContingenciesProvider contingenciesProvider = network -> ImmutableList.of(cty);
        MetrixDslData metrixDslData = new MetrixDslData();
        metrixDslData.addBranchMonitoringNk("FS.BIS1  FVALDI1  1");

        remedialTest(
            contingenciesProvider,
            metrixDslData,
            String.join(System.lineSeparator(), "NB;1;", "ctyId||FS.BIS1  FVALDI1  1;1;FP.AND1  FVERGE1  2;"),
            RESOURCE_BUNDLE.getString("invalid_remedial_line_empty_element")
        );
    }

    @Test
    void invalidRemedialLineConstraintMonitoredTest() throws IOException {
        ContingencyElement ctyElt = new BranchContingency("FP.AND1  FVERGE1  1");
        Contingency cty = new Contingency("ctyId", Collections.singletonList(ctyElt));
        ContingenciesProvider contingenciesProvider = network -> ImmutableList.of(cty);

        remedialTest(
            contingenciesProvider,
            String.join(System.lineSeparator(), "NB;1;", "ctyId|FS.BIS1  FVALDI1  1;1;FP.AND1  FVERGE1  2;"),
            RESOURCE_BUNDLE.getString("invalid_remedial_line_constraint_monitored")
        );
    }

    @Test
    void invalidNbRemedialMoreLinesTest() throws IOException {
        ContingencyElement ctyElt = new BranchContingency("FP.AND1  FVERGE1  1");
        Contingency cty = new Contingency("ctyId", Collections.singletonList(ctyElt));
        ContingenciesProvider contingenciesProvider = network -> ImmutableList.of(cty);
        remedialTest(
            contingenciesProvider,
            String.join(System.lineSeparator(), "NB;2;", "ctyId;1;FP.AND1  FVERGE1  2;"),
            RESOURCE_BUNDLE.getString("invalid_remedial_more_lines")
        );
    }

    @Test
    void invalidNbRemedialLessLinesTest() throws IOException {
        ContingencyElement ctyElt = new BranchContingency("FP.AND1  FVERGE1  1");
        Contingency cty = new Contingency("ctyId", Collections.singletonList(ctyElt));
        ContingenciesProvider contingenciesProvider = network -> ImmutableList.of(cty);
        remedialTest(
            contingenciesProvider,
            String.join(System.lineSeparator(), "NB;1;", "ctyId;1;FP.AND1  FVERGE1  2;", "ctyId;1;FS.BIS1  FVALDI1  1;"),
            RESOURCE_BUNDLE.getString("invalid_remedial_less_lines")
        );
    }

    @Test
    void validRemedialFileTest() throws IOException {
        ContingencyElement ctyElt = new BranchContingency("FP.AND1  FVERGE1  1");
        Contingency cty = new Contingency("ctyId", Collections.singletonList(ctyElt));
        ContingenciesProvider contingenciesProvider = network -> ImmutableList.of(cty);
        MetrixDslData metrixDslData = new MetrixDslData();
        metrixDslData.addBranchMonitoringNk("FS.BIS1  FVALDI1  1");

        remedialTest(
                contingenciesProvider,
                metrixDslData,
                String.join(System.lineSeparator(), "NB;1;", "ctyId|FS.BIS1  FVALDI1  1;1;FP.AND1  FVERGE1  2;"),
                "");
    }

    @Test
    void validRemedialFileTrimTest() throws IOException {
        ContingencyElement ctyElt = new BranchContingency("FP.AND1  FVERGE1  1");
        Contingency cty = new Contingency("ctyId", Collections.singletonList(ctyElt));
        ContingenciesProvider contingenciesProvider = network -> ImmutableList.of(cty);
        MetrixDslData metrixDslData = new MetrixDslData();
        metrixDslData.addBranchMonitoringNk("FS.BIS1  FVALDI1  1");

        remedialTest(
                contingenciesProvider,
                metrixDslData,
                String.join(System.lineSeparator(), "NB;1;", "ctyId  |FS.BIS1  FVALDI1  1    ;1;FP.AND1  FVERGE1  2        ;"),
                "");
    }
}
