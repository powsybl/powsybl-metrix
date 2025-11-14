/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import com.powsybl.metrix.integration.binding.MetrixGeneratorsBinding;
import com.powsybl.metrix.integration.binding.MetrixLoadsBinding;
import com.powsybl.metrix.integration.configuration.MetrixParameters;
import com.powsybl.metrix.integration.dataGenerator.MetrixInputData;
import com.powsybl.metrix.integration.dsl.BoundVariablesDslData;
import com.powsybl.metrix.integration.dsl.BranchDslData;
import com.powsybl.metrix.integration.dsl.GeneratorDslData;
import com.powsybl.metrix.integration.dsl.HvdcDslData;
import com.powsybl.metrix.integration.dsl.LoadDslData;
import com.powsybl.metrix.integration.dsl.PhaseTapChangerDslData;
import com.powsybl.metrix.integration.dsl.SectionDslData;
import com.powsybl.metrix.integration.type.MetrixComputationType;
import com.powsybl.metrix.integration.type.MetrixHvdcControlType;
import com.powsybl.metrix.integration.type.MetrixPtcControlType;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author Paul Bui-Quang {@literal <paul.buiquang at rte-france.com>}
 */
public class MetrixDslData {

    private static final double OVERLOAD_RATIO = .3; // Assume 30% of monitored branches get overloaded at some point

    private final BranchDslData branchDslData;
    private final PhaseTapChangerDslData phaseTapChangerDslData;
    private final HvdcDslData hvdcDslData;
    private final SectionDslData sectionDslData;
    private final GeneratorDslData generatorDslData;
    private final LoadDslData loadDslData;
    private final BoundVariablesDslData boundVariablesDslData;
    private final Set<String> specificContingenciesList;

    private MetrixComputationType computationType;

    public MetrixDslData() {
        branchDslData = new BranchDslData();
        phaseTapChangerDslData = new PhaseTapChangerDslData();
        hvdcDslData = new HvdcDslData();
        sectionDslData = new SectionDslData();
        generatorDslData = new GeneratorDslData();
        loadDslData = new LoadDslData();
        boundVariablesDslData = new BoundVariablesDslData();
        specificContingenciesList = new HashSet<>();
    }

    @JsonCreator
    public MetrixDslData(@JsonProperty("branchMonitoringListN") Map<String, MetrixInputData.MonitoringType> branchMonitoringListN,
                         @JsonProperty("branchMonitoringListNk") Map<String, MetrixInputData.MonitoringType> branchMonitoringListNk,
                         @JsonProperty("branchMonitoringStatisticsThresholdN") Map<String, MetrixVariable> branchMonitoringStatisticsThresholdN,
                         @JsonProperty("branchMonitoringStatisticsThresholdNk") Map<String, MetrixVariable> branchMonitoringStatisticsThresholdNk,
                         @JsonProperty("contingencyFlowResults") Map<String, List<String>> contingencyFlowResults,
                         @JsonProperty("contingencyDetailedMarginalVariations") Map<String, List<String>> contingencyDetailedMarginalVariations,
                         @JsonProperty("ptcContingenciesMap") Map<String, List<String>> ptcContingenciesMap,
                         @JsonProperty("ptcControlMap") Map<String, MetrixPtcControlType> ptcControlMap,
                         @JsonProperty("pstAngleTapResults") Set<String> pstAngleTapResults,
                         @JsonProperty("ptcLowerTapchangeMap") Map<String, Integer> ptcLowerTapchangeMap,
                         @JsonProperty("ptcUpperTapchangeMap") Map<String, Integer> ptcUpperTapchangeMap,
                         @JsonProperty("hvdcContingenciesMap") Map<String, List<String>> hvdcContingenciesMap,
                         @JsonProperty("hvdcControlMap") Map<String, MetrixHvdcControlType> hvdcControlMap,
                         @JsonProperty("hvdcFlowResults") Set<String> hvdcFlowResults,
                         @JsonProperty("sectionList") Set<MetrixSection> sectionList,
                         @JsonProperty("generatorsForAdequacy") Set<String> generatorsForAdequacy,
                         @JsonProperty("generatorsForRedispatching") Set<String> generatorsForRedispatching,
                         @JsonProperty("generatorContingenciesMap") Map<String, List<String>> generatorContingenciesMap,
                         @JsonProperty("loadPreventivePercentageMap") Map<String, Integer> loadPreventivePercentageMap,
                         @JsonProperty("loadPreventiveCostsMap") Map<String, Float> loadPreventiveCostsMap,
                         @JsonProperty("loadCurativePercentageMap") Map<String, Integer> loadCurativePercentageMap,
                         @JsonProperty("loadContingenciesMap") Map<String, List<String>> loadContingenciesMap,
                         @JsonProperty("generatorsBindings") Map<String, MetrixGeneratorsBinding> generatorsBindings,
                         @JsonProperty("loadsBindings") Map<String, MetrixLoadsBinding> loadsBindings,
                         @JsonProperty("specificContingenciesList") Set<String> specificContingenciesList) {
        this.branchDslData = new BranchDslData(branchMonitoringListN,
            branchMonitoringListNk,
            branchMonitoringStatisticsThresholdN,
            branchMonitoringStatisticsThresholdNk,
            contingencyFlowResults,
            contingencyDetailedMarginalVariations);
        this.phaseTapChangerDslData = new PhaseTapChangerDslData(ptcContingenciesMap,
            ptcControlMap,
            pstAngleTapResults,
            ptcLowerTapchangeMap,
            ptcUpperTapchangeMap);
        this.hvdcDslData = new HvdcDslData(hvdcContingenciesMap, hvdcControlMap, hvdcFlowResults);
        this.sectionDslData = new SectionDslData(sectionList);
        this.generatorDslData = new GeneratorDslData(generatorsForAdequacy, generatorsForRedispatching, generatorContingenciesMap);
        this.loadDslData = new LoadDslData(loadPreventivePercentageMap, loadPreventiveCostsMap, loadCurativePercentageMap, loadContingenciesMap);
        this.boundVariablesDslData = new BoundVariablesDslData(generatorsBindings, loadsBindings);
        this.specificContingenciesList = specificContingenciesList;
    }

    @Override
    public int hashCode() {
        return Objects.hash(branchDslData,
            phaseTapChangerDslData,
            hvdcDslData,
            sectionDslData,
            generatorDslData,
            loadDslData,
            boundVariablesDslData,
            specificContingenciesList);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MetrixDslData other) {
            return branchDslData.equals(other.branchDslData) &&
                phaseTapChangerDslData.equals(other.phaseTapChangerDslData) &&
                hvdcDslData.equals(other.hvdcDslData) &&
                sectionDslData.equals(other.sectionDslData) &&
                generatorDslData.equals(other.generatorDslData) &&
                loadDslData.equals(other.loadDslData) &&
                boundVariablesDslData.equals(other.boundVariablesDslData) &&
                specificContingenciesList.equals(other.specificContingenciesList);
        }
        return false;
    }

    @Override
    public String toString() {
        ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
        branchDslData.addToBuilder(builder);
        phaseTapChangerDslData.addToBuilder(builder);
        hvdcDslData.addToBuilder(builder);
        sectionDslData.addToBuilder(builder);
        generatorDslData.addToBuilder(builder);
        loadDslData.addToBuilder(builder);
        boundVariablesDslData.addToBuilder(builder);
        builder.put("specificContingenciesList", specificContingenciesList);
        return builder.build().toString();
    }

    // Getters
    public Map<String, MetrixInputData.MonitoringType> getBranchMonitoringListN() {
        return Collections.unmodifiableMap(branchDslData.getBranchMonitoringListN());
    }

    public Map<String, MetrixInputData.MonitoringType> getBranchMonitoringListNk() {
        return Collections.unmodifiableMap(branchDslData.getBranchMonitoringListNk());
    }

    public Map<String, MetrixVariable> getBranchMonitoringStatisticsThresholdN() {
        return Collections.unmodifiableMap(branchDslData.getBranchMonitoringStatisticsThresholdN());
    }

    public Map<String, MetrixVariable> getBranchMonitoringStatisticsThresholdNk() {
        return Collections.unmodifiableMap(branchDslData.getBranchMonitoringStatisticsThresholdNk());
    }

    public Map<String, List<String>> getContingencyFlowResults() {
        return Collections.unmodifiableMap(branchDslData.getContingencyFlowResults());
    }

    public Map<String, List<String>> getContingencyDetailedMarginalVariations() {
        return Collections.unmodifiableMap(branchDslData.getContingencyDetailedMarginalVariations());
    }

    public Map<String, List<String>> getPtcContingenciesMap() {
        return Collections.unmodifiableMap(phaseTapChangerDslData.getPtcContingenciesMap());
    }

    public Map<String, MetrixPtcControlType> getPtcControlMap() {
        return Collections.unmodifiableMap(phaseTapChangerDslData.getPtcControlMap());
    }

    public Map<String, Integer> getPtcLowerTapchangeMap() {
        return Collections.unmodifiableMap(phaseTapChangerDslData.getPtcLowerTapChangerMap());
    }

    public Map<String, Integer> getPtcUpperTapchangeMap() {
        return Collections.unmodifiableMap(phaseTapChangerDslData.getPtcUpperTapChangerMap());
    }

    public Set<String> getPstAngleTapResults() {
        return Collections.unmodifiableSet(phaseTapChangerDslData.getPstAngleTapResults());
    }

    public Map<String, List<String>> getHvdcContingenciesMap() {
        return Collections.unmodifiableMap(hvdcDslData.getHvdcContingenciesMap());
    }

    public Map<String, MetrixHvdcControlType> getHvdcControlMap() {
        return Collections.unmodifiableMap(hvdcDslData.getHvdcControlMap());
    }

    public Set<String> getHvdcFlowResults() {
        return Collections.unmodifiableSet(hvdcDslData.getHvdcFlowResults());
    }

    public Set<MetrixSection> getSectionList() {
        return Collections.unmodifiableSet(sectionDslData.getSectionList());
    }

    public Set<String> getGeneratorsForAdequacy() {
        return Collections.unmodifiableSet(generatorDslData.getGeneratorsForAdequacy());
    }

    public Set<String> getGeneratorsForRedispatching() {
        return Collections.unmodifiableSet(generatorDslData.getGeneratorsForRedispatching());
    }

    public Map<String, List<String>> getGeneratorContingenciesMap() {
        return Collections.unmodifiableMap(generatorDslData.getGeneratorContingenciesMap());
    }

    public Map<String, Integer> getLoadPreventivePercentageMap() {
        return Collections.unmodifiableMap(loadDslData.getLoadPreventivePercentageMap());
    }

    public Map<String, Float> getLoadPreventiveCostsMap() {
        return Collections.unmodifiableMap(loadDslData.getLoadPreventiveCostsMap());
    }

    public Map<String, Integer> getLoadCurativePercentageMap() {
        return Collections.unmodifiableMap(loadDslData.getLoadCurativePercentageMap());
    }

    public Map<String, List<String>> getLoadContingenciesMap() {
        return Collections.unmodifiableMap(loadDslData.getLoadContingenciesMap());
    }

    public Map<String, MetrixGeneratorsBinding> getGeneratorsBindings() {
        return Collections.unmodifiableMap(boundVariablesDslData.getGeneratorsBindings());
    }

    public Map<String, MetrixLoadsBinding> getLoadsBindings() {
        return Collections.unmodifiableMap(boundVariablesDslData.getLoadsBindings());
    }

    public Set<String> getSpecificContingenciesList() {
        return Collections.unmodifiableSet(specificContingenciesList);
    }

    // Branch monitoring
    public final MetrixInputData.MonitoringType getBranchMonitoringN(String id) {
        return branchDslData.getBranchMonitoringN(id);
    }

    @JsonIgnore
    public final Set<String> getBranchMonitoringNList() {
        return branchDslData.getBranchMonitoringNList();
    }

    public final MetrixInputData.MonitoringType getBranchMonitoringNk(String id) {
        return branchDslData.getBranchMonitoringNk(id);
    }

    public final MetrixVariable getBranchMonitoringStatisticsThresholdN(String id) {
        return branchDslData.getBranchMonitoringStatisticsThresholdN(id);
    }

    @JsonIgnore
    public final Set<String> getBranchMonitoringNkList() {
        return branchDslData.getBranchMonitoringNkList();
    }

    public final MetrixVariable getBranchMonitoringStatisticsThresholdNk(String id) {
        return branchDslData.getBranchMonitoringStatisticsThresholdNk(id);
    }

    @JsonIgnore
    public final Set<String> getContingencyFlowResultList() {
        return branchDslData.getContingencyFlowResultList();
    }

    public final List<String> getContingencyFlowResult(String id) {
        return branchDslData.getContingencyFlowResult(id);
    }

    @JsonIgnore
    public final Set<String> getContingencyDetailedMarginalVariationsList() {
        return branchDslData.getContingencyDetailedMarginalVariationsList();
    }

    public final List<String> getContingencyDetailedMarginalVariations(String id) {
        return branchDslData.getContingencyDetailedMarginalVariations(id);
    }

    public void addBranchMonitoringN(String id) {
        branchDslData.addBranchMonitoringN(id);
    }

    public void addBranchResultN(String id) {
        branchDslData.addBranchResultN(id);
    }

    public void addBranchMonitoringNk(String id) {
        branchDslData.addBranchMonitoringNk(id);
    }

    public void addBranchResultNk(String id) {
        branchDslData.addBranchResultNk(id);
    }

    public void addContingencyFlowResults(String id, List<String> contingencies) {
        branchDslData.addContingencyFlowResults(id, contingencies);
    }

    public void addContingencyDetailedMarginalVariations(String id, List<String> contingencies) {
        branchDslData.addContingencyDetailedMarginalVariations(id, contingencies);
    }

    // PhaseTapChanger
    @JsonIgnore
    public final Set<String> getPtcContingenciesList() {
        return phaseTapChangerDslData.getPtcContingenciesList();
    }

    public final List<String> getPtcContingencies(String id) {
        return phaseTapChangerDslData.getPtcContingencies(id);
    }

    @JsonIgnore
    public final Set<String> getPtcControlList() {
        return phaseTapChangerDslData.getPtcControlList();
    }

    public final MetrixPtcControlType getPtcControl(String id) {
        return phaseTapChangerDslData.getPtcControl(id);
    }

    @JsonIgnore
    public final Set<String> getPtcLowerTapChangerList() {
        return phaseTapChangerDslData.getPtcLowerTapChangerList();
    }

    public final Integer getPtcLowerTapChanger(String id) {
        return phaseTapChangerDslData.getPtcLowerTapChanger(id);
    }

    @JsonIgnore
    public final Set<String> getPtcUpperTapChangerList() {
        return phaseTapChangerDslData.getPtcUpperTapChangerList();
    }

    public final Integer getPtcUpperTapChanger(String id) {
        return phaseTapChangerDslData.getPtcUpperTapChanger(id);
    }

    public void addPtc(String id, MetrixPtcControlType type, List<String> contingencies) {
        phaseTapChangerDslData.addPtc(id, type, contingencies);
    }

    public void addLowerTapChanger(String id, Integer preventiveLowerTapChange) {
        phaseTapChangerDslData.addLowerTapChanger(id, preventiveLowerTapChange);
    }

    public void addUpperTapChanger(String id, Integer preventiveUpperTapChange) {
        phaseTapChangerDslData.addUpperTapChanger(id, preventiveUpperTapChange);
    }

    public void addPstAngleTapResults(String id) {
        phaseTapChangerDslData.addPstAngleTapResults(id);
    }

    // Hvdc
    @JsonIgnore
    public final Set<String> getHvdcContingenciesList() {
        return hvdcDslData.getHvdcContingenciesList();
    }

    public final List<String> getHvdcContingencies(String id) {
        return hvdcDslData.getHvdcContingencies(id);
    }

    @JsonIgnore
    public final Set<String> getHvdcControlList() {
        return hvdcDslData.getHvdcControlList();
    }

    public final MetrixHvdcControlType getHvdcControl(String id) {
        return hvdcDslData.getHvdcControl(id);
    }

    public void addHvdc(String id, MetrixHvdcControlType type, List<String> contingencies) {
        hvdcDslData.addHvdc(id, type, contingencies);
    }

    public void addHvdcFlowResults(String id) {
        hvdcDslData.addHvdcFlowResults(id);
    }

    // Section monitoring
    public void addSection(MetrixSection section) {
        sectionDslData.addSection(section);
    }

    // Generator costs
    public void addGeneratorForAdequacy(String id) {
        generatorDslData.addGeneratorForAdequacy(id);
    }

    public void addGeneratorForRedispatching(String id, List<String> contingencies) {
        generatorDslData.addGeneratorForRedispatching(id, contingencies);
    }

    // Generator contingencies
    @JsonIgnore
    public final Set<String> getGeneratorContingenciesList() {
        return generatorDslData.getGeneratorContingenciesList();
    }

    public final List<String> getGeneratorContingencies(String id) {
        return generatorDslData.getGeneratorContingencies(id);
    }

    // Load shedding (preventive and curative)
    public void addPreventiveLoad(String id, int percentage) {
        loadDslData.addPreventiveLoad(id, percentage);
    }

    public void addPreventiveLoadCost(String id, float cost) {
        loadDslData.addPreventiveLoadCost(id, cost);
    }

    @JsonIgnore
    public final Set<String> getPreventiveLoadsList() {
        return loadDslData.getPreventiveLoadsList();
    }

    @JsonIgnore
    public final Set<String> getCurativeLoadsList() {
        return loadDslData.getCurativeLoadsList();
    }

    public final Integer getPreventiveLoadPercentage(String id) {
        return loadDslData.getPreventiveLoadPercentage(id);
    }

    public final Float getPreventiveLoadCost(String id) {
        return loadDslData.getPreventiveLoadCost(id);
    }

    public final Integer getCurativeLoadPercentage(String id) {
        return loadDslData.getCurativeLoadPercentage(id);
    }

    public void addCurativeLoad(String id, Integer percentage, List<String> contingencies) {
        loadDslData.addCurativeLoad(id, percentage, contingencies);
    }

    // Contingencies with load shedding capabilities
    public final List<String> getLoadContingencies(String id) {
        return loadDslData.getLoadContingencies(id);
    }

    // Bound variables
    public void addGeneratorsBinding(String id, Collection<String> generatorsIds, MetrixGeneratorsBinding.ReferenceVariable referenceVariable) {
        boundVariablesDslData.addGeneratorsBinding(id, generatorsIds, referenceVariable);
    }

    public void addGeneratorsBinding(String id, Collection<String> generatorsIds) {
        boundVariablesDslData.addGeneratorsBinding(id, generatorsIds);
    }

    @JsonIgnore
    public Collection<MetrixGeneratorsBinding> getGeneratorsBindingsValues() {
        return boundVariablesDslData.getGeneratorsBindingsValues();
    }

    public void addLoadsBinding(String id, Collection<String> loadsIds) {
        boundVariablesDslData.addLoadsBinding(id, loadsIds);
    }

    @JsonIgnore
    public Collection<MetrixLoadsBinding> getLoadsBindingsValues() {
        return boundVariablesDslData.getLoadsBindingsValues();
    }

    // Specific contingencies list
    public void setSpecificContingenciesList(List<String> specificContingenciesList) {
        this.specificContingenciesList.addAll(specificContingenciesList);
    }

    /**
     * Estimation of the minimum number of result time-series for this metrix configuration
     */
    public int minResultNumberEstimate(MetrixParameters parameters) {
        int nbTimeSeries = 0;
        int nbBranchN = getBranchMonitoringNList().size() + getSectionList().size();
        int nbBranchNk = getBranchMonitoringNkList().size();
        if (parameters.isOverloadResultsOnly().orElse(false)) {
            nbBranchN = (int) Math.round(nbBranchN * OVERLOAD_RATIO);
            nbBranchNk = (int) Math.round(nbBranchNk * OVERLOAD_RATIO);
        }
        nbTimeSeries += nbBranchN;
        nbTimeSeries += 2 * nbBranchNk * parameters.getOptionalNbThreatResults().orElse(1);
        nbTimeSeries += 2 * nbBranchNk * parameters.isPreCurativeResults().map(isPreCurative -> isPreCurative ? 1 : 0).orElse(0);
        nbTimeSeries += getContingencyFlowResultList().stream().mapToInt(s -> getContingencyFlowResult(s).size()).sum();
        return nbTimeSeries;
    }

    @JsonIgnore
    public MetrixComputationType getComputationType() {
        return computationType;
    }

    public void setComputationType(MetrixComputationType computationType) {
        this.computationType = computationType;
    }
}
