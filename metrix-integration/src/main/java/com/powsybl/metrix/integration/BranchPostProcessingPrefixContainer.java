/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration;

import java.util.List;

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
public record BranchPostProcessingPrefixContainer(String postProcessingType, String loadPrefix, String overloadPrefix,
                                                  String overallOverloadPrefix, String maxThreatPrefix) {

    public List<String> postProcessingPrefixList() {
        return List.of(loadPrefix, overloadPrefix, overallOverloadPrefix);
    }
}
