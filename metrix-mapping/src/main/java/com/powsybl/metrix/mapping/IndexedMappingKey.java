/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping;

import org.jspecify.annotations.NonNull;

import java.util.Objects;

/**
 * @author Paul Bui-Quang {@literal <paul.buiquang at rte-france.com>}
 */
public record IndexedMappingKey(MappingKey key, int num) {

    public IndexedMappingKey(MappingKey key, int num) {
        this.key = Objects.requireNonNull(key);
        this.num = num;
    }

    @Override
    public int hashCode() {
        return key.hashCode() + Integer.hashCode(num);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof IndexedMappingKey(MappingKey key1, int num1)) {
            return key.equals(key1) && num == num1;
        }
        return false;
    }

    @Override
    @NonNull
    public String toString() {
        return "IndexedMappingKey(key=" + key + ", num=" + num + ")";
    }
}
