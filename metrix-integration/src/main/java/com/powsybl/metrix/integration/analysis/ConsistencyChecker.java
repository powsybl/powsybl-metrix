/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration.analysis;

import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.IdentifiableType;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Switch;
import com.powsybl.metrix.integration.MetrixDslData;
import com.powsybl.metrix.integration.remedials.Remedial;

import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

import static com.powsybl.metrix.integration.analysis.AnalysisLogger.defaultLogger;

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
public class ConsistencyChecker {
    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.MetrixAnalysis");
    private static final String REMEDIALS_SECTION = RESOURCE_BUNDLE.getString("remedialsSection");
    private static final String CONTINGENCIES_SECTION = RESOURCE_BUNDLE.getString("contingenciesSection");
    private static final String SECTION_SEPARATOR = " - ";
    private static final String INVALID_REMEDIAL_MESSAGE_KEY = "invalidRemedial";

    private final Network network;
    private final MetrixDslData metrixDslData;
    private final AnalysisLogger analysisLogger;

    public ConsistencyChecker(Network network,
                              MetrixDslData metrixDslData,
                              AnalysisLogger analysisLogger) {
        this.network = network;
        this.metrixDslData = metrixDslData;
        this.analysisLogger = analysisLogger != null ? analysisLogger : defaultLogger();
    }

    /**
     * Check the consistency of remedials and contingencies
     * <ul>
     *     <li>check that all actions and constraints are valid for each remedial</li>
     *     <li>check that all contingencies used in the metrix configuration are valid</li>
     * </ul>
     * @param remedials     list of remedials
     * @param contingencies list of contingencies
     */
    public void run(List<Remedial> remedials, List<Contingency> contingencies) {
        Set<String> contingencyIds = contingencies.stream().map(Contingency::getId).collect(Collectors.toUnmodifiableSet());

        // Check remedials
        remedials.forEach(remedial -> checkRemedial(remedial, contingencyIds));

        // Check contingencies used by metrixDslData
        if (metrixDslData != null) {
            checkMetrixDslContingencies(contingencyIds, IdentifiableType.HVDC_LINE);
            checkMetrixDslContingencies(contingencyIds, IdentifiableType.GENERATOR);
            checkMetrixDslContingencies(contingencyIds, IdentifiableType.LOAD);
            checkMetrixDslContingencies(contingencyIds, IdentifiableType.TWO_WINDINGS_TRANSFORMER);
        }
    }

    /**
     * Check remedial consistency
     * @param remedial       remedial to check
     * @param contingencyIds list of contingency ids
     */
    private void checkRemedial(Remedial remedial, Set<String> contingencyIds) {
        validateRemedial(remedial);
        if (!remedial.contingency().isEmpty() && !contingencyIds.contains(remedial.contingency())) {
            analysisLogger.warnWithReason(REMEDIALS_SECTION, new Reason("invalidMetrixRemedialContingency", remedial.contingency()),
                INVALID_REMEDIAL_MESSAGE_KEY, remedial.lineFile());
        }
    }

    private void validateRemedial(Remedial remedial) {
        for (String action : remedial.branchToOpen()) {
            validateAction(remedial.lineFile(), action);
        }
        for (String action : remedial.branchToClose()) {
            validateAction(remedial.lineFile(), action);
        }
        for (String constraint : remedial.constraint()) {
            validateConstraint(remedial.lineFile(), constraint);
        }
    }

    /**
     * check a constraint of a remedial line: constraint is equipment of the network, is of Branch type
     * and monitored on contingencies (branchRatingOnContingencies)
     *
     * @param line number
     * @param constraint to check
     */
    private void validateConstraint(int line, String constraint) {
        boolean valid = validateIdentifiable(line, constraint,
            List.of(Branch.class),
            "invalidRemedialConstraintType");
        if (!valid || metrixDslData == null) {
            return;
        }
        if (!metrixDslData.getBranchMonitoringListNk().containsKey(constraint)) {
            analysisLogger.warnWithReason(REMEDIALS_SECTION, new Reason("invalidMetrixRemedialConstraint", constraint),
                INVALID_REMEDIAL_MESSAGE_KEY, line);
        }
    }

    /**
     * check an action of a remedial line: action is equipment of the network and is of Branch or Switch type
     *
     * @param line line number
     * @param action to check
     */
    private void validateAction(int line, String action) {
        validateIdentifiable(line, action,
            List.of(Branch.class, Switch.class),
            "invalidRemedialActionType");
    }

    private boolean validateIdentifiable(int line, String identifier,
                                         List<Class<?>> expectedTypes,
                                         String invalidTypeMessageKey) {
        if (identifier == null || identifier.isEmpty()) {
            return false;
        }

        Identifiable<?> identifiable = network.getIdentifiable(identifier);
        if (identifiable == null) {
            analysisLogger.warnWithReason(REMEDIALS_SECTION, new Reason("invalidRemedialNetwork", identifier),
                INVALID_REMEDIAL_MESSAGE_KEY, line);
            return false;
        }

        boolean isTypeMatched = expectedTypes.stream().anyMatch(type -> type.isInstance(identifiable));
        if (!isTypeMatched) {
            analysisLogger.warnWithReason(REMEDIALS_SECTION, new Reason(invalidTypeMessageKey, identifiable.getId()),
                INVALID_REMEDIAL_MESSAGE_KEY, line);
        }
        return isTypeMatched;
    }

    /**
     * check contingencies used in Metrix configuration
     */
    private void checkMetrixDslContingencies(Set<String> contingencyIds, IdentifiableType identifiableType) {
        Map<String, List<String>> contingenciesMap = getContingenciesMap(identifiableType);
        if (contingenciesMap == null) {
            return;
        }
        for (Map.Entry<String, List<String>> entry : contingenciesMap.entrySet()) {
            validateContingencyList(entry, contingencyIds, identifiableType);
        }
    }

    private void validateContingencyList(Map.Entry<String, List<String>> entry,
                                         Set<String> contingencyIds,
                                         IdentifiableType identifiableType) {
        for (String ctyId : entry.getValue()) {
            if (!contingencyIds.contains(ctyId)) {
                analysisLogger.warn(CONTINGENCIES_SECTION + SECTION_SEPARATOR + entry.getKey(), "invalidMetrixDslDataContingency", identifiableType.name(), ctyId);
            }
        }
    }

    private Map<String, List<String>> getContingenciesMap(IdentifiableType identifiableType) {
        Map<String, List<String>> contingenciesMap = null;
        switch (identifiableType) {
            case HVDC_LINE -> contingenciesMap = metrixDslData.getHvdcContingenciesMap();
            case GENERATOR -> contingenciesMap = metrixDslData.getGeneratorContingenciesMap();
            case LOAD -> contingenciesMap = metrixDslData.getLoadContingenciesMap();
            case TWO_WINDINGS_TRANSFORMER -> contingenciesMap = metrixDslData.getPtcContingenciesMap();
        }
        return contingenciesMap;
    }
}
