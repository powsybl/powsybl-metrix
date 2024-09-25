/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration;

import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.util.StringToIntMapper;
import com.powsybl.contingency.*;
import com.powsybl.iidm.modification.tripping.Tripping;
import com.powsybl.iidm.network.*;
import com.powsybl.metrix.integration.metrix.MetrixInputAnalysis;
import com.powsybl.metrix.integration.remedials.Remedial;
import com.powsybl.metrix.integration.remedials.RemedialReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Paul Bui-Quang {@literal <paul.buiquang at rte-france.com>}
 */
public class MetrixNetwork {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetrixNetwork.class);
    private static final String PAYS_CVG_PROPERTY = "paysCvg";
    private static final String PAYS_CVG_UNDEFINED = "Undefined";

    private final Network network;

    private final StringToIntMapper<MetrixSubset> mapper = new StringToIntMapper<>(MetrixSubset.class);

    private final Set<String> countryList = new HashSet<>();

    private final Set<Load> loadList = new LinkedHashSet<>();

    private final Set<Generator> generatorList = new LinkedHashSet<>();
    private final Set<String> generatorTypeList = new HashSet<>();

    private final Set<Line> lineList = new LinkedHashSet<>();
    private final Set<TwoWindingsTransformer> twoWindingsTransformerList = new LinkedHashSet<>();
    private final Set<ThreeWindingsTransformer> threeWindingsTransformerList = new LinkedHashSet<>();
    private final Set<DanglingLine> danglingLineList = new LinkedHashSet<>();
    private final Set<Switch> switchList = new LinkedHashSet<>();

    private final Set<PhaseTapChanger> phaseTapChangerList = new LinkedHashSet<>();

    private final Set<HvdcLine> hvdcLineList = new LinkedHashSet<>();

    private final Set<Bus> busList = new LinkedHashSet<>();

    private final List<Contingency> contingencyList = new ArrayList<>();

    private final Set<Identifiable<?>> disconnectedElements = new HashSet<>();

    private final Map<String, String> mappedSwitchMap = new HashMap<>();

    protected MetrixNetwork(Network network) {
        // TODO: switch T3T for 3xTWT in the network using a network modification
        this.network = Objects.requireNonNull(network);
    }

    public Network getNetwork() {
        return network;
    }

    public List<String> getCountryList() {
        return List.copyOf(countryList);
    }

    public List<Load> getLoadList() {
        return List.copyOf(loadList);
    }

    public List<Generator> getGeneratorList() {
        return List.copyOf(generatorList);
    }

    public List<String> getGeneratorTypeList() {
        return List.copyOf(generatorTypeList);
    }

    public List<Line> getLineList() {
        return List.copyOf(lineList);
    }

    public List<TwoWindingsTransformer> getTwoWindingsTransformerList() {
        return List.copyOf(twoWindingsTransformerList);
    }

    public List<ThreeWindingsTransformer> getThreeWindingsTransformerList() {
        return List.copyOf(threeWindingsTransformerList);
    }

    public List<DanglingLine> getDanglingLineList() {
        return List.copyOf(danglingLineList);
    }

    public List<Switch> getSwitchList() {
        return List.copyOf(switchList);
    }

    public List<PhaseTapChanger> getPhaseTapChangerList() {
        return List.copyOf(phaseTapChangerList);
    }

    public List<HvdcLine> getHvdcLineList() {
        return List.copyOf(hvdcLineList);
    }

    public List<Bus> getBusList() {
        return List.copyOf(busList);
    }

    public List<Contingency> getContingencyList() {
        return List.copyOf(contingencyList);
    }

    public Set<Identifiable<?>> getDisconnectedElements() {
        return Set.copyOf(disconnectedElements);
    }

    public int getIndex(Identifiable<?> identifiable) {
        MetrixSubset subset = getMetrixSubset(identifiable);
        return mapper.getInt(subset, identifiable.getId());
    }

    private MetrixSubset getMetrixSubset(Identifiable<?> identifiable) {
        MetrixSubset subset = MetrixSubset.QUAD;
        if (identifiable instanceof Generator) {
            subset = MetrixSubset.GROUPE;
        } else if (identifiable instanceof Bus) {
            subset = MetrixSubset.NOEUD;
        } else if (identifiable instanceof HvdcLine) {
            subset = MetrixSubset.HVDC;
        } else if (identifiable instanceof Load || identifiable instanceof DanglingLine) {
            subset = MetrixSubset.LOAD;
        }
        return subset;
    }

    public Identifiable<?> getIdentifiable(String id) {
        return network.getIdentifiable(id);
    }

    public int getIndex(String id) {
        return getIndex(getIdentifiable(id));
    }

    public int getIndex(MetrixSubset subset, String id) {
        return mapper.getInt(subset, id);
    }

    public boolean isMapped(Identifiable<?> identifiable) {
        MetrixSubset subset = getMetrixSubset(identifiable);
        return mapper.isMapped(subset, identifiable.getId());
    }

    public int getCountryIndex(String country) {
        return mapper.getInt(MetrixSubset.REGION, country);
    }

    public String getGeneratorType(Generator generator) {
        return generator.getProperty("genreCvg", generator.getEnergySource().toString());
    }

    public String getCountryCode(VoltageLevel voltageLevel) {
        return voltageLevel.getSubstation().map(s -> {
            if (s.hasProperty(PAYS_CVG_PROPERTY)) {
                return s.getProperty(PAYS_CVG_PROPERTY);
            } else {
                return s.getCountry().map(Country::toString).orElse(PAYS_CVG_UNDEFINED);
            }
        }).orElse(PAYS_CVG_UNDEFINED);
    }

    private void addCountry(String country) {
        if (countryList.add(country)) {
            mapper.newInt(MetrixSubset.REGION, country);
        }
    }

    private void addGenerator(Generator generator) {
        if (generatorList.add(generator)) {
            mapper.newInt(MetrixSubset.GROUPE, generator.getId());
            generatorTypeList.add(getGeneratorType(generator));
        }
    }

    private void addLine(Line line) {
        if (lineList.add(line)) {
            mapper.newInt(MetrixSubset.QUAD, line.getId());
        }
    }

    private void addTwoWindingsTransformer(TwoWindingsTransformer twt) {
        if (twoWindingsTransformerList.add(twt)) {
            mapper.newInt(MetrixSubset.QUAD, twt.getId());
        }
    }

    private void addThreeWindingsTransformer(ThreeWindingsTransformer twt) {
        if (threeWindingsTransformerList.add(twt)) {
            mapper.newInt(MetrixSubset.QUAD, twt.getId());
        }
    }

    private void addDanglingLine(DanglingLine dl) {
        if (danglingLineList.add(dl)) {
            mapper.newInt(MetrixSubset.LOAD, dl.getId());
        }
    }

    private void addHvdcLine(HvdcLine line) {
        if (hvdcLineList.add(line)) {
            mapper.newInt(MetrixSubset.HVDC, line.getId());
        }
    }

    private void addSwitch(Switch sw) {
        if (!isMapped(sw) && switchList.add(sw)) {
            mapper.newInt(MetrixSubset.QUAD, sw.getId());
        }
    }

    private void addLoad(Load load) {
        if (loadList.add(load)) {
            mapper.newInt(MetrixSubset.LOAD, load.getId());
        }
    }

    private void addPhaseTapChanger(TwoWindingsTransformer twt) {
        if (phaseTapChangerList.add(twt.getPhaseTapChanger())) {
            mapper.newInt(MetrixSubset.DEPHA, twt.getId());
        }
    }

    private void addBus(Bus bus) {
        if (busList.add(bus)) {
            mapper.newInt(MetrixSubset.NOEUD, bus.getId());
        }
    }

    private void createLoadList() {
        int nbNok = 0;
        for (Load load : network.getLoads()) {
            Terminal t = load.getTerminal();
            Bus b = t.getBusBreakerView().getBus();
            if (b != null) {
                if (busList.contains(b)) {
                    addLoad(load);
                } else {
                    nbNok++;
                }
            } else {
                nbNok++;
            }
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Loads      total = <%5d> ok = <%5d> not = <%5d>", loadList.size() + nbNok, loadList.size(), nbNok));
        }
    }

    private void createGeneratorList() {
        int nbNok = 0;
        for (Generator generator : network.getGenerators()) {
            Terminal t = generator.getTerminal();
            Bus b = t.getBusBreakerView().getBus();
            if (b != null) {
                if (busList.contains(b)) {
                    addGenerator(generator);
                } else {
                    nbNok++;
                }
            } else {
                nbNok++;
            }
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Generators total = <%5d> ok = <%5d> not = <%5d>", generatorList.size() + nbNok, generatorList.size(), nbNok));
        }
    }

    private void createLineList() {
        int nbNok = 0;
        for (Line line : network.getLines()) {
            Terminal t1 = line.getTerminal1();
            Bus b1 = t1.getBusBreakerView().getBus();
            Terminal t2 = line.getTerminal2();
            Bus b2 = t2.getBusBreakerView().getBus();
            if (b1 != null && b2 != null) {
                if (busList.contains(b1) && busList.contains(b2)) {
                    addLine(line);
                } else {
                    nbNok++;
                }
            } else {
                nbNok++;
            }
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Lines      total = <%5d> ok = <%5d> not = <%5d>", lineList.size() + nbNok, lineList.size(), nbNok));
        }
    }

    private void addTwoWindingsTransformer(TwoWindingsTransformer twt, AtomicInteger nbNok, AtomicInteger nbPtcNok) {
        Terminal t1 = twt.getTerminal1();
        Terminal t2 = twt.getTerminal2();
        Bus b1 = t1.getBusBreakerView().getBus();
        Bus b2 = t2.getBusBreakerView().getBus();
        if (b1 != null && b2 != null) {
            if (busList.contains(b1) && busList.contains(b2)) {
                addTwoWindingsTransformer(twt);
                if (twt.hasPhaseTapChanger()) {
                    addPhaseTapChanger(twt);
                }
            } else {
                nbNok.incrementAndGet();
                if (twt.hasPhaseTapChanger()) {
                    nbPtcNok.incrementAndGet();
                }
            }
        } else {
            nbNok.incrementAndGet();
        }
    }

    private void createTwoWindingsTransformersList() {
        AtomicInteger nbNok = new AtomicInteger(0);
        AtomicInteger nbPtcNok = new AtomicInteger(0);
        network.getTwoWindingsTransformers().forEach(twt -> addTwoWindingsTransformer(twt, nbNok, nbPtcNok));
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Twotrfo    total = <%5d> ok = <%5d> not = <%5d>", twoWindingsTransformerList.size() + nbNok.get(), twoWindingsTransformerList.size(), nbNok.get()));
            LOGGER.debug(String.format("PhaseTC    total = <%5d> ok = <%5d> not = <%5d>", phaseTapChangerList.size() + nbPtcNok.get(), phaseTapChangerList.size(), nbPtcNok.get()));
        }
    }

    private void createThreeWindingsTransformersList() {
        int nbNok = 0;
        for (ThreeWindingsTransformer twt : network.getThreeWindingsTransformers()) {
            ThreeWindingsTransformer.Leg leg1 = twt.getLeg1();
            ThreeWindingsTransformer.Leg leg2 = twt.getLeg2();
            ThreeWindingsTransformer.Leg leg3 = twt.getLeg3();
            Terminal t1 = leg1.getTerminal();
            Terminal t2 = leg2.getTerminal();
            Terminal t3 = leg3.getTerminal();
            Bus b1 = t1.getBusBreakerView().getBus();
            Bus b2 = t2.getBusBreakerView().getBus();
            Bus b3 = t3.getBusBreakerView().getBus();
            if (b1 != null && b2 != null && b3 != null) {
                if (busList.contains(b1) && busList.contains(b2) && busList.contains(b3)) {
                    addThreeWindingsTransformer(twt);
                } else {
                    nbNok++;
                }
            } else {
                nbNok++;
            }
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Threetrfo  total = <%5d> ok = <%5d> not = <%5d>", threeWindingsTransformerList.size() + nbNok, threeWindingsTransformerList.size(), nbNok));
        }
    }

    private void createTransformerList() {

        // List the TwoWindingsTransformers
        createTwoWindingsTransformersList();

        // List the ThreeWindingsTransformers
        createThreeWindingsTransformersList();
    }

    private void createDanglingLineList() {
        int nbNok = 0;
        for (DanglingLine dl : network.getDanglingLines()) {
            Terminal t = dl.getTerminal();
            Bus b = t.getBusBreakerView().getBus();
            if (b != null) {
                if (busList.contains(b)) {
                    addDanglingLine(dl);
                } else {
                    nbNok++;
                }
            } else {
                nbNok++;
            }
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Dangling   total = <%5d> ok = <%5d> not = <%5d>", danglingLineList.size() + nbNok, danglingLineList.size(), nbNok));
        }
    }

    private void createSwitchList() {
        int nbNok = 0;
        for (VoltageLevel vl : network.getVoltageLevels()) {
            for (Switch sw : vl.getBusBreakerView().getSwitches()) {
                if (sw.isRetained() && !sw.isOpen()) {
                    Bus b1 = sw.getVoltageLevel().getBusBreakerView().getBus1(sw.getId());
                    Bus b2 = sw.getVoltageLevel().getBusBreakerView().getBus2(sw.getId());
                    if (b1 != null && b2 != null && busList.contains(b1) && busList.contains(b2)) {
                        addSwitch(sw);
                    } else {
                        nbNok++;
                    }
                } else {
                    nbNok++;
                }
            }
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Switches    total = <%5d> ok = <%5d> not = <%5d>", switchList.size() + nbNok, switchList.size(), nbNok));
        }
    }

    private void createHvdcLineList() {
        int nbNok = 0;
        for (HvdcLine line : network.getHvdcLines()) {
            Terminal t1 = line.getConverterStation1().getTerminal();
            Bus b1 = t1.getBusBreakerView().getBus();
            Terminal t2 = line.getConverterStation2().getTerminal();
            Bus b2 = t2.getBusBreakerView().getBus();
            if (b1 != null && b2 != null) {
                if (busList.contains(b1) && busList.contains(b2)) {
                    addHvdcLine(line);
                } else {
                    nbNok++;
                }
            } else {
                nbNok++;
            }
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Hvdc       total = <%5d> ok = <%5d> not = <%5d>", hvdcLineList.size() + nbNok, hvdcLineList.size(), nbNok));
        }
    }

    private void createBusList() {
        int nbNok = 0;
        for (VoltageLevel vl : network.getVoltageLevels()) {
            for (Bus bus : vl.getBusBreakerView().getBuses()) {
                if (bus.isInMainConnectedComponent()) {
                    addBus(bus);
                    addCountry(getCountryCode(vl));
                } else {
                    nbNok++;
                }
            }
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Bus        total = <%5d> ok = <%5d> not = <%5d>", busList.size() + nbNok, busList.size(), nbNok));
        }
    }

    private void createContingencyList(ContingenciesProvider provider, boolean propagate) {

        if (Objects.isNull(provider)) {
            return;
        }

        List<Contingency> ctyList = provider.getContingencies(network);
        ctyList.forEach(contingency -> addContingencyToList(contingency, propagate));

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Cty        total = <%5d> ok = <%5d> not = <%5d>", contingencyList.size(), ctyList.size(), contingencyList.size() - ctyList.size()));
        }
    }

    private void addContingencyToList(Contingency contingency, boolean propagate) {
        boolean ctyOk = true;
        for (ContingencyElement element : contingency.getElements()) {
            boolean elemOk = true;
            Identifiable<?> identifiable = network.getIdentifiable(element.getId());
            if (identifiable == null || !MetrixInputAnalysis.isValidContingencyElement(identifiable.getType(), element.getType())) {
                elemOk = false;
            }
            if (!elemOk) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn(String.format("Contingency '%s' : element '%s' not found in the network", contingency.getId(), element.getId()));
                }
                ctyOk = false;
            }
        }
        if (ctyOk) {
            if (propagate) {
                // replace elements with new elements from propagation
                // this will keep the original extensions
                List<ContingencyElement> extendedElements = new ArrayList<>(getElementsToTrip(contingency, true));
                Collection<ContingencyElement> originalElements = new ArrayList<>(contingency.getElements());
                originalElements.forEach(contingency::removeElement);
                extendedElements.forEach(contingency::addElement);
            }
            contingencyList.add(contingency);
        }
    }

    private void setSwitchRetainToFalse() {
        for (VoltageLevel vl : network.getVoltageLevels()) {
            if (vl.getTopologyKind() == TopologyKind.NODE_BREAKER) {
                for (Switch sw : vl.getNodeBreakerView().getSwitches()) {
                    sw.setRetained(false);
                }
            }
        }
    }

    private void init() {
        createBusList();
        createGeneratorList();
        createLineList();
        createTransformerList();
        createDanglingLineList();
        createSwitchList();
        createLoadList();
        createHvdcLineList();
    }

    public static MetrixNetwork create(Network network) {
        return create(network, null, null, new MetrixParameters(), (Path) null);
    }

    public static MetrixNetwork create(Network network, ContingenciesProvider contingenciesProvider, Set<String> mappedSwitches,
                                       MetrixParameters parameters, Path remedialActionFile) {
        Reader reader = null;
        if (remedialActionFile != null) {
            try {
                reader = Files.newBufferedReader(remedialActionFile, StandardCharsets.UTF_8);
            } catch (IOException e) {
                LOGGER.error("Failed to read remedialActionFile {}", remedialActionFile, e);
                throw new UncheckedIOException(e);
            }
        }
        return create(
                network,
                contingenciesProvider,
                mappedSwitches,
                parameters,
                reader
        );
    }

    public static MetrixNetwork create(Network network, ContingenciesProvider contingenciesProvider, Set<String> mappedSwitches,
                                       MetrixParameters parameters, Reader remedialActionReader) {

        Objects.requireNonNull(network);
        Objects.requireNonNull(parameters);

        MetrixNetwork metrixNetwork = new MetrixNetwork(network);

        // Create contingencies list
        metrixNetwork.createContingencyList(contingenciesProvider, parameters.isPropagateBranchTripping());

        // Create opened and switch-retained lists (network will be modified)
        List<Remedial> remedials = RemedialReader.parseFile(remedialActionReader);
        Set<String> retainedElements = Stream.concat(
                remedials.stream().flatMap(remedial -> remedial.getBranchToOpen().stream()),
                remedials.stream().flatMap(remedial -> remedial.getBranchToClose().stream())
        ).collect(Collectors.toSet());
        Set<String> openedElements = remedials.stream().flatMap(remedial -> remedial.getBranchToClose().stream()).collect(Collectors.toSet());
        if (!Objects.isNull(mappedSwitches)) {
            // close mapped switches as their openness is set via mapping
            for (String switchId : mappedSwitches) {
                network.getSwitch(switchId).setOpen(false);
            }
            retainedElements.addAll(mappedSwitches);
        }
        metrixNetwork.setSwitchRetainToFalse();
        metrixNetwork.createOpenedBranchesList(openedElements);
        metrixNetwork.createRetainedBreakersList(retainedElements);
        metrixNetwork.init();

        return metrixNetwork;
    }

    private void createRetainedBreakersList(Set<String> breakerList) {
        for (String breakerId : breakerList) {

            if (breakerId == null || breakerId.isEmpty()) {
                throw new PowsyblException("Empty switch name in configuration");
            }

            Switch sw = network.getSwitch(breakerId);

            // Stop the step if needed
            boolean skipThisStep = false;
            if (sw == null) {
                LOGGER.debug("Switch '{}' not found or not a switch", breakerId);
                skipThisStep = true;
            } else if (sw.isOpen()) {
                LOGGER.warn("Switch '{}' is opened in basecase", breakerId);
                skipThisStep = true;
            }
            if (skipThisStep) {
                continue;
            }

            addElementToRetainedBreakersList(sw);
        }
    }

    private void addElementToRetainedBreakersList(Switch sw) {
        String switchId = sw.getId();

        VoltageLevel voltageLevel = sw.getVoltageLevel();

        if (voltageLevel.getTopologyKind() == TopologyKind.NODE_BREAKER) {
            // Get the terminals on both sides of the switch
            VoltageLevel.NodeBreakerView nodeBreakerView = voltageLevel.getNodeBreakerView();
            Terminal terminal1 = nodeBreakerView.getTerminal1(switchId);
            Terminal terminal2 = nodeBreakerView.getTerminal2(switchId);

            // Check on both sides of the switch to find a connectable that is not another switch nor a bus bar section
            if (isSwitchNotConnectedToOtherSwitchOrBbs(terminal1)) {
                addElementToRetainedBreakersList(sw, terminal1, true);
            } else if (isSwitchNotConnectedToOtherSwitchOrBbs(terminal2)) {
                addElementToRetainedBreakersList(sw, terminal2, true);
            } else {
                // Both sides have either a switch or a bus bar section : the switch is registered by itself
                addElementToRetainedBreakersList(sw, switchId, true);
            }
        } else if (voltageLevel.getTopologyKind() == TopologyKind.BUS_BREAKER) {
            // Get the terminals on both sides of the switch
            VoltageLevel.BusBreakerView busBreakerView = voltageLevel.getBusBreakerView();
            List<? extends Terminal> terminalsBus1 = busBreakerView.getBus1(switchId).getConnectedTerminalStream().toList();
            List<? extends Terminal> terminalsBus2 = busBreakerView.getBus2(switchId).getConnectedTerminalStream().toList();

            // Check on both sides of the switch to check if there is one and only one connectable
            if (terminalsBus1.size() == 1) {
                addElementToRetainedBreakersList(sw, terminalsBus1.get(0), false);
            } else if (terminalsBus2.size() == 1) {
                addElementToRetainedBreakersList(sw, terminalsBus2.get(0), false);
            } else {
                addElementToRetainedBreakersList(sw, switchId, false);
            }
        }
    }

    private boolean isSwitchNotConnectedToOtherSwitchOrBbs(Terminal terminal) {
        return terminal != null && terminal.getConnectable().getType() != IdentifiableType.BUSBAR_SECTION;
    }

    private void addElementToRetainedBreakersList(Switch sw, Terminal terminal, boolean setRetained) {
        String switchId = sw.getId();
        switch (terminal.getConnectable().getType()) {
            // Since switches connected to lines and TWT are "replaced" by those connectables, no need to set them retained
            case LINE, TWO_WINDINGS_TRANSFORMER -> addElementToRetainedBreakersList(sw, terminal.getConnectable().getId(), false);
            case LOAD, GENERATOR, DANGLING_LINE -> addElementToRetainedBreakersList(sw, switchId, setRetained);
            case HVDC_CONVERTER_STATION, SHUNT_COMPENSATOR, STATIC_VAR_COMPENSATOR,
                 THREE_WINDINGS_TRANSFORMER -> {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("Unsupported connectable type ({}) for switch '{}'", terminal.getConnectable().getType(), switchId);
                }
            }
            default ->
                throw new PowsyblException("Unexpected connectable type : " + terminal.getConnectable().getType());
        }
    }

    private void addElementToRetainedBreakersList(Switch sw, String id, boolean setRetained) {
        if (setRetained) {
            sw.setRetained(true);
        }
        mappedSwitchMap.put(sw.getId(), id);
    }

    private void createOpenedBranchesList(Set<String> openedBranches) {
        for (String branchId : openedBranches) {
            Identifiable<?> identifiable = network.getIdentifiable(branchId);
            if (identifiable != null) {
                addElementToOpenedBranchesList(identifiable);
            } else {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("Opened branch '{}' is missing in the network", branchId);
                }
            }
        }
    }

    private void addElementToOpenedBranchesList(Identifiable<?> identifiable) {
        if (identifiable instanceof Branch<?> branchToClose) {
            if (!branchToClose.getTerminal1().isConnected() || !branchToClose.getTerminal2().isConnected()) {
                closeBranch(branchToClose);
                disconnectedElements.add(branchToClose);
            }
        } else if (identifiable instanceof Switch switchToClose) {
            if (switchToClose.isOpen()) {
                closeSwitch(switchToClose);
                disconnectedElements.add(switchToClose);
            }
        } else {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Unsupported open branch type : {}", identifiable.getClass());
            }
        }
    }

    private void closeBranch(Branch<?> branchToClose) {
        branchToClose.getTerminal1().connect();
        branchToClose.getTerminal2().connect();
        if (branchToClose.getTerminal1().isConnected() && branchToClose.getTerminal2().isConnected()) {
            LOGGER.debug("Reconnecting open branch : {}", branchToClose.getId());
        } else {
            LOGGER.warn("Unable to reconnect open branch : {}", branchToClose.getId());
        }
    }

    private void closeSwitch(Switch switchToClose) {
        switchToClose.setOpen(false);
        switchToClose.setRetained(true);
        LOGGER.debug("Reconnecting open switch : {}", switchToClose.getId());
    }

    Optional<String> getMappedBranch(Switch sw) {
        if (!Objects.isNull(sw)) {
            return Optional.ofNullable(mappedSwitchMap.get(sw.getId()));
        }
        return Optional.empty();
    }

    public Set<ContingencyElement> getElementsToTrip(Contingency contingency, boolean propagate) {

        if (!propagate) {
            return new HashSet<>(contingency.getElements());
        } else {

            Set<ContingencyElement> elementsToTrip = new HashSet<>();

            Set<Switch> switchesToOpen = new HashSet<>();
            Set<Terminal> terminalsToDisconnect = new HashSet<>();

            for (ContingencyElement element : contingency.getElements()) {
                if (element.getType() == ContingencyElementType.GENERATOR ||
                        element.getType() == ContingencyElementType.HVDC_LINE) {
                    elementsToTrip.add(element);
                } else {
                    Tripping modification = element.toModification();
                    modification.traverse(network, switchesToOpen, terminalsToDisconnect);
                }
            }

            Set<IdentifiableType> types = EnumSet.of(IdentifiableType.LINE,
                    IdentifiableType.TWO_WINDINGS_TRANSFORMER,
                    IdentifiableType.THREE_WINDINGS_TRANSFORMER,
                    IdentifiableType.HVDC_CONVERTER_STATION);

            // disconnect equipments and open switches
            for (Switch s : switchesToOpen) {
                VoltageLevel.NodeBreakerView nodeBreakerView = s.getVoltageLevel().getNodeBreakerView();
                terminalsToDisconnect.add(nodeBreakerView.getTerminal1(s.getId()));
                terminalsToDisconnect.add(nodeBreakerView.getTerminal2(s.getId()));
            }
            terminalsToDisconnect.stream()
                    .filter(Objects::nonNull)
                    .forEach(t -> {
                        Connectable<?> connectable = t.getConnectable();
                        if (connectable != null && types.contains(connectable.getType())) {
                            elementsToTrip.add(new BranchContingency(connectable.getId()));
                        }
                    });
            return elementsToTrip;
        }
    }
}
