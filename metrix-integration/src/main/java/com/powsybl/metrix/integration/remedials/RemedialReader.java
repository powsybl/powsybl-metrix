/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration.remedials;

import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class RemedialReader {

    public static final String COLUMN_SEPARATOR = ";";
    public static final String CONSTRAINT_RESTRICTION_SEPARATOR = "\\|";
    public static final String BRANCH_TO_CLOSE_SYMBOL = "+";
    public static final String NUMBER_OF_LINES_SYMBOL = "NB";
    public static final int FIRST_ACTION_INDEX = 2;

    private RemedialReader() {
    }

    public static String rTrim(String line) {
        return StringUtils.stripEnd(line, " ");
    }

    public static List<Remedial> parseFile(String fileContent) {
        if (StringUtils.isEmpty(fileContent)) {
            return Collections.emptyList();
        }

        return parseFile(new StringReader(fileContent));
    }

    public static List<Remedial> parseFile(Reader reader) {
        if (reader == null) {
            return Collections.emptyList();
        }
        AtomicInteger line = new AtomicInteger(2); //Remedial starts at line 2, header is on line 1
        try (BufferedReader bufferedReader = new BufferedReader(reader)) {
            return bufferedReader.lines()
                    .skip(1) // skip header line
                    .map(s -> s.split(COLUMN_SEPARATOR))
                    .filter(columns -> columns.length >= FIRST_ACTION_INDEX + 1)
                    .map(columns -> createRemedialFromLine(columns, line.getAndIncrement()))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Erreur lors de la lecture des parades", e);
        }
    }

    public static String extractContingencyFromContingencyAndConstraint(String contingencyAndConstraint) {
        String[] nameAndConstraint = contingencyAndConstraint.split(CONSTRAINT_RESTRICTION_SEPARATOR);
        return rTrim(nameAndConstraint[0]);
    }

    public static List<String> extractConstraintFromContingencyAndConstraint(String contingencyAndConstraint) {
        String[] nameAndConstraint = contingencyAndConstraint.split(CONSTRAINT_RESTRICTION_SEPARATOR);
        return Arrays.stream(nameAndConstraint)
                .skip(1) //skip contingency
                .map(RemedialReader::rTrim)
                .toList();
    }

    private static Remedial createRemedialFromLine(String[] columns, int line) {
        // column 0 => contingency and constraints
        String contingency = extractContingencyFromContingencyAndConstraint(columns[0]);
        List<String> constraint = extractConstraintFromContingencyAndConstraint(columns[0]);

        // column 1 => nb actions (unused)
        // column 2 -> n => actions
        List<String> branchToOpen = new ArrayList<>();
        List<String> branchToClose = new ArrayList<>();
        List<String> actions = new ArrayList<>();
        Arrays.stream(columns).skip(FIRST_ACTION_INDEX).forEach(a -> {
            String action = rTrim(a);
            actions.add(action);
            if (action.startsWith(BRANCH_TO_CLOSE_SYMBOL)) {
                branchToClose.add(action.substring(1));
            } else {
                branchToOpen.add(action);
            }
        });
        return new Remedial(line, contingency, constraint, branchToOpen, branchToClose, String.join(COLUMN_SEPARATOR, actions));
    }
}
