/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration;

import com.powsybl.metrix.mapping.ComputationRange;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * @author Paul Bui-Quang {@literal <paul.buiquang at rte-france.com>}
 */
public class MetrixRunParameters {

    private final int firstVariant;

    private final int variantCount;

    private final SortedSet<Integer> versions;

    private final int chunkSize;

    private final boolean ignoreLimits;

    private final boolean ignoreEmptyFilter;

    private final boolean isNetworkComputation;

    private final boolean writePtdfMatrix;

    private final boolean writeLodfMatrix;

    public MetrixRunParameters(ComputationRange computationRange, int chunkSize,
                               boolean ignoreLimits, boolean ignoreEmptyFilter, boolean isNetworkComputation,
                               boolean writePtdfMatrix, boolean writeLodfMatrix) {
        this.firstVariant = computationRange.getFirstVariant();
        this.variantCount = computationRange.getVariantCount();
        this.versions = new TreeSet<>(computationRange.getVersions());
        this.chunkSize = chunkSize;
        this.ignoreLimits = ignoreLimits;
        this.ignoreEmptyFilter = ignoreEmptyFilter;
        this.isNetworkComputation = isNetworkComputation;
        this.writePtdfMatrix = writePtdfMatrix;
        this.writeLodfMatrix = writeLodfMatrix;
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

    public boolean isNetworkComputation() {
        return isNetworkComputation;
    }

    public boolean writePtdfMatrix() {
        return writePtdfMatrix;
    }

    public boolean writeLodfMatrix() {
        return writeLodfMatrix;
    }
}
