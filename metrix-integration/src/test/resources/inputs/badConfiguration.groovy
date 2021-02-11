/*
 * Copyright (c) 2021 RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License v. 2.0. If a copy of the MPL was not distributed with this
 * file You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

branch('FP.AND1  FVERGE1  1') {
}
branch('FVALDI1  FTDPRA1  1') {
    maxThreatFlowResults false
    branchRatingsOnContingency 'tsN_1'
}
branch('FS.BIS1  FVALDI1  1') {
    maxThreatFlowResults false
    branchRatingsOnContingency 'tsN'
    branchRatingsBeforeCurative 'tsITAM'
}
branch('FP.AND1  FVERGE1  2') {
    baseCaseFlowResults false
    branchRatingsBaseCase 'tsN'
    branchRatingsOnContingency 'tsN' // ok
}
branch('FS.BIS1  FVALDI1  2') {
    branchRatingsBeforeCurative 'tsITAM'
}
branch('FVALDI1  FTDPRA1  2') {
    maxThreatFlowResults false
    branchRatingsBaseCase 'tsN' // ok
    branchRatingsBeforeCurative 'tsITAM'
}
phaseShifter('FP.AND1  FTDPRA1  1') {
    onContingencies 'cty1', 'cty2'
}
hvdc('HVDC1') {
    onContingencies 'cty3', 'cty4'
}
sectionMonitoring('sectionOk') {
    maxFlowN 2000
    branch('HVDC1', 0.9)
    branch('FP.AND1  FTDPRA1  1', 1.1)
    branch('FVALDI1  FTDPRA1  1', 2)
}
sectionMonitoring('sectionBad1') {
    branch('HVDC1', 0.9)
}
sectionMonitoring('sectionBad2') {
    maxFlowN 0.1
}
sectionMonitoring('sectionBadType') {
    maxFlowN 100
    branch('HVDC1', 1.0)
    branch('FSSV.O11_L', 2.0)
}
generator('FSSV.O11_G') {
    adequacyDownCosts 'ts1'
    redispatchingDownCosts 'ts3'
}
generator('FSSV.O12_G') {
    adequacyUpCosts 'ts3'
    redispatchingUpCosts 'ts4'
}
generator('FVALDI11_G') {
    redispatchingUpCosts 'ts4'
    redispatchingDownCosts 'ts3'
}
generator('FVERGE11_G') {
    onContingencies 'cty3', 'cty4'
}
load('FSSV.O11_L') {
    preventiveSheddingPercentage 101
}
load('FSSV.O11_L') {
    preventiveSheddingPercentage (-1)
}
load('FSSV.O11_L') {
    preventiveSheddingCost 100.2
    curativeSheddingCost 'ts3'
}
load('FSSV.O11_L') {
    curativeSheddingPercentage 20
}
load('FVALDI11_L') {
    curativeSheddingCost 'ts4'
}
load('FVALDI11_L2') {
    curativeSheddingPercentage 20
}