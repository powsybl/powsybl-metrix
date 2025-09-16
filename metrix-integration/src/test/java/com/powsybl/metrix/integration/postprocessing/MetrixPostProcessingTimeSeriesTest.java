/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration.postprocessing;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static com.powsybl.metrix.integration.postprocessing.MetrixPostProcessingTimeSeries.findIdsToProcess;

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
class MetrixPostProcessingTimeSeriesTest {

    @Test
    void findIdsToProcessSimpleTest() {
        List<String> actual = findIdsToProcess(Set.of("id1", "id2"), Set.of("PREFIX_id1"), "PREFIX_");
        Assertions.assertThat(actual).containsExactly("id1");
    }

    @Test
    void findIdsToProcessSimpleWithSuffixTest() {
        List<String> actual = findIdsToProcess(Set.of("id1", "id1_suffix"), Set.of("PREFIX_id1"), "PREFIX_");
        Assertions.assertThat(actual).containsExactly("id1");
    }

    @Test
    void findIdsToProcessComplexTest() {
        List<String> actual = findIdsToProcess(Set.of("id1", "id2"), Set.of("PREFIX_id1_cty"), "PREFIX_", Set.of("cty"));
        Assertions.assertThat(actual).containsExactly("id1");
    }

    @Test
    void findIdsToProcessComplexWithSuffixTest() {
        List<String> actual = findIdsToProcess(Set.of("id1", "id1_suffix"), Set.of("PREFIX_id1_cty"), "PREFIX_", Set.of("cty"));
        Assertions.assertThat(actual).containsExactly("id1");
    }
}
