/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration.analysis;

import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Network;
import com.powsybl.metrix.integration.MetrixDslData;
import com.powsybl.metrix.integration.configuration.MetrixParameters;
import com.powsybl.metrix.integration.io.MetrixConfigResult;
import com.powsybl.metrix.integration.remedials.Remedial;
import com.powsybl.metrix.mapping.MappingParameters;
import com.powsybl.metrix.mapping.config.TimeSeriesMappingConfig;

import javax.annotation.Nullable;
import java.util.List;

/**
 * @author Valentin Berthault {@literal <valentin.berthault at rte-france.com>}
 */
public record MetrixAnalysisResult(@Nullable MetrixDslData metrixDslData, TimeSeriesMappingConfig mappingConfig,
                                   Network network, MetrixParameters metrixParameters,
                                   MappingParameters mappingParameters, MetrixConfigResult metrixConfigResult,
                                   List<Contingency> contingencies, List<Remedial> remedials) {
}
