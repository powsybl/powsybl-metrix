/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Paul Bui-Quang {@literal <paul.buiquang at rte-france.com>}
 */
class MetrixParametersTest {

    @Test
    void getSetTest() {
        MetrixParameters p = new MetrixParameters(
            MetrixComputationType.OPF_WITHOUT_REDISPATCHING,
            10f,
            1000);
        assertEquals(MetrixComputationType.OPF_WITHOUT_REDISPATCHING, p.getComputationType());
        assertEquals(10f, p.getLossFactor(), 0f);
        assertEquals(1000, p.getNominalU());
        assertFalse(p.isWithGridCost().isPresent());
        assertFalse(p.isPreCurativeResults().isPresent());
        assertFalse(p.isOutagesBreakingConnexity().isPresent());
        assertFalse(p.isRemedialActionsBreakingConnexity().isPresent());
        assertFalse(p.isAnalogousRemedialActionDetection().isPresent());
        assertFalse(p.isPropagateBranchTripping());
        assertFalse(p.isWithAdequacyResults().isPresent());
        assertFalse(p.isWithRedispatchingResults().isPresent());
        assertFalse(p.isMarginalVariationsOnBranches().isPresent());
        assertFalse(p.isMarginalVariationsOnHvdc().isPresent());
        assertFalse(p.isLossDetailPerCountry().isPresent());
        assertFalse(p.isOverloadResultsOnly().isPresent());
        assertFalse(p.isShowAllTDandHVDCresults().isPresent());
        assertFalse(p.getOptionalPstCostPenality().isPresent());
        assertFalse(p.getOptionalHvdcCostPenality().isPresent());
        assertFalse(p.getOptionalLossOfLoadCost().isPresent());
        assertFalse(p.getOptionalCurativeLossOfLoadCost().isPresent());
        assertFalse(p.getOptionalCurativeLossOfGenerationCost().isPresent());
        assertFalse(p.getOptionalContingenciesProbability().isPresent());
        assertFalse(p.getOptionalMaxSolverTime().isPresent());
        assertFalse(p.getOptionalLossNbRelaunch().isPresent());
        assertFalse(p.getOptionalLossThreshold().isPresent());
        assertFalse(p.getOptionalNbMaxIteration().isPresent());
        assertFalse(p.getOptionalNbMaxCurativeAction().isPresent());
        assertFalse(p.getOptionalNbMaxLostLoadDetailedResults().isPresent());
        assertFalse(p.getOptionalGapVariableCost().isPresent());
        assertFalse(p.getOptionalNbThreatResults().isPresent());
        assertFalse(p.getOptionalRedispatchingCostOffset().isPresent());
        assertFalse(p.getOptionalAdequacyCostOffset().isPresent());
        assertFalse(p.getOptionalCurativeRedispatchingLimit().isPresent());
        assertFalse(p.isWithLostLoadDetailedResultsOnContingency().isPresent());

        p.setWithGridCost(false)
            .setPreCurativeResults(true)
            .setOutagesBreakingConnexity(false)
            .setRemedialActionsBreakingConnexity(false)
            .setAnalogousRemedialActionDetection(true)
            .setPropagateBranchTripping(true)
            .setWithAdequacyResults(false)
            .setWithRedispatchingResults(true)
            .setMarginalVariationsOnBranches(true)
            .setMarginalVariationsOnHvdc(true)
            .setLossDetailPerCountry(true)
            .setOverloadResultsOnly(true)
            .setShowAllTDandHVDCresults(true)
            .setPstCostPenality(0.0001f)
            .setHvdcCostPenality(0.01f)
            .setLossOfLoadCost(9000f)
            .setCurativeLossOfLoadCost(1000f)
            .setCurativeLossOfGenerationCost(11000f)
            .setContingenciesProbability(0.001f)
            .setMaxSolverTime(-1)
            .setLossNbRelaunch(2)
            .setLossThreshold(504)
            .setNbMaxIteration(3)
            .setNbMaxCurativeAction(4)
            .setNbMaxLostLoadDetailedResults(5)
            .setGapVariableCost(10000)
            .setNbThreatResults(2)
            .setRedispatchingCostOffset(50)
            .setAdequacyCostOffset(4)
            .setCurativeRedispatchingLimit(1500)
            .setWithLostLoadDetailedResultsOnContingency(true);

        assertFalse(p.isWithGridCost().get());
        assertTrue(p.isPreCurativeResults().get());
        assertFalse(p.isOutagesBreakingConnexity().get());
        assertTrue(p.isAnalogousRemedialActionDetection().get());
        assertTrue(p.isPropagateBranchTripping());
        assertFalse(p.isWithAdequacyResults().get());
        assertTrue(p.isWithRedispatchingResults().get());
        assertTrue(p.isMarginalVariationsOnBranches().get());
        assertTrue(p.isMarginalVariationsOnHvdc().get());
        assertTrue(p.isLossDetailPerCountry().get());
        assertTrue(p.isOverloadResultsOnly().get());
        assertTrue(p.isShowAllTDandHVDCresults().get());
        assertEquals(2, p.getOptionalLossNbRelaunch().getAsInt());
        assertEquals(504, p.getOptionalLossThreshold().getAsInt());
        assertEquals(0.0001f, p.getOptionalPstCostPenality().get(), 0f);
        assertEquals(0.01f, p.getOptionalHvdcCostPenality().get(), 0f);
        assertEquals(9000f, p.getOptionalLossOfLoadCost().get(), 0f);
        assertEquals(0.001f, p.getOptionalContingenciesProbability().get(), 0f);
        assertEquals(-1, p.getOptionalMaxSolverTime().getAsInt());
        assertEquals(3, p.getOptionalNbMaxIteration().getAsInt());
        assertEquals(4, p.getOptionalNbMaxCurativeAction().getAsInt());
        assertEquals(5, p.getOptionalNbMaxLostLoadDetailedResults().getAsInt());
        assertEquals(10000, p.getOptionalGapVariableCost().getAsInt());
        assertEquals(2, p.getOptionalNbThreatResults().getAsInt());
        assertEquals(4, p.getOptionalAdequacyCostOffset().getAsInt());
        assertEquals(50, p.getOptionalRedispatchingCostOffset().getAsInt());
        assertEquals(1500, p.getOptionalCurativeRedispatchingLimit().getAsInt());

        assertEquals(p, p.setComputationType(MetrixComputationType.OPF));
        assertEquals(MetrixComputationType.OPF, p.getComputationType());

        assertEquals(p, p.setLossFactor(20f));
        assertEquals(20f, p.getLossFactor(), 0f);

        assertEquals(p, p.setNominalU(37));
        assertEquals(37, p.getNominalU());

        assertEquals(p, p.setWithGridCost(true));
        assertTrue(p.isWithGridCost().get());

        assertEquals(p, p.setPreCurativeResults(false));
        assertFalse(p.isPreCurativeResults().get());

        assertEquals(p, p.setOutagesBreakingConnexity(true));
        assertTrue(p.isOutagesBreakingConnexity().get());

        assertEquals(p, p.setAnalogousRemedialActionDetection(false));
        assertFalse(p.isAnalogousRemedialActionDetection().get());

        assertEquals(p, p.setPropagateBranchTripping(false));
        assertFalse(p.isPropagateBranchTripping());

        assertEquals(p, p.setWithAdequacyResults(true));
        assertTrue(p.isWithAdequacyResults().get());

        assertEquals(p, p.setMarginalVariationsOnBranches(false));
        assertFalse(p.isMarginalVariationsOnBranches().get());

        assertEquals(p, p.setMarginalVariationsOnHvdc(false));
        assertFalse(p.isMarginalVariationsOnHvdc().get());

        assertEquals(p, p.setLossDetailPerCountry(false));
        assertFalse(p.isLossDetailPerCountry().get());

        assertEquals(p, p.setWithLostLoadDetailedResultsOnContingency(true));
        assertTrue(p.isWithLostLoadDetailedResultsOnContingency().get());

        assertEquals(p, p.setMaxSolverTime(60));
        assertEquals(60, p.getOptionalMaxSolverTime().getAsInt());

        assertEquals(p, p.setLossNbRelaunch(5));
        assertEquals(5, p.getOptionalLossNbRelaunch().getAsInt());

        assertEquals(p, p.setLossThreshold(390));
        assertEquals(390, p.getOptionalLossThreshold().getAsInt());

        assertEquals(p, p.setPstCostPenality(0.02f));
        assertEquals(0.02f, p.getOptionalPstCostPenality().get(), 0f);

        assertEquals(p, p.setHvdcCostPenality(0.3f));
        assertEquals(0.3f, p.getOptionalHvdcCostPenality().get(), 0f);

        assertEquals(p, p.setLossOfLoadCost(8000f));
        assertEquals(8000f, p.getOptionalLossOfLoadCost().get(), 0f);

        assertEquals(p, p.setContingenciesProbability(0.003f));
        assertEquals(0.003f, p.getOptionalContingenciesProbability().get(), 0f);

        assertEquals(p, p.setNbMaxIteration(57));
        assertEquals(57, p.getOptionalNbMaxIteration().getAsInt());

        assertEquals(p, p.setNbMaxCurativeAction(33));
        assertEquals(33, p.getOptionalNbMaxCurativeAction().getAsInt());

        assertEquals(p, p.setNbMaxLostLoadDetailedResults(67));
        assertEquals(67, p.getOptionalNbMaxLostLoadDetailedResults().getAsInt());

        assertEquals(p, p.setGapVariableCost(15000));
        assertEquals(15000, p.getOptionalGapVariableCost().getAsInt());

        assertEquals(p, p.setNbThreatResults(10));
        assertEquals(10, p.getOptionalNbThreatResults().getAsInt());

        assertEquals(p, p.setCurativeRedispatchingLimit(-1));
        assertEquals(-1, p.getOptionalCurativeRedispatchingLimit().getAsInt());
    }

    @Test
    void toStringTest() {
        MetrixParameters p = new MetrixParameters();
        String expected = "{computationType=LF, lossFactor=0.0, nominalU=100, propagateBranchTripping=false}";
        assertEquals(expected, p.toString());

        p = new MetrixParameters(MetrixComputationType.OPF,
                10f,
                2000)
            .setWithGridCost(true)
            .setPreCurativeResults(false)
            .setOutagesBreakingConnexity(true)
            .setRemedialActionsBreakingConnexity(true)
            .setAnalogousRemedialActionDetection(true)
            .setPropagateBranchTripping(false)
            .setWithAdequacyResults(true)
            .setWithRedispatchingResults(true)
            .setMarginalVariationsOnBranches(false)
            .setMarginalVariationsOnHvdc(true)
            .setLossDetailPerCountry(true)
            .setOverloadResultsOnly(true)
            .setShowAllTDandHVDCresults(true)
            .setPstCostPenality(0.001f)
            .setHvdcCostPenality(0.01f)
            .setLossOfLoadCost(12000f)
            .setCurativeLossOfLoadCost(13000f)
            .setCurativeLossOfGenerationCost(14000f)
            .setContingenciesProbability(0.02f)
            .setMaxSolverTime(120)
            .setLossNbRelaunch(2)
            .setLossThreshold(504)
            .setNbMaxIteration(3)
            .setNbMaxCurativeAction(4)
            .setNbMaxLostLoadDetailedResults(5)
            .setGapVariableCost(10000)
            .setNbThreatResults(5)
            .setRedispatchingCostOffset(100)
            .setAdequacyCostOffset(20)
            .setCurativeRedispatchingLimit(12345)
            .setWithLostLoadDetailedResultsOnContingency(true);

        expected = "{computationType=OPF, lossFactor=10.0, nominalU=2000, " +
            "withGridCost=true, " +
            "preCurativeResults=false, " +
            "outagesBreakingConnexity=true, " +
            "remedialActionsBreakingConnexity=true, " +
            "analogousRemedialActionDetection=true, " +
            "propagateBranchTripping=false, " +
            "withAdequacyResults=true, " +
            "withRedispatchingResults=true, " +
            "marginalVariationsOnBranches=false, " +
            "marginalVariationsOnHvdc=true, " +
            "lossDetailPerCountry=true, " +
            "overloadResultsOnly=true, " +
            "showAllTDandHVDCresults=true, " +
            "withLostLoadDetailedResultsOnContingency=true, " +
            "lossNbRelaunch=2, " +
            "lossThreshold=504, " +
            "pstCostPenality=0.001, " +
            "hvdcCostPenality=0.01, " +
            "lossOfLoadCost=12000.0, " +
            "curativeLossOfLoadCost=13000.0, " +
            "curativeLossOfGenerationCost=14000.0, " +
            "contingenciesProbability=0.02, " +
            "maxSolverTime=120, " +
            "nbMaxIteration=3, " +
            "nbMaxCurativeAction=4, " +
            "nbMaxLostLoadDetailedResults=5, " +
            "gapVariableCost=10000, " +
            "nbThreatResults=5, " +
            "redispatchingCostOffset=100, " +
            "adequacyCostOffset=20, " +
            "curativeRedispatchingLimit=12345}";
        assertEquals(expected, p.toString());
    }
}
