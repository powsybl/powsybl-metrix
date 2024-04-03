/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration;

import com.powsybl.metrix.integration.utils.LocalThreadExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalThreadExecutorTest {

    @Test
    void testMissingConstructorParam() {
        assertThatThrownBy(() -> new LocalThreadExecutor<Void>(""))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("threadName cannot be null");
    }

    @Test
    @Timeout(1)
    void testSupplyAsync() {
        CompletableFuture<String> tName = new LocalThreadExecutor<String>("TName").supplyAsync(() -> "Result");
        String result = tName.join();
        assertThat(result).isEqualTo("Result");
    }

    @Test
    @Timeout(1)
    void testRunAsync() {
        AtomicBoolean atomicBoolean = new AtomicBoolean(false);
        CompletableFuture<Void> tName = new LocalThreadExecutor<String>("TName").runAsync(() -> atomicBoolean.set(true));
        tName.join();
        assertThat(atomicBoolean).isTrue();
    }
}
