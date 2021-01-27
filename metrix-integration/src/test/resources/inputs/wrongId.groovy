/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

branch('MONITORING N WRONG ID') {
    branchRatingsBaseCase 'tsN'
}
branch('RESULT N WRONG ID') {
    baseCaseFlowResults()
}
branch('MONITORING Nk WRONG ID') {
    branchRatingsOnContingency 'tsN_1'
}
branch('RESULT Nk WRONG ID') {
    maxThreatFlowResults()
}
phaseShifter('PHASE SHIFTER WRONG ID') {
    onContingencies 'cty1', 'cty2'
    controlType CONTROL_OFF
}
hvdc('HVDC WRONG ID') {
    onContingencies 'cty3', 'cty4'
    controlType OPTIMIZED
}
sectionMonitoring('section') {
    maxFlowN 1000
    branch('BRANCH WRONG ID', 1)
}
generator('GENERATOR WRONG ID') {
    adequacyDownCosts 'ts1'
    adequacyUpCosts 'ts2'
    redispatchingDownCosts 'ts3'
    redispatchingUpCosts 'ts4'
    onContingencies 'cty3'
}
load('LOAD WRONG ID') {
    preventiveSheddingPercentage 10
    curativeSheddingPercentage 10
    onContingencies 'cty3', 'cty4'
}