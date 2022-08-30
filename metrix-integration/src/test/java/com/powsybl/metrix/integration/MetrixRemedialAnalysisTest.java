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
            "ERROR;Parades topologiques;Le fichier de parades ne sera pas reconnu car la ligne 1 contient un commentaire"
        );
    }

    @Test
    void invalidRemedialHeaderCommentTest() throws IOException {
        remedialTest(
            "/* comment",
            "ERROR;Parades topologiques;Le fichier de parades ne sera pas reconnu car la ligne 1 contient un commentaire"
        );
    }

    @Test
    void invalidRemedialLineCommentLineTest() throws IOException {
        remedialTest(
            String.join(System.lineSeparator(), "NB;1;", "// comment"),
            "ERROR;Parades topologiques;Le fichier de parades ne sera pas reconnu car la ligne 2 contient un commentaire"
        );
    }

    @Test
    void invalidRemedialLineCommentTest() throws IOException {
        remedialTest(
            String.join(System.lineSeparator(), "NB;1;", "/* comment"),
            "ERROR;Parades topologiques;Le fichier de parades ne sera pas reconnu car la ligne 2 contient un commentaire"
        );
    }

    @Test
    void invalidRemedialHeaderEndLineTest() throws IOException {
        remedialTest(
            "line",
            "ERROR;Parades topologiques;Le fichier de parades ne sera pas reconnu car la ligne 1 ne se termine pas par un point virgule"
        );
    }

    @Test
    void invalidRemedialLineEndLineTest() throws IOException {
        remedialTest(
            String.join(System.lineSeparator(), "NB;1;", "line"),
            "ERROR;Parades topologiques;Le fichier de parades ne sera pas reconnu car la ligne 2 ne se termine pas par un point virgule"
        );
    }

    @Test
    void invalidRemedialHeaderNbColumnTest() throws IOException {
        remedialTest(
            "column1;column2;column3;",
            "ERROR;Parades topologiques;Le fichier de parades ne sera pas reconnu car la ligne 1 est mal formée (entête)"
        );
    }

    @Test
    void invalidRemedialHeaderNbColumnEmptyTest() throws IOException {
        remedialTest(
            ";column;",
            "ERROR;Parades topologiques;Le fichier de parades ne sera pas reconnu car la ligne 1 est mal formée (entête)"
        );
    }

    @Test
    void invalidRemedialHeaderNbColumnNumberEmptyTest() throws IOException {
        remedialTest(
            "NB;;",
            "ERROR;Parades topologiques;Le fichier de parades ne sera pas reconnu car la ligne 1 est mal formée (entête)"
        );
    }

    @Test
    void invalidRemedialHeaderNbMissingTest() throws IOException {
        remedialTest(
            "notNB;column;",
            "ERROR;Parades topologiques;Le fichier de parades ne sera pas reconnu car la ligne 1 est mal formée (entête)"
        );
    }

    @Test
    void invalidRemedialHeaderNbColumnNumberMissingTest() throws IOException {
        remedialTest(
            "NB;column;",
            "ERROR;Parades topologiques;Le fichier de parades ne sera pas reconnu car la ligne 1 est mal formée (entête)"
        );
    }

    @Test
    void invalidRemedialHeaderNbColumnNumberWrongIntegerTest() throws IOException {
        remedialTest(
            "NB;-1;",
            "ERROR;Parades topologiques;Le fichier de parades ne sera pas reconnu car la ligne 1 est mal formée (entête)"
        );
    }

    @Test
    void invalidRemedialLineNbColumnTest() throws IOException {
        remedialTest(
            String.join(System.lineSeparator(), "NB;1;", "column;"),
            "ERROR;Parades topologiques;Le fichier de parades ne sera pas reconnu car la ligne 2 est mal formée"
        );
    }

    @Test
    void invalidRemedialLineNbColumnEmptyTest() throws IOException {
        remedialTest(
            String.join(System.lineSeparator(), "NB;1;", ";;"),
            "ERROR;Parades topologiques;Le fichier de parades ne sera pas reconnu car la ligne 2 est mal formée"
        );
    }

    @Test
    void invalidRemedialLineNbActionMissingTest() throws IOException {
        remedialTest(
            String.join(System.lineSeparator(), "NB;1;", "ctyId;column;"),
            "ERROR;Parades topologiques;Le fichier de parades ne sera pas reconnu car la ligne 2 est mal formée"
        );
    }

    @Test
    void invalidRemediaLineNbActionWrongIntegerTest() throws IOException {
        remedialTest(
            String.join(System.lineSeparator(), "NB;1;", "ctyId;-1;"),
            "ERROR;Parades topologiques;Le fichier de parades ne sera pas reconnu car la ligne 2 est mal formée"
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
            "ERROR;Parades topologiques;Le fichier de parades ne sera pas reconnu car la ligne 2 contient un élément vide"
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
            "ERROR;Parades topologiques;Le fichier de parades ne sera pas reconnu car la ligne 2 contient un élément vide"
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
            "ERROR;Parades topologiques;Le fichier de parades ne sera pas reconnu car la ligne 2 est mal formée"
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
            "ERROR;Parades topologiques;Le fichier de parades ne sera pas reconnu car la ligne 2 contient un élément vide"
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
            "WARNING;Parades topologiques;La parade à la ligne 2 ne sera pas prise en compte car l'ouvrage action n'existe pas dans le réseau"
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
            "WARNING;Parades topologiques;La parade à la ligne 2 ne sera pas prise en compte car l'ouvrage action n'existe pas dans le réseau"
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
            "WARNING;Parades topologiques;La parade à la ligne 2 ne sera pas prise en compte car l'ouvrage FSSV.O11_G n'est pas de type Branch ni de type Switch"
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
            "WARNING;Parades topologiques;La parade à la ligne 2 ne sera pas prise en compte car l'ouvrage FSSV.O11_G n'est pas de type Branch ni de type Switch"
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
            "WARNING;Parades topologiques;La parade à la ligne 2 ne sera pas prise en compte car l'ouvrage equipment n'existe pas dans le réseau"
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
            "WARNING;Parades topologiques;La parade à la ligne 2 ne sera pas prise en compte car l'ouvrage FSSV.O11_G n'est pas de type Branch"
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
            "ERROR;Parades topologiques;Le fichier de parades ne sera pas reconnu car la ligne 2 contient un élément vide"
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
            "WARNING;Parades topologiques;La parade à la ligne 2 ne sera pas prise en compte car l'ouvrage FS.BIS1  FVALDI1  1 n'est pas surveillé sur incident"
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
            "ERROR;Parades topologiques;Le fichier de parades ne sera pas reconnu car le nombre de parades indiqué dans l'entête (NB = 2) est supérieur au nombre de lignes de parades (1)"
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
            "WARNING;Parades topologiques;La parade à la ligne 3 ne sera pas prise en compte car le numéro de ligne de la parade est supérieur au nombre de parades indiqué dans l'entête (NB = 1)"
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
