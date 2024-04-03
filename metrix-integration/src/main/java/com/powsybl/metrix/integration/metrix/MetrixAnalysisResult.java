/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package com.powsybl.metrix.integration.metrix;

import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Network;
import com.powsybl.metrix.integration.MetrixDslData;
import com.powsybl.metrix.integration.MetrixParameters;
import com.powsybl.metrix.integration.io.MetrixConfigResult;
import com.powsybl.metrix.integration.remedials.Remedial;
import com.powsybl.metrix.mapping.MappingParameters;
import com.powsybl.metrix.mapping.TimeSeriesMappingConfig;

import javax.annotation.Nullable;
import java.util.List;

public class MetrixAnalysisResult {
    @Nullable
    public final MetrixDslData metrixDslData;
    public final TimeSeriesMappingConfig mappingConfig;
    public final Network network;
    public final MetrixParameters metrixParameters;
    public final MappingParameters mappingParameters;
    public final MetrixConfigResult metrixConfigResult;
    public final List<Contingency> contingencies;
    public final List<Remedial> remedials;

    public MetrixAnalysisResult(@Nullable MetrixDslData metrixDslData, TimeSeriesMappingConfig mappingConfig, Network network,
                                MetrixParameters metrixParameters, MappingParameters mappingParameters, MetrixConfigResult metrixConfigResult,
                                List<Contingency> contingencies, List<Remedial> remedials) {
        this.metrixDslData = metrixDslData;
        this.mappingConfig = mappingConfig;
        this.network = network;
        this.metrixParameters = metrixParameters;
        this.mappingParameters = mappingParameters;
        this.metrixConfigResult = metrixConfigResult;
        this.contingencies = contingencies;
        this.remedials = remedials;
    }
}
