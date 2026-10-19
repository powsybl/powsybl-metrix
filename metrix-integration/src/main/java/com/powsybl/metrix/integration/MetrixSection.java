/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration;

import com.google.common.collect.ImmutableMap;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Paul Bui-Quang {@literal <paul.buiquang at rte-france.com>}
 */
public class MetrixSection {

    private String id;
    private float maxFlowN;
    private Map<String, Float> coefFlowList = new LinkedHashMap<>();

    public MetrixSection() {
    }

    public MetrixSection(String id) {
        this.id = id;
    }

    public MetrixSection(String id, float maxFlowN, Map<String, Float> coefFlowList) {
        this.id = id;
        this.maxFlowN = maxFlowN;
        this.coefFlowList = coefFlowList;
    }

    public String getId() {
        return id;
    }

    public MetrixSection setId(String id) {
        this.id = id;
        return this;
    }

    public float getMaxFlowN() {
        return maxFlowN;
    }

    public MetrixSection setMaxFlowN(float maxFlowN) {
        this.maxFlowN = maxFlowN;
        return this;
    }

    public Map<String, Float> getCoefFlowList() {
        return coefFlowList;
    }

    public MetrixSection setCoefFlowList(Map<String, Float> coefFlowList) {
        this.coefFlowList = coefFlowList;
        return this;
    }

    @Override
    public int hashCode() {
        return id.hashCode() + Float.hashCode(maxFlowN) + coefFlowList.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MetrixSection other) {
            return id.equals(other.id) && maxFlowN == other.maxFlowN && coefFlowList.equals(other.coefFlowList);
        }
        return false;
    }

    @Override
    public String toString() {
        return ImmutableMap.builder()
                .put("id", id)
                .put("maxFlowN", maxFlowN)
                .putAll(coefFlowList)
                .build().toString();
    }
}
