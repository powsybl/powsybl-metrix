/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration.network;

import com.google.common.collect.Range;
import com.powsybl.timeseries.TimeSeriesIndex;

import java.nio.file.Path;
import java.util.Set;

/**
 * @author Paul Bui-Quang {@literal <paul.buiquang at rte-france.com>}
 */
public interface MetrixVariantProvider {
    class Variants {
        final int firstVariant;
        final int lastVariant;
        final int variantCount;

        public Variants(int firstVariant, int lastVariant) {
            this.firstVariant = firstVariant;
            this.lastVariant = lastVariant;
            this.variantCount = lastVariant - firstVariant + 1;
        }

        public static Variants empty() {
            return new Variants(-1, -1);
        }

        public Range<Integer> closedRange() {
            return Range.closed(firstVariant, lastVariant);
        }

        public int count() {
            return variantCount;
        }

        public int lastVariant() {
            return lastVariant;
        }

        public int firstVariant() {
            return firstVariant;
        }

        public int variantCount() {
            return variantCount;
        }
    }

    Range<Integer> getVariantRange();

    TimeSeriesIndex getIndex();

    Set<String> getMappedBreakers();

    void readVariants(Range<Integer> variantReadRange, MetrixVariantReader reader, Path workingDir);

    default Variants variants() {
        return new Variants(getVariantRange().lowerEndpoint(), getVariantRange().upperEndpoint());
    }
}
