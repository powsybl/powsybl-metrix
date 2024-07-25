/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping;

import java.util.Objects;

/**
 * @author Paul Bui-Quang {@literal <paul.buiquang at rte-france.com>}
 */
public class IndexedName {

    private final String name;

    private final int num;

    public IndexedName(String name, int num) {
        this.name = Objects.requireNonNull(name);
        this.num = num;
    }

    public String getName() {
        return name;
    }

    public int getNum() {
        return num;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, num);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof IndexedName other) {
            return name.equals(other.name) && num == other.num;
        }
        return false;
    }

    @Override
    public String toString() {
        return "IndexedName(name=" + name + ", num=" + num + ")";
    }
}
