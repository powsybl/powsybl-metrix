/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration.analysis;

import com.powsybl.metrix.integration.remedials.Remedial;
import com.powsybl.metrix.integration.remedials.RemedialReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import static com.powsybl.metrix.integration.analysis.AnalysisLogger.defaultLogger;
import static com.powsybl.metrix.integration.remedials.RemedialReader.rTrim;

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
public class RemedialLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(RemedialLoader.class);

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.MetrixAnalysis");
    private static final String REMEDIALS_SECTION = RESOURCE_BUNDLE.getString("remedialsSection");
    private static final String INVALID_REMEDIAL_FILE_MESSAGE_KEY = "invalidRemedialFile";

    private static final String COMMENT_SYMBOL = "/*";
    private static final String COMMENT_LINE_SYMBOL = "//";

    private final Reader remedialActionsReader;
    private final AnalysisLogger analysisLogger;

    public RemedialLoader(Reader remedialActionsReader,
                          AnalysisLogger analysisLogger) {
        this.remedialActionsReader = remedialActionsReader;
        this.analysisLogger = analysisLogger != null ? analysisLogger : defaultLogger();
    }

    /**
     * Load remedials
     */
    public List<Remedial> load() {
        LOGGER.info("Loading remedials");
        if (remedialActionsReader == null) {
            return Collections.emptyList();
        }
        List<Remedial> remedials;
        try (BufferedReader bufferedReader = new BufferedReader(remedialActionsReader)) {
            String fileContent = bufferedReader.lines().collect(Collectors.joining(System.lineSeparator()));
            checkFile(fileContent);
            remedials = RemedialReader.parseFile(fileContent);
        } catch (IOException e) {
            LOGGER.error("Error encountered while reading remedials", e);
            throw new UncheckedIOException(e);
        }
        return remedials;
    }

    private boolean checkNumber(String number, int lineId, String messageReason) {
        try {
            if (Integer.parseInt(number) < 0) {
                analysisLogger.errorWithReason(REMEDIALS_SECTION, new Reason(messageReason),
                    INVALID_REMEDIAL_FILE_MESSAGE_KEY, lineId);
                return false;
            }
        } catch (NumberFormatException e) {
            analysisLogger.errorWithReason(REMEDIALS_SECTION, new Reason(messageReason),
                INVALID_REMEDIAL_FILE_MESSAGE_KEY, lineId);
            return false;
        }
        return true;
    }

    private void checkFile(String fileContent) {
        checkFile(new StringReader(fileContent));
    }

    /**
     * Check a remedial file content:
     * <ul>
     *     <li>check that the header is correctly formatted</li>
     *     <li>check that each line is correctly formatted</li>
     *     <li>check that the number of lines is consistent with the number of remedials</li>
     * </ul>
     */
    private void checkFile(Reader reader) {
        try (BufferedReader bufferedReader = new BufferedReader(reader)) {
            int lineId = 1;
            String headerLine = bufferedReader.readLine();
            if (!checkHeader(headerLine, lineId)) {
                return;
            }
            int nbRemedial = Integer.parseInt(headerLine.split(RemedialReader.COLUMN_SEPARATOR)[1]);

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                lineId++;
                checkLine(line, lineId);
                if (lineId - 1 > nbRemedial) {
                    analysisLogger.warnWithReason(REMEDIALS_SECTION, new Reason("invalidRemedialNbRemedialLessLines", nbRemedial),
                        "invalidRemedial", lineId);
                }
            }

            if (lineId - 1 < nbRemedial) {
                analysisLogger.errorWithReason(REMEDIALS_SECTION, new Reason("invalidRemedialFileNbRemedialMoreLines", nbRemedial, lineId - 1),
                    INVALID_REMEDIAL_FILE_MESSAGE_KEY, lineId);
            }
        } catch (IOException e) {
            LOGGER.error("Error encountered while checking remedials", e);
            throw new UncheckedIOException(e);
        }
    }

    /**
     * check remedial header NB;<nb remedials>;
     *
     * @param line the first line in the remedial file
     * @return false if header is malformed
     */
    private boolean checkHeader(String line, int lineId) {
        if (line == null) {
            return false;
        }

        if (checkIfErrorAtBeginningOrEnd(line, lineId)) {
            return false;
        }

        String[] columns = line.split(RemedialReader.COLUMN_SEPARATOR);
        if (columns.length != 2 || !RemedialReader.NUMBER_OF_LINES_SYMBOL.equals(columns[0])) {
            analysisLogger.errorWithReason(REMEDIALS_SECTION, new Reason("invalidRemedialFileHeader"),
                INVALID_REMEDIAL_FILE_MESSAGE_KEY, lineId);
            return false;
        }

        return checkNumber(columns[1], lineId, "invalidRemedialFileHeader");
    }

    /**
     * check remedial line <contingency|<constraints>;<nb actions>;<actions>;
     * constraints are optional
     *
     * @param line   a line describing a remedial in the remedial file
     * @param lineId line number in the remedial file
     */
    private void checkLine(String line, int lineId) {
        if (checkIfErrorAtBeginningOrEnd(line, lineId)) {
            return;
        }

        String[] actions = line.split(RemedialReader.COLUMN_SEPARATOR);
        if (actions.length >= RemedialReader.FIRST_ACTION_INDEX) {
            if (!checkNumber(actions[1], lineId, "invalidRemedialFileAction")) {
                return;
            }
        }

        boolean isNbActionsEqualToZero = actions.length >= RemedialReader.FIRST_ACTION_INDEX && Integer.parseInt(actions[1]) == 0;
        if (actions.length <= RemedialReader.FIRST_ACTION_INDEX && !isNbActionsEqualToZero) {
            analysisLogger.errorWithReason(REMEDIALS_SECTION, new Reason("invalidRemedialFileLine"),
                INVALID_REMEDIAL_FILE_MESSAGE_KEY, lineId);
            return;
        }

        for (String action : actions) {
            String actionToCheck = rTrim(action);
            if (actionToCheck == null || actionToCheck.isEmpty()) {
                analysisLogger.errorWithReason(REMEDIALS_SECTION, new Reason("invalidRemedialFileEmptyElement"),
                    INVALID_REMEDIAL_FILE_MESSAGE_KEY, lineId);
            }
        }

        List<String> constraints = RemedialReader.extractConstraintFromContingencyAndConstraint(actions[0]);
        for (String constraint : constraints) {
            String constraintToCheck = rTrim(constraint);
            if (constraintToCheck == null || constraintToCheck.isEmpty()) {
                analysisLogger.errorWithReason(REMEDIALS_SECTION, new Reason("invalidRemedialFileEmptyElement"),
                    INVALID_REMEDIAL_FILE_MESSAGE_KEY, lineId);
            }
        }
    }

    /**
     * Check the beginning and end of a remedial file line
     *
     * @param line   a line of a remedial file
     * @param lineId line number in a remedial file
     * @return true if a line is a comment or ends with a wrong separator
     */
    private boolean checkIfErrorAtBeginningOrEnd(String line, int lineId) {
        String lineToCheck = rTrim(line);
        if (lineToCheck.startsWith(COMMENT_SYMBOL) || lineToCheck.startsWith(COMMENT_LINE_SYMBOL)) {
            analysisLogger.errorWithReason(REMEDIALS_SECTION, new Reason("invalidRemedialFileComment"),
                INVALID_REMEDIAL_FILE_MESSAGE_KEY, lineId);
            return true;
        }
        if (!lineToCheck.endsWith(RemedialReader.COLUMN_SEPARATOR)) {
            analysisLogger.errorWithReason(REMEDIALS_SECTION, new Reason("invalidRemedialFileEndLine"),
                INVALID_REMEDIAL_FILE_MESSAGE_KEY, lineId);
            return true;
        }
        return false;
    }
}
