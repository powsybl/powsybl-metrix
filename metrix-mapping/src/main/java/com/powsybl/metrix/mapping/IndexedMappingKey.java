/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping;

import java.util.Objects;

public class IndexedMappingKey {

    private final MappingKey key;

    private final int num;

    public IndexedMappingKey(MappingKey key, int num) {
        this.key = Objects.requireNonNull(key);
        this.num = num;
    }

    public MappingKey getKey() {
        return key;
    }

    public int getNum() {
        return num;
    }

    @Override
    public int hashCode() {
        return key.hashCode() + Integer.hashCode(num);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof IndexedMappingKey) {
            IndexedMappingKey other = (IndexedMappingKey) obj;
            return key.equals(other.key) && num == other.num;
        }
        return false;
    }

    @Override
    public String toString() {
        return "IndexedMappingKey(key=" + key + ", num=" + num + ")";
    }
}
