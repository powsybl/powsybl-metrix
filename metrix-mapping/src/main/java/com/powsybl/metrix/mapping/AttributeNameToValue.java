/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
public class AttributeNameToValue {
    @JsonProperty("attributeNameToValue")
    private Map<String, String> attributeNameToValueVariable;

    @JsonCreator
    public AttributeNameToValue(Map<String, String> attributeNameToValue) {
        this.attributeNameToValueVariable = new LinkedHashMap<>(attributeNameToValue);
    }

    public AttributeNameToValue() {
        this.attributeNameToValueVariable = new LinkedHashMap<>();
    }

    public void put(String attributeName, String value) {
        this.attributeNameToValueVariable.putIfAbsent(attributeName, value);
    }

    public String getValue(String attributeName) {
        return this.attributeNameToValueVariable.get(attributeName);
    }

    public Set<String> getAttributeNames() {
        return attributeNameToValueVariable.keySet();
    }

    public AttributeNameToValue filterSelectedColumns(List<String> selectedColumns) {
        AttributeNameToValue filteredInfo = new AttributeNameToValue();
        filteredInfo.attributeNameToValueVariable = attributeNameToValueVariable.entrySet().stream()
                .filter(attributeInfo -> selectedColumns.contains(attributeInfo.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return filteredInfo;
    }
}
