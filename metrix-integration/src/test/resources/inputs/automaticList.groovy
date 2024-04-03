/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

for (l in network.lines) {
    branch(l.id) {
        branchRatingsBaseCase 'tsN'
        maxThreatFlowResults true
    }
}
for (twt in network.twoWindingsTransformers) {
    branch(twt.id) {
        baseCaseFlowResults true
        branchRatingsOnContingency 'tsN_1'
        branchRatingsBeforeCurative 'tsITAM'
    }
    phaseShifter(twt.id) {
        onContingencies 'cty'
        controlType FIXED_POWER_CONTROL
    }
}
for (h in network.hvdcLines) {
    hvdc(h.id) {
        onContingencies 'cty1'
        controlType OPTIMIZED
    }
}
for (g in network.generators) {
    generator(g.id) {
        adequacyDownCosts 'ts1'
        adequacyUpCosts 'ts2'
        redispatchingDownCosts 'ts3'
        redispatchingUpCosts 'ts4'
        onContingencies 'cty2', 'cty3'
    }
}
int i=0
for (l in network.loads) {
    load(l.id) {
        preventiveSheddingPercentage 20+i
        preventiveSheddingCost 12000+i
        curativeSheddingPercentage 5+i
        curativeSheddingCost 'ts5'
        onContingencies 'cty4'
    }
    i++;
}