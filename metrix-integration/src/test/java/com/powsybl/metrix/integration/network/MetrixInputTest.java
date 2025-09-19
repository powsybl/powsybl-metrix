/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration.network;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.commons.PowsyblException;
import com.powsybl.contingency.BranchContingency;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyElement;
import com.powsybl.contingency.GeneratorContingency;
import com.powsybl.contingency.HvdcLineContingency;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.PhaseTapChanger;
import com.powsybl.iidm.network.Switch;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.iidm.network.test.DanglingLineNetworkFactory;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import com.powsybl.iidm.network.test.ThreeWindingsTransformerNetworkFactory;
import com.powsybl.iidm.serde.NetworkSerDe;
import com.powsybl.metrix.integration.MetrixDslData;
import com.powsybl.metrix.integration.binding.MetrixGeneratorsBinding;
import com.powsybl.metrix.integration.type.MetrixHvdcControlType;
import com.powsybl.metrix.integration.MetrixParameters;
import com.powsybl.metrix.integration.type.MetrixPtcControlType;
import com.powsybl.metrix.integration.MetrixSection;
import com.powsybl.metrix.integration.MetrixTimeSeriesVariantProvider;
import com.powsybl.metrix.integration.dataGenerator.MetrixInputData;
import com.powsybl.metrix.integration.chunk.MetrixChunkParam;
import com.powsybl.metrix.commons.data.datatable.DataTableStore;
import com.powsybl.metrix.mapping.MappingParameters;
import com.powsybl.metrix.mapping.TimeSeriesDslLoader;
import com.powsybl.metrix.mapping.config.TimeSeriesMappingConfig;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStore;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStoreCache;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.powsybl.metrix.integration.AbstractCompareTxt.compareStreamTxt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Paul Bui-Quang {@literal <paul.buiquang at rte-france.com>}
 */
class MetrixInputTest {

    protected FileSystem fileSystem;

    @BeforeEach
    void setUp() {
        this.fileSystem = Jimfs.newFileSystem(Configuration.unix());
    }

    @AfterEach
    void tearDown() throws IOException {
        this.fileSystem.close();
    }

    @Test
    void metrixNetworkTest() throws IOException {

        Path remedialActionFile = fileSystem.getPath("/remedialActions.csv");
        try (Writer writer = Files.newBufferedWriter(remedialActionFile, StandardCharsets.UTF_8)) {
            writer.write(String.join(System.lineSeparator(),
                    "NB;1;",
                    "cty1;2;FS.BIS1_FS.BIS1_DJ_OMN;+FVALDI1_FVALDI1_DJ_OMN;"
            ));
        }

        Network n = NetworkSerDe.read(Objects.requireNonNull(getClass().getResourceAsStream("/simpleNetwork.xml")));

        MetrixNetwork metrixNetwork = MetrixNetwork.create(n, null, null, new MetrixParameters(), remedialActionFile);
        Set<Switch> retainedSwitchList = new HashSet<>();
        for (VoltageLevel vl : n.getVoltageLevels()) {
            for (Switch sw : vl.getBusBreakerView().getSwitches()) {
                if (sw.isRetained()) {
                    retainedSwitchList.add(sw);
                }
            }
        }

        List<String> countryList = metrixNetwork.getCountryList();
        assertEquals(countryList.size(), n.getCountries().size());
        for (Country c : n.getCountries()) {
            assertTrue(countryList.contains(c.toString()));
        }

        List<Generator> generatorList = metrixNetwork.getGeneratorList();
        assertEquals(generatorList.size(), n.getGeneratorCount());
        for (Generator g : n.getGenerators()) {
            assertTrue(generatorList.contains(g));
        }

        List<Line> lineList = metrixNetwork.getLineList();
        assertEquals(lineList.size(), n.getLineCount() - 1); // one line opened in the test network
        for (Line l : n.getLines()) {
            assertEquals(l.getTerminal1().isConnected() && l.getTerminal2().isConnected(), lineList.contains(l));
        }

        List<TwoWindingsTransformer> twoWindingsTransformerList = metrixNetwork.getTwoWindingsTransformerList();
        assertEquals(twoWindingsTransformerList.size(), n.getTwoWindingsTransformerCount());
        List<PhaseTapChanger> phaseTapChangerList = metrixNetwork.getPhaseTapChangerList();
        int nbPtc = 0;
        for (TwoWindingsTransformer twt : n.getTwoWindingsTransformers()) {
            assertEquals(twt.getTerminal1().isConnected() && twt.getTerminal2().isConnected(), twoWindingsTransformerList.contains(twt));
            PhaseTapChanger ptc = twt.getPhaseTapChanger();
            if (ptc != null) {
                assertTrue(phaseTapChangerList.contains(ptc));
                nbPtc++;
            }
        }
        assertEquals(twoWindingsTransformerList.size(), nbPtc);

        List<Load> loadList = metrixNetwork.getLoadList();
        assertEquals(loadList.size(), n.getLoadCount() - 1); // There is one disconnected load
        for (Load l : n.getLoads()) {
            assertEquals(l.getTerminal().isConnected(), loadList.contains(l));
        }

        List<HvdcLine> hvdcLineList = metrixNetwork.getHvdcLineList();
        assertEquals(hvdcLineList.size(), n.getHvdcLineCount());
        for (HvdcLine l : n.getHvdcLines()) {
            assertTrue(hvdcLineList.contains(l));
        }

        List<Bus> busList = metrixNetwork.getBusList();
        int nbBus = 0;
        for (VoltageLevel vl : n.getVoltageLevels()) {
            for (Bus bus : vl.getBusBreakerView().getBuses()) {
                if (bus.isInMainConnectedComponent()) {
                    assertTrue(busList.contains(bus));
                    nbBus++;
                }
            }
        }
        assertEquals(busList.size(), nbBus);

        List<Switch> switchList = metrixNetwork.getSwitchList();
        assertEquals(switchList.size(), retainedSwitchList.size());
        for (Switch s : retainedSwitchList) {
            assertTrue(switchList.contains(s));
        }
    }

    @Test
    void metrixDefaultInputTest() throws IOException {
        Network n = NetworkSerDe.read(Objects.requireNonNull(getClass().getResourceAsStream("/simpleNetwork.xml")));
        // Conversion iidm to die
        StringWriter writer = new StringWriter();
        new MetrixInputData(MetrixNetwork.create(n), null, new MetrixParameters()).writeJson(writer);
        writer.close();

        // Results comparison
        String actual = writer.toString();
        assertNotNull(compareStreamTxt(getClass().getResourceAsStream("/simpleNetworkDefault.json"),
                new ByteArrayInputStream(actual.getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    void metrixInputDataWithT3TTest() throws IOException {
        Network n = ThreeWindingsTransformerNetworkFactory.create();
        MetrixInputData metrixInputData = new MetrixInputData(MetrixNetwork.create(n), null, new MetrixParameters());
        try (StringWriter writer = new StringWriter()) {
            assertThrows(PowsyblException.class,
                () -> metrixInputData.writeJson(writer),
                "Three Windings Transformers are not yet supported in metrix");
        }
    }

    @Test
    void metrixInputDataWithTieLinesTests() throws IOException {
        Network n = EurostagTutorialExample1Factory.createWithTieLine();
        // Conversion iidm to die
        StringWriter writer = new StringWriter();
        new MetrixInputData(MetrixNetwork.create(n), null, new MetrixParameters()).writeJson(writer);
        writer.close();

        // Results comparison
        String actual = writer.toString();
        assertNotNull(compareStreamTxt(getClass().getResourceAsStream("/tieLinesNetwork.json"),
                new ByteArrayInputStream(actual.getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    void metrixInputDataWithUnpairedDanglingLineTests() throws IOException {
        Network n = DanglingLineNetworkFactory.createWithGeneration();
        // Conversion iidm to die
        StringWriter writer = new StringWriter();
        new MetrixInputData(MetrixNetwork.create(n), null, new MetrixParameters()).writeJson(writer);
        writer.close();

        // Results comparison
        String actual = writer.toString();
        assertNotNull(compareStreamTxt(getClass().getResourceAsStream("/unpairedDanglingLineNetwork.json"),
                new ByteArrayInputStream(actual.getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    void metrixInputTest() throws IOException {
        Network n = NetworkSerDe.read(Objects.requireNonNull(getClass().getResourceAsStream("/simpleNetwork.xml")));

        // Contingencies
        ContingencyElement l1 = new BranchContingency("FP.AND1  FVERGE1  1");
        ContingencyElement l2 = new BranchContingency("FP.AND1  FVERGE1  2");
        ContingencyElement l3 = new BranchContingency("FS.BIS1  FVALDI1  1");
        ContingencyElement l4 = new BranchContingency("FS.BIS1  FVALDI1  2");
        ContingencyElement l5 = new BranchContingency("FS.BIS1 FSSV.O1 1");
        ContingencyElement l6 = new BranchContingency("FS.BIS1 FSSV.O1 2");
        ContingencyElement l7 = new BranchContingency("FSSV.O1  FP.AND1  1");
        ContingencyElement l8 = new BranchContingency("FSSV.O1  FP.AND1  2");
        ContingencyElement l9 = new BranchContingency("FTDPRA1  FVERGE1  1");
        ContingencyElement l10 = new BranchContingency("FTDPRA1  FVERGE1  2");
        ContingencyElement l11 = new BranchContingency("FVALDI1  FTDPRA1  1");
        ContingencyElement l12 = new BranchContingency("FVALDI1  FTDPRA1  2");
        ContingencyElement l13 = new BranchContingency("BRANCH WRONG ID");

        ContingencyElement g1 = new GeneratorContingency("FSSV.O11_G");
        ContingencyElement g2 = new GeneratorContingency("FSSV.O12_G");
        ContingencyElement g3 = new GeneratorContingency("FVALDI11_G");
        ContingencyElement g4 = new GeneratorContingency("FVERGE11_G");

        ContingencyElement t1 = new BranchContingency("FP.AND1  FTDPRA1  1");

        ContingencyElement h1 = new HvdcLineContingency("HVDC1");
        ContingencyElement h2 = new HvdcLineContingency("HVDC2");

        Contingency cty1 = new Contingency("cty1", Collections.singletonList(l1));
        Contingency cty2 = new Contingency("cty2", Collections.singletonList(g1));
        Contingency cty3 = new Contingency("cty3", Collections.singletonList(t1));
        Contingency cty4 = new Contingency("cty4", Collections.singletonList(h1));
        Contingency cty5 = new Contingency("cty5", Arrays.asList(l1, g1, t1, h1));
        Contingency cty6 = new Contingency("cty6", Arrays.asList(l1, l2, l3, l4, l5, l6, l7, l8, l9, l10, l11, l12));
        Contingency cty7 = new Contingency("cty7", Arrays.asList(g1, g2, g3, g4));
        Contingency cty8 = new Contingency("cty8", Arrays.asList(h1, h2));
        Contingency cty9 = new Contingency("cty9", Collections.singletonList(l13));
        Contingency cty10 = new Contingency("cty10", Arrays.asList(l1, l13));

        ContingenciesProvider provider = network -> List.of(cty1, cty2, cty3, cty4, cty5, cty6, cty7, cty8, cty9, cty10);

        // Metrix dsl data
        MetrixDslData metrixDslData = new MetrixDslData();

        metrixDslData.addBranchMonitoringN("FP.AND1  FVERGE1  1");
        metrixDslData.addBranchMonitoringN("FP.AND1  FVERGE1  2");
        metrixDslData.addBranchMonitoringN("FS.BIS1  FVALDI1  1");
        metrixDslData.addBranchMonitoringN("FS.BIS1  FVALDI1  2");
        metrixDslData.addBranchMonitoringN("FS.BIS1 FSSV.O1 1");
        metrixDslData.addBranchMonitoringN("FS.BIS1 FSSV.O1 2");
        metrixDslData.addBranchMonitoringN("FSSV.O1  FP.AND1  1");
        metrixDslData.addBranchMonitoringN("FSSV.O1  FP.AND1  2");
        metrixDslData.addBranchMonitoringN("FTDPRA1  FVERGE1  1");
        metrixDslData.addBranchMonitoringN("FTDPRA1  FVERGE1  2");
        metrixDslData.addBranchMonitoringN("FVALDI1  FTDPRA1  1");
        metrixDslData.addBranchMonitoringN("FVALDI1  FTDPRA1  2");
        metrixDslData.addBranchResultN("FP.AND1  FTDPRA1  1");

        metrixDslData.addBranchResultNk("FP.AND1  FVERGE1  1");
        metrixDslData.addBranchMonitoringNk("FP.AND1  FVERGE1  2");
        metrixDslData.addBranchMonitoringNk("FS.BIS1  FVALDI1  1");
        metrixDslData.addBranchMonitoringNk("FS.BIS1  FVALDI1  2");
        metrixDslData.addBranchMonitoringNk("FS.BIS1 FSSV.O1 1");
        metrixDslData.addBranchMonitoringNk("FS.BIS1 FSSV.O1 2");
        metrixDslData.addBranchMonitoringNk("FSSV.O1  FP.AND1  1");
        metrixDslData.addBranchMonitoringNk("FSSV.O1  FP.AND1  2");
        metrixDslData.addBranchMonitoringNk("FTDPRA1  FVERGE1  1");
        metrixDslData.addBranchMonitoringNk("FTDPRA1  FVERGE1  2");
        metrixDslData.addBranchMonitoringNk("FVALDI1  FTDPRA1  1");
        metrixDslData.addBranchMonitoringNk("FVALDI1  FTDPRA1  2");
        metrixDslData.addBranchMonitoringNk("FP.AND1  FTDPRA1  1");

        metrixDslData.addContingencyFlowResults("FP.AND1  FVERGE1  2", List.of("cty1", "cty2", "cty10"));
        metrixDslData.addContingencyFlowResults("FS.BIS1 FSSV.O1 1", List.of("cty3"));
        metrixDslData.addContingencyFlowResults("FSSV.O1  FP.AND1  1", List.of("cty10"));

        metrixDslData.setSpecificContingenciesList(List.of("cty1", "cty2", "cty3", "cty4", "cty5", "cty9"));

        metrixDslData.addContingencyDetailedMarginalVariations("FTDPRA1  FVERGE1  1", List.of("cty3", "cty4", "cty9"));
        metrixDslData.addContingencyDetailedMarginalVariations("FTDPRA1  FVERGE1  2", List.of("cty7"));
        metrixDslData.addContingencyDetailedMarginalVariations("FVALDI1  FTDPRA1  1", List.of("cty9"));

        metrixDslData.addPtc("FP.AND1  FTDPRA1  1", MetrixPtcControlType.CONTROL_OFF, List.of(cty1.getId(), cty5.getId()));
        metrixDslData.addLowerTapChange("FP.AND1  FTDPRA1  1", 3);
        metrixDslData.addUpperTapChange("FP.AND1  FTDPRA1  1", 7);

        metrixDslData.addHvdc("HVDC1", MetrixHvdcControlType.OPTIMIZED, List.of(cty5.getId(), cty7.getId(), cty8.getId(), cty9.getId(), cty10.getId()));

        MetrixSection section = new MetrixSection("SECTION", 3000, ImmutableMap.of("HVDC1", 1f, "FVALDI1  FTDPRA1  1", -1f));
        metrixDslData.addSection(section);

        metrixDslData.addGeneratorForAdequacy("FSSV.O11_G");
        metrixDslData.addGeneratorForAdequacy("FVERGE11_G");

        metrixDslData.addGeneratorForRedispatching("FSSV.O11_G", List.of(cty2.getId(), cty4.getId(), "cty9"));
        metrixDslData.addGeneratorForRedispatching("FVALDI11_G", List.of("cty9"));

        metrixDslData.addPreventiveLoad("FVALDI11_L", 20);
        metrixDslData.addPreventiveLoadCost("FVALDI11_L", 20);

        metrixDslData.addPreventiveLoad("FVALDI11_L2", 10);

        metrixDslData.addPreventiveLoad("FVERGE11_L", 100);

        metrixDslData.addPreventiveLoad("FSSV.O11_L", 0);

        metrixDslData.addCurativeLoad("FSSV.O11_L", 10, List.of(cty1.getId(), cty2.getId(), cty4.getId(), "cty9"));
        metrixDslData.addCurativeLoad("FVALDI11_L", 10, new ArrayList<>());

        metrixDslData.addGeneratorsBinding("1 generator group", ImmutableSet.of("FSSV.O11_G", "TOTO"));
        metrixDslData.addGeneratorsBinding("2 generator group", ImmutableSet.of("FSSV.O11_G", "FSSV.O12_G"));
        metrixDslData.addGeneratorsBinding("3 generator group", ImmutableSet.of("FSSV.O12_G", "FVALDI11_G", "FVERGE11_G"), MetrixGeneratorsBinding.ReferenceVariable.POBJ);
        metrixDslData.addLoadsBinding("1 load group", ImmutableSet.of("FVALDI11_L", "TOTO"));
        metrixDslData.addLoadsBinding("2 load group", ImmutableSet.of("FVALDI11_L", "FVALDI11_L2"));
        metrixDslData.addLoadsBinding("3 load group", ImmutableSet.of("FSSV.O11_L", "FVERGE11_L", "FVALDI11_L2"));

        final ObjectMapper objectMapper = new ObjectMapper();
        try (StringWriter sw = new StringWriter()) {
            objectMapper.writeValue(sw, metrixDslData);
            StringReader sr = new StringReader(sw.toString());
            MetrixDslData metrixDslData2 = objectMapper.readValue(sr, MetrixDslData.class);
            assertEquals(metrixDslData, metrixDslData2);
        } catch (IOException ioe) {
            fail();
        }

        // Metrix parameters
        MetrixParameters parameters = new MetrixParameters()
                .setWithGridCost(true)
                .setPreCurativeResults(true)
                .setOutagesBreakingConnexity(true)
                .setRemedialActionsBreakingConnexity(true)
                .setWithAdequacyResults(true)
                .setWithRedispatchingResults(true)
                .setMarginalVariationsOnHvdc(true)
                .setLossDetailPerCountry(true)
                .setPstCostPenality(0.003f)
                .setHvdcCostPenality(0.05f)
                .setMaxSolverTime(120)
                .setLossNbRelaunch(2)
                .setLossThreshold(504)
                .setLossOfLoadCost(13000f)
                .setCurativeLossOfLoadCost(26000f)
                .setCurativeLossOfGenerationCost(100f)
                .setContingenciesProbability(0.001f)
                .setNbMaxIteration(3)
                .setNbMaxCurativeAction(4)
                .setGapVariableCost(10000)
                .setNbThreatResults(5)
                .setRedispatchingCostOffset(40)
                .setAdequacyCostOffset(50)
                .setCurativeRedispatchingLimit(1500);

        try (StringWriter sw = new StringWriter()) {
            objectMapper.writeValue(sw, parameters);
            StringReader sr = new StringReader(sw.toString());
            MetrixParameters parameters2 = objectMapper.readValue(sr, MetrixParameters.class);
            assertEquals(parameters, parameters2);
        } catch (IOException ioe) {
            fail();
        }

        Path remedialActionFile = fileSystem.getPath("/remedialActions.csv");
        try (Writer writer = Files.newBufferedWriter(remedialActionFile, StandardCharsets.UTF_8)) {
            writer.write(String.join(System.lineSeparator(),
                    "NB;6;",
                    "cty1;2;FS.BIS1_FS.BIS1_DJ_OMN;FSSV.O1_FSSV.O1_DJ_OMN;",
                    "cty1;1;+FP.AND1_FP.AND1  FVERGE1  1_DJ5;", // This shall not raise an exception
                    "cty1;1;+FVALDI1_FVALDI1_DJ_OMN;",
                    //"cty1;1;+FP.AND1  FVERGE1  1;",
                    //"cty1;2;+FVALDI1_FVALDI1_DJ_OMN;+FP.AND1  FVERGE1  1;",
                    "cty1;2;UNKNOWN_BRANCH;+UNKNOWN_BRANCH;"
            ));
        }

        // Conversion iidm to die
        StringWriter writer = new StringWriter();
        MetrixInputData inputData = new MetrixInputData(MetrixNetwork.create(n, provider, null, parameters, remedialActionFile), metrixDslData, parameters);

        // 175 = 13 branches * 13 (N, 2*5 Nk, 2*Itam) + 5 detailed + 1 section
        assertEquals(175, inputData.minResultNumberEstimate());
        // 315 = 175 + (2adcy, 2prev, 4cur) gen + 2pst cur + (1prev, 5cur) hvdc + 4 load cur + (12*9 + 1section + 1hvdc + 2*5 detailed)marg.var.
        assertEquals(315, inputData.maxResultNumberEstimate());

        inputData.writeJson(writer);
        writer.close();

        // Results comparison
        String actual = writer.toString();
        try {
            assertNotNull(compareStreamTxt(getClass().getResourceAsStream("/simpleNetwork.json"),
                    new ByteArrayInputStream(actual.getBytes(StandardCharsets.UTF_8))));
        } catch (UncheckedIOException e) {
            fail();
        }
    }

    @Test
    void mappedBreakerTest() throws IOException {

        Network n = NetworkSerDe.read(Objects.requireNonNull(getClass().getResourceAsStream("/simpleNetwork.xml")));

        String[] mappedBreakers = {
            "FP.AND1_FP.AND1  FTDPRA1  1_DJ13", // PST breaker
            "FS.BIS1_FS.BIS1  FVALDI1  1_DJ5", // Line breaker
            "FSSV.O1_FSSV.O1_DJ_OMN", // bus breaker
            "FP.AND1_FP.AND1  FVERGE1  1_DJ5", // opened line breaker -> will be closed
            "FVERGE1_FP.AND1  FVERGE1  1_DJ5", // closed breaker of an opened line,
            "FSSV.O1_Sect 1 FS.BIS1 FSSV.O1 2",  // disconnector,
            "FSSV.O1_Disj FSSV.O11_L", // load breaker,
            "FSSV.O1_Disj FSSV.O12_G", // generator breaker
            "FSSV.O1_Disj HVDC2,", //hvdc breaker -> ignored
            "UNKNOWN_SWITCH"
        }; //unknown switch -> ignored

        // Creates mapping file
        Path mappingFile = fileSystem.getPath("/mapping.groovy");
        try (Writer writer = Files.newBufferedWriter(mappingFile, StandardCharsets.UTF_8)) {
            writer.write("timeSeries['foo'] = 0");
            for (String s : mappedBreakers) {
                writer.write(String.join(System.lineSeparator(), System.lineSeparator(),
                        "mapToBreakers {",
                        "    timeSeriesName 'foo'",
                        "    filter { breaker.id == '" + s + "' }",
                        "}"));
            }
        }

        MetrixVariantProvider variantProvider;
        try (Reader mappingReader = Files.newBufferedReader(mappingFile, StandardCharsets.UTF_8)) {
            ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache();
            MappingParameters mappingParameters = MappingParameters.load();
            TimeSeriesMappingConfig mappingConfig = new TimeSeriesDslLoader(mappingReader).load(n, mappingParameters, store, new DataTableStore(), null, null);
            MetrixChunkParam metrixChunkParam = new MetrixChunkParam.MetrixChunkParamBuilder()
                    .simpleInit(1, false, false, ignored -> Collections.emptyList(),
                            null, null, null, null).build();
            variantProvider = new MetrixTimeSeriesVariantProvider(n, store, mappingParameters, mappingConfig, new MetrixDslData(), metrixChunkParam,
                    Range.closed(0, 1), System.err);
        }

        MetrixNetwork metrixNetwork = MetrixNetwork.create(n, null, variantProvider.getMappedBreakers(), new MetrixParameters(), (Path) null);

        assertEquals("FP.AND1  FTDPRA1  1", metrixNetwork.getMappedBranch(n.getSwitch(mappedBreakers[0])).orElse("NOT PRESENT"));
        assertEquals("FS.BIS1  FVALDI1  1", metrixNetwork.getMappedBranch(n.getSwitch(mappedBreakers[1])).orElse("NOT PRESENT"));
        assertEquals("FSSV.O1_FSSV.O1_DJ_OMN", metrixNetwork.getMappedBranch(n.getSwitch(mappedBreakers[2])).orElse("NOT PRESENT"));
        assertEquals("FP.AND1  FVERGE1  1", metrixNetwork.getMappedBranch(n.getSwitch(mappedBreakers[3])).orElse("NOT PRESENT"));
        assertEquals("FP.AND1  FVERGE1  1", metrixNetwork.getMappedBranch(n.getSwitch(mappedBreakers[4])).orElse("NOT PRESENT"));
        assertEquals("FSSV.O1_Sect 1 FS.BIS1 FSSV.O1 2", metrixNetwork.getMappedBranch(n.getSwitch(mappedBreakers[5])).orElse("NOT PRESENT"));
        assertEquals("FSSV.O1_Disj FSSV.O11_L", metrixNetwork.getMappedBranch(n.getSwitch(mappedBreakers[6])).orElse("NOT PRESENT"));
        assertEquals("FSSV.O1_Disj FSSV.O12_G", metrixNetwork.getMappedBranch(n.getSwitch(mappedBreakers[7])).orElse("NOT PRESENT"));
        assertEquals("NOT PRESENT", metrixNetwork.getMappedBranch(n.getSwitch(mappedBreakers[8])).orElse("NOT PRESENT"));
        assertEquals("NOT PRESENT", metrixNetwork.getMappedBranch(n.getSwitch(mappedBreakers[9])).orElse("NOT PRESENT"));
    }

    @Test
    void propagateTrippingTest() {
        Network n = NetworkSerDe.read(Objects.requireNonNull(getClass().getResourceAsStream("/simpleNetwork.xml")));

        ContingencyElement l = new BranchContingency("FTDPRA1  FVERGE1  1");
        Contingency cty = new Contingency("cty", l);

        MetrixNetwork metrixNetwork = MetrixNetwork.create(n);

        ContingencyElement l2 = new BranchContingency("FTDPRA1  FVERGE1  2");
        ContingencyElement l3 = new BranchContingency("FVALDI1  FTDPRA1  1");
        ContingencyElement l4 = new BranchContingency("FVALDI1  FTDPRA1  2");
        ContingencyElement l5 = new BranchContingency("FP.AND1  FTDPRA1  1");
        assertEquals(metrixNetwork.getElementsToTrip(cty, true), ImmutableSet.of(l, l2, l3, l4, l5));
        assertEquals(metrixNetwork.getElementsToTrip(cty, false), ImmutableSet.of(l));

        ContingencyElement h = new HvdcLineContingency("HVDC1");
        cty = new Contingency("cty", l4, h);
        assertEquals(metrixNetwork.getElementsToTrip(cty, true), ImmutableSet.of(l4, h));
        assertEquals(metrixNetwork.getElementsToTrip(cty, false), ImmutableSet.of(l4, h));

        ContingencyElement g = new GeneratorContingency("FSSV.O11_G");
        cty = new Contingency("cty", g, l3);
        assertEquals(metrixNetwork.getElementsToTrip(cty, true), ImmutableSet.of(g, l3));
        assertEquals(metrixNetwork.getElementsToTrip(cty, false), ImmutableSet.of(g, l3));
    }

    @Test
    void loadBreakTest() throws IOException {
        Network n = NetworkSerDe.read(Objects.requireNonNull(getClass().getResourceAsStream("/simpleNetwork.xml")));

        // Contingencies
        ContingencyElement l1 = new BranchContingency("FP.AND1  FVERGE1  2");
        Contingency cty1 = new Contingency("cty1", Collections.singletonList(l1));
        ContingenciesProvider provider = network -> List.of(cty1);

        // Metrix parameters
        MetrixParameters parameters = new MetrixParameters()
                .setRemedialActionsBreakingConnexity(true);

        Path remedialActionFile = fileSystem.getPath("/remedialActions.csv");
        try (Writer writer = Files.newBufferedWriter(remedialActionFile, StandardCharsets.UTF_8)) {
            writer.write(String.join(System.lineSeparator(),
                    "NB;2;",
                    "cty1;1;FSSV.O1_Disj FSSV.O11_L;"));
        }

        MetrixNetwork metrixNetwork = MetrixNetwork.create(n, provider, null, parameters, remedialActionFile);
        new MetrixInputData(metrixNetwork, new MetrixDslData(), parameters);

        assertEquals(List.of(n.getSwitch("FSSV.O1_Disj FSSV.O11_L")), metrixNetwork.getSwitchList());
        assertEquals(Collections.EMPTY_SET, metrixNetwork.getDisconnectedElements());
    }
}
