/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.integration;

import com.google.common.collect.Range;
import com.google.common.collect.Sets;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.extensions.LoadDetail;
import com.powsybl.iidm.network.extensions.LoadDetailAdder;
import com.powsybl.iidm.network.impl.VariantManagerHolder;
import com.powsybl.iidm.network.impl.VariantManagerImpl;
import com.powsybl.iidm.network.impl.extensions.LoadDetailAdderImpl;
import com.powsybl.metrix.mapping.EquipmentVariable;
import com.powsybl.timeseries.RegularTimeSeriesIndex;
import com.powsybl.timeseries.TimeSeriesIndex;
import org.junit.Test;
import org.mockito.Mockito;
import org.threeten.extra.Interval;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.time.Duration;
import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian@rte-france.com>
 */
public class MetrixVariantsWriterTest {

    @Test
    public void baseCaseTest() throws IOException {
        StringWriter writer = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(writer)) {
            new MetrixVariantsWriter(null, null)
                    .write(null, bufferedWriter);
        }
        assertEquals(String.join(System.lineSeparator(),
                "NT;1;",
                "0;") + System.lineSeparator(),
                writer.toString());
    }

    @Test
    public void variantsTest() throws IOException {
        TimeSeriesIndex index = RegularTimeSeriesIndex.create(
                Interval.parse("2015-01-01T00:00:00Z/2015-01-01T01:00:00Z"),
                Duration.ofMinutes(15));
        StringWriter writer = new StringWriter();

        MetrixNetwork network =  Mockito.mock(MetrixNetwork.class);
        Mockito.when(network.getIndex(MetrixSubset.LOAD, "l1")).thenReturn(2);
        Mockito.when(network.getIndex(MetrixSubset.LOAD, "l2")).thenReturn(1);
        Mockito.when(network.getIndex(MetrixSubset.LOAD, "l3")).thenReturn(1);
        Mockito.when(network.getIndex(MetrixSubset.LOAD, "l4")).thenReturn(1);
        Mockito.when(network.getIndex(MetrixSubset.LOAD, "l5")).thenReturn(1);
        Mockito.when(network.getIndex(MetrixSubset.LOAD, "l6")).thenReturn(1);
        Mockito.when(network.getIndex(MetrixSubset.LOAD, "l7")).thenReturn(1);
        Mockito.when(network.getIndex(MetrixSubset.LOAD, "l8")).thenReturn(1);
        Mockito.when(network.getIndex(MetrixSubset.LOAD, "l9")).thenReturn(1);
        Mockito.when(network.getIndex(MetrixSubset.LOAD, "l10")).thenReturn(1);
        Mockito.when(network.getIndex(MetrixSubset.LOAD, "l13")).thenThrow(new IllegalStateException());

        try (BufferedWriter bufferedWriter = new BufferedWriter(writer)) {
            new MetrixVariantsWriter(new MetrixVariantProvider() {
                @Override
                public Range<Integer> getVariantRange() {
                    return Range.closed(0,  3);
                }

                @Override
                public TimeSeriesIndex getIndex() {
                    return index;
                }

                public Set<String> getMappedBreakers() {
                    return Sets.newHashSet("sw1", "sw2", "sw3");
                }

                private Generator createGenerator(String id, double targetP) {
                    Generator g = Mockito.mock(Generator.class);
                    Mockito.when(g.getId()).thenReturn(id);
                    Mockito.when(g.getTargetP()).thenReturn(targetP);
                    return g;
                }

                Map<String, LoadDetail> loadDetailMap = new HashMap<>();

                private Load createLoad(String id, double p0, String busId, LoadDetail detail) {
                    loadDetailMap.put(id, detail);
                    AbstractNetworkImplTest underlyingNetwork = Mockito.mock(AbstractNetworkImplTest.class);
                    Mockito.when(underlyingNetwork.getVariantIndex()).thenReturn(0);
                    Mockito.when(underlyingNetwork.getVariantManager()).thenReturn(Mockito.mock(VariantManagerImpl.class));
                    Mockito.when(underlyingNetwork.getVariantManager().getVariantArraySize()).thenReturn(1);
                    Load l = Mockito.mock(Load.class);
                    Mockito.when(l.getNetwork()).thenReturn(underlyingNetwork);
                    Mockito.when(l.getId()).thenReturn(id);
                    Mockito.when(l.getP0()).thenReturn(p0);
                    Terminal t = Mockito.mock(Terminal.class);
                    Mockito.when(l.getTerminal()).thenReturn(t);
                    Terminal.BusBreakerView view = Mockito.mock(Terminal.BusBreakerView.class);
                    Mockito.when(t.getBusBreakerView()).thenReturn(view);
                    Bus b = Mockito.mock(Bus.class);
                    Mockito.when(view.getBus()).thenReturn(b);
                    if (busId.isEmpty()) {
                        Mockito.when(b.getId()).thenReturn(id + "_bus");
                    } else {
                        Mockito.when(b.getId()).thenReturn(busId);
                    }
                    Mockito.when(b.getLoads()).thenReturn(Arrays.asList(l));
                    Mockito.when(l.getExtension(LoadDetail.class)).thenAnswer(invocationOnMock -> loadDetailMap.get(id));
                    Mockito.when(l.newExtension(LoadDetailAdder.class)).thenReturn(new LoadDetailAdderImpl(l));
                    Mockito.doAnswer(invocationOnMock -> loadDetailMap.put(id, (LoadDetail) invocationOnMock.getArguments()[1]))
                            .when(l).addExtension(Mockito.any(), Mockito.any());
                    return l;
                }

                private LoadDetail createLoadDetail(float fixedActivePower, float variableActivePower) {
                    LoadDetail l = Mockito.mock(LoadDetail.class);
                    Mockito.when(l.getFixedActivePower()).thenReturn(fixedActivePower);
                    Mockito.when(l.getVariableActivePower()).thenReturn(variableActivePower);
                    return l;
                }

                private List<Load> createLoads(List<String> idList, List<Float> p0List, String busId, List<LoadDetail> detailList) {
                    List<Load> loadList = new ArrayList<Load>();
                    for (int i = 0; i < idList.size(); i++) {
                        Load l = createLoad(idList.get(i), p0List.get(i), busId, detailList == null ? null : detailList.get(i));
                        loadList.add(l);
                    }
                    Bus b = loadList.get(0).getTerminal().getBusBreakerView().getBus();
                    Mockito.when(b.getLoads()).thenReturn(loadList);
                    return loadList;
                }

                private HvdcLine createHvdcLine(String id, double activePowerSetpoint) {
                    HvdcLine l = Mockito.mock(HvdcLine.class);
                    Mockito.when(l.getId()).thenReturn(id);
                    Mockito.when(l.getActivePowerSetpoint()).thenReturn(activePowerSetpoint);
                    return l;
                }

                private TwoWindingsTransformer createPst(String id, double currentTap) {
                    TwoWindingsTransformer twc = Mockito.mock(TwoWindingsTransformer.class);
                    Mockito.when(twc.getId()).thenReturn(id);
                    PhaseTapChanger pst = Mockito.mock(PhaseTapChanger.class);
                    Mockito.when(twc.getPhaseTapChanger()).thenReturn(pst);
                    Mockito.when(pst.getTapPosition()).thenReturn((int) currentTap);
                    return twc;
                }

                private Line createLine(String id) {
                    Line l = Mockito.mock(Line.class);
                    Mockito.when(l.getId()).thenReturn(id);
                    return l;
                }

                private Switch createSwitch(String id) {
                    Switch s = Mockito.mock(Switch.class);
                    Mockito.when(s.getId()).thenReturn(id);
                    Mockito.when(s.getKind()).thenReturn(SwitchKind.BREAKER);
                    Mockito.when(s.isRetained()).thenReturn(true);
                    Mockito.when(s.isOpen()).thenReturn(false);
                    return s;
                }

                @Override
                public void readVariants(Range<Integer> variantReadRange, MetrixVariantReader reader) {
                    Generator g1 = createGenerator("g1", 100f);
                    Load l1 = createLoad("l1", 10f, "", null);
                    Load l2 = createLoad("l2", 15f, "", null);
                    Load l3 = createLoad("l3", 7f, "", null);
                    Load l4 = createLoad("l4", 10f, "", null);
                    Load l5 = createLoad("l5", 16f, "", null);
                    LoadDetail l6d = createLoadDetail(3f, 12f);
                    Load l6 = createLoad("l6", 15f, "", l6d);
                    LoadDetail l7d = createLoadDetail(3f, 12f);
                    Load l7 = createLoad("l7", 18f, "", l7d);
                    LoadDetail l8d = createLoadDetail(3f, 12f);
                    Load l8 = createLoad("l8", 19f, "", l8d);
                    LoadDetail l9d = createLoadDetail(3f, 10f);
                    Load l9 = createLoad("l9", 15f, "", l9d);
                    LoadDetail l10d = createLoadDetail(3f, 12f);
                    Load l10 = createLoad("l10", 15f, "", l10d);
                    List<Load> loadList = createLoads(Arrays.asList("l11", "l12"), Arrays.asList(12f, 18f), "l1", null);
                    Load l11 = loadList.get(0);
                    Load l12 = loadList.get(1);
                    Load l13 = createLoad("l13", 0f, "", null);

                    HvdcLine hl1 = createHvdcLine("hl1", 200f);
                    TwoWindingsTransformer pst1 = createPst("pst1", 17f);
                    Line line1 = createLine("line1");

                    Switch sw1 = createSwitch("sw1");
                    Switch sw2 = createSwitch("sw2");
                    Switch sw3 = createSwitch("sw3");

                    Mockito.when(network.getMappedBranch(sw1)).thenReturn(Optional.of("sw1"));
                    Mockito.when(network.getMappedBranch(sw2)).thenReturn(Optional.of("line1"));
                    Mockito.when(network.getMappedBranch(sw3)).thenReturn(Optional.of("hl1"));

                    for (int i = variantReadRange.lowerEndpoint(); i < variantReadRange.upperEndpoint(); i++) {
                        reader.onVariantStart(i);

                        // Generator
                        reader.onEquipmentVariant(g1, EquipmentVariable.targetP, 100.01f + i);
                        reader.onEquipmentVariant(g1, EquipmentVariable.minP, 111f + i);
                        reader.onEquipmentVariant(g1, EquipmentVariable.maxP, 121f + i);

                        // Load without LoadDetail
                        reader.onEquipmentVariant(l1, EquipmentVariable.p0, 10f + i);
                        reader.onEquipmentVariant(l2, EquipmentVariable.p0, 16f + i);                   // CONELE = 16 + i
                        reader.onEquipmentVariant(l3, EquipmentVariable.fixedActivePower, 6f + i);      // CONELE = 6 + i
                        reader.onEquipmentVariant(l4, EquipmentVariable.variableActivePower, 8f + i);   // CONELE = 8 + i
                        reader.onEquipmentVariant(l5, EquipmentVariable.fixedActivePower, 6f + i);
                        reader.onEquipmentVariant(l5, EquipmentVariable.variableActivePower, 8f + i);   // CONELE = 14 + 2*i
                        reader.onEquipmentVariant(l6, EquipmentVariable.fixedActivePower, 5f + i);
                        reader.onEquipmentVariant(l6, EquipmentVariable.variableActivePower, 10f - i);  // not present cause identical to base case

                        // Load with LoadDetail
                        reader.onEquipmentVariant(l7, EquipmentVariable.p0, 17f + i);                   // CONELE = 17 + i
                        reader.onEquipmentVariant(l8, EquipmentVariable.fixedActivePower, 6f + i);      // CONELE = 6 + i + 12
                        reader.onEquipmentVariant(l9, EquipmentVariable.variableActivePower, 8f + i);   // CONELE = 3 + 8 + i
                        reader.onEquipmentVariant(l10, EquipmentVariable.fixedActivePower, 6f + i);
                        reader.onEquipmentVariant(l10, EquipmentVariable.variableActivePower, 8f + i);  // CONELE = 14 + 2*i

                        // Loads on a same Bus
                        reader.onEquipmentVariant(l11, EquipmentVariable.p0, 10f + i);
                        reader.onEquipmentVariant(l12, EquipmentVariable.p0, 20f - i);                  // not present cause identical to base case

                        // Hvdc
                        reader.onEquipmentVariant(hl1, EquipmentVariable.activePowerSetpoint, 200.0001f + i);
                        reader.onEquipmentVariant(hl1, EquipmentVariable.minP, 210f + i);
                        reader.onEquipmentVariant(hl1, EquipmentVariable.maxP, 220f + i);

                        // Breakers
                        reader.onEquipmentVariant(sw1, EquipmentVariable.open, (i + 1) % 2);
                        reader.onEquipmentVariant(sw2, EquipmentVariable.open, i % 2);
                        reader.onEquipmentVariant(sw3, EquipmentVariable.open, (i + 1) % 2);

                        // Metrix
                        reader.onEquipmentVariant(line1, MetrixVariable.thresholdN, 1001f + i);
                        reader.onEquipmentVariant(line1, MetrixVariable.thresholdN1, 1011f + i);
                        reader.onEquipmentVariant(line1, MetrixVariable.thresholdNk, 1021f + i);
                        reader.onEquipmentVariant(line1, MetrixVariable.thresholdITAM, 1031f + i);
                        reader.onEquipmentVariant(line1, MetrixVariable.thresholdITAMNk, 1041f + i);
                        reader.onEquipmentVariant(line1, MetrixVariable.thresholdNEndOr, 2001f + i);
                        reader.onEquipmentVariant(line1, MetrixVariable.thresholdN1EndOr, 2011f + i);
                        reader.onEquipmentVariant(line1, MetrixVariable.thresholdNkEndOr, 2021f + i);
                        reader.onEquipmentVariant(line1, MetrixVariable.thresholdITAMEndOr, 2031f + i);
                        reader.onEquipmentVariant(line1, MetrixVariable.thresholdITAMNkEndOr, 2041f + i);

                        reader.onEquipmentVariant(g1, MetrixVariable.offGridCostDown, 1111f + i);
                        reader.onEquipmentVariant(g1, MetrixVariable.offGridCostUp, 1121f + i);
                        reader.onEquipmentVariant(g1, MetrixVariable.onGridCostDown, 1131f + i);
                        reader.onEquipmentVariant(g1, MetrixVariable.onGridCostUp, 1141f + i);

                        // Pst
                        reader.onEquipmentVariant(pst1, EquipmentVariable.currentTap, 17f + i);

                        reader.onEquipmentVariant(l1, MetrixVariable.curativeCostDown, 10f + i);

                        // load out of main cc
                        reader.onEquipmentVariant(l13, MetrixVariable.curativeCostDown, i);

                        reader.onVariantEnd(i);
                    }
                    reader.onVariantStart(variantReadRange.upperEndpoint());
                    reader.onVariantEnd(variantReadRange.upperEndpoint());
                }
            }, network).write(Range.closed(0, 3), bufferedWriter);
        }
        assertEquals(String.join(System.lineSeparator(),
            "NT;4;",
            "0;QUADIN;1;line1;",
            "0;PRODIM;1;g1;100.01;",
            "0;TRPUIMIN;1;g1;111;",
            "0;TRVALPMD;1;g1;121;",
//            "0;DCIMPPUI;1;hl1;200;", // hl1 not present cause too close to base case
            "0;DCMAXPUI;1;hl1;220;",
            "0;DCMINPUI;1;hl1;210;",
            "0;CONELE;10;l2;16;l7;17;l11;10;l12;20;l3;6;l4;8;l5;14;l8;18;l9;11;l10;14;", // // l1 not present cause always identical to base case
            "0;QATI00MN;1;line1;1001;",
            "0;QATI00MN2;1;line1;2001;",
            "0;QATI20MN;1;line1;1021;",
            "0;QATITAMN;1;line1;1031;",
            "0;COUHAR;1;g1;1141;",
            "0;QATI20MN2;1;line1;2021;",
            "0;QATITAMK;1;line1;1041;",
            "0;COUEFF;2;l1;10;l13;0;",
            "0;QATI5MNS;1;line1;1011;",
            "0;QATITAMK2;1;line1;2041;",
            "0;QATI5MNS2;1;line1;2011;",
            "0;QATITAMN2;1;line1;2031;",
            "0;COUBHR;1;g1;1111;",
            "0;CTORDR;1;g1;1121;",
            "0;COUBAR;1;g1;1131;",
            "1;QUADIN;2;sw1;hl1;",
            "1;PRODIM;1;g1;101.01;",
            "1;TRPUIMIN;1;g1;112;",
            "1;TRVALPMD;1;g1;122;",
            "1;DCIMPPUI;1;hl1;201;", // rounded
            "1;DCMAXPUI;1;hl1;221;",
            "1;DCMINPUI;1;hl1;211;",
            "1;CONELE;7;l1;11;l2;17;l11;11;l12;19;l4;9;l9;12;l10;16;",
                "1;DTVALDEP;1;pst1;18;",
            "1;QATI00MN;1;line1;1002;",
            "1;QATI00MN2;1;line1;2002;",
            "1;QATI20MN;1;line1;1022;",
            "1;QATITAMN;1;line1;1032;",
            "1;COUHAR;1;g1;1142;",
            "1;QATI20MN2;1;line1;2022;",
            "1;QATITAMK;1;line1;1042;",
            "1;COUEFF;2;l1;11;l13;1;",
            "1;QATI5MNS;1;line1;1012;",
            "1;QATITAMK2;1;line1;2042;",
            "1;QATI5MNS2;1;line1;2012;",
            "1;QATITAMN2;1;line1;2032;",
            "1;COUBHR;1;g1;1112;",
            "1;CTORDR;1;g1;1122;",
            "1;COUBAR;1;g1;1132;",
            "2;QUADIN;1;line1;",
            "2;PRODIM;1;g1;102.01;",
            "2;TRPUIMIN;1;g1;113;",
            "2;TRVALPMD;1;g1;123;",
            "2;DCIMPPUI;1;hl1;202;", // rounded
            "2;DCMAXPUI;1;hl1;222;",
            "2;DCMINPUI;1;hl1;212;",
            "2;CONELE;8;l1;12;l2;18;l7;19;l3;8;l5;18;l8;20;l9;13;l10;18;",
                "2;DTVALDEP;1;pst1;19;",
            "2;QATI00MN;1;line1;1003;",
            "2;QATI00MN2;1;line1;2003;",
            "2;QATI20MN;1;line1;1023;",
            "2;QATITAMN;1;line1;1033;",
            "2;COUHAR;1;g1;1143;",
            "2;QATI20MN2;1;line1;2023;",
            "2;QATITAMK;1;line1;1043;",
            "2;COUEFF;2;l1;12;l13;2;",
            "2;QATI5MNS;1;line1;1013;",
            "2;QATITAMK2;1;line1;2043;",
            "2;QATI5MNS2;1;line1;2013;",
            "2;QATITAMN2;1;line1;2033;",
            "2;COUBHR;1;g1;1113;",
            "2;CTORDR;1;g1;1123;",
            "2;COUBAR;1;g1;1133;",
            "3;") // 4th variant is empty
                + System.lineSeparator(),
            writer.toString());
    }

    abstract class AbstractNetworkImplTest implements Network, VariantManagerHolder {

    }
}