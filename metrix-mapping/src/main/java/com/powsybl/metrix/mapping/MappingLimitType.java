/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian@rte-france.com>
 */
public enum MappingLimitType {
    MIN("min"),
    MAX("max");

    private final String type;

    MappingLimitType(String type) {
        this.type = Objects.requireNonNull(type);
    }

    public String getType() {
        return type;
    }
}
