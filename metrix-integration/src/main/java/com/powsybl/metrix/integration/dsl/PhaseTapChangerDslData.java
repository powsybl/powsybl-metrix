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
    private final Map<String, Integer> ptcLowerTapChangerMap;
    private final Map<String, Integer> ptcUpperTapChangerMap;
    private final Set<String> pstAngleTapResults;

    public PhaseTapChangerDslData() {
        this(new HashMap<>(), new HashMap<>(), new HashSet<>(), new HashMap<>(), new HashMap<>());
    }

    public PhaseTapChangerDslData(Map<String, List<String>> ptcContingenciesMap,
                                  Map<String, MetrixPtcControlType> ptcControlMap,
                                  Set<String> pstAngleTapResults,
                                  Map<String, Integer> ptcLowerTapChangerMap,
                                  Map<String, Integer> ptcUpperTapChangerMap) {
        this.ptcContingenciesMap = ptcContingenciesMap;
        this.ptcControlMap = ptcControlMap;
        this.pstAngleTapResults = pstAngleTapResults;
        this.ptcLowerTapChangerMap = ptcLowerTapChangerMap;
        this.ptcUpperTapChangerMap = ptcUpperTapChangerMap;
    }

    @Override
    protected LinkedHashMap<String, Object> getMapElements() {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("ptcContingenciesMap", ptcContingenciesMap);
        map.put("ptcControlMap", ptcControlMap);
        map.put("ptcLowerTapchangeMap", ptcLowerTapChangerMap);
        map.put("ptcUpperTapchangeMap", ptcUpperTapChangerMap);
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

    public Map<String, Integer> getPtcLowerTapChangerMap() {
        return ptcLowerTapChangerMap;
    }

    public Map<String, Integer> getPtcUpperTapChangerMap() {
        return ptcUpperTapChangerMap;
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
    public final Set<String> getPtcLowerTapChangerList() {
        return ptcLowerTapChangerMap.keySet();
    }

    public final Integer getPtcLowerTapChanger(String id) {
        return ptcLowerTapChangerMap.getOrDefault(id, null);
    }

    @JsonIgnore
    public final Set<String> getPtcUpperTapChangerList() {
        return ptcUpperTapChangerMap.keySet();
    }

    public final Integer getPtcUpperTapChanger(String id) {
        return ptcUpperTapChangerMap.getOrDefault(id, null);
    }

    public void addPtc(String id, MetrixPtcControlType type, List<String> contingencies) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(type);
        ptcControlMap.put(id, type);
        if (!Objects.isNull(contingencies)) {
            ptcContingenciesMap.put(id, contingencies);
        }
    }

    public void addLowerTapChanger(String id, Integer preventiveLowerTapChange) {
        Objects.requireNonNull(id);
        if (!Objects.isNull(preventiveLowerTapChange)) {
            ptcLowerTapChangerMap.put(id, preventiveLowerTapChange);
        }
    }

    public void addUpperTapChanger(String id, Integer preventiveUpperTapChange) {
        Objects.requireNonNull(id);
        if (!Objects.isNull(preventiveUpperTapChange)) {
            ptcUpperTapChangerMap.put(id, preventiveUpperTapChange);
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
            ptcLowerTapChangerMap,
            ptcUpperTapChangerMap,
            pstAngleTapResults);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PhaseTapChangerDslData other) {
            return ptcContingenciesMap.equals(other.ptcContingenciesMap) &&
                ptcControlMap.equals(other.ptcControlMap) &&
                ptcLowerTapChangerMap.equals(other.ptcLowerTapChangerMap) &&
                ptcUpperTapChangerMap.equals(other.ptcUpperTapChangerMap) &&
                pstAngleTapResults.equals(other.pstAngleTapResults);
        }
        return false;
    }
}
