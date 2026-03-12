/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration.postprocessing;

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
public enum PostProcessingEquipmentType {
    GENERATOR {
        @Override
        public PostProcessingPrefixContainer preventivePrefixContainer() {
            return MetrixGeneratorPostProcessingTimeSeries.PREVENTIVE_PREFIX_CONTAINER;
        }

        @Override
        public PostProcessingPrefixContainer curativePrefixContainer() {
            return MetrixGeneratorPostProcessingTimeSeries.CURATIVE_PREFIX_CONTAINER;
        }
    },

    LOAD {
        @Override
        public PostProcessingPrefixContainer preventivePrefixContainer() {
            return MetrixLoadPostProcessingTimeSeries.PREVENTIVE_PREFIX_CONTAINER;
        }

        @Override
        public PostProcessingPrefixContainer curativePrefixContainer() {
            return MetrixLoadPostProcessingTimeSeries.CURATIVE_PREFIX_CONTAINER;
        }
    };

    public abstract PostProcessingPrefixContainer preventivePrefixContainer();

    public abstract PostProcessingPrefixContainer curativePrefixContainer();
}
