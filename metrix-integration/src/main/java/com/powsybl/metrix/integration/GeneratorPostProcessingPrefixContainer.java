/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration;

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
public class GeneratorPostProcessingPrefixContainer {
    public final String postProcessingType;
    public final String redispatchingUpPrefix;
    public final String redispatchingUpCostPrefix;
    public final String redispatchingDownPrefix;
    public final String redispatchingDownCostPrefix;
    public final String redispatchingPrefix;
    public final String redispatchingCostPrefix;

    public GeneratorPostProcessingPrefixContainer(String postProcessingType,
                                                  String redispatchingUpPrefix, String redispatchingUpCostPrefix,
                                                  String redispatchingDownPrefix, String redispatchingDownCostPrefix,
                                                  String redispatchingPrefix, String redispatchingCostPrefix) {
        this.postProcessingType = postProcessingType;
        this.redispatchingUpPrefix = redispatchingUpPrefix;
        this.redispatchingUpCostPrefix = redispatchingUpCostPrefix;
        this.redispatchingDownPrefix = redispatchingDownPrefix;
        this.redispatchingDownCostPrefix = redispatchingDownCostPrefix;
        this.redispatchingPrefix = redispatchingPrefix;
        this.redispatchingCostPrefix = redispatchingCostPrefix;
    }
}
