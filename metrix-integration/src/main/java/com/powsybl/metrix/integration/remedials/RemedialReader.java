/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.metrix.integration.remedials;

import com.powsybl.commons.PowsyblException;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class RemedialReader {

    public static final String COLUMN_SEPARATOR = ";";
    public static final String CONSTRAINT_RESTRICTION_SEPARATOR = "\\|";

    private RemedialReader() {
    }

    public static List<Remedial> parseFile(Supplier<Reader> readerSupplier) {
        if (readerSupplier == null) {
            return Collections.emptyList();
        }
        AtomicInteger line = new AtomicInteger(2);
        try (BufferedReader bufferedReader = new BufferedReader(readerSupplier.get())) {
            return bufferedReader.lines()
                    .skip(1)
                    .map(s -> s.split(COLUMN_SEPARATOR))
                    .filter(columns -> columns.length >= 3)
                    .map(columns -> {
                                String[] nameAndConstraint = columns[0].split(CONSTRAINT_RESTRICTION_SEPARATOR);
                                List<String> constraint = Arrays.stream(nameAndConstraint).skip(1).collect(Collectors.toList());

                                List<String> branchToOpen = new ArrayList<>();
                                List<String> branchToClose = new ArrayList<>();
                                Arrays.stream(columns).skip(2).forEach(action -> {
                                    if (action.startsWith("+")) {
                                        branchToClose.add(action.substring(1));
                                    } else {
                                        branchToOpen.add(action);
                                    }
                                });
                                return new Remedial(line.getAndIncrement(), nameAndConstraint[0], constraint, branchToOpen, branchToClose);
                            }
                    )
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException("Error when reading remedials", e);
        }
    }

    public static List<Remedial> parseFile(String fileContent) {
        if (StringUtils.isEmpty(fileContent)) {
            return Collections.emptyList();
        }

        return parseFile(() -> new StringReader(fileContent));
    }

    public static void checkFile(Reader reader) {
        try (BufferedReader bufferedReader = new BufferedReader(reader)) {
            String[] actions;
            String line;

            // Check header
            if ((line = bufferedReader.readLine()) != null) {
                if (!line.trim().endsWith(COLUMN_SEPARATOR)) {
                    throw new PowsyblException("Missing '" + COLUMN_SEPARATOR + "' in remedial action file header");
                }
                actions = line.split(COLUMN_SEPARATOR);
                if (actions.length != 2 ||
                        !"NB".equals(actions[0]) ||
                        Integer.parseInt(actions[1]) < 0) {
                    throw new PowsyblException("Malformed remedial action file header");
                }
                int lineId = 1;
                while ((line = bufferedReader.readLine()) != null) {
                    lineId++;

                    if (!line.trim().endsWith(COLUMN_SEPARATOR)) {
                        throw new PowsyblException("Missing '" + COLUMN_SEPARATOR + "' in remedial action file, line " + lineId);
                    }

                    actions = line.split(COLUMN_SEPARATOR);

                    if (actions.length < 2) {
                        throw new PowsyblException("Malformed remedial action file, line : " + lineId);
                    } else {
                        for (String action : actions) {
                            if (action == null || action.isEmpty()) {
                                throw new PowsyblException("Empty element in remedial action file, line : " + lineId);
                            }
                        }
                    }
                    if (Integer.parseInt(actions[1]) < 0) {
                        throw new PowsyblException("Malformed number of actions in remedial action file, line : " + lineId);
                    }

                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
