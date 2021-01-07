/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping;

import java.util.Set;

/**
 * @author Paul Bui-Quang <paul.buiquang at rte-france.com>
 */
public class ComputationRange {

    private Set<Integer> versions;
    private int firstVariant;
    private int variantCount;

    public ComputationRange() {
    }

    public ComputationRange(Set<Integer> versions, int firstVariant, int variantCount) {
        this.versions = versions;
        this.firstVariant = firstVariant;
        this.variantCount = variantCount;
    }

    public Set<Integer> getVersions() {
        return versions;
    }

    public void setVersions(Set<Integer> versions) {
        this.versions = versions;
    }

    public int getFirstVariant() {
        return firstVariant;
    }

    public void setFirstVariant(int firstVariant) {
        this.firstVariant = firstVariant;
    }

    public int getVariantCount() {
        return variantCount;
    }

    public void setVariantCount(int variantCount) {
        this.variantCount = variantCount;
    }
}
