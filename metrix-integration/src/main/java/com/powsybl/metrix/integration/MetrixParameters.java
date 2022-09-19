/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.integration;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ImmutableMap;
import com.powsybl.commons.config.PlatformConfig;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

public class MetrixParameters {

    private static final MetrixComputationType DEFAULT_COMPUTATION_TYPE = MetrixComputationType.LF;
    private static final float DEFAULT_LOSS_FACTOR = 0f;
    private static final int DEFAULT_NOMINAL_U = 100;

    public static MetrixParameters load() {
        MetrixParameters parameters = new MetrixParameters();
        PlatformConfig.defaultConfig().getOptionalModuleConfig("metrix-default-parameters")
                .ifPresent(config -> {
                    parameters.setComputationType(config.getOptionalEnumProperty("computation-type", MetrixComputationType.class)
                            .orElseGet(() -> config.getEnumProperty("computationType", MetrixComputationType.class, DEFAULT_COMPUTATION_TYPE)));
                    parameters.setLossFactor(config.getOptionalFloatProperty("loss-factor")
                            .orElseGet(() -> config.getFloatProperty("lossFactor", DEFAULT_LOSS_FACTOR)));
                    parameters.setNominalU(config.getOptionalIntProperty("nominal-u")
                            .orElseGet(() -> config.getIntProperty("nominalU", DEFAULT_NOMINAL_U)));
                });
        return parameters;
    }

    /* Mandatory parameters */
    private MetrixComputationType computationType;
    private float lossFactor;
    private int nominalU;

    /* Optional parameters */
    private Integer lossNbRelaunch = null;
    private Integer lossThreshold = null;
    private Integer nbMaxIteration = null;
    private Integer nbMaxCurativeAction = null;
    private Integer nbMaxLostLoadDetailedResults = null;
    private Boolean withLostLoadDetailedResultsOnContingency = null;
    private Integer gapVariableCost = null;
    private Integer nbThreatResults = null;
    private Integer maxSolverTime = null;
    private Integer redispatchingCostOffset = null;
    private Integer adequacyCostOffset = null;
    private Integer curativeRedispatchingLimit = null;
    private Boolean showAllTDandHVDCresults = null;
    private boolean propagateBranchTripping = false;
    private Boolean withGridCost = null;
    private Boolean preCurativeResults;
    private Boolean outagesBreakingConnexity = null;
    private Boolean remedialActionsBreakingConnexity = null;
    private Boolean analogousRemedialActionDetection = null;
    private Boolean lossDetailPerCountry = null;
    private Boolean overloadResultsOnly = null;
    private Boolean withAdequacyResults = null;
    private Boolean withRedispatchingResults = null;
    private Boolean marginalVariationsOnBranches = null;
    private Boolean marginalVariationsOnHvdc = null;
    private Float pstCostPenality = null;
    private Float hvdcCostPenality = null;
    private Float lossOfLoadCost = null;
    private Float curativeLossOfLoadCost = null;
    private Float curativeLossOfGenerationCost = null;
    private Float contingenciesProbability = null;

    public MetrixParameters() {
        this(DEFAULT_COMPUTATION_TYPE, DEFAULT_LOSS_FACTOR, DEFAULT_NOMINAL_U);
    }

    public MetrixParameters(MetrixComputationType computationType,
                            float lossFactor,
                            int nominalU) {
        this.computationType = computationType;
        this.lossFactor = lossFactor;
        this.nominalU = nominalU;
    }

    @JsonGetter
    private Integer getLossNbRelaunch() {
        return lossNbRelaunch;
    }

    @JsonGetter
    private Integer getLossThreshold() {
        return lossThreshold;
    }

    @JsonGetter
    private Integer getNbMaxIteration() {
        return nbMaxIteration;
    }

    @JsonGetter
    private Integer getNbMaxCurativeAction() {
        return nbMaxCurativeAction;
    }

    @JsonGetter
    private Boolean getWithLostLoadDetailedResultsOnContingency() {
        return withLostLoadDetailedResultsOnContingency;
    }

    @JsonGetter
    private Integer getGapVariableCost() {
        return gapVariableCost;
    }

    @JsonGetter
    private Integer getNbThreatResults() {
        return nbThreatResults;
    }

    @JsonGetter
    private Integer getMaxSolverTime() {
        return maxSolverTime;
    }

    @JsonGetter
    private Integer getRedispatchingCostOffset() {
        return redispatchingCostOffset;
    }

    @JsonGetter
    private Integer getAdequacyCostOffset() {
        return adequacyCostOffset;
    }

    @JsonGetter
    private Integer getCurativeRedispatchingLimit() {
        return curativeRedispatchingLimit;
    }

    @JsonGetter
    private boolean getPropagateBranchTripping() {
        return propagateBranchTripping;
    }

    @JsonGetter
    private Boolean getWithGridCost() {
        return withGridCost;
    }

    @JsonGetter
    private Boolean getPreCurativeResults() {
        return preCurativeResults;
    }

    @JsonGetter
    private Boolean getOutagesBreakingConnexity() {
        return outagesBreakingConnexity;
    }

    @JsonGetter
    private Boolean getRemedialActionsBreakingConnexity() {
        return remedialActionsBreakingConnexity;
    }

    @JsonGetter
    private Boolean getAnalogousRemedialActionDetection() {
        return analogousRemedialActionDetection;
    }

    @JsonGetter
    private Boolean getLossDetailPerCountry() {
        return lossDetailPerCountry;
    }

    @JsonGetter
    private Boolean getOverloadResultsOnly() {
        return overloadResultsOnly;
    }

    @JsonGetter
    private Boolean getShowAllTDandHVDCresults() {
        return showAllTDandHVDCresults;
    }

    @JsonGetter
    private Boolean getWithAdequacyResults() {
        return withAdequacyResults;
    }

    @JsonGetter
    private Boolean getWithRedispatchingResults() {
        return withRedispatchingResults;
    }

    @JsonGetter
    private Boolean getMarginalVariationsOnBranches() {
        return marginalVariationsOnBranches;
    }

    @JsonGetter
    private Boolean getMarginalVariationsOnHvdc() {
        return marginalVariationsOnHvdc;
    }

    @JsonGetter
    private Float getPstCostPenality() {
        return pstCostPenality;
    }

    @JsonGetter
    private Float getHvdcCostPenality() {
        return hvdcCostPenality;
    }

    @JsonGetter
    private Float getLossOfLoadCost() {
        return lossOfLoadCost;
    }

    @JsonGetter
    private Float getCurativeLossOfLoadCost() {
        return curativeLossOfLoadCost;
    }

    @JsonGetter
    private Float getCurativeLossOfGenerationCost() {
        return curativeLossOfGenerationCost;
    }

    @JsonGetter
    private Float getContingenciesProbability() {
        return contingenciesProbability;
    }

    public MetrixComputationType getComputationType() {
        return computationType;
    }

    public MetrixParameters setComputationType(MetrixComputationType computationType) {
        this.computationType = computationType;
        return this;
    }


    // Loss factor
    public float getLossFactor() {
        return lossFactor;
    }

    public MetrixParameters setLossFactor(float lossFactor) {
        this.lossFactor = lossFactor;
        return this;
    }


    // Nominal U
    public int getNominalU() {
        return nominalU;
    }

    public MetrixParameters setNominalU(int nominalU) {
        this.nominalU = nominalU;
        return this;
    }

    // With grid cost
    public Optional<Boolean> isWithGridCost() {
        return Optional.ofNullable(withGridCost);
    }

    public MetrixParameters setWithGridCost(Boolean withGridCost) {
        this.withGridCost = withGridCost;
        return this;
    }

    // Use pre-outage thresholds
    public Optional<Boolean> isPreCurativeResults() {
        return Optional.ofNullable(preCurativeResults);
    }

    public MetrixParameters setPreCurativeResults(Boolean preCurativeResults) {
        this.preCurativeResults = preCurativeResults;
        return this;
    }

    // Allow outages breaking connexity
    public Optional<Boolean> isOutagesBreakingConnexity() {
        return Optional.ofNullable(outagesBreakingConnexity);
    }

    public MetrixParameters setOutagesBreakingConnexity(Boolean outagesBreakingConnexity) {
        this.outagesBreakingConnexity = outagesBreakingConnexity;
        return this;
    }

    // Allow remedial actions to break connexity
    public Optional<Boolean> isRemedialActionsBreakingConnexity() {
        return Optional.ofNullable(remedialActionsBreakingConnexity);
    }

    public MetrixParameters setRemedialActionsBreakingConnexity(Boolean remedialActionsBreakingConnexity) {
        this.remedialActionsBreakingConnexity = remedialActionsBreakingConnexity;
        return this;
    }

    // Detection of similar remedial actions
    public Optional<Boolean> isAnalogousRemedialActionDetection() {
        return Optional.ofNullable(analogousRemedialActionDetection);
    }

    public MetrixParameters setAnalogousRemedialActionDetection(Boolean analogousRemedialActionDetection) {
        this.analogousRemedialActionDetection = analogousRemedialActionDetection;
        return this;
    }

    // Propagate outages through switches without breakers
    public boolean isPropagateBranchTripping() {
        return propagateBranchTripping;
    }

    public MetrixParameters setPropagateBranchTripping(boolean propagateBranchTripping) {
        this.propagateBranchTripping = propagateBranchTripping;
        return this;
    }

    // With adequacy results
    public Optional<Boolean> isWithAdequacyResults() {
        return Optional.ofNullable(withAdequacyResults);
    }

    public MetrixParameters setWithAdequacyResults(Boolean withAdequacyResults) {
        this.withAdequacyResults = withAdequacyResults;
        return this;
    }

    // With redispatching results
    public Optional<Boolean> isWithRedispatchingResults() {
        return Optional.ofNullable(withRedispatchingResults);
    }

    public MetrixParameters setWithRedispatchingResults(Boolean withRedispatchingResults) {
        this.withRedispatchingResults = withRedispatchingResults;
        return this;
    }

    // With marginal variations on branches
    public Optional<Boolean> isMarginalVariationsOnBranches() {
        return Optional.ofNullable(marginalVariationsOnBranches);
    }

    public MetrixParameters setMarginalVariationsOnBranches(Boolean marginalVariationsOnBranches) {
        this.marginalVariationsOnBranches = marginalVariationsOnBranches;
        return this;
    }

    // With marginal variations on hvdc
    public Optional<Boolean> isMarginalVariationsOnHvdc() {
        return Optional.ofNullable(marginalVariationsOnHvdc);
    }

    public MetrixParameters setMarginalVariationsOnHvdc(Boolean marginalVariationsOnHvdc) {
        this.marginalVariationsOnHvdc = marginalVariationsOnHvdc;
        return this;
    }

    // Loss detail per country
    public Optional<Boolean> isLossDetailPerCountry() {
        return Optional.ofNullable(lossDetailPerCountry);
    }

    public MetrixParameters setLossDetailPerCountry(Boolean lossDetailPerCountry) {
        this.lossDetailPerCountry = lossDetailPerCountry;
        return this;
    }

    // Return only overload results
    public Optional<Boolean> isOverloadResultsOnly() {
        return Optional.ofNullable(overloadResultsOnly);
    }

    public MetrixParameters setOverloadResultsOnly(Boolean overloadResultsOnly) {
        this.overloadResultsOnly = overloadResultsOnly;
        return this;
    }

    // Show All TD and HVDC results
    public Optional<Boolean> isShowAllTDandHVDCresults() {
        return Optional.ofNullable(showAllTDandHVDCresults);
    }

    public MetrixParameters setShowAllTDandHVDCresults(Boolean showAllTDandHVDCresults) {
        this.showAllTDandHVDCresults = showAllTDandHVDCresults;
        return this;
    }

    // With lost load detailed results on contingency
    public Optional<Boolean> isWithLostLoadDetailedResultsOnContingency() {
        return Optional.ofNullable(withLostLoadDetailedResultsOnContingency);
    }

    public MetrixParameters setWithLostLoadDetailedResultsOnContingency(Boolean withLostLoadDetailedResultsOnContingency) {
        this.withLostLoadDetailedResultsOnContingency = withLostLoadDetailedResultsOnContingency;
        return this;
    }

    // Max solver time
    @JsonIgnore
    public OptionalInt getOptionalMaxSolverTime() {
        return maxSolverTime == null ? OptionalInt.empty() : OptionalInt.of(maxSolverTime);
    }

    public MetrixParameters setMaxSolverTime(Integer maxSolverTime) {
        this.maxSolverTime = maxSolverTime;
        return this;
    }

    // Redispatching cost offset
    @JsonIgnore
    public OptionalInt getOptionalRedispatchingCostOffset() {
        return redispatchingCostOffset == null ? OptionalInt.empty() : OptionalInt.of(redispatchingCostOffset);
    }

    public MetrixParameters setRedispatchingCostOffset(Integer redispatchingCostOffset) {
        this.redispatchingCostOffset = redispatchingCostOffset;
        return this;
    }

    // Adequacy cost offset
    @JsonIgnore
    public OptionalInt getOptionalAdequacyCostOffset() {
        return adequacyCostOffset == null ? OptionalInt.empty() : OptionalInt.of(adequacyCostOffset);
    }

    public MetrixParameters setAdequacyCostOffset(Integer adequacyCostOffset) {
        this.adequacyCostOffset = adequacyCostOffset;
        return this;
    }

    // Curative redispatchingLimit
    @JsonIgnore
    public OptionalInt getOptionalCurativeRedispatchingLimit() {
        return curativeRedispatchingLimit == null ? OptionalInt.empty() : OptionalInt.of(curativeRedispatchingLimit);
    }

    public MetrixParameters setCurativeRedispatchingLimit(Integer curativeRedispatchingLimit) {
        this.curativeRedispatchingLimit = curativeRedispatchingLimit;
        return this;
    }

    // Nb loss relaunch
    @JsonIgnore
    public OptionalInt getOptionalLossNbRelaunch() {
        return lossNbRelaunch == null ? OptionalInt.empty() : OptionalInt.of(lossNbRelaunch);
    }

    public MetrixParameters setLossNbRelaunch(Integer lossNbRelaunch) {
        this.lossNbRelaunch = lossNbRelaunch;
        return this;
    }

    // Loss relaunch threshold
    @JsonIgnore
    public OptionalInt getOptionalLossThreshold() {
        return lossThreshold == null ? OptionalInt.empty() : OptionalInt.of(lossThreshold);
    }

    public MetrixParameters setLossThreshold(Integer lossThreshold) {
        this.lossThreshold = lossThreshold;
        return this;
    }

    // PST cost penalty
    @JsonIgnore
    public Optional<Float> getOptionalPstCostPenality() {
        return Optional.ofNullable(pstCostPenality);
    }

    public MetrixParameters setPstCostPenality(Float pstCostPenality) {
        this.pstCostPenality = pstCostPenality;
        return this;
    }

    // HVDC cost penalty
    @JsonIgnore
    public Optional<Float> getOptionalHvdcCostPenality() {
        return Optional.ofNullable(hvdcCostPenality);
    }

    public MetrixParameters setHvdcCostPenality(Float hvdcCostPenality) {
        this.hvdcCostPenality = hvdcCostPenality;
        return this;
    }

    // Loss of load cost
    @JsonIgnore
    public Optional<Float> getOptionalLossOfLoadCost() {
        return Optional.ofNullable(lossOfLoadCost);
    }

    public MetrixParameters setLossOfLoadCost(Float lossOfLoadCost) {
        this.lossOfLoadCost = lossOfLoadCost;
        return this;
    }

    // Curative loss of load cost
    @JsonIgnore
    public Optional<Float> getOptionalCurativeLossOfLoadCost() {
        return Optional.ofNullable(curativeLossOfLoadCost);
    }

    public MetrixParameters setCurativeLossOfLoadCost(Float curativeLossOfLoadCost) {
        this.curativeLossOfLoadCost = curativeLossOfLoadCost;
        return this;
    }

    // Curative loss of generation cost
    @JsonIgnore
    public Optional<Float> getOptionalCurativeLossOfGenerationCost() {
        return Optional.ofNullable(curativeLossOfGenerationCost);
    }

    public MetrixParameters setCurativeLossOfGenerationCost(Float curativeLossOfGenerationCost) {
        this.curativeLossOfGenerationCost = curativeLossOfGenerationCost;
        return this;
    }

    // Outage probability
    @JsonIgnore
    public Optional<Float> getOptionalContingenciesProbability() {
        return Optional.ofNullable(contingenciesProbability);
    }

    public MetrixParameters setContingenciesProbability(Float contingenciesProbability) {
        this.contingenciesProbability = contingenciesProbability;
        return this;
    }

    // Max nb iterations
    @JsonIgnore
    public OptionalInt getOptionalNbMaxIteration() {
        return nbMaxIteration == null ? OptionalInt.empty() : OptionalInt.of(nbMaxIteration);
    }

    public MetrixParameters setNbMaxIteration(Integer nbMaxIteration) {
        this.nbMaxIteration = nbMaxIteration;
        return this;
    }

    // Nb max curative action
    @JsonIgnore
    public OptionalInt getOptionalNbMaxCurativeAction() {
        return nbMaxCurativeAction == null ? OptionalInt.empty() : OptionalInt.of(nbMaxCurativeAction);
    }

    public MetrixParameters setNbMaxCurativeAction(Integer nbMaxCurativeAction) {
        this.nbMaxCurativeAction = nbMaxCurativeAction;
        return this;
    }

    // Nb max lost load detailed results
    @JsonIgnore
    public OptionalInt getOptionalNbMaxLostLoadDetailedResults() {
        return nbMaxLostLoadDetailedResults == null ? OptionalInt.empty() : OptionalInt.of(nbMaxLostLoadDetailedResults);
    }

    public MetrixParameters setNbMaxLostLoadDetailedResults(Integer nbMaxLostLoadDetailedResults) {
        this.nbMaxLostLoadDetailedResults = nbMaxLostLoadDetailedResults;
        return this;
    }

    // Gap variable cost
    @JsonIgnore
    public OptionalInt getOptionalGapVariableCost() {
        return gapVariableCost == null ? OptionalInt.empty() : OptionalInt.of(gapVariableCost);
    }

    public MetrixParameters setGapVariableCost(Integer gapVariableCost) {
        this.gapVariableCost = gapVariableCost;
        return this;
    }

    // Nb threat results
    @JsonIgnore
    public OptionalInt getOptionalNbThreatResults() {
        return nbThreatResults == null ? OptionalInt.empty() : OptionalInt.of(nbThreatResults);
    }

    public MetrixParameters setNbThreatResults(Integer nbThreatResults) {
        this.nbThreatResults = nbThreatResults;
        return this;
    }

    @Override
    public int hashCode() {
        return Objects.hash(withGridCost,
                    preCurativeResults,
                    outagesBreakingConnexity,
                    remedialActionsBreakingConnexity,
                    analogousRemedialActionDetection,
                    propagateBranchTripping,
                    withAdequacyResults,
                    withRedispatchingResults,
                    marginalVariationsOnBranches,
                    marginalVariationsOnHvdc,
                    lossDetailPerCountry,
                    overloadResultsOnly,
                    showAllTDandHVDCresults,
                    lossNbRelaunch,
                    lossThreshold,
                    pstCostPenality,
                    hvdcCostPenality,
                    lossOfLoadCost,
                    curativeLossOfLoadCost,
                    curativeLossOfGenerationCost,
                    contingenciesProbability,
                    maxSolverTime,
                    nbMaxIteration,
                    nbMaxCurativeAction,
                    withLostLoadDetailedResultsOnContingency,
                    nbMaxLostLoadDetailedResults,
                    gapVariableCost,
                    nbThreatResults,
                    redispatchingCostOffset,
                    adequacyCostOffset,
                    curativeRedispatchingLimit);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MetrixParameters) {
            MetrixParameters other = (MetrixParameters) obj;
            return propagateBranchTripping == other.propagateBranchTripping &&
                    Objects.equals(withGridCost, other.withGridCost) &&
                    Objects.equals(preCurativeResults, other.preCurativeResults) &&
                    Objects.equals(outagesBreakingConnexity, other.outagesBreakingConnexity) &&
                    Objects.equals(remedialActionsBreakingConnexity, other.remedialActionsBreakingConnexity) &&
                    Objects.equals(analogousRemedialActionDetection, other.analogousRemedialActionDetection) &&
                    Objects.equals(withAdequacyResults, other.withAdequacyResults) &&
                    Objects.equals(withRedispatchingResults, other.withRedispatchingResults) &&
                    Objects.equals(marginalVariationsOnBranches, other.marginalVariationsOnBranches) &&
                    Objects.equals(marginalVariationsOnHvdc, other.marginalVariationsOnHvdc) &&
                    Objects.equals(lossDetailPerCountry, other.lossDetailPerCountry) &&
                    Objects.equals(overloadResultsOnly, other.overloadResultsOnly) &&
                    Objects.equals(showAllTDandHVDCresults, other.showAllTDandHVDCresults) &&
                    Objects.equals(withLostLoadDetailedResultsOnContingency, other.withLostLoadDetailedResultsOnContingency) &&
                    Objects.equals(lossNbRelaunch, other.lossNbRelaunch) &&
                    Objects.equals(lossThreshold, other.lossThreshold) &&
                    Objects.equals(pstCostPenality, other.pstCostPenality) &&
                    Objects.equals(hvdcCostPenality, other.hvdcCostPenality) &&
                    Objects.equals(lossOfLoadCost, other.lossOfLoadCost) &&
                    Objects.equals(curativeLossOfLoadCost, other.curativeLossOfLoadCost) &&
                    Objects.equals(curativeLossOfGenerationCost, other.curativeLossOfGenerationCost) &&
                    Objects.equals(contingenciesProbability, other.contingenciesProbability) &&
                    Objects.equals(maxSolverTime, other.maxSolverTime) &&
                    Objects.equals(nbMaxIteration, other.nbMaxIteration) &&
                    Objects.equals(nbMaxCurativeAction, other.nbMaxCurativeAction) &&
                    Objects.equals(nbMaxLostLoadDetailedResults, other.nbMaxLostLoadDetailedResults) &&
                    Objects.equals(gapVariableCost, other.gapVariableCost) &&
                    Objects.equals(nbThreatResults, other.nbThreatResults) &&
                    Objects.equals(redispatchingCostOffset, other.redispatchingCostOffset) &&
                    Objects.equals(adequacyCostOffset, other.adequacyCostOffset) &&
                    Objects.equals(curativeRedispatchingLimit, other.curativeRedispatchingLimit);
        }
        return false;
    }

    @Override
    public String toString() {
        ImmutableMap.Builder<String, Object> builder = ImmutableMap.<String, Object>builder()
                .put("computationType", computationType)
                .put("lossFactor", lossFactor)
                .put("nominalU", nominalU);

        isWithGridCost().ifPresent(value -> builder.put("withGridCost", value));
        isPreCurativeResults().ifPresent(value -> builder.put("preCurativeResults", value));
        isOutagesBreakingConnexity().ifPresent(value -> builder.put("outagesBreakingConnexity", value));
        isRemedialActionsBreakingConnexity().ifPresent(value -> builder.put("remedialActionsBreakingConnexity", value));
        isAnalogousRemedialActionDetection().ifPresent(value -> builder.put("analogousRemedialActionDetection", value));
        builder.put("propagateBranchTripping", propagateBranchTripping);
        isWithAdequacyResults().ifPresent(value -> builder.put("withAdequacyResults", value));
        isWithRedispatchingResults().ifPresent(value -> builder.put("withRedispatchingResults", value));
        isMarginalVariationsOnBranches().ifPresent(value -> builder.put("marginalVariationsOnBranches", value));
        isMarginalVariationsOnHvdc().ifPresent(value -> builder.put("marginalVariationsOnHvdc", value));
        isLossDetailPerCountry().ifPresent(value -> builder.put("lossDetailPerCountry", value));
        isOverloadResultsOnly().ifPresent(value -> builder.put("overloadResultsOnly", value));
        isShowAllTDandHVDCresults().ifPresent(value -> builder.put("showAllTDandHVDCresults", value));
        isWithLostLoadDetailedResultsOnContingency().ifPresent(value -> builder.put("withLostLoadDetailedResultsOnContingency", value));
        getOptionalLossNbRelaunch().ifPresent(value -> builder.put("lossNbRelaunch", value));
        getOptionalLossThreshold().ifPresent(value -> builder.put("lossThreshold", value));
        getOptionalPstCostPenality().ifPresent(value -> builder.put("pstCostPenality", value));
        getOptionalHvdcCostPenality().ifPresent(value -> builder.put("hvdcCostPenality", value));
        getOptionalLossOfLoadCost().ifPresent(value -> builder.put("lossOfLoadCost", value));
        getOptionalCurativeLossOfLoadCost().ifPresent(value -> builder.put("curativeLossOfLoadCost", value));
        getOptionalCurativeLossOfGenerationCost().ifPresent(value -> builder.put("curativeLossOfGenerationCost", value));
        getOptionalContingenciesProbability().ifPresent(value -> builder.put("contingenciesProbability", value));
        getOptionalMaxSolverTime().ifPresent(value -> builder.put("maxSolverTime", value));
        getOptionalNbMaxIteration().ifPresent(value -> builder.put("nbMaxIteration", value));
        getOptionalNbMaxCurativeAction().ifPresent(value -> builder.put("nbMaxCurativeAction", value));
        getOptionalNbMaxLostLoadDetailedResults().ifPresent(value -> builder.put("nbMaxLostLoadDetailedResults", value));
        getOptionalGapVariableCost().ifPresent(value -> builder.put("gapVariableCost", value));
        getOptionalNbThreatResults().ifPresent(value -> builder.put("nbThreatResults", value));
        getOptionalRedispatchingCostOffset().ifPresent(value -> builder.put("redispatchingCostOffset", value));
        getOptionalAdequacyCostOffset().ifPresent(value -> builder.put("adequacyCostOffset", value));
        getOptionalCurativeRedispatchingLimit().ifPresent(value -> builder.put("curativeRedispatchingLimit", value));

        return builder.build().toString();
    }
}
