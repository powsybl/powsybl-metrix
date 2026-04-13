/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping.references;

import org.jspecify.annotations.NonNull;

import java.util.Objects;

/**
 * @author Paul Bui-Quang {@literal <paul.buiquang at rte-france.com>}
 */
public record IndexedName(String name, int num) {

    public IndexedName(String name, int num) {
        this.name = Objects.requireNonNull(name);
        this.num = num;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, num);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof IndexedName(String name1, int num1)) {
            return name.equals(name1) && num == num1;
        }
        return false;
    }

    @Override
    @NonNull
    public String toString() {
        return "IndexedName(name=" + name + ", num=" + num + ")";
    }
}
