/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.integration;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian@rte-france.com>
 */
public class MetrixRunParameters {

    private final int firstVariant;

    private final int variantCount;

    private final SortedSet<Integer> versions;

    private final int chunkSize;

    private final boolean ignoreLimits;

    private final boolean ignoreEmptyFilter;

    public MetrixRunParameters(int firstVariant, int variantCount, SortedSet<Integer> versions, int chunkSize,
                               boolean ignoreLimits, boolean ignoreEmptyFilter) {
        this.firstVariant = firstVariant;
        this.variantCount = variantCount;
        this.versions = new TreeSet<>(versions);
        this.chunkSize = chunkSize;
        this.ignoreLimits = ignoreLimits;
        this.ignoreEmptyFilter = ignoreEmptyFilter;
    }

    public int getFirstVariant() {
        return firstVariant;
    }

    public int getVariantCount() {
        return variantCount;
    }

    public SortedSet<Integer> getVersions() {
        return Collections.unmodifiableSortedSet(versions);
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public boolean isIgnoreLimits() {
        return ignoreLimits;
    }

    public boolean isIgnoreEmptyFilter() {
        return ignoreEmptyFilter;
    }
}
