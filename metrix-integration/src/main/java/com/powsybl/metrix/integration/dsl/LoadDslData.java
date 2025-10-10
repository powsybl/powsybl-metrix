package com.powsybl.metrix.integration.dsl;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
public class LoadDslData extends AbstractDslData {

    // Load shedding
    private final Map<String, Integer> loadPreventivePercentageMap;
    private final Map<String, Float> loadPreventiveCostsMap;
    private final Map<String, Integer> loadCurativePercentageMap;
    private final Map<String, List<String>> loadContingenciesMap;

    public LoadDslData() {
        this(new HashMap<>(), new HashMap<>(), new LinkedHashMap<>(), new HashMap<>());
    }

    public LoadDslData(Map<String, Integer> loadPreventivePercentageMap,
                       Map<String, Float> loadPreventiveCostsMap,
                       Map<String, Integer> loadCurativePercentageMap,
                       Map<String, List<String>> loadContingenciesMap) {
        this.loadPreventivePercentageMap = loadPreventivePercentageMap;
        this.loadPreventiveCostsMap = loadPreventiveCostsMap;
        this.loadCurativePercentageMap = loadCurativePercentageMap;
        this.loadContingenciesMap = loadContingenciesMap;
    }

    @Override
    protected LinkedHashMap<String, Object> getMapElements() {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("loadPreventivePercentageMap", loadPreventivePercentageMap);
        map.put("loadPreventiveCostsMap", loadPreventiveCostsMap);
        map.put("loadCurativePercentageMap", loadCurativePercentageMap);
        map.put("loadContingenciesMap", loadContingenciesMap);
        return map;
    }

    // Getters
    public Map<String, Integer> getLoadPreventivePercentageMap() {
        return loadPreventivePercentageMap;
    }

    public Map<String, Float> getLoadPreventiveCostsMap() {
        return loadPreventiveCostsMap;
    }

    public Map<String, Integer> getLoadCurativePercentageMap() {
        return loadCurativePercentageMap;
    }

    public Map<String, List<String>> getLoadContingenciesMap() {
        return loadContingenciesMap;
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

    @Override
    public int hashCode() {
        return Objects.hash(loadPreventivePercentageMap,
            loadPreventiveCostsMap,
            loadCurativePercentageMap,
            loadContingenciesMap);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof LoadDslData other) {
            return loadPreventivePercentageMap.equals(other.loadPreventivePercentageMap) &&
                loadPreventiveCostsMap.equals(other.loadPreventiveCostsMap) &&
                loadCurativePercentageMap.equals(other.loadCurativePercentageMap) &&
                loadContingenciesMap.equals(other.loadContingenciesMap);
        }
        return false;
    }
}
