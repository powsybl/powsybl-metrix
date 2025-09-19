package com.powsybl.metrix.integration.dsl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.powsybl.metrix.integration.type.MetrixHvdcControlType;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Nicolas Rol {@literal <nicolas.rol at rte-france.com>}
 */
public class HvdcDslData extends AbstractDslData {

    private final Map<String, List<String>> hvdcContingenciesMap;
    private final Map<String, MetrixHvdcControlType> hvdcControlMap;
    private final Set<String> hvdcFlowResults;

    public HvdcDslData() {
        this(new HashMap<>(), new HashMap<>(), new HashSet<>());
    }

    public HvdcDslData(Map<String, List<String>> hvdcContingenciesMap,
                      Map<String, MetrixHvdcControlType> hvdcControlMap,
                      Set<String> hvdcFlowResults) {
        this.hvdcContingenciesMap = hvdcContingenciesMap;
        this.hvdcControlMap = hvdcControlMap;
        this.hvdcFlowResults = hvdcFlowResults;
    }

    protected LinkedHashMap<String, Object> getMapElements() {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("hvdcContingenciesMap", hvdcContingenciesMap);
        map.put("hvdcControlMap", hvdcControlMap);
        map.put("hvdcFlowResults", hvdcFlowResults);
        return map;
    }

    // Getters
    public Map<String, List<String>> getHvdcContingenciesMap() {
        return hvdcContingenciesMap;
    }

    public Map<String, MetrixHvdcControlType> getHvdcControlMap() {
        return hvdcControlMap;
    }

    public Set<String> getHvdcFlowResults() {
        return hvdcFlowResults;
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

    @Override
    public int hashCode() {
        return Objects.hash(hvdcContingenciesMap,
            hvdcControlMap,
            hvdcFlowResults);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof HvdcDslData other) {
            return hvdcContingenciesMap.equals(other.hvdcContingenciesMap) &&
                hvdcControlMap.equals(other.hvdcControlMap) &&
                hvdcFlowResults.equals(other.hvdcFlowResults);
        }
        return false;
    }
}
