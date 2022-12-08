/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.integration;

import com.powsybl.metrix.mapping.LogDslLoader;
import com.powsybl.metrix.mapping.MappingKey;
import com.powsybl.metrix.mapping.MappingVariable;
import com.powsybl.metrix.mapping.TimeSeriesMappingConfig;

import java.util.*;

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
            checkBranchThreshold(BRANCH_RATINGS_BASE_CASE, MetrixVariable.thresholdN, MetrixVariable.thresholdNEndOr, id);
            checkBranchThreshold(BRANCH_ANALYSIS_RATINGS_BASE_CASE, MetrixVariable.analysisThresholdN, MetrixVariable.analysisThresholdNEndOr, id);
        });
        equipmentIds.forEach(id -> {
            checkBranchThreshold(BRANCH_RATINGS_ON_CONTINGENCY, MetrixVariable.thresholdN1, MetrixVariable.thresholdN1EndOr, id);
            checkBranchThreshold(BRANCH_RATINGS_BEFORE_CURATIVE, MetrixVariable.thresholdITAM, MetrixVariable.thresholdITAMEndOr, id);
            checkBranchThreshold(BRANCH_ANALYSIS_RATINGS_ON_CONTINGENCY, MetrixVariable.analysisThresholdNk, MetrixVariable.analysisThresholdNkEndOr, id);
            checkBranchThreshold(BRANCH_RATINGS_ON_SPECIFIC_CONTINGENCY, MetrixVariable.thresholdNk, MetrixVariable.thresholdNkEndOr, id);
            checkBranchThreshold(BRANCH_RATINGS_BEFORE_CURATIVE_ON_SPECIFIC_CONTINGENCY, MetrixVariable.thresholdITAMNk, MetrixVariable.thresholdITAMNkEndOr, id);
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

