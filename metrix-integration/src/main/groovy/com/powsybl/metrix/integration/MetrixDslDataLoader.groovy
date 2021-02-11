/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

/**
 * Created by marifunf on 28/04/17.
 */
package com.powsybl.metrix.integration


import com.powsybl.dsl.DslLoader
import com.powsybl.iidm.network.*
import com.powsybl.metrix.integration.exceptions.MetrixException
import com.powsybl.metrix.mapping.Filter
import com.powsybl.metrix.mapping.FilteringContext
import com.powsybl.metrix.mapping.MappingVariable
import com.powsybl.metrix.mapping.TimeSeriesMappingConfig
import com.powsybl.timeseries.CalculatedTimeSeriesDslLoader
import com.powsybl.timeseries.ReadOnlyTimeSeriesStore
import com.powsybl.timeseries.ast.CalculatedTimeSeriesDslAstTransformation
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer
import org.codehaus.groovy.control.customizers.ImportCustomizer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

class MetrixDslDataLoader extends DslLoader {

    static Logger LOGGER = LoggerFactory.getLogger(MetrixDslDataLoader.class)

    static final String DEBUG = "DEBUG - "
    static final String WARNING = "WARNING - "
    static final String ERROR = "ERROR - "

    static class ParametersSpec {

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

    }

    static class PhaseShifterSpec {

        List<String> onContingencies
        MetrixPtcControlType controlType
        Integer preventiveUpperTapRange
        Integer preventiveLowerTapRange

        void onContingencies(String[] onContingencies) {
            this.onContingencies = onContingencies
        }

        void onContingencies(List<String> onContingencies) {
            this.onContingencies = onContingencies
        }
        void controlType(MetrixPtcControlType controlType) {
            this.controlType = controlType
        }
        void preventiveUpperTapRange(Integer preventiveUpperTapRange) {
            this.preventiveUpperTapRange = preventiveUpperTapRange
        }
        void preventiveLowerTapRange(Integer preventiveLowerTapRange) {
            this.preventiveLowerTapRange = preventiveLowerTapRange
        }
    }

    static class HvdcSpec {

        List<String> onContingencies
        MetrixHvdcControlType controlType

        void onContingencies(String[] onContingencies) {
            this.onContingencies = onContingencies
        }

        void onContingencies(List<String> onContingencies) {
            this.onContingencies = onContingencies
        }

        void controlType(MetrixHvdcControlType controlType) {
            this.controlType = controlType
        }
    }

    static class SectionMonitoring {

        Map<String, Float> branchList = new HashMap<>()
        float maxFlowN

        void branch(String branch, Float coef) {
            this.branchList.put(branch, coef)
        }

        void maxFlowN(float maxFlowN) {
            this.maxFlowN = maxFlowN
        }
    }

    static class BranchMonitoringSpec {

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
    }

    static class GeneratorSpec {

        List<String> onContingencies
        Object adequacyUpCosts
        Object adequacyDownCosts
        Object redispatchingUpCosts
        Object redispatchingDownCosts

        void adequacyUpCosts(Object timeSeriesNames) {
            this.adequacyUpCosts = timeSeriesNames
        }

        void adequacyDownCosts(Object timeSeriesNames) {
            this.adequacyDownCosts = timeSeriesNames
        }

        void redispatchingUpCosts(Object timeSeriesNames) {
            this.redispatchingUpCosts = timeSeriesNames
        }

        void redispatchingDownCosts(Object timeSeriesNames) {
            this.redispatchingDownCosts = timeSeriesNames
        }

        void onContingencies(String[] contingencies) {
            this.onContingencies = contingencies
        }

        void onContingencies(List<String> onContingencies) {
            this.onContingencies = onContingencies
        }

    }

    static class LoadSpec {

        List<String> onContingencies
        Integer preventiveSheddingPercentage
        Float preventiveSheddingCost
        Integer curativeSheddingPercentage
        Object curativeSheddingCost

        void preventiveSheddingPercentage(int percent) {
            this.preventiveSheddingPercentage = percent
        }

        void preventiveSheddingCost(float loadSheddingCost) {
            this.preventiveSheddingCost = loadSheddingCost
        }

        void curativeSheddingPercentage(int percent) {
            this.curativeSheddingPercentage = percent
        }

        void curativeSheddingCost(Object timeSeriesName) {
            this.curativeSheddingCost = timeSeriesName
        }

        void onContingencies(String[] onContingencies) {
            this.onContingencies = onContingencies
        }

        void onContingencies(List<String> onContingencies) {
            this.onContingencies = onContingencies
        }
    }

    static class LoadsBindingSpec {

        Closure<Boolean> filter

        void filter(Closure<Boolean> filter) {
            this.filter = filter
        }

    }

    static class GeneratorsBindingSpec extends LoadsBindingSpec {

        MetrixGeneratorsBinding.ReferenceVariable referenceVariable

        void referenceVariable(MetrixGeneratorsBinding.ReferenceVariable referenceVariable) {
            this.referenceVariable = referenceVariable
        }

    }

    static class ContingenciesSpec {

        List<String> specificContingencies

        void specificContingencies(String[] contingencies) {
            this.specificContingencies = contingencies
        }

        void specificContingencies(List<String> contingencies) {
            this.specificContingencies = contingencies
        }

    }

    private static logOut(Writer out, String message) {
        if (out != null) {
            out.write(message + "\n")
        }
    }

    private static logDebug(Writer out, String message) {
        LOGGER.debug(message)
        //logOut(out, DEBUG + message)
    }

    private static logWarn(Writer out, String message) {
        LOGGER.warn(message)
        logOut(out, WARNING + message)
    }

    private static logError(Writer out, String message) {
        LOGGER.error(message)
        logOut(out, ERROR + message)
    }

    private static logDebug(Writer out, String pattern, Object... arguments) {
        String formattedString = String.format(pattern, arguments);
        LOGGER.debug(formattedString);
        //logOut(out, DEBUG + formattedString)
    }

    private static logWarn(Writer out, String pattern, Object... arguments) {
        String formattedString = String.format(pattern, arguments);
        LOGGER.warn(formattedString)
        logOut(out, WARNING + formattedString)
    }

    private static logError(Writer out, String pattern, Object... arguments) {
        String formattedString = String.format(pattern, arguments);
        LOGGER.error(formattedString)
        logOut(out, ERROR + formattedString)
    }

    private static branchData(Binding binding, Closure closure, String id, Network network, TimeSeriesMappingConfig tsConfig, MetrixDslData data, Writer out) {
        Identifiable identifiable = network.getBranch(id)
        if (identifiable == null) {
            logWarn(out, "Branch %s not found in the network", id)
            return
        }

        def branchSpec = branchMonitoringData(closure)

        // Basecase monitoring
        if (branchSpec.branchRatingsBaseCase) {
            addEquipmentTimeSeries(branchSpec.branchRatingsBaseCase, MetrixVariable.thresholdN, id, tsConfig, binding)
            data.addBranchMonitoringN(id)
            if (branchSpec.branchRatingsBaseCaseEndOr) {
                addEquipmentTimeSeries(branchSpec.branchRatingsBaseCaseEndOr, MetrixVariable.thresholdNEndOr, id, tsConfig, binding)
            }
        } else if (branchSpec.branchAnalysisRatingsBaseCase) {
            // analysis threshold
            addEquipmentTimeSeries(branchSpec.branchAnalysisRatingsBaseCase, MetrixVariable.analysisThresholdN, id, tsConfig, binding)
            data.addBranchResultN(id)
            if (branchSpec.branchAnalysisRatingsBaseCaseEndOr) {
                addEquipmentTimeSeries(branchSpec.branchAnalysisRatingsBaseCaseEndOr, MetrixVariable.analysisThresholdNEndOr, id, tsConfig, binding)
            }
        } else if (branchSpec.baseCaseFlowResults) {
            data.addBranchResultN(id)
        }


        // N-1 monitoring
        if (branchSpec.branchRatingsOnContingency) {
            addEquipmentTimeSeries(branchSpec.branchRatingsOnContingency, MetrixVariable.thresholdN1, id, tsConfig, binding)
            data.addBranchMonitoringNk(id)
            if (branchSpec.branchRatingsOnContingencyEndOr) {
                addEquipmentTimeSeries(branchSpec.branchRatingsOnContingencyEndOr, MetrixVariable.thresholdN1EndOr, id, tsConfig, binding)
            }
            // Before curative monitoring
            if (branchSpec.branchRatingsBeforeCurative) {
                addEquipmentTimeSeries(branchSpec.branchRatingsBeforeCurative, MetrixVariable.thresholdITAM, id, tsConfig, binding)
                if (branchSpec.branchRatingsBeforeCurativeEndOr) {
                    addEquipmentTimeSeries(branchSpec.branchRatingsBeforeCurativeEndOr, MetrixVariable.thresholdITAMEndOr, id, tsConfig, binding)
                }
            }
        } else if (branchSpec.branchAnalysisRatingsOnContingency) {
            // analysis threshold (n-k)
            addEquipmentTimeSeries(branchSpec.branchAnalysisRatingsOnContingency, MetrixVariable.analysisThresholdNk, id, tsConfig, binding)
            data.addBranchResultNk(id)
            if (branchSpec.branchAnalysisRatingsOnContingencyEndOr) {
                addEquipmentTimeSeries(branchSpec.branchAnalysisRatingsOnContingencyEndOr, MetrixVariable.analysisThresholdNkEndOr, id, tsConfig, binding)
            }
        } else if (branchSpec.maxThreatFlowResults) {
            data.addBranchResultNk(id)
        }



        // Specific N-k contingencies monitoring
        if (branchSpec.branchRatingsOnSpecificContingency) {
            addEquipmentTimeSeries(branchSpec.branchRatingsOnSpecificContingency, MetrixVariable.thresholdNk, id, tsConfig, binding)
            data.addBranchMonitoringNk(id)
            if (branchSpec.branchRatingsOnSpecificContingencyEndOr) {
                addEquipmentTimeSeries(branchSpec.branchRatingsOnSpecificContingencyEndOr, MetrixVariable.thresholdNkEndOr, id, tsConfig, binding)
            }

            // Specific before curative monitoring
            if (branchSpec.branchRatingsBeforeCurativeOnSpecificContingency) {
                addEquipmentTimeSeries(branchSpec.branchRatingsBeforeCurativeOnSpecificContingency, MetrixVariable.thresholdITAMNk, id, tsConfig, binding)
                if (branchSpec.branchRatingsBeforeCurativeOnSpecificContingencyEndOr) {
                    addEquipmentTimeSeries(branchSpec.branchRatingsBeforeCurativeOnSpecificContingencyEndOr, MetrixVariable.thresholdITAMNkEndOr, id, tsConfig, binding)
                }
            }
        }

        if (branchSpec.contingencyFlowResults != null && branchSpec.contingencyFlowResults.size() > 0) {
            data.addContingencyFlowResults(id, branchSpec.contingencyFlowResults)
        }

        if (branchSpec.contingencyDetailedMarginalVariations != null && branchSpec.contingencyDetailedMarginalVariations.size() > 0) {
            data.addContingencyDetailedMarginalVariations(id, branchSpec.contingencyDetailedMarginalVariations)
        }
    }


    private static generatorData(Binding binding, Closure closure, String id, Network network, TimeSeriesMappingConfig tsConfig, MetrixDslData data, Writer out) {
        Identifiable identifiable = network.getGenerator(id)
        if (identifiable == null) {
            logWarn(out, "generator id %s not found in the network", id)
            return
        }

        GeneratorSpec spec = generatorData(closure)

        if (spec) {
            if (spec.adequacyUpCosts != null || spec.adequacyDownCosts != null) {
                if (spec.adequacyUpCosts != null && spec.adequacyDownCosts != null) {
                    addEquipmentTimeSeries(spec.adequacyDownCosts, MetrixVariable.offGridCostDown, id, tsConfig, binding)
                    addEquipmentTimeSeries(spec.adequacyUpCosts, MetrixVariable.offGridCostUp, id, tsConfig, binding)
                    data.addGeneratorForAdequacy(id)
                } else if (spec.adequacyUpCosts == null) {
                    logDebug(out, "generator %s is missing adequacy up-cost time-series to be properly configured", id)
                } else {
                    logDebug(out, "generator %s is missing adequacy down-cost time-series to be properly configured", id)
                }
            }
            if (spec.redispatchingUpCosts != null || spec.redispatchingDownCosts != null) {
                if (spec.redispatchingUpCosts != null && spec.redispatchingDownCosts != null) {
                    addEquipmentTimeSeries(spec.redispatchingDownCosts, MetrixVariable.onGridCostDown, id, tsConfig, binding)
                    addEquipmentTimeSeries(spec.redispatchingUpCosts, MetrixVariable.onGridCostUp, id, tsConfig, binding)
                    data.addGeneratorForRedispatching(id, spec.onContingencies)
                } else if (spec.redispatchingUpCosts == null) {
                    logDebug(out, "generator %s is missing redispatching up-cost time-series to be properly configured", id)
                } else {
                    logDebug(out, "generator %s is missing redispatching down-cost time-series to be properly configured", id)
                }
            }
        }
    }

    private static generatorsBindingData(Binding binding, Closure closure, String id, Network network, MetrixDslData data, Writer out) {

        if (id == "") {
            logError(out, "missing generators group name")
            return
        } else if (id.indexOf(';') != -1) {
            logError(out, "semi-colons are forbidden in generators group name %s", id)
            return
        }


        GeneratorsBindingSpec spec = generatorsBindingData(closure)

        if (spec && spec.filter) {

            // evaluate filter
            def filteringContext = network.getGenerators().collect { g -> new FilteringContext(g) }
            Collection<Identifiable> filteredGenerators = Filter.evaluate(binding, filteringContext, "generator", spec.filter)

            List<String> generatorIds = filteredGenerators.collect { it -> it.id }

            if (generatorIds. size() > 1) {
                if (spec.referenceVariable) {
                    data.addGeneratorsBinding(id, generatorIds, spec.referenceVariable)
                } else {
                    data.addGeneratorsBinding(id, generatorIds)
                }
            } else {
                logWarn(out, "generators group %s ignored because it contains %d element", id, generatorIds.size())
            }
        } else {
            logError(out, "missing filter for generators group %s", id)
        }
    }


    private static loadsBindingData(Binding binding, Closure closure, String id, Network network, MetrixDslData data, Writer out) {

        if (id == "") {
            logWarn(out, "missing a name for loads group")
            return
        } else if (id.indexOf(';') != -1) {
            logError(out, "semi-colons are forbidden in loads group name %s", id)
            return
        }

        LoadsBindingSpec spec = loadsBindingData(closure)

        if (spec && spec.filter) {

            // evaluate filter
            def filteringContext = network.getLoads().collect { l -> new FilteringContext(l) }
            Collection<Identifiable> filteredLoads = Filter.evaluate(binding, filteringContext, "load", spec.filter)

            List<String> loadIds = filteredLoads.collect { it -> it.id }
            if (loadIds. size() > 1) {
                data.addLoadsBinding(id, loadIds)
            } else {
                logWarn(out, "loads group %s ignored because it contains %d element", id, loadIds.size())
            }
        } else {
            logError(out, "missing filter for loads group %s", id)
        }
    }

    private static loadData(Binding binding, Closure closure, String id, Network network, TimeSeriesMappingConfig tsConfig, MetrixDslData data, Writer out) {
        Identifiable identifiable = network.getLoad(id)
        if (identifiable == null) {
            logWarn(out, "load id %s not found in the network", id)
            return
        }

        LoadSpec loadSpec = loadData(closure)

        if (loadSpec.preventiveSheddingPercentage != null) {
            if (loadSpec.preventiveSheddingPercentage > 100 || loadSpec.preventiveSheddingPercentage < 0) {
                logWarn(out, "preventive shedding percentage for load %s is not valid", id)
            }
            else {
                data.addPreventiveLoad(id, loadSpec.preventiveSheddingPercentage)
                if (loadSpec.preventiveSheddingCost != null) {
                    data.addPreventiveLoadCost(id, loadSpec.preventiveSheddingCost)
                }
            }
        }

        if (loadSpec.curativeSheddingPercentage || loadSpec.curativeSheddingCost != null || loadSpec.onContingencies) {
            if (loadSpec.curativeSheddingPercentage && loadSpec.curativeSheddingCost != null && loadSpec.onContingencies) {
                if (loadSpec.curativeSheddingPercentage > 100 ||loadSpec.curativeSheddingPercentage < 0) {
                    logWarn(out, "curative shedding percentage for load %s is not valid", id)
                }
                else {
                    addEquipmentTimeSeries(loadSpec.curativeSheddingCost, MetrixVariable.curativeCostDown, id, tsConfig, binding)
                    data.addCurativeLoad(id, loadSpec.curativeSheddingPercentage, loadSpec.onContingencies)
                }
            } else {
                logWarn(out, "configuration error for load %s : curative costs, percentage and contingencies list must be set altogether", id)
            }
        }
    }

    private static void addEquipmentTimeSeries(Object timeSeriesName, MappingVariable variable, String equipmentId, TimeSeriesMappingConfig tsConfig, Binding binding) {
        CalculatedTimeSeriesDslLoader.TimeSeriesGroovyObject timeSeries = ((CalculatedTimeSeriesDslLoader.TimeSeriesGroovyObject) binding.getVariable("timeSeries"))
        if (timeSeriesName instanceof Number && !timeSeries.exists(timeSeriesName.toString())) {
            timeSeries[timeSeriesName.toString()] = timeSeriesName.floatValue()
        } else {
            timeSeries[timeSeriesName.toString()] // to check time series exists
        }
        tsConfig.addEquipmentTimeSeries(timeSeriesName.toString(), variable, equipmentId)
    }

    private static BranchMonitoringSpec branchMonitoringData(Closure closure) {
        def cloned = closure.clone()
        BranchMonitoringSpec spec = new BranchMonitoringSpec()
        cloned.delegate = spec
        cloned()
        spec
    }

    private static GeneratorSpec generatorData(Closure closure) {
        def cloned = closure.clone()
        GeneratorSpec spec = new GeneratorSpec()
        cloned.delegate = spec
        cloned()
        spec
    }

    private static LoadSpec loadData(Closure closure) {
        def cloned = closure.clone()
        LoadSpec spec = new LoadSpec()
        cloned.delegate = spec
        cloned()
        spec
    }

    private static LoadsBindingSpec loadsBindingData(Closure closure) {
        def cloned = closure.clone()
        LoadsBindingSpec spec = new LoadsBindingSpec()
        cloned.delegate = spec
        cloned()
        spec
    }

    private static GeneratorsBindingSpec generatorsBindingData(Closure closure) {
        def cloned = closure.clone()
        GeneratorsBindingSpec spec = new GeneratorsBindingSpec()
        cloned.delegate = spec
        cloned()
        spec
    }

    private static parametersData(Closure closure, MetrixParameters parameters) {
        def cloned = closure.clone()
        ParametersSpec spec = new ParametersSpec()
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
        if (spec.gapVariableCost != null) {
            parameters.setGapVariableCost(spec.gapVariableCost)
        }
        if (spec.nbThreatResults != null) {
            parameters.setNbThreatResults(spec.nbThreatResults)
        }
    }

    private static phaseShifterData(Closure closure, String id, Network network, MetrixDslData data, Writer out) {
        Identifiable twt = network.getTwoWindingsTransformer(id)
        if (twt == null) {
            logWarn(out, "transformer id %s not found in the network", id)
            return
        }
        if (twt.getPhaseTapChanger() == null) {
            throw new MetrixException("transformer id '" + id + "' without phase shifter")
        }
        def cloned = closure.clone()
        PhaseShifterSpec spec = new PhaseShifterSpec()
        cloned.delegate = spec
        cloned()

        if (spec.controlType) {
            data.addPtc(id, spec.controlType, spec.onContingencies)
            logDebug(out, "Found phaseTapChanger for id %s", id)
        }
        if (spec.preventiveLowerTapRange!=null){
            data.addLowerTapChange(id, spec.preventiveLowerTapRange)
        }
        if (spec.preventiveUpperTapRange!=null){
            data.addUpperTapChange(id, spec.preventiveUpperTapRange)
        }
    }

    private static hvdcData(Closure closure, String id, Network network, MetrixDslData data, Writer out) {
        Identifiable identifiable = network.getHvdcLine(id)
        if (identifiable == null) {
            logWarn(out, "hvdc id %s not found in the network", id)
            return
        }
        def cloned = closure.clone()
        HvdcSpec spec = new HvdcSpec()
        cloned.delegate = spec
        cloned()

        if (spec.controlType) {
            data.addHvdc(id, spec.controlType, spec.onContingencies)
            logDebug(out, "Found hvdc for id %s", id)
        }
    }

    private static sectionMonitoringData(Closure closure, String id, Network network, MetrixDslData data, Writer out) {
        def cloned = closure.clone()
        SectionMonitoring spec = new SectionMonitoring()
        cloned.delegate = spec
        cloned()
        MetrixSection section = new MetrixSection(id)
        if (! spec.maxFlowN) {
            logWarn(out, "Section Monitoring '"+id+"' is missing flow limit")
            return
        } else {
            section.setMaxFlowN(spec.maxFlowN)
        }
        if (!spec.branchList) {
            logWarn(out, "Section Monitoring '"+id+"' without branches")
            return
        }
        for (Map.Entry<String, Float> branch : spec.branchList) {
            Identifiable identifiable = network.getIdentifiable(branch.getKey())
            if (identifiable == null) {
                logWarn(out, "sectionMonitoring '"+id+"' branch id '"+branch.getKey()+"' not found in the network")
                return
            }
            if (!(identifiable instanceof Line || identifiable instanceof TwoWindingsTransformer || identifiable instanceof HvdcLine)) {
                logWarn(out, "sectionMonitoring '"+id+"' type " + identifiable.getClass().name + " not supported")
                return
            }
        }
        section.setCoefFlowList(spec.branchList)
        data.addSection(section)
        logDebug(out, "Found sectionMonitoring %s", id)
    }

    private static contingenciesData(Closure closure, MetrixDslData data, Writer out) {
        def cloned = closure.clone()
        ContingenciesSpec spec = new ContingenciesSpec()
        cloned.delegate = spec
        cloned()

        if (spec.specificContingencies != null) {
            data.setSpecificContingenciesList(spec.specificContingencies)
            logDebug(out, "Specific contingencies list : %s", data.getSpecificContingenciesList())
        }
    }

    MetrixDslDataLoader(GroovyCodeSource dslSrc) {
        super(dslSrc)
    }

    MetrixDslDataLoader(File dslFile) {
        super(dslFile)
    }

    MetrixDslDataLoader(String script) {
        super(script)
    }

    MetrixDslDataLoader(Reader reader, String fileName) {
        this(new GroovyCodeSource(reader, fileName, GroovyShell.DEFAULT_CODE_BASE))
    }

    private static CompilerConfiguration createCompilerConfig() {
        def imports = new ImportCustomizer()
        imports.addStaticStars("com.powsybl.metrix.integration.MetrixPtcControlType")
        imports.addStaticStars("com.powsybl.metrix.integration.MetrixHvdcControlType")
        imports.addStaticStars("com.powsybl.metrix.integration.MetrixComputationType")
        imports.addStaticStars("com.powsybl.metrix.integration.MetrixGeneratorsBinding.ReferenceVariable")
        def astCustomizer = new ASTTransformationCustomizer(new CalculatedTimeSeriesDslAstTransformation())
        def config = new CompilerConfiguration()
        config.addCompilationCustomizers(imports)
        config.addCompilationCustomizers(astCustomizer)
    }

    static void evaluate(GroovyCodeSource dslSrc, Binding binding) {
        def config = createCompilerConfig()
        def shell = new GroovyShell(binding, config)
        shell.evaluate(dslSrc)
    }

    MetrixDslData load(Network network, MetrixParameters parameters, TimeSeriesMappingConfig mappingConfig) {
        load(new Binding(), network, parameters, mappingConfig, null)
    }

    MetrixDslData load(Binding binding, Network network, MetrixParameters parameters, TimeSeriesMappingConfig mappingConfig, Writer out) {
        MetrixDslData data = new MetrixDslData()

        logDebug(out, "Loading DSL %s", dslSrc.getName())

        // parameters
        binding.parameters = { Closure<Void> closure ->
            parametersData(closure, parameters)
        }

        // branch monitoring
        binding.branch = { String id, Closure<Void> closure ->
            branchData(binding, closure, id, network, mappingConfig, data, out)
        }

        // generator costs
        binding.generator = { String id, Closure<Void> closure ->
            generatorData(binding, closure, id, network, mappingConfig, data, out)
        }

        // load shedding costs
        binding.load = { String id, Closure<Void> closure ->
            loadData(binding, closure, id, network, mappingConfig, data, out)
        }

        // phase tap changer
        binding.phaseShifter = { String id, Closure<Void> closure ->
            phaseShifterData(closure, id, network, data, out)
        }

        // hvdc
        binding.hvdc = { String id, Closure<Void> closure ->
            hvdcData(closure, id, network, data, out)
        }

        // section monitoring
        binding.sectionMonitoring = { String id, Closure<Void> closure ->
            sectionMonitoringData(closure, id, network, data, out)
        }

        // bound generators
        binding.generatorsGroup = { String id, Closure<Void> closure ->
            generatorsBindingData(binding, closure, id, network, data, out)
        }

        // bound loads
        binding.loadsGroup = { String id, Closure<Void> closure ->
            loadsBindingData(binding, closure, id, network, data, out)
        }

        // specific contingency list
        binding.contingencies = { Closure<Void> closure ->
            contingenciesData(closure, data, out)
        }

        // set base network
        binding.setVariable("network", network)

        evaluate(dslSrc, binding)

        data
    }

    static MetrixDslData load(Reader reader, Network network, MetrixParameters parameters, ReadOnlyTimeSeriesStore store,
                              TimeSeriesMappingConfig mappingConfig) {
        load(reader, network, parameters, store, mappingConfig, null)
    }

    static MetrixDslData load(Reader reader, Network network, MetrixParameters parameters, ReadOnlyTimeSeriesStore store,
                              TimeSeriesMappingConfig mappingConfig, Writer out) {

        Binding binding = new Binding()

        CalculatedTimeSeriesDslLoader.bind(binding, store, mappingConfig.getTimeSeriesNodes())

        if (out != null) {
            binding.out = out
        }

        MetrixDslDataLoader dslLoader = new MetrixDslDataLoader(reader, "metrixDsl.groovy")
        dslLoader.load(binding, network, parameters, mappingConfig, out)
    }

    static MetrixDslData load(Path metrixDslFile, Network network, MetrixParameters parameters, ReadOnlyTimeSeriesStore store,
                              TimeSeriesMappingConfig mappingConfig) {
        load(metrixDslFile, network, parameters, store, mappingConfig, null)
    }

    static MetrixDslData load(Path metrixDslFile, Network network, MetrixParameters parameters, ReadOnlyTimeSeriesStore store,
                              TimeSeriesMappingConfig mappingConfig, Writer out) {

        Binding binding = new Binding()

        CalculatedTimeSeriesDslLoader.bind(binding, store, mappingConfig.getTimeSeriesNodes())

        if (out != null) {
            binding.out = out
        }

        Files.newBufferedReader(metrixDslFile, StandardCharsets.UTF_8).withReader { Reader reader ->
            MetrixDslDataLoader dslLoader = new MetrixDslDataLoader(reader, metrixDslFile.getFileName().toString())
            dslLoader.load(binding, network, parameters, mappingConfig, out)
        }
    }
}
