/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.integration

class ParametersData {

    MetrixComputationType computationType
    Boolean withGridCost
    Boolean preCurativeResults
    Boolean outagesBreakingConnexity
    Boolean remedialActionsBreakingConnexity
    Boolean analogousRemedialActionDetection
    Boolean propagateBranchTripping
    Boolean withAdequacyResults
    Boolean withRedispatchingResults
    Boolean marginalVariationsOnBranches
    Boolean marginalVariationsOnHvdc
    Boolean lossDetailPerCountry
    Boolean overloadResultsOnly
    Boolean showAllTDandHVDCresults
    Boolean withLostLoadDetailedResultsOnContingency
    Float lossFactor
    Integer lossNbRelaunch
    Integer lossThreshold
    Integer curativeRedispatchingLimit
    Float pstCostPenality
    Float hvdcCostPenality
    Float lossOfLoadCost
    Float curativeLossOfLoadCost
    Float curativeLossOfGenerationCost
    Float contingenciesProbability
    Integer nominalU
    Integer nbMaxIteration
    Integer nbMaxCurativeAction
    Integer gapVariableCost
    Integer nbThreatResults
    Integer maxSolverTime
    Integer redispatchingCostOffset
    Integer adequacyCostOffset
    Integer nbMaxLostLoadDetailedResults

    void computationType(MetrixComputationType computationType) {
        this.computationType = computationType
    }

    void withGridCost(Boolean withGridCost) {
        this.withGridCost = withGridCost
    }

    void preCurativeResults(Boolean preCurativeResults) {
        this.preCurativeResults = preCurativeResults
    }

    void outagesBreakingConnexity(Boolean outagesBreakingConnexity) {
        this.outagesBreakingConnexity = outagesBreakingConnexity
    }

    void remedialActionsBreakingConnexity(Boolean remedialActionsBreakingConnexity) {
        this.remedialActionsBreakingConnexity = remedialActionsBreakingConnexity
    }

    void analogousRemedialActionDetection(Boolean analogousRemedialActionDetection) {
        this.analogousRemedialActionDetection = analogousRemedialActionDetection
    }

    void propagateBranchTripping(Boolean propagateBranchTripping) {
        this.propagateBranchTripping = propagateBranchTripping
    }

    void withAdequacyResults(Boolean withAdequacyResults) {
        this.withAdequacyResults = withAdequacyResults
    }

    void withRedispatchingResults(Boolean withRedispatchingResults) {
        this.withRedispatchingResults = withRedispatchingResults
    }

    void marginalVariationsOnBranches(Boolean marginalVariationsOnBranches) {
        this.marginalVariationsOnBranches = marginalVariationsOnBranches
    }

    void marginalVariationsOnHvdc(Boolean marginalVariationsOnHvdc) {
        this.marginalVariationsOnHvdc = marginalVariationsOnHvdc
    }

    void lossDetailPerCountry(Boolean lossDetailPerCountry) {
        this.lossDetailPerCountry = lossDetailPerCountry
    }

    void overloadResultsOnly(Boolean overloadResultsOnly) {
        this.overloadResultsOnly = overloadResultsOnly
    }

    void showAllTDandHVDCresults(Boolean showAllTDandHVDCresults) {
        this.showAllTDandHVDCresults = showAllTDandHVDCresults
    }

    void withLostLoadDetailedResultsOnContingency(Boolean withLostLoadDetailedResultsOnContingency) {
        this.withLostLoadDetailedResultsOnContingency = withLostLoadDetailedResultsOnContingency
    }

    void lossFactor(Float lossFactor) {
        this.lossFactor = lossFactor
    }

    void lossNbRelaunch(Integer lossNbRelaunch) {
        this.lossNbRelaunch = lossNbRelaunch
    }

    void lossThreshold(Integer lossThreshold) {
        this.lossThreshold = lossThreshold
    }

    void curativeRedispatchingLimit(Integer curativeRedispatchingLimit) {
        this.curativeRedispatchingLimit = curativeRedispatchingLimit
    }

    void pstCostPenality(Float pstCostPenality) {
        this.pstCostPenality = pstCostPenality
    }

    void hvdcCostPenality(Float hvdcCostPenality) {
        this.hvdcCostPenality = hvdcCostPenality
    }

    void lossOfLoadCost(Float lossOfLoadCost) {
        this.lossOfLoadCost = lossOfLoadCost
    }

    void curativeLossOfLoadCost(Float lossOfLoadCost) {
        this.curativeLossOfLoadCost = lossOfLoadCost
    }

    void curativeLossOfGenerationCost(Float lossOfGenerationCost) {
        this.curativeLossOfGenerationCost = lossOfGenerationCost
    }

    void contingenciesProbability(Float contingenciesProbability) {
        this.contingenciesProbability = contingenciesProbability
    }

    void nominalU(Integer nominalU) {
        this.nominalU = nominalU
    }

    void nbMaxIteration(Integer nbMaxIteration) {
        this.nbMaxIteration = nbMaxIteration
    }

    void nbMaxCurativeAction(Integer nbMaxCurativeAction) {
        this.nbMaxCurativeAction = nbMaxCurativeAction
    }

    void nbMaxLostLoadDetailedResults(Integer nbMaxLostLoadDetailedResults) {
        this.nbMaxLostLoadDetailedResults = nbMaxLostLoadDetailedResults
    }

    void gapVariableCost(Integer gapVariableCost) {
        this.gapVariableCost = gapVariableCost
    }

    void maxSolverTime(Integer maxSolverTime) {
        this.maxSolverTime = maxSolverTime
    }

    void nbThreatResults(Integer nbThreatResults) {
        this.nbThreatResults = nbThreatResults
    }

    void redispatchingCostOffset(Integer redispatchingCostOffset) {
        this.redispatchingCostOffset = redispatchingCostOffset
    }

    void adequacyCostOffset(Integer adequacyCostOffset) {
        this.adequacyCostOffset = adequacyCostOffset
    }

    protected static parametersData(Closure closure, MetrixParameters parameters) {
        def cloned = closure.clone()
        ParametersData spec = new ParametersData()
        cloned.delegate = spec
        cloned()
        if (spec.computationType) {
            parameters.setComputationType(spec.computationType)
        }
        if (spec.withGridCost != null) {
            parameters.setWithGridCost(spec.withGridCost)
        }
        if (spec.preCurativeResults != null) {
            parameters.setPreCurativeResults(spec.preCurativeResults)
        }
        if (spec.outagesBreakingConnexity != null) {
            parameters.setOutagesBreakingConnexity(spec.outagesBreakingConnexity)
        }
        if (spec.remedialActionsBreakingConnexity != null) {
            parameters.setRemedialActionsBreakingConnexity(spec.remedialActionsBreakingConnexity)
        }
        if (spec.analogousRemedialActionDetection != null) {
            parameters.setAnalogousRemedialActionDetection(spec.analogousRemedialActionDetection)
        }
        if (spec.propagateBranchTripping != null) {
            parameters.setPropagateBranchTripping(spec.propagateBranchTripping)
        }
        if (spec.withAdequacyResults != null) {
            parameters.setWithAdequacyResults(spec.withAdequacyResults)
        }
        if (spec.withRedispatchingResults != null) {
            parameters.setWithRedispatchingResults(spec.withRedispatchingResults)
        }
        if (spec.marginalVariationsOnBranches != null) {
            parameters.setMarginalVariationsOnBranches(spec.marginalVariationsOnBranches)
        }
        if (spec.marginalVariationsOnHvdc != null) {
            parameters.setMarginalVariationsOnHvdc(spec.marginalVariationsOnHvdc)
        }
        if (spec.lossDetailPerCountry != null) {
            parameters.setLossDetailPerCountry(spec.lossDetailPerCountry)
        }
        if (spec.overloadResultsOnly != null) {
            parameters.setOverloadResultsOnly(spec.overloadResultsOnly)
        }
        if (spec.showAllTDandHVDCresults != null) {
            parameters.setShowAllTDandHVDCresults(spec.showAllTDandHVDCresults)
        }
        if (spec.withLostLoadDetailedResultsOnContingency != null) {
            parameters.setWithLostLoadDetailedResultsOnContingency(spec.withLostLoadDetailedResultsOnContingency)
        }
        if (spec.maxSolverTime) {
            parameters.setMaxSolverTime(spec.maxSolverTime)
        }
        if (spec.redispatchingCostOffset) {
            parameters.setRedispatchingCostOffset(spec.redispatchingCostOffset)
        }
        if (spec.adequacyCostOffset) {
            parameters.setAdequacyCostOffset(spec.adequacyCostOffset)
        }
        if (spec.lossFactor) {
            parameters.setLossFactor(spec.lossFactor)
        }
        if (spec.lossNbRelaunch) {
            parameters.setLossNbRelaunch(spec.lossNbRelaunch)
        }
        if (spec.lossThreshold) {
            parameters.setLossThreshold(spec.lossThreshold)
        }
        if (spec.curativeRedispatchingLimit != null) {
            parameters.setCurativeRedispatchingLimit(spec.curativeRedispatchingLimit)
        }
        if (spec.pstCostPenality) {
            parameters.setPstCostPenality(spec.pstCostPenality)
        }
        if (spec.hvdcCostPenality) {
            parameters.setHvdcCostPenality(spec.hvdcCostPenality)
        }
        if (spec.lossOfLoadCost != null) {
            parameters.setLossOfLoadCost(spec.lossOfLoadCost)
        }
        if (spec.curativeLossOfLoadCost != null) {
            parameters.setCurativeLossOfLoadCost(spec.curativeLossOfLoadCost)
        }
        if (spec.curativeLossOfGenerationCost != null) {
            parameters.setCurativeLossOfGenerationCost(spec.curativeLossOfGenerationCost)
        }
        if (spec.contingenciesProbability != null) {
            parameters.setContingenciesProbability(spec.contingenciesProbability)
        }
        if (spec.nominalU) {
            parameters.setNominalU(spec.nominalU)
        }
        if (spec.nbMaxIteration != null) {
            parameters.setNbMaxIteration(spec.nbMaxIteration)
        }
        if (spec.nbMaxCurativeAction != null) {
            parameters.setNbMaxCurativeAction(spec.nbMaxCurativeAction)
        }
        if (spec.nbMaxLostLoadDetailedResults != null) {
            parameters.setNbMaxLostLoadDetailedResults(spec.nbMaxLostLoadDetailedResults)
        }
        if (spec.gapVariableCost != null) {
            parameters.setGapVariableCost(spec.gapVariableCost)
        }
        if (spec.nbThreatResults != null) {
            parameters.setNbThreatResults(spec.nbThreatResults)
        }
    }
}
