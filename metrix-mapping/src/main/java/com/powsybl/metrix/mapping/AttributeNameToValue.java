/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class AttributeNameToValue {
    @JsonProperty("attributeNameToValue")
    private Map<String, String> attributeNameToValue;

    @JsonCreator
    public AttributeNameToValue(Map<String, String> attributeNameToValue) {
        this.attributeNameToValue = new LinkedHashMap<>(attributeNameToValue);
    }

    public AttributeNameToValue() {
        this.attributeNameToValue = new LinkedHashMap<>();
    }

    public void put(String attributeName, String value) {
        this.attributeNameToValue.putIfAbsent(attributeName, value);
    }

    public String getValue(String attributeName) {
        return this.attributeNameToValue.get(attributeName);
    }

    public Set<String> getAttributeNames() {
        return attributeNameToValue.keySet();
    }
}
