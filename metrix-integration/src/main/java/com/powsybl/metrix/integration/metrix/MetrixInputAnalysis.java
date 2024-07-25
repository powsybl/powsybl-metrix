/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration.metrix;

import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyElement;
import com.powsybl.contingency.ContingencyElementType;
import com.powsybl.iidm.network.*;
import com.powsybl.metrix.integration.MetrixDslData;
import com.powsybl.metrix.integration.exceptions.ContingenciesScriptLoadingException;
import com.powsybl.metrix.integration.remedials.Remedial;
import com.powsybl.metrix.integration.remedials.RemedialReader;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import static com.powsybl.metrix.integration.remedials.RemedialReader.rTrim;

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
public class MetrixInputAnalysis {

    enum LogType {
        ERROR,
        WARNING,
        INFO
    }

    private static final char SEPARATOR = ';';
    private static final String SECTION_SEPARATOR = " - ";
    private static final String COMMENT_SYMBOL = "/*";
    private static final String COMMENT_LINE_SYMBOL = "//";

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.MetrixAnalysis");
    private static final String CONTINGENCIES_SECTION = RESOURCE_BUNDLE.getString("contingenciesSection");
    private static final String REMEDIALS_SECTION = RESOURCE_BUNDLE.getString("remedialsSection");

    private static final String HVDC_LINE_TYPE = "hvdcLine";
    private static final String GENERATOR_TYPE = "generator";
    private static final String LOAD_TYPE = "load";
    private static final String PHASE_TAP_CHANGER_TYPE = "phaseTapChanger";

    private final Reader remedialActionsReader;
    private final ContingenciesProvider contingenciesProvider;
    private final Network network;
    private final MetrixDslData metrixDslData;
    private final BufferedWriter writer;

    public MetrixInputAnalysis(Reader remedialActionsReader, ContingenciesProvider contingenciesProvider, Network network, MetrixDslData metrixDslData, BufferedWriter writer) {
        Objects.requireNonNull(contingenciesProvider);
        Objects.requireNonNull(network);
        this.remedialActionsReader = remedialActionsReader;
        this.contingenciesProvider = contingenciesProvider;
        this.network = network;
        this.metrixDslData = metrixDslData;
        this.writer = writer;
    }

    public MetrixInputAnalysisResult runAnalysis() throws IOException {
        List<Contingency> contingencies = loadContingencies();
        List<Remedial> remedials = loadRemedials();
        Set<String> contingencyIds = contingencies.stream().map(Contingency::getId).collect(Collectors.toSet());
        runMetrixDslDataAnalysis(contingencyIds);
        runRemedialAnalysis(remedials, contingencyIds);
        return new MetrixInputAnalysisResult(remedials, contingencies);
    }

    private void runMetrixDslDataAnalysis(Set<String> contingencyIds) {
        if (metrixDslData == null) {
            return;
        }
        checkMetrixDslContingencies(contingencyIds, HVDC_LINE_TYPE, metrixDslData.getHvdcContingenciesMap());
        checkMetrixDslContingencies(contingencyIds, GENERATOR_TYPE, metrixDslData.getGeneratorContingenciesMap());
        checkMetrixDslContingencies(contingencyIds, LOAD_TYPE, metrixDslData.getLoadContingenciesMap());
        checkMetrixDslContingencies(contingencyIds, PHASE_TAP_CHANGER_TYPE, metrixDslData.getPtcContingenciesMap());
    }

    private void runRemedialAnalysis(List<Remedial> remedials, Set<String> contingencyIds) {
        remedials.forEach(remedial -> checkRemedial(remedial, contingencyIds));
    }

    private void writeLog(String type, String section, String message, BufferedWriter writer) {
        if (writer == null) {
            return;
        }
        try {
            writer.write(type + SEPARATOR + section + SEPARATOR + message);
            writer.newLine();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void writeLog(String type, String section, String message) {
        writeLog(type, section, message, writer);
    }

    private void writeContingencyLog(String contingencyId, String elementId, String messageReason) {
        String message = String.format(RESOURCE_BUNDLE.getString("invalidContingency"), contingencyId, elementId);
        writeLog(String.valueOf(LogType.WARNING), CONTINGENCIES_SECTION, message + " " + messageReason);
    }

    private void writeDslDataContingencyLog(String equipmentType, String equipmentId, String contingencyId) {
        String message = String.format(RESOURCE_BUNDLE.getString("invalidMetrixDslDataContingency"), equipmentType, contingencyId);
        writeLog(String.valueOf(LogType.WARNING), CONTINGENCIES_SECTION + SECTION_SEPARATOR + equipmentId, message);
    }

    private void writeRemedialLog(int line, String messageReason) {
        String message = String.format(RESOURCE_BUNDLE.getString("invalidRemedial"), line);
        writeLog(String.valueOf(LogType.WARNING), REMEDIALS_SECTION, message + " " + messageReason);
    }

    private void writeRemedialFileLog(int line, String messageReason) {
        String message = String.format(RESOURCE_BUNDLE.getString("invalidRemedialFile"), line);
        writeLog(String.valueOf(LogType.ERROR), REMEDIALS_SECTION, message + " " + messageReason);
    }

    /**
     * load contingencies
     * @return list of contingencies
     */
    private List<Contingency> loadContingencies() {
        List<Contingency> allContingencies;
        try {
            allContingencies = contingenciesProvider.getContingencies(network);
        } catch (RuntimeException e) {
            throw new ContingenciesScriptLoadingException(e);
        }
        List<Contingency> contingencies = new ArrayList<>();
        for (Contingency cty : allContingencies) {
            if (isValidContingency(cty)) {
                contingencies.add(cty);
            }
        }
        return contingencies;
    }

    private static boolean isValidContingencyType(ContingencyElementType elementType) {
        return elementType == ContingencyElementType.LINE ||
            elementType == ContingencyElementType.TWO_WINDINGS_TRANSFORMER ||
            elementType == ContingencyElementType.BRANCH ||
            elementType == ContingencyElementType.GENERATOR ||
            elementType == ContingencyElementType.HVDC_LINE;
    }

    private static boolean isValidContingencyTypeForIdentifiable(IdentifiableType identifiableType, ContingencyElementType elementType) {
        if (elementType == ContingencyElementType.LINE && identifiableType == IdentifiableType.LINE) {
            return true;
        }
        if (elementType == ContingencyElementType.TWO_WINDINGS_TRANSFORMER && identifiableType == IdentifiableType.TWO_WINDINGS_TRANSFORMER) {
            return true;
        }
        if (elementType == ContingencyElementType.BRANCH && (identifiableType == IdentifiableType.LINE || identifiableType == IdentifiableType.TWO_WINDINGS_TRANSFORMER)) {
            return true;
        }
        if (elementType == ContingencyElementType.GENERATOR && identifiableType == IdentifiableType.GENERATOR) {
            return true;
        }
        return elementType == ContingencyElementType.HVDC_LINE && identifiableType == IdentifiableType.HVDC_LINE;
    }

    public static boolean isValidContingencyElement(IdentifiableType identifiableType, ContingencyElementType elementType) {
        if (!isValidContingencyType(elementType)) {
            return false;
        }
        return isValidContingencyTypeForIdentifiable(identifiableType, elementType);
    }

    private boolean isValidContingencyElement(Identifiable<?> identifiable, String contingencyId, ContingencyElement element) {
        boolean isValid = true;
        boolean isExistingIdentifiable = identifiable != null;
        boolean isValidContingencyType = isValidContingencyType(element.getType());
        if (!isExistingIdentifiable) {
            String message = RESOURCE_BUNDLE.getString("invalidContingencyNetwork");
            writeContingencyLog(contingencyId, element.getId(), message);
            isValid = false;
        }
        if (!isValidContingencyType) {
            String message = String.format(RESOURCE_BUNDLE.getString("invalidContingencyType"), element.getType());
            writeContingencyLog(contingencyId, element.getId(), message);
            isValid = false;
        }
        if (isValid && !isValidContingencyTypeForIdentifiable(identifiable.getType(), element.getType())) {
            String message = String.format(RESOURCE_BUNDLE.getString("invalidContingencyNetworkType"), element.getType(), identifiable.getType());
            writeContingencyLog(contingencyId, element.getId(), message);
            isValid = false;
        }
        return isValid;
    }

    private boolean isValidContingency(Contingency contingency) {
        boolean isValid = true;
        for (ContingencyElement element : contingency.getElements()) {
            Identifiable<?> identifiable = network.getIdentifiable(element.getId());
            isValid = isValid && isValidContingencyElement(identifiable, contingency.getId(), element);
        }
        return isValid;
    }

    /**
     * load remedials
     * @return list of remedials
     */
    private List<Remedial> loadRemedials() {
        if (remedialActionsReader == null) {
            return Collections.emptyList();
        }
        List<Remedial> remedials;
        try (BufferedReader bufferedReader = new BufferedReader(remedialActionsReader)) {
            String fileContent = bufferedReader.lines().collect(Collectors.joining(System.lineSeparator()));
            checkFile(fileContent);
            remedials = RemedialReader.parseFile(fileContent);
            remedials.forEach(this::isValidRemedial);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return remedials;
    }

    private boolean checkNumber(String number, int lineId, String message) {
        try {
            if (Integer.parseInt(number) < 0) {
                writeRemedialFileLog(lineId, message);
                return false;
            }
        } catch (NumberFormatException e) {
            writeRemedialFileLog(lineId, message);
            return false;
        }
        return true;
    }

    private void checkFile(String fileContent) {
        checkFile(new StringReader(fileContent));
    }

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
                    String message = String.format(RESOURCE_BUNDLE.getString("invalidRemedialNbRemedialLessLines"), nbRemedial);
                    writeRemedialLog(lineId, message);
                }
            }

            if (lineId - 1 < nbRemedial) {
                String message = String.format(RESOURCE_BUNDLE.getString("invalidRemedialFileNbRemedialMoreLines"), nbRemedial, lineId - 1);
                writeRemedialFileLog(lineId, message);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * check remedial header NB;<nb remedials>;
     *
     * @param line the first line in remedial file
     * @return false if header is malformed
     */
    private boolean checkHeader(String line, int lineId) {
        if (line == null) {
            return false;
        }

        if (!checkBeginAndEnd(line, lineId)) {
            return false;
        }

        String message = String.format(RESOURCE_BUNDLE.getString("invalidRemedialFileHeader"), lineId);
        String[] columns = line.split(RemedialReader.COLUMN_SEPARATOR);
        if (columns.length != 2 || !RemedialReader.NUMBER_OF_LINES_SYMBOL.equals(columns[0])) {
            writeRemedialFileLog(lineId, message);
            return false;
        }

        return checkNumber(columns[1], lineId, message);
    }

    /**
     * check remedial line <contingency|<constraints>;<nb actions>;<actions>;
     * constraints are optional
     *
     * @param line a line describing a remedial in remedial file
     * @param lineId line number in remedial file
     */
    private void checkLine(String line, int lineId) throws IOException {
        if (!checkBeginAndEnd(line, lineId)) {
            return;
        }

        String[] actions = line.split(RemedialReader.COLUMN_SEPARATOR);
        if (actions.length >= RemedialReader.FIRST_ACTION_INDEX) {
            String message = String.format(RESOURCE_BUNDLE.getString("invalidRemedialFileAction"), lineId);
            if (!checkNumber(actions[1], lineId, message)) {
                return;
            }
        }

        boolean isNbActionsEqualToZero = actions.length >= RemedialReader.FIRST_ACTION_INDEX && Integer.parseInt(actions[1]) == 0;
        if (actions.length <= RemedialReader.FIRST_ACTION_INDEX && !isNbActionsEqualToZero) {
            String message = String.format(RESOURCE_BUNDLE.getString("invalidRemedialFileLine"), lineId);
            writeRemedialFileLog(lineId, message);
            return;
        }

        for (String action : actions) {
            rTrim(action);
            if (action == null || action.isEmpty()) {
                String message = String.format(RESOURCE_BUNDLE.getString("invalidRemedialFileEmptyElement"), lineId);
                writeRemedialFileLog(lineId, message);
            }
        }

        List<String> constraints = RemedialReader.extractConstraintFromContingencyAndConstraint(actions[0]);
        for (String constraint : constraints) {
            rTrim(constraint);
            if (constraint == null || constraint.isEmpty()) {
                String message = String.format(RESOURCE_BUNDLE.getString("invalidRemedialFileEmptyElement"), lineId);
                writeRemedialFileLog(lineId, message);
            }
        }
    }

    /**
     * check begin and end of a remedial file line
     *
     * @param line a line of remedial file
     * @param lineId line number in remedial file
     * @return true if line is not a comment and ends with the correct separator
     */
    private boolean checkBeginAndEnd(String line, int lineId) {
        rTrim(line);
        if (line.startsWith(COMMENT_SYMBOL) || line.startsWith(COMMENT_LINE_SYMBOL)) {
            String message = String.format(RESOURCE_BUNDLE.getString("invalidRemedialFileComment"), lineId);
            writeRemedialFileLog(lineId, message);
            return false;
        }
        if (!line.endsWith(RemedialReader.COLUMN_SEPARATOR)) {
            String message = String.format(RESOURCE_BUNDLE.getString("invalidRemedialFileEndLine"), lineId);
            writeRemedialFileLog(lineId, message);
            return false;
        }
        return true;
    }

    private void checkRemedial(Remedial remedial, Set<String> contingencyIds) {
        if (!remedial.getContingency().isEmpty() && !contingencyIds.contains(remedial.getContingency())) {
            String message = String.format(RESOURCE_BUNDLE.getString("invalidMetrixRemedialContingency"), remedial.getContingency());
            writeRemedialLog(remedial.getLineFile(), message);
        }
    }

    /**
     * check an action of a remedial line : action is equipment of the network and is of Branch or Switch type
     *
     * @param line line number
     * @param network network
     * @param action to check
     */
    private void isValidAction(int line, Network network, String action) {
        if (action.isEmpty()) {
            return;
        }
        Identifiable<?> identifiable = network.getIdentifiable(action);
        if (identifiable == null) {
            String message = String.format(RESOURCE_BUNDLE.getString("invalidRemedialNetwork"), action);
            writeRemedialLog(line, message);
            return;
        }
        if (!(identifiable instanceof Branch) && !(identifiable instanceof Switch)) {
            String message = String.format(RESOURCE_BUNDLE.getString("invalidRemedialActionType"), identifiable.getId());
            writeRemedialLog(line, message);
        }
    }

    /**
     * check a constraint of a remedial line : constraint is equipment of the network, is of Branch type
     * and monitored on contingencies (branchRatingOnContingencies)
     *
     * @param line number
     * @param network network
     * @param constraint to check
     */
    private void isValidConstraint(int line, Network network, String constraint) {
        if (constraint.isEmpty()) {
            return;
        }
        Identifiable<?> identifiable = network.getIdentifiable(constraint);
        if (identifiable == null) {
            String message = String.format(RESOURCE_BUNDLE.getString("invalidRemedialNetwork"), constraint);
            writeRemedialLog(line, message);
            return;
        }
        if (!(identifiable instanceof Branch)) {
            String message = String.format(RESOURCE_BUNDLE.getString("invalidRemedialConstraintType"), identifiable.getId());
            writeRemedialLog(line, message);
            return;
        }
        if (metrixDslData == null) {
            return;
        }
        if (!metrixDslData.getBranchMonitoringListNk().containsKey(constraint)) {
            String message = String.format(RESOURCE_BUNDLE.getString("invalidMetrixRemedialConstraint"), constraint);
            writeRemedialLog(line, message);
        }
    }

    private void isValidRemedial(Remedial remedial) {
        for (String action : remedial.getBranchToOpen()) {
            isValidAction(remedial.getLineFile(), network, action);
        }
        for (String action : remedial.getBranchToClose()) {
            isValidAction(remedial.getLineFile(), network, action);
        }
        for (String constraint : remedial.getConstraint()) {
            isValidConstraint(remedial.getLineFile(), network, constraint);
        }
    }

    /**
     * check contingencies used in Metrix configuration
     */
    private void checkMetrixDslContingencies(Set<String> contingencyIds, String equipmentType, Map<String, List<String>> equipmentMap) {
        for (Map.Entry<String, List<String>> entry : equipmentMap.entrySet()) {
            for (String ctyId : entry.getValue()) {
                if (!contingencyIds.contains(ctyId)) {
                    writeDslDataContingencyLog(RESOURCE_BUNDLE.getString(equipmentType), entry.getKey(), ctyId);
                }
            }
        }
    }
}
