package com.powsybl.metrix.integration;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.contingency.BranchContingency;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.test.FourSubstationsNodeBreakerFactory;
import com.powsybl.metrix.integration.contingency.Probability;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Nicolas Rol {@literal <nicolas.rol at rte-france.com>}
 */
class MetrixNetworkTest {
    private FileSystem fileSystem;

    @BeforeEach
    public void setUp() {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
    }

    @AfterEach
    public void tearDown() throws IOException {
        fileSystem.close();
    }

    @Test
    void testNetworkElementsLists() {
        // Network
        Network network = createNetwork();

        // Set some switches as retained
        Set<String> mappedSwitches = Set.of("S1VL2_GH2_BREAKER", "S3VL1_LINES3S4_BREAKER", "S1VL1_LD1_BREAKER");
        List<Switch> switchList = mappedSwitches.stream()
            .map(network::getSwitch).toList();

        // Contingencies
        Contingency a = new Contingency("a", Collections.singletonList(new BranchContingency("LINE_S2S3")));
        Contingency b = new Contingency("b", Arrays.asList(
            new BranchContingency("LINE_S2S3"),
            new BranchContingency("LINE_S3S4")));

        // Create a contingency provider
        ContingenciesProvider contingenciesProvider = networkLocal -> {
            a.addExtension(Probability.class, new Probability(0.002d, null));
            b.addExtension(Probability.class, new Probability(null, "variable_ts1"));
            return Arrays.asList(a, b);
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
        assertThat(metrixNetwork.getDanglingLineList()).containsExactlyInAnyOrderElementsOf(network.getDanglingLines());
        assertThat(metrixNetwork.getSwitchList()).containsExactlyInAnyOrderElementsOf(switchList);
        assertThat(metrixNetwork.getHvdcLineList()).containsExactlyInAnyOrderElementsOf(network.getHvdcLines());
        assertThat(metrixNetwork.getBusList()).containsExactlyInAnyOrderElementsOf(network.getBusBreakerView().getBuses());
        assertThat(metrixNetwork.getContingencyList()).containsExactlyInAnyOrderElementsOf(List.of(a, b));
    }

    private Network createNetwork() {
        // Initial network
        Network network = FourSubstationsNodeBreakerFactory.create();

        // We add a substation, a ThreeWindingsTransformer and a DanglingLine
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

        createSwitch(s1vl3, "S1VL3_BBS_3WT_DISCONNECTOR", SwitchKind.DISCONNECTOR, false, 0, 1);
        createSwitch(s1vl3, "S1VL3_3WT_BREAKER", SwitchKind.BREAKER, false, 1, 2);
        createSwitch(network.getVoltageLevel("S1VL1"), "S1VL1_BBS_3WT_DISCONNECTOR", SwitchKind.DISCONNECTOR, false, 0, 10);
        createSwitch(network.getVoltageLevel("S1VL1"), "S1VL1_3WT_BREAKER", SwitchKind.BREAKER, false, 10, 11);
        createSwitch(network.getVoltageLevel("S1VL2"), "S1VL2_BBS_3WT_DISCONNECTOR", SwitchKind.DISCONNECTOR, false, 0, 40);
        createSwitch(network.getVoltageLevel("S1VL2"), "S1VL2_3WT_BREAKER", SwitchKind.BREAKER, false, 40, 41);
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
        createSwitch(s1vl3, "S1VL3_BBS_DL_DISCONNECTOR", SwitchKind.DISCONNECTOR, false, 0, 3);
        createSwitch(s1vl3, "S1VL3_DL_BREAKER", SwitchKind.BREAKER, false, 3, 4);
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

        return network;
    }

    private static void createSwitch(VoltageLevel vl, String id, SwitchKind kind, boolean open, int node1, int node2) {
        vl.getNodeBreakerView().newSwitch()
            .setId(id)
            .setName(id)
            .setKind(kind)
            .setRetained(kind.equals(SwitchKind.BREAKER))
            .setOpen(open)
            .setFictitious(false)
            .setNode1(node1)
            .setNode2(node2)
            .add();
    }
}
