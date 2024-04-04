/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

public final class MetrixDataName {

    public static final String SEPARATOR = "<>";

    public static String getNameWithSchema(String... names) {
        Objects.requireNonNull(names);
        return Arrays.stream(names)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(SEPARATOR));
    }

    public static String[] getSplittedName(String name) {
        Objects.requireNonNull(name);
        return name.split(MetrixDataName.SEPARATOR);
    }

    public static String getNameWithoutSchema(String name) {
        Objects.requireNonNull(name);
        return name.split(MetrixDataName.SEPARATOR)[0];
    }

    private MetrixDataName() throws IllegalAccessException {
        throw new IllegalAccessException();
    }
}
