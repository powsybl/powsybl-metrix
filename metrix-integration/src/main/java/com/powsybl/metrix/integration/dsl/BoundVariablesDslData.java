package com.powsybl.metrix.integration.dsl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.powsybl.metrix.integration.binding.MetrixGeneratorsBinding;
import com.powsybl.metrix.integration.binding.MetrixLoadsBinding;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author Nicolas Rol {@literal <nicolas.rol at rte-france.com>}
 */
public class BoundVariablesDslData extends AbstractDslData {

    // Bound variables
    private final Map<String, MetrixGeneratorsBinding> generatorsBindings;
    private final Map<String, MetrixLoadsBinding> loadsBindings;

    public BoundVariablesDslData() {
        this(new HashMap<>(), new HashMap<>());
    }

    public BoundVariablesDslData(Map<String, MetrixGeneratorsBinding> generatorsBindings,
                                 Map<String, MetrixLoadsBinding> loadsBindings) {
        this.generatorsBindings = generatorsBindings;
        this.loadsBindings = loadsBindings;
    }

    @Override
    protected LinkedHashMap<String, Object> getMapElements() {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("generatorsBindings", generatorsBindings);
        map.put("loadsBindings", loadsBindings);
        return map;
    }

    // Getters
    public Map<String, MetrixGeneratorsBinding> getGeneratorsBindings() {
        return generatorsBindings;
    }

    public Map<String, MetrixLoadsBinding> getLoadsBindings() {
        return loadsBindings;
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

    @Override
    public int hashCode() {
        return Objects.hash(generatorsBindings,
            loadsBindings);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BoundVariablesDslData other) {
            return generatorsBindings.equals(other.generatorsBindings) &&
                loadsBindings.equals(other.loadsBindings);
        }
        return false;
    }
}
