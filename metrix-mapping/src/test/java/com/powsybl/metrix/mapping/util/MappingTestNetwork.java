/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping.util;

import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.EnergySource;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.TopologyKind;
import com.powsybl.iidm.network.VoltageLevel;

/**
 * 2 buses connected by a line.
 * Bus 1 connects 2 generators and 1 load.
 * Bus 2 connects 1 generator and 2 loads.
 *
 * @author Paul Bui-Quang {@literal <paul.buiquang at rte-france.com>}
 */

public final class MappingTestNetwork {

    private MappingTestNetwork() {
    }

    public static Network create() {
        Network network = Network.create("test", "test");
        Substation s1 = network.newSubstation()
                .setId("S1")
                .setCountry(Country.FR)
                .add();
        Substation s2 = network.newSubstation()
                .setId("S2")
                .setCountry(Country.FR)
                .add();
        VoltageLevel vl1 = s1.newVoltageLevel()
                .setId("VL1")
                .setNominalV(380f)
                .setTopologyKind(TopologyKind.NODE_BREAKER)
                .add();
        VoltageLevel vl2 = s2.newVoltageLevel()
                .setId("VL2")
                .setNominalV(380f)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        vl1.getNodeBreakerView().newBusbarSection()
            .setId("B1")
            .setNode(0)
            .add();
        vl1.getNodeBreakerView().newBusbarSection()
            .setNode(1)
            .setId("MVN")
            .add();
        vl1.getNodeBreakerView().newBreaker()
            .setNode1(0)
            .setNode2(1)
            .setId("SW1")
            .add();
        vl1.getNodeBreakerView().newBreaker()
            .setNode1(1)
            .setNode2(0)
            .setId("SW2")
            .add();
        vl1.getNodeBreakerView().newDisconnector()
            .setNode1(0)
            .setNode2(2)
            .setId("SW3")
            .add();
        vl1.getNodeBreakerView().newDisconnector()
            .setNode1(0)
            .setNode2(3)
            .setId("SW4")
            .add();
        vl1.getNodeBreakerView().newDisconnector()
            .setNode1(0)
            .setNode2(4)
            .setId("SW5")
            .add();
        vl1.getNodeBreakerView().newDisconnector()
            .setNode1(0)
            .setNode2(5)
            .setId("SW6")
            .add();
        vl2.getBusBreakerView().newBus()
                .setId("B2")
                .add();
        network.newLine()
                .setId("L1")
                .setVoltageLevel1("VL1")
                .setNode1(2)
                .setVoltageLevel2("VL2")
                .setConnectableBus2("B2")
                .setBus2("B2")
                .setR(1)
                .setX(1)
                .setB1(0f)
                .setB2(0f)
                .setG1(0f)
                .setG2(0f)
                .add();
        vl1.newGenerator()
                .setId("G1")
                .setNode(3)
                .setEnergySource(EnergySource.NUCLEAR)
                .setMinP(0f)
                .setMaxP(1000f)
                .setTargetP(900f)
                .setTargetV(380f)
                .setVoltageRegulatorOn(true)
                .add();
        vl1.newGenerator()
                .setId("G2")
                .setNode(4)
                .setEnergySource(EnergySource.NUCLEAR)
                .setMinP(0f)
                .setMaxP(500f)
                .setTargetP(400f)
                .setTargetV(380f)
                .setVoltageRegulatorOn(true)
                .add();
        vl2.newGenerator()
                .setId("G3")
                .setConnectableBus("B2")
                .setBus("B2")
                .setEnergySource(EnergySource.HYDRO)
                .setMinP(0f)
                .setMaxP(1000f)
                .setTargetP(900f)
                .setTargetV(380f)
                .setVoltageRegulatorOn(true)
                .add();
        vl2.newGenerator()
                .setId("G4")
                .setConnectableBus("B2")
                .setBus("B2")
                .setEnergySource(EnergySource.OTHER)
                .setMinP(10f)
                .setMaxP(90f)
                .setTargetP(35f)
                .setTargetQ(0f)
                .setVoltageRegulatorOn(false)
                .add();
        vl1.newLoad()
                .setId("LD1")
                .setNode(5)
                .setP0(100)
                .setQ0(0)
                .add();
        vl2.newLoad()
                .setId("LD2")
                .setConnectableBus("B2")
                .setBus("B2")
                .setP0(100)
                .setQ0(0)
                .add();
        vl2.newLoad()
                .setId("LD3")
                .setConnectableBus("B2")
                .setBus("B2")
                .setP0(100)
                .setQ0(0)
                .add();
        s1.newTwoWindingsTransformer()
                .setId("twt")
                .setName("twt_name")
                .setR(1.0)
                .setX(2.0)
                .setG(3.0)
                .setB(4.0)
                .setRatedU1(5.0)
                .setRatedU2(6.0)
                .setVoltageLevel1("VL1")
                .setVoltageLevel2("VL1")
                .setNode1(6)
                .setNode2(7)
                .add().newPhaseTapChanger()
                .setTapPosition(1)
                .setLowTapPosition(0)
                .setTargetDeadband(1.0)
                .setRegulationValue(10.0)
                .beginStep()
                .setR(1.0)
                .setX(2.0)
                .setG(3.0)
                .setB(4.0)
                .setAlpha(5.0)
                .setRho(6.0)
                .endStep()
                .beginStep()
                .setR(1.0)
                .setX(2.0)
                .setG(3.0)
                .setB(4.0)
                .setAlpha(5.0)
                .setRho(6.0)
                .endStep()
                .add();
        return network;
    }
}
