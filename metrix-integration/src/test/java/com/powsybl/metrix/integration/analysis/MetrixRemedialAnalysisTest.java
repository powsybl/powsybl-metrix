/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration.analysis;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ResourceBundle;

import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.WARNING;

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
class MetrixRemedialAnalysisTest {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.MetrixAnalysis");

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
        StringWriter writer = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(writer)) {
            AnalysisLogger logger = new AnalysisLogger(bufferedWriter);
            RemedialLoader remedialLoader = new RemedialLoader(new StringReader(remedialScript), logger);
            remedialLoader.load();
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
                String.join(System.lineSeparator(), "NB;1;", ";1;FP.AND1  FVERGE1  2;"),
                getErrorInvalidRemedialFile(2) + RESOURCE_BUNDLE.getString("invalidRemedialFileEmptyElement")
        );
    }

    @Test
    void invalidRemedialLineNbActionEmptyTest() throws IOException {
        remedialTest(
                String.join(System.lineSeparator(), "NB;1;", "ctyId;;FP.AND1  FVERGE1  2;"),
                getErrorInvalidRemedialFile(2) + RESOURCE_BUNDLE.getString("invalidRemedialFileAction")
        );
    }

    @Test
    void invalidRemedialLineNoActionTest() throws IOException {
        remedialTest(
                String.join(System.lineSeparator(), "NB;1;", "ctyId;1;;"),
                getErrorInvalidRemedialFile(2) + RESOURCE_BUNDLE.getString("invalidRemedialFileLine")
        );
    }

    @Test
    void invalidRemedialLineActionEmptyTest() throws IOException {
        remedialTest(
                String.join(System.lineSeparator(), "NB;1;", "ctyId;2;;FP.AND1  FVERGE1  2;"),
                getErrorInvalidRemedialFile(2) + RESOURCE_BUNDLE.getString("invalidRemedialFileEmptyElement")
        );
    }

    @Test
    void invalidRemedialLineEmptyConstraintTest() throws IOException {
        remedialTest(
                String.join(System.lineSeparator(), "NB;1;", "ctyId||FS.BIS1  FVALDI1  1;1;FP.AND1  FVERGE1  2;"),
                getErrorInvalidRemedialFile(2) + RESOURCE_BUNDLE.getString("invalidRemedialFileEmptyElement")
        );
    }

    @Test
    void invalidNbRemedialMoreLinesTest() throws IOException {
        remedialTest(
                String.join(System.lineSeparator(), "NB;2;", "ctyId;1;FP.AND1  FVERGE1  2;"),
                getErrorInvalidRemedialFile(2) + String.format(RESOURCE_BUNDLE.getString("invalidRemedialFileNbRemedialMoreLines"), 2, 1)
        );
    }

    @Test
    void invalidNbRemedialLessLinesTest() throws IOException {
        remedialTest(
                String.join(System.lineSeparator(), "NB;1;", "ctyId;1;FP.AND1  FVERGE1  2;", "ctyId;1;FS.BIS1  FVALDI1  1;"),
                getWarningInvalidRemedial(3) + String.format(RESOURCE_BUNDLE.getString("invalidRemedialNbRemedialLessLines"), 1)
        );
    }

    @Test
    void validRemedialFileTest() throws IOException {
        remedialTest(
                String.join(System.lineSeparator(), "NB;1;", "ctyId|FS.BIS1  FVALDI1  1;1;FP.AND1  FVERGE1  2;"),
                "");
    }

    @Test
    void validRemedialFileTrimTest() throws IOException {
        remedialTest(
                String.join(System.lineSeparator(), "NB;1;", "ctyId  |FS.BIS1  FVALDI1  1    ;1;FP.AND1  FVERGE1  2        ;"),
                "");
    }

    @Test
    void validRemedialFileNoActionTest() throws IOException {
        remedialTest(
                String.join(System.lineSeparator(), "NB;1;", "ctyId;0;"),
                "");
    }
}
