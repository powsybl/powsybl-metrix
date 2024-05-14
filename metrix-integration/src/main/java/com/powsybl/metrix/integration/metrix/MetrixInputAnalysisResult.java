/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration.metrix;

import com.powsybl.contingency.Contingency;
import com.powsybl.metrix.integration.remedials.Remedial;

import java.util.List;

/**
 * @author marifunf {@literal <marianne.funfrock at rte-france.com>}
 */
public class MetrixInputAnalysisResult {

    private final List<Remedial> remedials;
    private final List<Contingency> contingencies;

    public MetrixInputAnalysisResult(List<Remedial> remedials, List<Contingency> contingencies) {
        this.remedials = remedials;
        this.contingencies = contingencies;
    }

    public List<Remedial> getRemedials() {
        return remedials;
    }

    public List<Contingency> getContingencies() {
        return contingencies;
    }
}
