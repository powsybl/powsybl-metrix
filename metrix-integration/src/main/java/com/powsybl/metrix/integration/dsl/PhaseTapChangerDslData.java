package com.powsybl.metrix.integration.dsl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.powsybl.metrix.integration.type.MetrixPtcControlType;

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
public class PhaseTapChangerDslData extends AbstractDslData {

    // PhaseTapChanger
    private final Map<String, List<String>> ptcContingenciesMap;
    private final Map<String, MetrixPtcControlType> ptcControlMap;
    private final Map<String, Integer> ptcLowerTapchangeMap;
    private final Map<String, Integer> ptcUpperTapchangeMap;
    private final Set<String> pstAngleTapResults;

    public PhaseTapChangerDslData() {
        this(new HashMap<>(), new HashMap<>(), new HashSet<>(), new HashMap<>(), new HashMap<>());
    }

    public PhaseTapChangerDslData(Map<String, List<String>> ptcContingenciesMap,
                                  Map<String, MetrixPtcControlType> ptcControlMap,
                                  Set<String> pstAngleTapResults,
                                  Map<String, Integer> ptcLowerTapchangeMap,
                                  Map<String, Integer> ptcUpperTapchangeMap) {
        this.ptcContingenciesMap = ptcContingenciesMap;
        this.ptcControlMap = ptcControlMap;
        this.pstAngleTapResults = pstAngleTapResults;
        this.ptcLowerTapchangeMap = ptcLowerTapchangeMap;
        this.ptcUpperTapchangeMap = ptcUpperTapchangeMap;
    }

    @Override
    protected LinkedHashMap<String, Object> getMapElements() {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("ptcContingenciesMap", ptcContingenciesMap);
        map.put("ptcControlMap", ptcControlMap);
        map.put("ptcLowerTapchangeMap", ptcLowerTapchangeMap);
        map.put("ptcUpperTapchangeMap", ptcUpperTapchangeMap);
        map.put("pstAngleTapResults", pstAngleTapResults);
        return map;
    }

    // Getters
    public Map<String, List<String>> getPtcContingenciesMap() {
        return ptcContingenciesMap;
    }

    public Map<String, MetrixPtcControlType> getPtcControlMap() {
        return ptcControlMap;
    }

    public Map<String, Integer> getPtcLowerTapchangeMap() {
        return ptcLowerTapchangeMap;
    }

    public Map<String, Integer> getPtcUpperTapchangeMap() {
        return ptcUpperTapchangeMap;
    }

    public Set<String> getPstAngleTapResults() {
        return pstAngleTapResults;
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

    @Override
    public int hashCode() {
        return Objects.hash(
            ptcContingenciesMap,
            ptcControlMap,
            ptcLowerTapchangeMap,
            ptcUpperTapchangeMap,
            pstAngleTapResults);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PhaseTapChangerDslData other) {
            return ptcContingenciesMap.equals(other.ptcContingenciesMap) &&
                ptcControlMap.equals(other.ptcControlMap) &&
                ptcLowerTapchangeMap.equals(other.ptcLowerTapchangeMap) &&
                ptcUpperTapchangeMap.equals(other.ptcUpperTapchangeMap) &&
                pstAngleTapResults.equals(other.pstAngleTapResults);
        }
        return false;
    }
}
