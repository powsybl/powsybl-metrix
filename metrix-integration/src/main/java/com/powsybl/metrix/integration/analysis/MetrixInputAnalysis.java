/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration.analysis;

import com.powsybl.contingency.Contingency;
import com.powsybl.metrix.integration.remedials.Remedial;

import java.util.List;

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
public class MetrixInputAnalysis {
    private final ContingencyLoader contingencyLoader;
    private final RemedialLoader remedialLoader;
    private final ConsistencyChecker consistencyChecker;

    public MetrixInputAnalysis(ContingencyLoader contingencyLoader,
                               RemedialLoader remedialLoader,
                               ConsistencyChecker consistencyChecker) {
        this.contingencyLoader = contingencyLoader;
        this.remedialLoader = remedialLoader;
        this.consistencyChecker = consistencyChecker;
    }

    public MetrixInputAnalysisResult runAnalysis() {
        List<Contingency> contingencies = contingencyLoader.load();
        List<Remedial> remedials = remedialLoader.load();
        consistencyChecker.run(remedials, contingencies);
        return new MetrixInputAnalysisResult(remedials, contingencies);
    }
}
