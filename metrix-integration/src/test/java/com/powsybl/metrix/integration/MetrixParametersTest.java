/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Paul Bui-Quang {@literal <paul.buiquang at rte-france.com>}
 */
class MetrixParametersTest {

    private void checkOptionalIntParametersAreNotPresent(MetrixParameters p) {
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
    }

    private void checkOptionalParametersAreNotPresent(MetrixParameters p) {
        assertFalse(p.isWithGridCost().isPresent());
        assertFalse(p.isPreCurativeResults().isPresent());
        assertFalse(p.isOutagesBreakingConnexity().isPresent());
        assertFalse(p.isRemedialActionsBreakingConnexity().isPresent());
        assertFalse(p.isAnalogousRemedialActionDetection().isPresent());
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
        assertFalse(p.getOptionalGeneratorMinCost().isPresent());
        assertFalse(p.getOptionalContingenciesProbability().isPresent());
        assertFalse(p.isWithLostLoadDetailedResultsOnContingency().isPresent());
    }

    @Test
    void defaultParametersTest() {
        MetrixParameters p = new MetrixParameters();
        assertEquals(MetrixComputationType.LF, p.getComputationType());
        assertEquals(0f, p.getLossFactor(), 0f);
        assertEquals(100, p.getNominalU());
        checkOptionalIntParametersAreNotPresent(p);
        checkOptionalParametersAreNotPresent(p);
        assertFalse(p.isPropagateBranchTripping());
    }

    @Test
    void mandatoryParametersTest() {
        MetrixParameters p = new MetrixParameters(
            MetrixComputationType.OPF_WITHOUT_REDISPATCHING,
            10f,
            1000);

        assertEquals(MetrixComputationType.OPF_WITHOUT_REDISPATCHING, p.getComputationType());
        assertEquals(10f, p.getLossFactor(), 0f);
        assertEquals(1000, p.getNominalU());

        assertEquals(p, p.setComputationType(MetrixComputationType.OPF));
        assertEquals(MetrixComputationType.OPF, p.getComputationType());

        assertEquals(p, p.setComputationType(MetrixComputationType.LF));
        assertEquals(MetrixComputationType.LF, p.getComputationType());

        assertEquals(p, p.setComputationType(MetrixComputationType.OPF_WITH_OVERLOAD));
        assertEquals(MetrixComputationType.OPF_WITH_OVERLOAD, p.getComputationType());

        assertEquals(p, p.setComputationType(MetrixComputationType.OPF_WITHOUT_REDISPATCHING));
        assertEquals(MetrixComputationType.OPF_WITHOUT_REDISPATCHING, p.getComputationType());

        assertEquals(p, p.setLossFactor(20f));
        assertEquals(20f, p.getLossFactor(), 0f);

        assertEquals(p, p.setNominalU(37));
        assertEquals(37, p.getNominalU());
    }

    @Test
    void optionalIntParametersTest() {
        MetrixParameters p = new MetrixParameters();

        p.setMaxSolverTime(-1)
            .setLossNbRelaunch(2)
            .setLossThreshold(504)
            .setNbMaxIteration(3)
            .setNbMaxCurativeAction(4)
            .setNbMaxLostLoadDetailedResults(5)
            .setGapVariableCost(10000)
            .setNbThreatResults(2)
            .setRedispatchingCostOffset(50)
            .setAdequacyCostOffset(4)
            .setCurativeRedispatchingLimit(1500);

        assertTrue(p.getOptionalMaxSolverTime().isPresent());
        assertEquals(-1, p.getOptionalMaxSolverTime().getAsInt());
        assertTrue(p.getOptionalLossNbRelaunch().isPresent());
        assertEquals(2, p.getOptionalLossNbRelaunch().getAsInt());
        assertTrue(p.getOptionalLossThreshold().isPresent());
        assertEquals(504, p.getOptionalLossThreshold().getAsInt());
        assertTrue(p.getOptionalNbMaxIteration().isPresent());
        assertEquals(3, p.getOptionalNbMaxIteration().getAsInt());
        assertTrue(p.getOptionalNbMaxCurativeAction().isPresent());
        assertEquals(4, p.getOptionalNbMaxCurativeAction().getAsInt());
        assertTrue(p.getOptionalNbMaxLostLoadDetailedResults().isPresent());
        assertEquals(5, p.getOptionalNbMaxLostLoadDetailedResults().getAsInt());
        assertTrue(p.getOptionalGapVariableCost().isPresent());
        assertEquals(10000, p.getOptionalGapVariableCost().getAsInt());
        assertTrue(p.getOptionalNbThreatResults().isPresent());
        assertEquals(2, p.getOptionalNbThreatResults().getAsInt());
        assertTrue(p.getOptionalRedispatchingCostOffset().isPresent());
        assertEquals(50, p.getOptionalRedispatchingCostOffset().getAsInt());
        assertTrue(p.getOptionalAdequacyCostOffset().isPresent());
        assertEquals(4, p.getOptionalAdequacyCostOffset().getAsInt());
        assertTrue(p.getOptionalCurativeRedispatchingLimit().isPresent());
        assertEquals(1500, p.getOptionalCurativeRedispatchingLimit().getAsInt());
    }

    @Test
    void optionalParametersTest() {
        MetrixParameters p = new MetrixParameters();

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
            .setWithLostLoadDetailedResultsOnContingency(true)
            .setPstCostPenality(0.0001f)
            .setHvdcCostPenality(0.01f)
            .setLossOfLoadCost(9000f)
            .setCurativeLossOfLoadCost(1000f)
            .setCurativeLossOfGenerationCost(11000f)
            .setGeneratorMinCost(1.5f)
            .setContingenciesProbability(0.001f);

        assertThat(p.isWithGridCost()).isPresent().contains(false);
        assertThat(p.isPreCurativeResults()).isPresent().contains(true);
        assertThat(p.isOutagesBreakingConnexity()).isPresent().contains(false);
        assertThat(p.isRemedialActionsBreakingConnexity()).isPresent().contains(false);
        assertThat(p.isAnalogousRemedialActionDetection()).isPresent().contains(true);
        assertThat(p.isPropagateBranchTripping()).isTrue();
        assertThat(p.isWithAdequacyResults()).isPresent().contains(false);
        assertThat(p.isWithRedispatchingResults()).isPresent().contains(true);
        assertThat(p.isMarginalVariationsOnBranches()).isPresent().contains(true);
        assertThat(p.isMarginalVariationsOnHvdc()).isPresent().contains(true);
        assertThat(p.isLossDetailPerCountry()).isPresent().contains(true);
        assertThat(p.isOverloadResultsOnly()).isPresent().contains(true);
        assertThat(p.isShowAllTDandHVDCresults()).isPresent().contains(true);
        assertThat(p.isWithLostLoadDetailedResultsOnContingency()).isPresent().contains(true);
        assertThat(p.getOptionalPstCostPenality()).isPresent().contains(0.0001f);
        assertThat(p.getOptionalHvdcCostPenality()).isPresent().contains(0.01f);
        assertThat(p.getOptionalGeneratorMinCost()).isPresent().contains(1.5f);
        assertThat(p.getOptionalLossOfLoadCost()).isPresent().contains(9000f);
        assertThat(p.getOptionalCurativeLossOfLoadCost()).isPresent().contains(1000f);
        assertThat(p.getOptionalCurativeLossOfGenerationCost()).isPresent().contains(11000f);
        assertThat(p.getOptionalContingenciesProbability()).isPresent().contains(0.001f);
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
