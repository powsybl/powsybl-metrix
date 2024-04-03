/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package com.powsybl.metrix.integration;

import java.util.List;

public class PostProcessingPrefixContainer {
    public final String postProcessingType;
    public final String loadPrefix;
    public final String overloadPrefix;
    public final String overallOverloadPrefix;
    public final String maxThreatPrefix;

    public PostProcessingPrefixContainer(String postProcessingType, String loadPrefix, String overloadPrefix, String overallOverloadPrefix, String maxThreatPrefix) {
        this.postProcessingType = postProcessingType;
        this.loadPrefix = loadPrefix;
        this.overloadPrefix = overloadPrefix;
        this.overallOverloadPrefix = overallOverloadPrefix;
        this.maxThreatPrefix = maxThreatPrefix;
    }

    public List<String> postProcessingPrefixList() {
        return List.of(loadPrefix, overloadPrefix, overallOverloadPrefix);
    }
}
