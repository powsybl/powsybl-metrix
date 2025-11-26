package com.powsybl.metrix.integration.dsl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.powsybl.metrix.integration.MetrixVariable;
import com.powsybl.metrix.integration.dataGenerator.MetrixInputData;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author Nicolas Rol {@literal <nicolas.rol at rte-france.com>}
 */
public class BranchDslData extends AbstractDslData {

    // Branch monitoring
    private final Map<String, MetrixInputData.MonitoringType> branchMonitoringListN;
    private final Map<String, MetrixInputData.MonitoringType> branchMonitoringListNk;
    private final Map<String, MetrixVariable> branchMonitoringStatisticsThresholdN;
    private final Map<String, MetrixVariable> branchMonitoringStatisticsThresholdNk;
    private final Map<String, List<String>> contingencyFlowResults;
    private final Map<String, List<String>> contingencyDetailedMarginalVariations;

    public BranchDslData() {
        this(new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>());
    }

    public BranchDslData(Map<String, MetrixInputData.MonitoringType> branchMonitoringListN,
                         Map<String, MetrixInputData.MonitoringType> branchMonitoringListNk,
                         Map<String, MetrixVariable> branchMonitoringStatisticsThresholdN,
                         Map<String, MetrixVariable> branchMonitoringStatisticsThresholdNk,
                         Map<String, List<String>> contingencyFlowResults,
                         Map<String, List<String>> contingencyDetailedMarginalVariations) {
        this.branchMonitoringListN = branchMonitoringListN;
        this.branchMonitoringListNk = branchMonitoringListNk;
        this.branchMonitoringStatisticsThresholdN = branchMonitoringStatisticsThresholdN;
        this.branchMonitoringStatisticsThresholdNk = branchMonitoringStatisticsThresholdNk;
        this.contingencyFlowResults = contingencyFlowResults;
        this.contingencyDetailedMarginalVariations = contingencyDetailedMarginalVariations;
    }

    @Override
    protected LinkedHashMap<String, Object> getMapElements() {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("branchMonitoringListN", branchMonitoringListN);
        map.put("branchMonitoringListNk", branchMonitoringListNk);
        map.put("branchMonitoringStatisticsThresholdN", branchMonitoringStatisticsThresholdN);
        map.put("branchMonitoringStatisticsThresholdNk", branchMonitoringStatisticsThresholdNk);
        map.put("contingencyFlowResults", contingencyFlowResults);
        map.put("contingencyDetailedMarginalVariations", contingencyDetailedMarginalVariations);
        return map;
    }

    // Getters
    public Map<String, MetrixInputData.MonitoringType> getBranchMonitoringListN() {
        return branchMonitoringListN;
    }

    public Map<String, MetrixInputData.MonitoringType> getBranchMonitoringListNk() {
        return branchMonitoringListNk;
    }

    public Map<String, MetrixVariable> getBranchMonitoringStatisticsThresholdN() {
        return branchMonitoringStatisticsThresholdN;
    }

    public Map<String, MetrixVariable> getBranchMonitoringStatisticsThresholdNk() {
        return branchMonitoringStatisticsThresholdNk;
    }

    public Map<String, List<String>> getContingencyFlowResults() {
        return contingencyFlowResults;
    }

    public Map<String, List<String>> getContingencyDetailedMarginalVariations() {
        return contingencyDetailedMarginalVariations;
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
        branchMonitoringStatisticsThresholdN.put(id, MetrixVariable.THRESHOLD_N);
        branchMonitoringListN.put(id, MetrixInputData.MonitoringType.MONITORING);
    }

    public void addBranchResultN(String id) {
        Objects.requireNonNull(id);
        branchMonitoringStatisticsThresholdN.put(id, MetrixVariable.ANALYSIS_THRESHOLD_N);
        if (getBranchMonitoringN(id) == MetrixInputData.MonitoringType.MONITORING) {
            return;
        }
        branchMonitoringListN.put(id, MetrixInputData.MonitoringType.RESULT);
    }

    public void addBranchMonitoringNk(String id) {
        Objects.requireNonNull(id);
        branchMonitoringStatisticsThresholdNk.put(id, MetrixVariable.THRESHOLD_N1);
        branchMonitoringListNk.put(id, MetrixInputData.MonitoringType.MONITORING);
    }

    public void addBranchResultNk(String id) {
        Objects.requireNonNull(id);
        branchMonitoringStatisticsThresholdNk.put(id, MetrixVariable.ANALYSIS_THRESHOLD_NK);
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

    @Override
    public int hashCode() {
        return Objects.hash(branchMonitoringListN,
            branchMonitoringListNk,
            branchMonitoringStatisticsThresholdN,
            branchMonitoringStatisticsThresholdNk,
            contingencyFlowResults,
            contingencyDetailedMarginalVariations);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BranchDslData other) {
            return branchMonitoringListN.equals(other.branchMonitoringListN) &&
                branchMonitoringListNk.equals(other.branchMonitoringListNk) &&
                branchMonitoringStatisticsThresholdN.equals(other.branchMonitoringStatisticsThresholdN) &&
                branchMonitoringStatisticsThresholdNk.equals(other.branchMonitoringStatisticsThresholdNk) &&
                contingencyFlowResults.equals(other.contingencyFlowResults) &&
                contingencyDetailedMarginalVariations.equals(other.contingencyDetailedMarginalVariations);
        }
        return false;
    }
}
