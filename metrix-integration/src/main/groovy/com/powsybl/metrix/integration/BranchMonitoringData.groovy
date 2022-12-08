/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.integration

import com.powsybl.iidm.network.Identifiable
import com.powsybl.iidm.network.Network
import com.powsybl.metrix.mapping.LogDslLoader
import com.powsybl.metrix.mapping.TimeSeriesMappingConfigLoader

class BranchMonitoringData {

    Boolean baseCaseFlowResults
    Boolean maxThreatFlowResults
    Object branchRatingsBaseCase
    Object branchRatingsOnContingency
    Object branchAnalysisRatingsBaseCase
    Object branchAnalysisRatingsOnContingency
    Object branchRatingsOnSpecificContingency
    Object branchRatingsBeforeCurative
    Object branchRatingsBeforeCurativeOnSpecificContingency
    Object branchRatingsBaseCaseEndOr
    Object branchRatingsOnContingencyEndOr
    Object branchAnalysisRatingsBaseCaseEndOr
    Object branchAnalysisRatingsOnContingencyEndOr
    Object branchRatingsOnSpecificContingencyEndOr
    Object branchRatingsBeforeCurativeEndOr
    Object branchRatingsBeforeCurativeOnSpecificContingencyEndOr

    List<String> contingencyFlowResults
    List<String> contingencyDetailedMarginalVariations

    void baseCaseFlowResults(boolean b) {
        this.baseCaseFlowResults = b
    }

    void maxThreatFlowResults(boolean b) {
        this.maxThreatFlowResults = b
    }

    void baseCaseFlowResults() {
        baseCaseFlowResults(true)
    }

    void maxThreatFlowResults() {
        maxThreatFlowResults(true)
    }

    void branchRatingsBaseCase(Object timeSeries) {
        this.branchRatingsBaseCase = timeSeries
    }

    void branchRatingsOnContingency(Object timeSeries) {
        this.branchRatingsOnContingency = timeSeries
    }

    void branchAnalysisRatingsBaseCase(Object timeSeries) {
        this.branchAnalysisRatingsBaseCase = timeSeries
    }

    void branchAnalysisRatingsOnContingency(Object timeSeries) {
        this.branchAnalysisRatingsOnContingency = timeSeries
    }

    void branchRatingsOnSpecificContingency(Object timeSeries) {
        this.branchRatingsOnSpecificContingency = timeSeries
    }

    void branchRatingsBeforeCurative(Object timeSeries) {
        this.branchRatingsBeforeCurative = timeSeries
    }

    void branchRatingsBeforeCurativeOnSpecificContingency(Object timeSeries) {
        this.branchRatingsBeforeCurativeOnSpecificContingency = timeSeries
    }

    void branchRatingsBaseCaseEndOr(Object timeSeries) {
        this.branchRatingsBaseCaseEndOr = timeSeries
    }

    void branchRatingsOnContingencyEndOr(Object timeSeries) {
        this.branchRatingsOnContingencyEndOr = timeSeries
    }

    void branchAnalysisRatingsBaseCaseEndOr(Object timeSeries) {
        this.branchAnalysisRatingsBaseCaseEndOr = timeSeries
    }

    void branchAnalysisRatingsOnContingencyEndOr(Object timeSeries) {
        this.branchAnalysisRatingsOnContingencyEndOr = timeSeries
    }

    void branchRatingsOnSpecificContingencyEndOr(Object timeSeries) {
        this.branchRatingsOnSpecificContingencyEndOr = timeSeries
    }

    void branchRatingsBeforeCurativeEndOr(Object timeSeries) {
        this.branchRatingsBeforeCurativeEndOr = timeSeries
    }

    void branchRatingsBeforeCurativeOnSpecificContingencyEndOr(Object timeSeries) {
        this.branchRatingsBeforeCurativeOnSpecificContingencyEndOr = timeSeries
    }

    void contingencyFlowResults(String[] contingencies) {
        this.contingencyFlowResults = contingencies
    }

    void contingencyFlowResults(List<String> contingencies) {
        this.contingencyFlowResults = contingencies
    }

    void contingencyDetailedMarginalVariations(String[] contingencies) {
        this.contingencyDetailedMarginalVariations = contingencies
    }

    void contingencyDetailedMarginalVariations(List<String> contingencies) {
        this.contingencyDetailedMarginalVariations = contingencies
    }

    protected static branchData(Closure closure, String id, Network network, TimeSeriesMappingConfigLoader configLoader, MetrixDslData data, LogDslLoader logDslLoader) {
        Identifiable identifiable = network.getBranch(id)
        if (identifiable == null) {
            logDslLoader.logWarn("Branch %s not found in the network", id)
            return
        }

        def branchSpec = branchMonitoringData(closure)

        // Base case monitoring
        if (branchSpec.branchRatingsBaseCase) {
            configLoader.addEquipmentTimeSeries(branchSpec.branchRatingsBaseCase, MetrixVariable.thresholdN, id)
            data.addBranchMonitoringN(id)
        } else if (branchSpec.branchAnalysisRatingsBaseCase) {
            // analysis threshold
            configLoader.addEquipmentTimeSeries(branchSpec.branchAnalysisRatingsBaseCase, MetrixVariable.analysisThresholdN, id)
            data.addBranchResultN(id)
        } else if (branchSpec.baseCaseFlowResults) {
            data.addBranchResultN(id)
        }
        if (branchSpec.branchRatingsBaseCaseEndOr) {
            configLoader.addEquipmentTimeSeries(branchSpec.branchRatingsBaseCaseEndOr, MetrixVariable.thresholdNEndOr, id)
        }
        if (branchSpec.branchAnalysisRatingsBaseCaseEndOr) {
            configLoader.addEquipmentTimeSeries(branchSpec.branchAnalysisRatingsBaseCaseEndOr, MetrixVariable.analysisThresholdNEndOr, id)
        }

        // N-1 monitoring
        if (branchSpec.branchRatingsOnContingency) {
            configLoader.addEquipmentTimeSeries(branchSpec.branchRatingsOnContingency, MetrixVariable.thresholdN1, id)
            data.addBranchMonitoringNk(id)
            // Before curative monitoring
            if (branchSpec.branchRatingsBeforeCurative) {
                configLoader.addEquipmentTimeSeries(branchSpec.branchRatingsBeforeCurative, MetrixVariable.thresholdITAM, id)
            }
        } else if (branchSpec.branchAnalysisRatingsOnContingency) {
            // analysis threshold (n-k)
            configLoader.addEquipmentTimeSeries(branchSpec.branchAnalysisRatingsOnContingency, MetrixVariable.analysisThresholdNk, id)
            data.addBranchResultNk(id)
        } else if (branchSpec.maxThreatFlowResults) {
            data.addBranchResultNk(id)
        }
        if (branchSpec.branchRatingsOnContingencyEndOr) {
            configLoader.addEquipmentTimeSeries(branchSpec.branchRatingsOnContingencyEndOr, MetrixVariable.thresholdN1EndOr, id)
        }
        if (branchSpec.branchRatingsBeforeCurativeEndOr) {
            configLoader.addEquipmentTimeSeries(branchSpec.branchRatingsBeforeCurativeEndOr, MetrixVariable.thresholdITAMEndOr, id)
        }
        if (branchSpec.branchAnalysisRatingsOnContingencyEndOr) {
            configLoader.addEquipmentTimeSeries(branchSpec.branchAnalysisRatingsOnContingencyEndOr, MetrixVariable.analysisThresholdNkEndOr, id)
        }

        // Specific N-k contingencies monitoring
        if (branchSpec.branchRatingsOnSpecificContingency) {
            configLoader.addEquipmentTimeSeries(branchSpec.branchRatingsOnSpecificContingency, MetrixVariable.thresholdNk, id)
            data.addBranchMonitoringNk(id)

            // Specific before curative monitoring
            if (branchSpec.branchRatingsBeforeCurativeOnSpecificContingency) {
                configLoader.addEquipmentTimeSeries(branchSpec.branchRatingsBeforeCurativeOnSpecificContingency, MetrixVariable.thresholdITAMNk, id)
            }
        }
        if (branchSpec.branchRatingsOnSpecificContingencyEndOr) {
            configLoader.addEquipmentTimeSeries(branchSpec.branchRatingsOnSpecificContingencyEndOr, MetrixVariable.thresholdNkEndOr, id)
        }
        if (branchSpec.branchRatingsBeforeCurativeOnSpecificContingencyEndOr) {
            configLoader.addEquipmentTimeSeries(branchSpec.branchRatingsBeforeCurativeOnSpecificContingencyEndOr, MetrixVariable.thresholdITAMNkEndOr, id)
        }


        if (branchSpec.contingencyFlowResults != null && branchSpec.contingencyFlowResults.size() > 0) {
            data.addContingencyFlowResults(id, branchSpec.contingencyFlowResults)
        }

        if (branchSpec.contingencyDetailedMarginalVariations != null && branchSpec.contingencyDetailedMarginalVariations.size() > 0) {
            data.addContingencyDetailedMarginalVariations(id, branchSpec.contingencyDetailedMarginalVariations)
        }
    }

    protected static BranchMonitoringData branchMonitoringData(Closure closure) {
        def cloned = closure.clone()
        BranchMonitoringData spec = new BranchMonitoringData()
        cloned.delegate = spec
        cloned()
        spec
    }
}
