/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.integration;

import com.google.common.collect.Range;
import com.powsybl.timeseries.TimeSeriesIndex;

import java.util.Set;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian@rte-france.com>
 */
public interface MetrixVariantProvider {

    Range<Integer> getVariantRange();

    TimeSeriesIndex getIndex();

    Set<String> getMappedBreakers();

    void readVariants(Range<Integer> variantReadRange, MetrixVariantReader reader);
}
