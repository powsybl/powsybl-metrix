/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration.analysis;

import java.util.Arrays;
import java.util.Objects;

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
public record Reason(String key, Object... args) {
    public Object[] args() {
        return args == null ? new Object[0] : args;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Reason(String key1, Object[] args1))) {
            return false;
        }
        return key.equals(key1) && Arrays.equals(args, args1);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, Arrays.hashCode(args));
    }

    @Override
    public String toString() {
        return "Reason[" + "key=" + key + ", args=" + Arrays.deepToString(args()) + ']';
    }
}
