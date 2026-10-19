/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
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
 * @author Matthieu SAUR {@literal <matthieu.saur at rte-france.com>}
 */
public class BatteryDslData extends AbstractDslData {

    // Battery
    private final Set<String> batteriesForAdequacy;
    private final Set<String> batteriesForRedispatching;
    private final Map<String, List<String>> batteryContingenciesMap;

    public BatteryDslData() {
        this(new HashSet<>(), new HashSet<>(), new HashMap<>());
    }

    public BatteryDslData(Set<String> batteriesForAdequacy,
                          Set<String> batteriesForRedispatching,
                          Map<String, List<String>> batteryContingenciesMap) {
        this.batteriesForAdequacy = batteriesForAdequacy;
        this.batteriesForRedispatching = batteriesForRedispatching;
        this.batteryContingenciesMap = batteryContingenciesMap;
    }

    @Override
    protected LinkedHashMap<String, Object> getMapElements() {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("batteriesForAdequacy", batteriesForAdequacy);
        map.put("batteriesForRedispatching", batteriesForRedispatching);
        map.put("batteryContingenciesMap", batteryContingenciesMap);
        return map;
    }

    // Getters
    public Set<String> getBatteriesForAdequacy() {
        return batteriesForAdequacy;
    }

    public Set<String> getBatteriesForRedispatching() {
        return batteriesForRedispatching;
    }

    public Map<String, List<String>> getBatteryContingenciesMap() {
        return batteryContingenciesMap;
    }

    // Battery costs
    public void addBatteryForAdequacy(String id) {
        Objects.requireNonNull(id);
        batteriesForAdequacy.add(id);
    }

    public void addBatteryForRedispatching(String id, List<String> contingencies) {
        Objects.requireNonNull(id);
        batteriesForRedispatching.add(id);
        if (!Objects.isNull(contingencies)) {
            batteryContingenciesMap.put(id, contingencies);
        }
    }

    // Battery contingencies
    @JsonIgnore
    public final Set<String> getBatteryContingenciesList() {
        return batteryContingenciesMap.keySet();
    }

    public final List<String> getBatteryContingencies(String id) {
        if (batteryContingenciesMap.containsKey(id)) {
            return Collections.unmodifiableList(batteryContingenciesMap.get(id));
        }
        return Collections.emptyList();
    }

    @Override
    public int hashCode() {
        return Objects.hash(batteriesForAdequacy,
            batteriesForRedispatching,
            batteryContingenciesMap);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BatteryDslData other) {
            return batteriesForAdequacy.equals(other.batteriesForAdequacy) &&
                batteriesForRedispatching.equals(other.batteriesForRedispatching) &&
                batteryContingenciesMap.equals(other.batteryContingenciesMap);
        }
        return false;
    }
}
