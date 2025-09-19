/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration;

import com.powsybl.metrix.mapping.LogDslLoader;
import com.powsybl.metrix.mapping.references.MappingKey;
import com.powsybl.metrix.commons.MappingVariable;
import com.powsybl.metrix.mapping.config.TimeSeriesMappingConfig;

import java.util.Objects;
import java.util.Set;

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
public class TimeSeriesMetrixConfigChecker {

    static final String BRANCH_RATINGS_BASE_CASE = "branchRatingsBaseCase";
    static final String BRANCH_ANALYSIS_RATINGS_BASE_CASE = "branchAnalysisRatingsBaseCase";
    static final String BRANCH_RATINGS_ON_CONTINGENCY = "branchRatingsOnContingency";
    static final String BRANCH_RATINGS_BEFORE_CURATIVE = "branchRatingsBeforeCurative";
    static final String BRANCH_ANALYSIS_RATINGS_ON_CONTINGENCY = "branchAnalysisRatingsOnContingency";
    static final String BRANCH_RATINGS_ON_SPECIFIC_CONTINGENCY = "branchRatingsOnSpecificContingency";
    static final String BRANCH_RATINGS_BEFORE_CURATIVE_ON_SPECIFIC_CONTINGENCY = "branchRatingsBeforeCurativeOnSpecificContingency";
    static final String END_OR = "EndOr";

    protected TimeSeriesMappingConfig config;
    protected LogDslLoader logDslLoader;

    public TimeSeriesMetrixConfigChecker(TimeSeriesMappingConfig config, LogDslLoader logDslLoader) {
        this.config = Objects.requireNonNull(config);
        this.logDslLoader = logDslLoader;
    }

    public boolean isEquipmentThresholdDefined(MappingVariable variable, String id) {
        return config.getEquipmentToTimeSeries().containsKey(new MappingKey(variable, id));
    }

    public void checkBranchThreshold() {
        Set<String> equipmentIds = config.getEquipmentIds();
        equipmentIds.forEach(id -> {
            checkBranchThreshold(BRANCH_RATINGS_BASE_CASE, MetrixVariable.THRESHOLD_N, MetrixVariable.THRESHOLD_N_END_OR, id);
            checkBranchThreshold(BRANCH_ANALYSIS_RATINGS_BASE_CASE, MetrixVariable.ANALYSIS_THRESHOLD_N, MetrixVariable.ANALYSIS_THRESHOLD_N_END_OR, id);
        });
        equipmentIds.forEach(id -> {
            checkBranchThreshold(BRANCH_RATINGS_ON_CONTINGENCY, MetrixVariable.THRESHOLD_N1, MetrixVariable.THRESHOLD_N1_END_OR, id);
            checkBranchThreshold(BRANCH_RATINGS_BEFORE_CURATIVE, MetrixVariable.THRESHOLD_ITAM, MetrixVariable.THRESHOLD_ITAM_END_OR, id);
            checkBranchThreshold(BRANCH_ANALYSIS_RATINGS_ON_CONTINGENCY, MetrixVariable.ANALYSIS_THRESHOLD_NK, MetrixVariable.ANALYSIS_THRESHOLD_NK_END_OR, id);
            checkBranchThreshold(BRANCH_RATINGS_ON_SPECIFIC_CONTINGENCY, MetrixVariable.THRESHOLD_NK, MetrixVariable.THRESHOLD_NK_END_OR, id);
            checkBranchThreshold(BRANCH_RATINGS_BEFORE_CURATIVE_ON_SPECIFIC_CONTINGENCY, MetrixVariable.THRESHOLD_ITAM_NK, MetrixVariable.THRESHOLD_ITAM_NK_END_OR, id);
        });
    }

    private void checkBranchThreshold(String thresholdName, MappingVariable variable, MappingVariable variableEndOr, String id) {
        if (isEquipmentThresholdDefined(variableEndOr, id) && !isEquipmentThresholdDefined(variable, id)) {
            config.removeEquipmentTimeSeries(variableEndOr, id);
            if (logDslLoader != null) {
                logDslLoader.logError("%s defined for id %s but %s is not -> %s is removed", thresholdName + END_OR, id, thresholdName, thresholdName + END_OR);
            }
        }
    }
}
