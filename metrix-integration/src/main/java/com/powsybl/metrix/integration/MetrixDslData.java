/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.integration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ImmutableMap;
import com.powsybl.metrix.integration.dataGenerator.MetrixInputData;

import java.util.*;
import java.util.stream.Collectors;

public class MetrixDslData {

    private static final double OVERLOAD_RATIO = .3; // Assume 30% of monitored branches get overloaded at some point

    // Branch monitoring
    private final Map<String, MetrixInputData.MonitoringType> branchMonitoringListN;
    private final Map<String, MetrixInputData.MonitoringType> branchMonitoringListNk;
    private final Map<String, MetrixVariable> branchMonitoringStatisticsThresholdN;
    private final Map<String, MetrixVariable> branchMonitoringStatisticsThresholdNk;
    private final Map<String, List<String>> contingencyFlowResults;
    private final Map<String, List<String>> contingencyDetailedMarginalVariations;

    // PhaseTapChanger
    private final Map<String, List<String>> ptcContingenciesMap;
    private final Map<String, MetrixPtcControlType> ptcControlMap;
    private final Map<String, Integer> ptcLowerTapchangeMap;
    private final Map<String, Integer> ptcUpperTapchangeMap;
    private final Set<String> pstAngleTapResults;

    // Hvdc
    private final Map<String, List<String>> hvdcContingenciesMap;
    private final Map<String, MetrixHvdcControlType> hvdcControlMap;
    private final Set<String> hvdcFlowResults;

    // Section monitoring
    private final Set<MetrixSection> sectionList;

    // Generator
    private final Set<String> generatorsForAdequacy;
    private final Set<String> generatorsForRedispatching;
    private final Map<String, List<String>> generatorContingenciesMap;

    // Load shedding
    private final Map<String, Integer> loadPreventivePercentageMap;
    private final Map<String, Float> loadPreventiveCostsMap;
    private final Map<String, Integer> loadCurativePercentageMap;
    private final Map<String, List<String>> loadContingenciesMap;

    // Bound variables
    private final Map<String, MetrixGeneratorsBinding> generatorsBindings;
    private final Map<String, MetrixLoadsBinding> loadsBindings;

    private final Set<String> specificContingenciesList;

    private MetrixComputationType computationType;

    public MetrixDslData() {
        branchMonitoringListN = new HashMap<>();
        branchMonitoringListNk = new HashMap<>();
        branchMonitoringStatisticsThresholdN = new HashMap<>();
        branchMonitoringStatisticsThresholdNk = new HashMap<>();
        contingencyFlowResults = new LinkedHashMap<>();
        contingencyDetailedMarginalVariations = new LinkedHashMap<>();
        ptcContingenciesMap = new HashMap<>();
        ptcControlMap = new HashMap<>();
        ptcLowerTapchangeMap = new HashMap<>();
        ptcUpperTapchangeMap = new HashMap<>();
        pstAngleTapResults = new HashSet<>();
        hvdcContingenciesMap = new HashMap<>();
        hvdcControlMap = new HashMap<>();
        hvdcFlowResults = new HashSet<>();
        sectionList = new HashSet<>();
        generatorsForAdequacy = new HashSet<>();
        generatorsForRedispatching = new HashSet<>();
        generatorContingenciesMap = new HashMap<>();
        loadPreventivePercentageMap = new HashMap<>();
        loadPreventiveCostsMap = new HashMap<>();
        loadCurativePercentageMap = new LinkedHashMap<>();
        loadContingenciesMap = new HashMap<>();
        generatorsBindings = new HashMap<>();
        loadsBindings = new HashMap<>();
        specificContingenciesList = new HashSet<>();
    }

    public MetrixDslData(Map<String, MetrixInputData.MonitoringType> branchMonitoringListN,
                         Map<String, MetrixInputData.MonitoringType> branchMonitoringListNk,
                         Map<String, MetrixVariable> branchMonitoringStatisticsThresholdN,
                         Map<String, MetrixVariable> branchMonitoringStatisticsThresholdNk,
                         Map<String, List<String>> contingencyFlowResults,
                         Map<String, List<String>> contingencyDetailedMarginalVariations,
                         Map<String, List<String>> ptcContingenciesMap,
                         Map<String, MetrixPtcControlType> ptcControlMap,
                         Set<String> pstAngleTapResults,
                         Map<String, Integer> ptcLowerTapchangeMap,
                         Map<String, Integer> ptcUpperTapchangeMap,
                         Map<String, List<String>> hvdcContingenciesMap,
                         Map<String, MetrixHvdcControlType> hvdcControlMap,
                         Set<String> hvdcFlowResults,
                         Set<MetrixSection> sectionList,
                         Set<String> generatorsForAdequacy,
                         Set<String> generatorsForRedispatching,
                         Map<String, List<String>> generatorContingenciesMap,
                         Map<String, Integer> loadPreventivePercentageMap,
                         Map<String, Float> loadPreventiveCostsMap,
                         Map<String, Integer> loadCurativePercentageMap,
                         Map<String, List<String>> loadContingenciesMap,
                         Map<String, MetrixGeneratorsBinding> generatorsBindings,
                         Map<String, MetrixLoadsBinding> loadsBindings,
                         Set<String> specificContingenciesList) {
        this.branchMonitoringListN = branchMonitoringListN;
        this.branchMonitoringListNk = branchMonitoringListNk;
        this.branchMonitoringStatisticsThresholdN = branchMonitoringStatisticsThresholdN;
        this.branchMonitoringStatisticsThresholdNk = branchMonitoringStatisticsThresholdNk;
        this.contingencyFlowResults = contingencyFlowResults;
        this.contingencyDetailedMarginalVariations = contingencyDetailedMarginalVariations;
        this.ptcContingenciesMap = ptcContingenciesMap;
        this.ptcControlMap = ptcControlMap;
        this.ptcLowerTapchangeMap = ptcLowerTapchangeMap;
        this.ptcUpperTapchangeMap = ptcUpperTapchangeMap;
        this.pstAngleTapResults = pstAngleTapResults;
        this.hvdcContingenciesMap = hvdcContingenciesMap;
        this.hvdcControlMap = hvdcControlMap;
        this.hvdcFlowResults = hvdcFlowResults;
        this.sectionList = sectionList;
        this.generatorsForAdequacy = generatorsForAdequacy;
        this.generatorsForRedispatching = generatorsForRedispatching;
        this.generatorContingenciesMap = generatorContingenciesMap;
        this.loadPreventivePercentageMap = loadPreventivePercentageMap;
        this.loadPreventiveCostsMap = loadPreventiveCostsMap;
        this.loadCurativePercentageMap = loadCurativePercentageMap;
        this.loadContingenciesMap = loadContingenciesMap;
        this.generatorsBindings = generatorsBindings;
        this.loadsBindings = loadsBindings;
        this.specificContingenciesList = specificContingenciesList;
    }

    @Override
    public int hashCode() {
        return Objects.hash(branchMonitoringListN,
                branchMonitoringListNk,
                branchMonitoringStatisticsThresholdN,
                branchMonitoringStatisticsThresholdNk,
                contingencyFlowResults,
                contingencyDetailedMarginalVariations,
                ptcContingenciesMap,
                ptcControlMap,
                ptcLowerTapchangeMap,
                ptcUpperTapchangeMap,
                pstAngleTapResults,
                hvdcContingenciesMap,
                hvdcControlMap,
                hvdcFlowResults,
                sectionList,
                generatorsForAdequacy,
                generatorsForRedispatching,
                generatorContingenciesMap,
                loadPreventivePercentageMap,
                loadPreventiveCostsMap,
                loadCurativePercentageMap,
                loadContingenciesMap,
                generatorsBindings,
                loadsBindings,
                specificContingenciesList);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MetrixDslData other) {
            return branchMonitoringListN.equals(other.branchMonitoringListN) &&
                    branchMonitoringListNk.equals(other.branchMonitoringListNk) &&
                    branchMonitoringStatisticsThresholdN.equals(other.branchMonitoringStatisticsThresholdN) &&
                    branchMonitoringStatisticsThresholdNk.equals(other.branchMonitoringStatisticsThresholdNk) &&
                    contingencyFlowResults.equals(other.contingencyFlowResults) &&
                    contingencyDetailedMarginalVariations.equals(other.contingencyDetailedMarginalVariations) &&
                    ptcContingenciesMap.equals(other.ptcContingenciesMap) &&
                    ptcControlMap.equals(other.ptcControlMap) &&
                    ptcLowerTapchangeMap.equals(other.ptcLowerTapchangeMap) &&
                    ptcUpperTapchangeMap.equals(other.ptcUpperTapchangeMap) &&
                    pstAngleTapResults.equals(other.pstAngleTapResults) &&
                    hvdcContingenciesMap.equals(other.hvdcContingenciesMap) &&
                    hvdcControlMap.equals(other.hvdcControlMap) &&
                    hvdcFlowResults.equals(other.hvdcFlowResults) &&
                    sectionList.equals(other.sectionList) &&
                    generatorsForAdequacy.equals(other.generatorsForAdequacy) &&
                    generatorsForRedispatching.equals(other.generatorsForRedispatching) &&
                    generatorContingenciesMap.equals(other.generatorContingenciesMap) &&
                    loadPreventivePercentageMap.equals(other.loadPreventivePercentageMap) &&
                    loadPreventiveCostsMap.equals(other.loadPreventiveCostsMap) &&
                    loadCurativePercentageMap.equals(other.loadCurativePercentageMap) &&
                    loadContingenciesMap.equals(other.loadContingenciesMap) &&
                    generatorsBindings.equals(other.generatorsBindings) &&
                    loadsBindings.equals(other.loadsBindings) &&
                    specificContingenciesList.equals(other.specificContingenciesList);
        }
        return false;
    }

    @Override
    public String toString() {
        ImmutableMap.Builder<String, Object> builder = ImmutableMap.<String, Object>builder()
                .put("branchMonitoringListN", branchMonitoringListN)
                .put("branchMonitoringListNk", branchMonitoringListNk)
                .put("branchMonitoringStatisticsThresholdN", branchMonitoringStatisticsThresholdN)
                .put("branchMonitoringStatisticsThresholdNk", branchMonitoringStatisticsThresholdNk)
                .put("contingencyFlowResults", contingencyFlowResults)
                .put("contingencyDetailedMarginalVariations", contingencyDetailedMarginalVariations)
                .put("ptcContingenciesMap", ptcContingenciesMap)
                .put("ptcControlMap", ptcControlMap)
                .put("ptcLowerTapchangeMap", ptcLowerTapchangeMap)
                .put("ptcUpperTapchangeMap", ptcUpperTapchangeMap)
                .put("pstAngleTapResults", pstAngleTapResults)
                .put("hvdcContingenciesMap", hvdcContingenciesMap)
                .put("hvdcControlMap", hvdcControlMap)
                .put("hvdcFlowResults", hvdcFlowResults)
                .put("sectionList", sectionList)
                .put("generatorsForAdequacy", generatorsForAdequacy)
                .put("generatorsForRedispatching", generatorsForRedispatching)
                .put("generatorContingenciesMap", generatorContingenciesMap)
                .put("loadPreventivePercentageMap", loadPreventivePercentageMap)
                .put("loadPreventiveCostsMap", loadPreventiveCostsMap)
                .put("loadCurativePercentageMap", loadCurativePercentageMap)
                .put("loadContingenciesMap", loadContingenciesMap)
                .put("generatorsBindings", generatorsBindings)
                .put("loadsBindings", loadsBindings)
                .put("specificContingenciesList", specificContingenciesList);
        return builder.build().toString();
    }

    // Getters
    public Map<String, MetrixInputData.MonitoringType> getBranchMonitoringListN() {
        return Collections.unmodifiableMap(branchMonitoringListN);
    }

    public Map<String, MetrixInputData.MonitoringType> getBranchMonitoringListNk() {
        return Collections.unmodifiableMap(branchMonitoringListNk);
    }

    public Map<String, MetrixVariable> getBranchMonitoringStatisticsThresholdN() {
        return Collections.unmodifiableMap(branchMonitoringStatisticsThresholdN);
    }

    public Map<String, MetrixVariable> getBranchMonitoringStatisticsThresholdNk() {
        return Collections.unmodifiableMap(branchMonitoringStatisticsThresholdNk);
    }

    public Map<String, List<String>> getContingencyFlowResults() {
        return Collections.unmodifiableMap(contingencyFlowResults);
    }

    public Map<String, List<String>> getContingencyDetailedMarginalVariations() {
        return Collections.unmodifiableMap(contingencyDetailedMarginalVariations);
    }

    public Map<String, List<String>> getPtcContingenciesMap() {
        return Collections.unmodifiableMap(ptcContingenciesMap);
    }

    public Map<String, MetrixPtcControlType> getPtcControlMap() {
        return Collections.unmodifiableMap(ptcControlMap);
    }

    public Map<String, Integer> getPtcLowerTapchangeMap() {
        return Collections.unmodifiableMap(ptcLowerTapchangeMap);
    }

    public Map<String, Integer> getPtcUpperTapchangeMap() {
        return Collections.unmodifiableMap(ptcUpperTapchangeMap);
    }

    public Set<String> getPstAngleTapResults() {
        return Collections.unmodifiableSet(pstAngleTapResults);
    }

    public Map<String, List<String>> getHvdcContingenciesMap() {
        return Collections.unmodifiableMap(hvdcContingenciesMap);
    }

    public Map<String, MetrixHvdcControlType> getHvdcControlMap() {
        return Collections.unmodifiableMap(hvdcControlMap);
    }

    public Set<String> getHvdcFlowResults() {
        return Collections.unmodifiableSet(hvdcFlowResults);
    }

    public Set<MetrixSection> getSectionList() {
        return Collections.unmodifiableSet(sectionList);
    }

    public Set<String> getGeneratorsForAdequacy() {
        return Collections.unmodifiableSet(generatorsForAdequacy);
    }

    public Set<String> getGeneratorsForRedispatching() {
        return Collections.unmodifiableSet(generatorsForRedispatching);
    }

    public Map<String, List<String>> getGeneratorContingenciesMap() {
        return Collections.unmodifiableMap(generatorContingenciesMap);
    }

    public Map<String, Integer> getLoadPreventivePercentageMap() {
        return Collections.unmodifiableMap(loadPreventivePercentageMap);
    }

    public Map<String, Float> getLoadPreventiveCostsMap() {
        return Collections.unmodifiableMap(loadPreventiveCostsMap);
    }

    public Map<String, Integer> getLoadCurativePercentageMap() {
        return Collections.unmodifiableMap(loadCurativePercentageMap);
    }

    public Map<String, List<String>> getLoadContingenciesMap() {
        return Collections.unmodifiableMap(loadContingenciesMap);
    }

    public Map<String, MetrixGeneratorsBinding> getGeneratorsBindings() {
        return Collections.unmodifiableMap(generatorsBindings);
    }

    public Map<String, MetrixLoadsBinding> getLoadsBindings() {
        return Collections.unmodifiableMap(loadsBindings);
    }

    public Set<String> getSpecificContingenciesList() {
        return Collections.unmodifiableSet(specificContingenciesList);
    }

    // Branch monitoring
    public final MetrixInputData.MonitoringType getBranchMonitoringN(String id) {
        return branchMonitoringListN.getOrDefault(id, MetrixInputData.MonitoringType.NO);
    }

    @JsonIgnore
    public final Set<String> getBranchMonitoringNList() {
        return branchMonitoringListN.keySet();
    }

    public final MetrixInputData.MonitoringType getBranchMonitoringNk(String id) {
        return branchMonitoringListNk.getOrDefault(id, MetrixInputData.MonitoringType.NO);
    }

    public final MetrixVariable getBranchMonitoringStatisticsThresholdN(String id) {
        return branchMonitoringStatisticsThresholdN.get(id);
    }

    @JsonIgnore
    public final Set<String> getBranchMonitoringNkList() {
        return branchMonitoringListNk.keySet();
    }

    public final MetrixVariable getBranchMonitoringStatisticsThresholdNk(String id) {
        return branchMonitoringStatisticsThresholdNk.get(id);
    }

    @JsonIgnore
    public final Set<String> getContingencyFlowResultList() {
        return contingencyFlowResults.keySet();
    }

    public final List<String> getContingencyFlowResult(String id) {
        if (contingencyFlowResults.containsKey(id)) {
            return Collections.unmodifiableList(contingencyFlowResults.get(id));
        }
        return Collections.emptyList();
    }

    @JsonIgnore
    public final Set<String> getContingencyDetailedMarginalVariationsList() {
        return contingencyDetailedMarginalVariations.keySet();
    }

    public final List<String> getContingencyDetailedMarginalVariations(String id) {
        if (contingencyDetailedMarginalVariations.containsKey(id)) {
            return Collections.unmodifiableList(contingencyDetailedMarginalVariations.get(id));
        }
        return Collections.emptyList();
    }

    public void addBranchMonitoringN(String id) {
        Objects.requireNonNull(id);
        branchMonitoringStatisticsThresholdN.put(id, MetrixVariable.thresholdN);
        branchMonitoringListN.put(id, MetrixInputData.MonitoringType.MONITORING);
    }

    public void addBranchResultN(String id) {
        Objects.requireNonNull(id);
        branchMonitoringStatisticsThresholdN.put(id, MetrixVariable.analysisThresholdN);
        if (getBranchMonitoringN(id) == MetrixInputData.MonitoringType.MONITORING) {
            return;
        }
        branchMonitoringListN.put(id, MetrixInputData.MonitoringType.RESULT);
    }

    public void addBranchMonitoringNk(String id) {
        Objects.requireNonNull(id);
        branchMonitoringStatisticsThresholdNk.put(id, MetrixVariable.thresholdN1);
        branchMonitoringListNk.put(id, MetrixInputData.MonitoringType.MONITORING);
    }

    public void addBranchResultNk(String id) {
        Objects.requireNonNull(id);
        branchMonitoringStatisticsThresholdNk.put(id, MetrixVariable.analysisThresholdNk);
        if (getBranchMonitoringNk(id) == MetrixInputData.MonitoringType.MONITORING) {
            return;
        }
        branchMonitoringListNk.put(id, MetrixInputData.MonitoringType.RESULT);
    }

    public void addContingencyFlowResults(String id, List<String> contingencies) {
        Objects.requireNonNull(id);
        if (!Objects.isNull(contingencies) && !contingencies.isEmpty()) {
            contingencyFlowResults.put(id, contingencies);
        }
    }

    public void addContingencyDetailedMarginalVariations(String id, List<String> contingencies) {
        Objects.requireNonNull(id);
        if (!Objects.isNull(contingencies) && !contingencies.isEmpty()) {
            contingencyDetailedMarginalVariations.put(id, contingencies);
        }
    }

    // PhaseTapChanger
    @JsonIgnore
    public final Set<String> getPtcContingenciesList() {
        return ptcContingenciesMap.keySet();
    }

    public final List<String> getPtcContingencies(String id) {
        if (ptcContingenciesMap.containsKey(id)) {
            return Collections.unmodifiableList(ptcContingenciesMap.get(id));
        }
        return Collections.emptyList();
    }

    @JsonIgnore
    public final Set<String> getPtcControlList() {
        return ptcControlMap.keySet()
                            .stream()
                            .filter(x -> ptcControlMap.get(x) == MetrixPtcControlType.OPTIMIZED_ANGLE_CONTROL)
                            .collect(Collectors.toSet());
    }

    public final MetrixPtcControlType getPtcControl(String id) {
        if (ptcControlMap.containsKey(id)) {
            return ptcControlMap.get(id);
        }
        return MetrixPtcControlType.FIXED_ANGLE_CONTROL;
    }

    @JsonIgnore
    public final Set<String> getPtcLowerTapChangeList() {
        return ptcLowerTapchangeMap.keySet();
    }

    public final Integer getPtcLowerTapChange(String id) {
        return ptcLowerTapchangeMap.getOrDefault(id, null);
    }

    @JsonIgnore
    public final Set<String> getPtcUpperTapChangeList() {
        return ptcUpperTapchangeMap.keySet();
    }

    public final Integer getPtcUpperTapChange(String id) {
        return ptcUpperTapchangeMap.getOrDefault(id, null);
    }

    public void addPtc(String id, MetrixPtcControlType type, List<String> contingencies) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(type);
        ptcControlMap.put(id, type);
        if (!Objects.isNull(contingencies)) {
            ptcContingenciesMap.put(id, contingencies);
        }
    }

    public void addLowerTapChange(String id, Integer preventiveLowerTapChange) {
        Objects.requireNonNull(id);
        if (!Objects.isNull(preventiveLowerTapChange)) {
            ptcLowerTapchangeMap.put(id, preventiveLowerTapChange);
        }
    }

    public void addUpperTapChange(String id, Integer preventiveUpperTapChange) {
        Objects.requireNonNull(id);
        if (!Objects.isNull(preventiveUpperTapChange)) {
            ptcUpperTapchangeMap.put(id, preventiveUpperTapChange);
        }
    }

    public void addPstAngleTapResults(String id) {
        Objects.requireNonNull(id);
        pstAngleTapResults.add(id);
    }

    // Hvdc
    @JsonIgnore
    public final Set<String> getHvdcContingenciesList() {
        return hvdcContingenciesMap.keySet();
    }

    public final List<String> getHvdcContingencies(String id) {
        if (hvdcContingenciesMap.containsKey(id)) {
            return Collections.unmodifiableList(hvdcContingenciesMap.get(id));
        }
        return Collections.emptyList();
    }

    @JsonIgnore
    public final Set<String> getHvdcControlList() {
        return hvdcControlMap.keySet()
                             .stream()
                             .filter(x -> hvdcControlMap.get(x) == MetrixHvdcControlType.OPTIMIZED)
                             .collect(Collectors.toSet());
    }

    public final MetrixHvdcControlType getHvdcControl(String id) {
        if (hvdcControlMap.containsKey(id)) {
            return hvdcControlMap.get(id);
        }
        return MetrixHvdcControlType.FIXED;
    }

    public void addHvdc(String id, MetrixHvdcControlType type, List<String> contingencies) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(type);
        hvdcControlMap.put(id, type);
        if (!Objects.isNull(contingencies)) {
            hvdcContingenciesMap.put(id, contingencies);
        }
    }

    public void addHvdcFlowResults(String id) {
        Objects.requireNonNull(id);
        hvdcFlowResults.add(id);
    }

    // Section monitoring
    public void addSection(MetrixSection section) {
        Objects.requireNonNull(section);
        sectionList.add(section);
    }

    // Generator costs
    public void addGeneratorForAdequacy(String id) {
        Objects.requireNonNull(id);
        generatorsForAdequacy.add(id);
    }

    public void addGeneratorForRedispatching(String id, List<String> contingencies) {
        Objects.requireNonNull(id);
        generatorsForRedispatching.add(id);
        if (!Objects.isNull(contingencies)) {
            generatorContingenciesMap.put(id, contingencies);
        }
    }

    // Generator contingencies
    @JsonIgnore
    public final Set<String> getGeneratorContingenciesList() {
        return generatorContingenciesMap.keySet();
    }

    public final List<String> getGeneratorContingencies(String id) {
        if (generatorContingenciesMap.containsKey(id)) {
            return Collections.unmodifiableList(generatorContingenciesMap.get(id));
        }
        return Collections.emptyList();
    }

    // Load shedding (preventive and curative)
    public void addPreventiveLoad(String id, int percentage) {
        Objects.requireNonNull(id);
        loadPreventivePercentageMap.put(id, percentage);
    }

    public void addPreventiveLoadCost(String id, float cost) {
        Objects.requireNonNull(id);
        loadPreventiveCostsMap.put(id, cost);
    }

    @JsonIgnore
    public final Set<String> getPreventiveLoadsList() {
        return loadPreventivePercentageMap.keySet();
    }

    @JsonIgnore
    public final Set<String> getCurativeLoadsList() {
        return loadCurativePercentageMap.keySet();
    }

    public final Integer getPreventiveLoadPercentage(String id) {
        return loadPreventivePercentageMap.get(id);
    }

    public final Float getPreventiveLoadCost(String id) {
        return loadPreventiveCostsMap.get(id);
    }

    public final Integer getCurativeLoadPercentage(String id) {
        return loadCurativePercentageMap.get(id);
    }

    public void addCurativeLoad(String id, Integer percentage, List<String> contingencies) {
        Objects.requireNonNull(id);
        loadCurativePercentageMap.put(id, percentage);
        if (!Objects.isNull(contingencies)) {
            loadContingenciesMap.put(id, contingencies);
        }
    }

    // Contingencies with load shedding capabilities
    public final List<String> getLoadContingencies(String id) {
        if (loadContingenciesMap.containsKey(id)) {
            return Collections.unmodifiableList(loadContingenciesMap.get(id));
        }
        return Collections.emptyList();
    }

    // Bound variables
    public void addGeneratorsBinding(String id, Collection<String> generatorsIds, MetrixGeneratorsBinding.ReferenceVariable referenceVariable) {
        generatorsBindings.put(id, new MetrixGeneratorsBinding(id, generatorsIds, referenceVariable));
    }

    public void addGeneratorsBinding(String id, Collection<String> generatorsIds) {
        generatorsBindings.put(id, new MetrixGeneratorsBinding(id, generatorsIds));
    }

    @JsonIgnore
    public Collection<MetrixGeneratorsBinding> getGeneratorsBindingsValues() {
        return Collections.unmodifiableCollection(generatorsBindings.values());
    }

    public void addLoadsBinding(String id, Collection<String> loadsIds) {
        loadsBindings.put(id, new MetrixLoadsBinding(id, loadsIds));
    }

    @JsonIgnore
    public Collection<MetrixLoadsBinding> getLoadsBindingsValues() {
        return Collections.unmodifiableCollection(loadsBindings.values());
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
        nbTimeSeries += 2 * nbBranchNk * parameters.isPreCurativeResults().map(b -> Boolean.TRUE.equals(b) ? 1 : 0).orElse(0);
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
