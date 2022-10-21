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

public class MetrixRemedialAnalysisTest {

    private Network network;

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
            "ERROR;Remedials;Remedial file will not be recognized because line 1 contains a comment"
        );
    }

    @Test
    void invalidRemedialHeaderCommentTest() throws IOException {
        remedialTest(
            "/* comment",
            "ERROR;Remedials;Remedial file will not be recognized because line 1 contains a comment"
        );
    }

    @Test
    void invalidRemedialLineCommentLineTest() throws IOException {
        remedialTest(
            String.join(System.lineSeparator(), "NB;1;", "// comment"),
            "ERROR;Remedials;Remedial file will not be recognized because line 2 contains a comment"
        );
    }

    @Test
    void invalidRemedialLineCommentTest() throws IOException {
        remedialTest(
            String.join(System.lineSeparator(), "NB;1;", "/* comment"),
            "ERROR;Remedials;Remedial file will not be recognized because line 2 contains a comment"
        );
    }

    @Test
    void invalidRemedialHeaderEndLineTest() throws IOException {
        remedialTest(
            "line",
            "ERROR;Remedials;Remedial file will not be recognized because line 1 does not end with semicolon"
        );
    }

    @Test
    void invalidRemedialLineEndLineTest() throws IOException {
        remedialTest(
            String.join(System.lineSeparator(), "NB;1;", "line"),
            "ERROR;Remedials;Remedial file will not be recognized because line 2 does not end with semicolon"
        );
    }

    @Test
    void invalidRemedialHeaderNbColumnTest() throws IOException {
        remedialTest(
            "column1;column2;column3;",
            "ERROR;Remedials;Remedial file will not be recognized because line 1 is malformed (header)"
        );
    }

    @Test
    void invalidRemedialHeaderNbColumnEmptyTest() throws IOException {
        remedialTest(
            ";column;",
            "ERROR;Remedials;Remedial file will not be recognized because line 1 is malformed (header)"
        );
    }

    @Test
    void invalidRemedialHeaderNbColumnNumberEmptyTest() throws IOException {
        remedialTest(
            "NB;;",
            "ERROR;Remedials;Remedial file will not be recognized because line 1 is malformed (header)"
        );
    }

    @Test
    void invalidRemedialHeaderNbMissingTest() throws IOException {
        remedialTest(
            "notNB;column;",
            "ERROR;Remedials;Remedial file will not be recognized because line 1 is malformed (header)"
        );
    }

    @Test
    void invalidRemedialHeaderNbColumnNumberMissingTest() throws IOException {
        remedialTest(
            "NB;column;",
            "ERROR;Remedials;Remedial file will not be recognized because line 1 is malformed (header)"
        );
    }

    @Test
    void invalidRemedialHeaderNbColumnNumberWrongIntegerTest() throws IOException {
        remedialTest(
            "NB;-1;",
            "ERROR;Remedials;Remedial file will not be recognized because line 1 is malformed (header)"
        );
    }

    @Test
    void invalidRemedialLineNbColumnTest() throws IOException {
        remedialTest(
            String.join(System.lineSeparator(), "NB;1;", "column;"),
            "ERROR;Remedials;Remedial file will not be recognized because line 2 is malformed"
        );
    }

    @Test
    void invalidRemedialLineNbColumnEmptyTest() throws IOException {
        remedialTest(
            String.join(System.lineSeparator(), "NB;1;", ";;"),
            "ERROR;Remedials;Remedial file will not be recognized because line 2 is malformed"
        );
    }

    @Test
    void invalidRemedialLineNbActionMissingTest() throws IOException {
        remedialTest(
            String.join(System.lineSeparator(), "NB;1;", "ctyId;column;"),
            "ERROR;Remedials;Remedial file will not be recognized because line 2 is malformed (number of actions)"
        );
    }

    @Test
    void invalidRemediaLineNbActionWrongIntegerTest() throws IOException {
        remedialTest(
            String.join(System.lineSeparator(), "NB;1;", "ctyId;-1;"),
            "ERROR;Remedials;Remedial file will not be recognized because line 2 is malformed (number of actions)"
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
            "ERROR;Remedials;Remedial file will not be recognized because line 2 contains an empty element"
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
            "ERROR;Remedials;Remedial file will not be recognized because line 2 is malformed (number of actions)"
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
            "ERROR;Remedials;Remedial file will not be recognized because line 2 is malformed"
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
            "ERROR;Remedials;Remedial file will not be recognized because line 2 contains an empty element"
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
            "WARNING;Remedials;The remedial at line 2 will not be taken into account because equipment action does not exist in the network"
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
            "WARNING;Remedials;The remedial at line 2 will not be taken into account because equipment action does not exist in the network"
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
            "WARNING;Remedials;The remedial at line 2 will not be taken into account because equipment FSSV.O11_G is not a Branch or Switch type"
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
            "WARNING;Remedials;The remedial at line 2 will not be taken into account because equipment FSSV.O11_G is not a Branch or Switch type"
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
            "WARNING;Remedials;The remedial at line 2 will not be taken into account because equipment equipment does not exist in the network"
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
            "WARNING;Remedials;The remedial at line 2 will not be taken into account because equipment FSSV.O11_G is not a Branch type"
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
            "ERROR;Remedials;Remedial file will not be recognized because line 2 contains an empty element"
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
            "WARNING;Remedials;The remedial at line 2 will not be taken into account because equipment FS.BIS1  FVALDI1  1 is not monitored on contingency"
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
            "ERROR;Remedials;Remedial file will not be recognized because line 2 because remedial number in header (NB = 2) is greater than the line number of the remedial (1)"
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
            "WARNING;Remedials;The remedial at line 3 will not be taken into account because the line number of the remedial is greater than the remedial number in header (NB = 1)"
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

    @Test
    void validRemedialFileNoActionTest() throws IOException {
        ContingencyElement ctyElt = new BranchContingency("FP.AND1  FVERGE1  1");
        Contingency cty = new Contingency("ctyId", Collections.singletonList(ctyElt));
        ContingenciesProvider contingenciesProvider = network -> ImmutableList.of(cty);
        MetrixDslData metrixDslData = new MetrixDslData();
        metrixDslData.addBranchMonitoringNk("FS.BIS1  FVALDI1  1");

        remedialTest(
                contingenciesProvider,
                metrixDslData,
                String.join(System.lineSeparator(), "NB;1;", "ctyId;0;"),
                "");
    }
}
