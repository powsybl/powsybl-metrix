package com.powsybl.metrix.integration.dsl;

import com.google.common.collect.ImmutableMap;

import java.util.LinkedHashMap;

/**
 * @author Nicolas Rol {@literal <nicolas.rol at rte-france.com>}
 */
public abstract class AbstractDslData {

    public void addToBuilder(ImmutableMap.Builder<String, Object> builder) {
        builder.putAll(getMapElements());
    }

    protected abstract LinkedHashMap<String, Object> getMapElements();
}
