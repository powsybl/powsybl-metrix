package com.powsybl.metrix.integration.dsl;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author Nicolas Rol {@literal <nicolas.rol at rte-france.com>}
 */
public class GeneratorDslData extends AbstractDslData {

    // Generator
    private final Set<String> generatorsForAdequacy;
    private final Set<String> generatorsForRedispatching;
    private final Map<String, List<String>> generatorContingenciesMap;

    public GeneratorDslData() {
        this(new HashSet<>(), new HashSet<>(), new HashMap<>());
    }

    public GeneratorDslData(Set<String> generatorsForAdequacy,
                            Set<String> generatorsForRedispatching,
                            Map<String, List<String>> generatorContingenciesMap) {
        this.generatorsForAdequacy = generatorsForAdequacy;
        this.generatorsForRedispatching = generatorsForRedispatching;
        this.generatorContingenciesMap = generatorContingenciesMap;
    }

    @Override
    protected LinkedHashMap<String, Object> getMapElements() {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("generatorsForAdequacy", generatorsForAdequacy);
        map.put("generatorsForRedispatching", generatorsForRedispatching);
        map.put("generatorContingenciesMap", generatorContingenciesMap);
        return map;
    }

    // Getters
    public Set<String> getGeneratorsForAdequacy() {
        return generatorsForAdequacy;
    }

    public Set<String> getGeneratorsForRedispatching() {
        return generatorsForRedispatching;
    }

    public Map<String, List<String>> getGeneratorContingenciesMap() {
        return generatorContingenciesMap;
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

    @Override
    public int hashCode() {
        return Objects.hash(generatorsForAdequacy,
            generatorsForRedispatching,
            generatorContingenciesMap);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof GeneratorDslData other) {
            return generatorsForAdequacy.equals(other.generatorsForAdequacy) &&
                generatorsForRedispatching.equals(other.generatorsForRedispatching) &&
                generatorContingenciesMap.equals(other.generatorContingenciesMap);
        }
        return false;
    }
}
