/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration.network;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.contingency.BranchContingency;
import com.powsybl.contingency.DanglingLineContingency;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.TieLineContingency;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Switch;
import com.powsybl.iidm.network.SwitchKind;
import com.powsybl.iidm.network.TopologyKind;
import com.powsybl.iidm.network.TopologyLevel;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.iidm.network.test.FourSubstationsNodeBreakerFactory;
import com.powsybl.iidm.serde.ExportOptions;
import com.powsybl.iidm.serde.NetworkSerDe;
import com.powsybl.metrix.integration.MetrixParameters;
import com.powsybl.metrix.integration.contingency.Probability;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Nicolas Rol {@literal <nicolas.rol at rte-france.com>}
 */
class MetrixNetworkTest {
    private FileSystem fileSystem;

    @BeforeEach
    void setUp() {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
    }

    @AfterEach
    void tearDown() throws IOException {
        fileSystem.close();
    }

    @Test
    void testNetworkElementsLists() {
        // Network
        Network network = createNetwork();

        // Set some switches as retained
        Set<String> mappedSwitches = Set.of("S1VL2_GH2_BREAKER", "S3VL1_LINES3S4_BREAKER", "S1VL1_LD1_BREAKER",
                "S1VL3_DL_BREAKER", "S1VL2_BBS1_BBS3", "S2VL2_DL_BREAKER");

        // Expected switch list in MetrixNetwork: switches next to branches (lines, two windings transformers) are not present
        List<Switch> switchList = Set.of("S1VL2_GH2_BREAKER", "S1VL1_LD1_BREAKER", "S1VL2_BBS1_BBS3")
                .stream()
                .map(network::getSwitch).toList();

        // Expected Dangling line list - only the unpaired dangling lines are listed
        List<DanglingLine> danglingLineList = Set.of("DL")
                .stream()
                .map(network::getDanglingLine).toList();

        // Contingencies
        Contingency a = new Contingency("a", Collections.singletonList(new BranchContingency("LINE_S2S3")));
        Contingency b = new Contingency("b", List.of(
            new BranchContingency("LINE_S2S3"),
            new BranchContingency("LINE_S3S4")));

        // Create a contingency provider
        ContingenciesProvider contingenciesProvider = networkLocal -> {
            a.addExtension(Probability.class, new Probability(0.002d, null));
            b.addExtension(Probability.class, new Probability(null, "variable_ts1"));
            return List.of(a, b);
        };

        // Initialize the MetrixNetwork
        MetrixNetwork metrixNetwork = MetrixNetwork.create(network, contingenciesProvider, mappedSwitches, new MetrixParameters(), (Path) null);

        // Check the lists
        assertThat(metrixNetwork.getCountryList()).containsExactlyInAnyOrderElementsOf(Collections.singletonList("Undefined"));
        assertThat(metrixNetwork.getLoadList()).containsExactlyInAnyOrderElementsOf(network.getLoads());
        assertThat(metrixNetwork.getGeneratorList()).containsExactlyInAnyOrderElementsOf(network.getGenerators());
        assertThat(metrixNetwork.getGeneratorTypeList()).containsExactlyInAnyOrderElementsOf(List.of("HYDRO", "THERMAL"));
        assertThat(metrixNetwork.getLineList()).containsExactlyInAnyOrderElementsOf(network.getLines());
        assertThat(metrixNetwork.getTwoWindingsTransformerList()).containsExactlyInAnyOrderElementsOf(network.getTwoWindingsTransformers());
        assertThat(metrixNetwork.getThreeWindingsTransformerList()).containsExactlyInAnyOrderElementsOf(network.getThreeWindingsTransformers());
        assertThat(metrixNetwork.getUnpairedDanglingLineList()).containsExactlyInAnyOrderElementsOf(danglingLineList);
        assertThat(metrixNetwork.getSwitchList()).containsExactlyInAnyOrderElementsOf(switchList);
        assertThat(metrixNetwork.getTieLineList()).containsExactlyInAnyOrderElementsOf(network.getTieLines());
        assertThat(metrixNetwork.getHvdcLineList()).containsExactlyInAnyOrderElementsOf(network.getHvdcLines());
        assertThat(metrixNetwork.getBusList()).containsExactlyInAnyOrderElementsOf(network.getBusBreakerView().getBuses());
        assertThat(metrixNetwork.getContingencyList()).containsExactlyInAnyOrderElementsOf(List.of(a, b));

        assertTrue(metrixNetwork.isMapped(network.getIdentifiable("S1VL2_GH2_BREAKER")));
        assertTrue(metrixNetwork.isMapped(network.getIdentifiable("DL")));
        assertTrue(metrixNetwork.isMapped(network.getIdentifiable("GH2")));
        assertTrue(metrixNetwork.isMapped(network.getIdentifiable("HVDC1")));
        assertTrue(metrixNetwork.isMapped(network.getIdentifiable("LINE_S2S3")));
        assertTrue(metrixNetwork.isMapped(network.getIdentifiable("LD5")));
        assertFalse(metrixNetwork.isMapped(network.getIdentifiable("S2VL1_BBS")));
        assertTrue(metrixNetwork.isMapped(network.getIdentifiable("TL")));
    }

    @Test
    void testNetworkBusBreakerElementsLists() {
        // Mapped switches
        Set<String> mappedSwitches = Set.of("S1VL2_GH2_BREAKER", "S3VL1_LINES3S4_BREAKER", "S1VL1_LD1_BREAKER",
            "S1VL3_DL_BREAKER", "S1VL2_BBS1_BBS3", "S1VL3_3WT_BREAKER");

        // Network
        Network network = createBusBreakerNetwork(mappedSwitches);

        // Expected switch list in MetrixNetwork: switches next to branches (lines, two windings transformers) are not present
        List<Switch> switchList = mappedSwitches.stream().map(network::getSwitch).toList();

        // Expected Dangling line list - only the unpaired dangling lines are listed
        List<DanglingLine> danglingLineList = Set.of("DL").stream().map(network::getDanglingLine).toList();

        // Contingencies
        Contingency a = new Contingency("a", Collections.singletonList(new BranchContingency("LINE_S2S3")));
        Contingency b = new Contingency("b", List.of(
            new BranchContingency("LINE_S2S3"),
            new BranchContingency("LINE_S3S4")));
        Contingency c = new Contingency("c", Collections.singletonList(new DanglingLineContingency("DL")));
        Contingency d = new Contingency("d", Collections.singletonList(new TieLineContingency("TL")));

        // Create a contingency provider
        ContingenciesProvider contingenciesProvider = networkLocal -> {
            a.addExtension(Probability.class, new Probability(0.002d, null));
            b.addExtension(Probability.class, new Probability(0.002d, null));
            c.addExtension(Probability.class, new Probability(0.002d, null));
            d.addExtension(Probability.class, new Probability(0.002d, null));
            return List.of(a, b, c, d);
        };

        // Initialize the MetrixNetwork
        MetrixNetwork metrixNetwork = MetrixNetwork.create(network, contingenciesProvider, mappedSwitches, new MetrixParameters(), (Path) null);

        // Check the lists
        assertThat(metrixNetwork.getCountryList()).containsExactlyInAnyOrderElementsOf(Collections.singletonList("Undefined"));
        assertThat(metrixNetwork.getLoadList()).containsExactlyInAnyOrderElementsOf(network.getLoads());
        assertThat(metrixNetwork.getGeneratorList()).containsExactlyInAnyOrderElementsOf(network.getGenerators());
        assertThat(metrixNetwork.getGeneratorTypeList()).containsExactlyInAnyOrderElementsOf(List.of("HYDRO", "THERMAL"));
        assertThat(metrixNetwork.getLineList()).containsExactlyInAnyOrderElementsOf(network.getLines());
        assertThat(metrixNetwork.getTwoWindingsTransformerList()).containsExactlyInAnyOrderElementsOf(network.getTwoWindingsTransformers());
        assertThat(metrixNetwork.getThreeWindingsTransformerList()).containsExactlyInAnyOrderElementsOf(network.getThreeWindingsTransformers());
        assertThat(metrixNetwork.getUnpairedDanglingLineList()).containsExactlyInAnyOrderElementsOf(danglingLineList);
        assertThat(metrixNetwork.getSwitchList()).containsExactlyInAnyOrderElementsOf(switchList);
        assertThat(metrixNetwork.getTieLineList()).containsExactlyInAnyOrderElementsOf(network.getTieLines());
        assertThat(metrixNetwork.getHvdcLineList()).containsExactlyInAnyOrderElementsOf(network.getHvdcLines());
        assertThat(metrixNetwork.getBusList()).containsExactlyInAnyOrderElementsOf(network.getBusBreakerView().getBuses());
        assertThat(metrixNetwork.getContingencyList()).containsExactlyInAnyOrderElementsOf(List.of(a, b, c, d));
    }

    private Network createBusBreakerNetwork(Set<String> mappedSwitches) {
        // Initial network
        Network network = createNetwork();

        // Set some switches as retained
        List<Switch> retainedSwitches = mappedSwitches.stream().map(network::getSwitch).toList();
        network.getSwitchStream()
                .forEach(sw -> sw.setRetained(retainedSwitches.contains(sw)));

        // Export the network as BusBreaker
        Path exportedFile = fileSystem.getPath("./network.xiidm");
        NetworkSerDe.write(network, new ExportOptions().setTopologyLevel(TopologyLevel.BUS_BREAKER), exportedFile);
        return NetworkSerDe.read(exportedFile);
    }

    private Network createNetwork() {
        // Initial network
        Network network = FourSubstationsNodeBreakerFactory.create();

        // We add a voltage level, a ThreeWindingsTransformer and a DanglingLine
        VoltageLevel s1vl3 = network.getSubstation("S1").newVoltageLevel()
            .setId("S1VL3")
            .setNominalV(225.0)
            .setLowVoltageLimit(220.0)
            .setHighVoltageLimit(240.0)
            .setTopologyKind(TopologyKind.NODE_BREAKER)
            .add();
        s1vl3.getNodeBreakerView().newBusbarSection()
            .setId("S1VL3_BBS")
            .setName("S1VL3_BBS")
            .setNode(0)
            .add();

        createSwitch(s1vl3, "S1VL3_BBS_3WT_DISCONNECTOR", SwitchKind.DISCONNECTOR, 0, 1);
        createSwitch(s1vl3, "S1VL3_3WT_BREAKER", SwitchKind.BREAKER, 1, 2);
        createSwitch(network.getVoltageLevel("S1VL1"), "S1VL1_BBS_3WT_DISCONNECTOR", SwitchKind.DISCONNECTOR, 0, 10);
        createSwitch(network.getVoltageLevel("S1VL1"), "S1VL1_3WT_BREAKER", SwitchKind.BREAKER, 10, 11);
        createSwitch(network.getVoltageLevel("S1VL2"), "S1VL2_BBS_3WT_DISCONNECTOR", SwitchKind.DISCONNECTOR, 0, 40);
        createSwitch(network.getVoltageLevel("S1VL2"), "S1VL2_3WT_BREAKER", SwitchKind.BREAKER, 40, 41);
        network.getSubstation("S1").newThreeWindingsTransformer()
            .setId("3WT")
            .setRatedU0(132.0)
            .newLeg1()
            .setR(17.424)
            .setX(1.7424)
            .setG(0.00573921028466483)
            .setB(0.000573921028466483)
            .setRatedU(132.0)
            .setVoltageLevel(s1vl3.getId())
            .setNode(2)
            .add()
            .newLeg2()
            .setR(1.089)
            .setX(0.1089)
            .setG(0.0)
            .setB(0.0)
            .setRatedU(33.0)
            .setVoltageLevel("S1VL1")
            .setNode(11)
            .add()
            .newLeg3()
            .setR(0.121)
            .setX(0.0121)
            .setG(0.0)
            .setB(0.0)
            .setRatedU(11.0)
            .setVoltageLevel("S1VL2")
            .setNode(41)
            .add()
            .add();

        // Dangling line
        createSwitch(s1vl3, "S1VL3_BBS_DL_DISCONNECTOR", SwitchKind.DISCONNECTOR, 0, 3);
        createSwitch(s1vl3, "S1VL3_DL_BREAKER", SwitchKind.BREAKER, 4, 3);
        s1vl3.newDanglingLine()
            .setId("DL")
            .setR(10.0)
            .setX(1.0)
            .setB(10e-6)
            .setG(10e-5)
            .setP0(50.0)
            .setQ0(30.0)
            .setNode(4)
            .add();

        // We add another bus bar section and link it to the others with a breaker
        network.getVoltageLevel("S1VL2").getNodeBreakerView().newBusbarSection()
            .setId("S1VL2_BBS3")
            .setName("S1VL2_BBS3")
            .setNode(90)
            .add();
        createSwitch(network.getVoltageLevel("S1VL2"), "S1VL2_BBS1_DISCONNECTOR", SwitchKind.DISCONNECTOR, 0, 91);
        createSwitch(network.getVoltageLevel("S1VL2"), "S1VL2_BBS3_DISCONNECTOR", SwitchKind.DISCONNECTOR, 92, 90);
        createSwitch(network.getVoltageLevel("S1VL2"), "S1VL2_BBS1_BBS3", SwitchKind.BREAKER, 91, 92);

        // We now add two dangling lines and a Tie line
        VoltageLevel vlS2VL2 = network.getSubstation("S2").newVoltageLevel()
                .setId("S2VL2")
                .setNominalV(225.0)
                .setLowVoltageLimit(220.0)
                .setHighVoltageLimit(240.0)
                .setTopologyKind(TopologyKind.NODE_BREAKER)
                .add();
        vlS2VL2.getNodeBreakerView().newBusbarSection()
                .setId("S2VL2_BBS")
                .setName("S2VL2_BBS")
                .setNode(10)
                .add();
        createSwitch(vlS2VL2, "S2VL2_BBS_DL_DISCONNECTOR", SwitchKind.DISCONNECTOR, 10, 11);
        createSwitch(vlS2VL2, "S2VL2_DL_BREAKER", SwitchKind.BREAKER, 12, 11);
        DanglingLine dl1 = vlS2VL2.newDanglingLine()
                .setId("DL_S2VL2")
                .setP0(0.0)
                .setQ0(0.0)
                .setR(1.5)
                .setX(20.0)
                .setG(1E-6)
                .setB(386E-6 / 2)
                .setPairingKey("XNODE1")
                .setNode(12)
                .add();
        createSwitch(network.getVoltageLevel("S1VL2"), "S1VL2_BBS_DL_DISCONNECTOR", SwitchKind.DISCONNECTOR, 0, 93);
        createSwitch(network.getVoltageLevel("S1VL2"), "S1VL2_DL_BREAKER", SwitchKind.BREAKER, 93, 94);
        DanglingLine dl2 = network.getVoltageLevel("S1VL2").newDanglingLine()
                .setId("DL_S1VL2")
                .setP0(0.0)
                .setQ0(0.0)
                .setR(1.5)
                .setX(13.0)
                .setG(2E-6)
                .setB(386E-6 / 2)
                .setNode(94)
                .setPairingKey("XNODE1")
                .add();
        network.newTieLine()
                .setId("TL")
                .setDanglingLine1(dl1.getId())
                .setDanglingLine2(dl2.getId())
                .add();

        return network;
    }

    private static void createSwitch(VoltageLevel vl, String id, SwitchKind kind, int node1, int node2) {
        vl.getNodeBreakerView().newSwitch()
            .setId(id)
            .setName(id)
            .setKind(kind)
            .setRetained(kind.equals(SwitchKind.BREAKER))
            .setOpen(false)
            .setFictitious(false)
            .setNode1(node1)
            .setNode2(node2)
            .add();
    }
}
