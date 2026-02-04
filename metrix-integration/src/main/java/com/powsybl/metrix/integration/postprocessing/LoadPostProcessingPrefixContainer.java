/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration.postprocessing;

import com.powsybl.metrix.integration.MetrixVariable;

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
public record LoadPostProcessingPrefixContainer(MetrixVariable doctrineCostVariable,
                                                String metrixResultPrefix,
                                                String loadSheddingPrefix,
                                                String loadSheddingCostPrefix) {
}
