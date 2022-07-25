/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.integration;

import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.util.StringToIntMapper;
import com.powsybl.contingency.*;
import com.powsybl.iidm.modification.tripping.Tripping;
import com.powsybl.iidm.network.*;
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
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MetrixNetwork {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetrixNetwork.class);

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
        this.network = Objects.requireNonNull(network);
    }

    public Network getNetwork() {
        return network;
    }

    public List<String> getCountryList() {
        return Collections.unmodifiableList(new ArrayList<>(countryList));
    }

    public List<Load> getLoadList() {
        return Collections.unmodifiableList(new ArrayList<>(loadList));
    }

    public List<Generator> getGeneratorList() {
        return Collections.unmodifiableList(new ArrayList<>(generatorList));
    }

    public List<String> getGeneratorTypeList() {
        return Collections.unmodifiableList(new ArrayList<>(generatorTypeList));
    }

    public List<Line> getLineList() {
        return Collections.unmodifiableList(new ArrayList<>(lineList));
    }

    public List<TwoWindingsTransformer> getTwoWindingsTransformerList() {
        return Collections.unmodifiableList(new ArrayList<>(twoWindingsTransformerList));
    }

    public List<ThreeWindingsTransformer> getThreeWindingsTransformerList() {
        return Collections.unmodifiableList(new ArrayList<>(threeWindingsTransformerList));
    }

    public List<DanglingLine> getDanglingLineList() {
        return Collections.unmodifiableList(new ArrayList<>(danglingLineList));
    }

    public List<Switch> getSwitchList() {
        return Collections.unmodifiableList(new ArrayList<>(switchList));
    }

    public List<PhaseTapChanger> getPhaseTapChangerList() {
        return Collections.unmodifiableList(new ArrayList<>(phaseTapChangerList));
    }

    public List<HvdcLine> getHvdcLineList() {
        return Collections.unmodifiableList(new ArrayList<>(hvdcLineList));
    }

    public List<Bus> getBusList() {
        return Collections.unmodifiableList(new ArrayList<>(busList));
    }

    public List<Contingency> getContingencyList() {
        return Collections.unmodifiableList(contingencyList);
    }

    public Set<Identifiable> getDisconnectedElements() {
        return Collections.unmodifiableSet(disconnectedElements);
    }

    public int getIndex(Identifiable identifiable) {
        MetrixSubset subset = MetrixSubset.QUAD;
        if (identifiable instanceof Generator) {
            subset = MetrixSubset.GROUPE;
        } else if (identifiable instanceof Bus) {
            subset = MetrixSubset.NOEUD;
        } else if (identifiable instanceof HvdcLine) {
            subset = MetrixSubset.HVDC;
        } else if (identifiable instanceof Load) {
            subset = MetrixSubset.LOAD;
        }
        return mapper.getInt(subset, identifiable.getId());
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
        MetrixSubset subset = MetrixSubset.QUAD;
        if (identifiable instanceof Generator) {
            subset = MetrixSubset.GROUPE;
        } else if (identifiable instanceof Bus) {
            subset = MetrixSubset.NOEUD;
        } else if (identifiable instanceof HvdcLine) {
            subset = MetrixSubset.HVDC;
        } else if (identifiable instanceof Load) {
            subset = MetrixSubset.LOAD;
        }
        return mapper.isMapped(subset, identifiable.getId());
    }

    public int getCountryIndex(String country) {
        return mapper.getInt(MetrixSubset.REGION, country);
    }

    public String getGeneratorType(Generator generator) {
        return generator.getProperty("genreCvg", generator.getEnergySource().toString());
    }

    public String getCountryCode(Substation substation) {
        String countryCode = substation.getProperty("paysCvg");
        if (countryCode == null) {
            Optional<Country> country = substation.getCountry();
            if (country.isPresent()) {
                countryCode = country.get().toString();
            } else {
                countryCode = "Undefined";
            }
        }
        return countryCode;
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
            mapper.newInt(MetrixSubset.QUAD, dl.getId());
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

    private void createTransformerList() {
        int nbNok = 0;
        int nbPtcNok = 0;

        for (TwoWindingsTransformer twt : network.getTwoWindingsTransformers()) {
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
                    nbNok++;
                    if (twt.hasPhaseTapChanger()) {
                        nbPtcNok++;
                    }
                }
            } else {
                nbNok++;
            }
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Twotrfo    total = <%5d> ok = <%5d> not = <%5d>", twoWindingsTransformerList.size() + nbNok, twoWindingsTransformerList.size(), nbNok));
            LOGGER.debug(String.format("PhaseTC    total = <%5d> ok = <%5d> not = <%5d>", phaseTapChangerList.size() + nbPtcNok, phaseTapChangerList.size(), nbPtcNok));
        }

        nbNok = 0;
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
                    Substation opt = vl.getSubstation()
                            .orElseThrow(() -> new IllegalStateException("No substation for vl [" + vl.getId() + "]"));
                    addCountry(getCountryCode(opt));
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

        for (Contingency contingency : ctyList) {
            boolean ctyOk = true;
            for (ContingencyElement element : contingency.getElements()) {
                boolean elemOk = true;
                Identifiable<?> identifiable = network.getIdentifiable(element.getId());
                if (identifiable == null ||
                        (element.getType() == ContingencyElementType.GENERATOR && !(identifiable instanceof Generator)) ||
                        (Arrays.asList(ContingencyElementType.BRANCH, ContingencyElementType.LINE, ContingencyElementType.TWO_WINDINGS_TRANSFORMER).contains(element.getType()) && !(identifiable instanceof Branch)) ||
                        (element.getType() == ContingencyElementType.HVDC_LINE && !(identifiable instanceof HvdcLine))) {
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

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Cty        total = <%5d> ok = <%5d> not = <%5d>", contingencyList.size(), ctyList.size(), contingencyList.size() - ctyList.size()));
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
        return create(
            network,
            contingenciesProvider,
            mappedSwitches,
            parameters,
            remedialActionFile != null ? () -> {
                try {
                    return Files.newBufferedReader(remedialActionFile, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    LOGGER.error("Failed to read remedialActionFile {}", remedialActionFile, e);
                    throw new UncheckedIOException(e);
                }
            } : null
        );
    }

    public static MetrixNetwork create(Network network, ContingenciesProvider contingenciesProvider, Set<String> mappedSwitches,
                                       MetrixParameters parameters, Supplier<Reader> remedialActionFile) {

        Objects.requireNonNull(network);
        Objects.requireNonNull(parameters);

        MetrixNetwork metrixNetwork = new MetrixNetwork(network);

        // Create contingencies list
        metrixNetwork.createContingencyList(contingenciesProvider, parameters.isPropagateBranchTripping());

        // Create opened and switch-retained lists (network will be modified)
        List<Remedial> remedials = RemedialReader.parseFile(remedialActionFile);
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

            if (sw == null) {
                LOGGER.debug(String.format("Switch '%s' not found or not a switch", breakerId));
                continue;
            }
            if (sw.isOpen()) {
                LOGGER.warn(String.format("Switch '%s' is opened in basecase", breakerId));
                continue;
            }

            String switchId = sw.getId();

            VoltageLevel.NodeBreakerView nodeBreakerView = sw.getVoltageLevel().getNodeBreakerView();
            Terminal terminal1 = nodeBreakerView.getTerminal1(switchId);
            Terminal terminal2 = nodeBreakerView.getTerminal2(switchId);

            if (terminal1 == null || terminal1.getConnectable().getType() == IdentifiableType.BUSBAR_SECTION) {
                terminal1 = terminal2;
            }

            if (terminal1 == null || terminal1.getConnectable().getType() == IdentifiableType.BUSBAR_SECTION) {
                sw.setRetained(true);
                mappedSwitchMap.put(switchId, switchId);
            } else {
                switch (terminal1.getConnectable().getType()) {
                    case LINE:
                    case TWO_WINDINGS_TRANSFORMER:
                        sw.setRetained(true);
                        mappedSwitchMap.put(switchId, terminal1.getConnectable().getId());
                        break;
                    case LOAD:
                    case GENERATOR:
                        sw.setRetained(true);
                        mappedSwitchMap.put(switchId, switchId);
                        break;
                    case DANGLING_LINE:
                    case HVDC_CONVERTER_STATION:
                    case SHUNT_COMPENSATOR:
                    case STATIC_VAR_COMPENSATOR:
                    case THREE_WINDINGS_TRANSFORMER:
                        if (LOGGER.isWarnEnabled()) {
                            LOGGER.warn(String.format("Unsupported connectable type (%s) for switch '%s'", terminal1.getConnectable().getType(), breakerId));
                        }
                        break;
                    default:
                        throw new PowsyblException("Unexpected connectable type : " + terminal1.getConnectable().getType());
                }
            }
        }
    }

    private void createOpenedBranchesList(Set<String> openedBranches) {
        for (String branchId : openedBranches) {
            Identifiable<?> identifiable = network.getIdentifiable(branchId);
            if (identifiable != null) {
                if (identifiable instanceof Branch) {
                    Branch<?> branchToClose = (Branch<?>) identifiable;
                    if (!branchToClose.getTerminal1().isConnected() || !branchToClose.getTerminal2().isConnected()) {
                        closeBranch(branchToClose);
                        disconnectedElements.add(branchToClose);
                    }
                } else if (identifiable instanceof Switch) {
                    Switch switchToClose = (Switch) identifiable;
                    if (switchToClose.isOpen()) {
                        closeSwitch(switchToClose);
                        disconnectedElements.add(switchToClose);
                    }
                } else {
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn(String.format("Unsupported open branch type : %s", identifiable.getClass()));
                    }
                }
            } else {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn(String.format("Opened branch '%s' is missing in the network", branchId));
                }
            }
        }
    }

    private void closeBranch(Branch<?> branchToClose) {
        branchToClose.getTerminal1().connect();
        branchToClose.getTerminal2().connect();
        if (branchToClose.getTerminal1().isConnected() && branchToClose.getTerminal2().isConnected()) {
            LOGGER.debug(String.format("Reconnecting open branch : %s", branchToClose.getId()));
        } else {
            LOGGER.warn(String.format("Unable to reconnect open branch : %s", branchToClose.getId()));
        }
    }

    private void closeSwitch(Switch switchToClose) {
        switchToClose.setOpen(false);
        switchToClose.setRetained(true);
        LOGGER.debug(String.format("Reconnecting open switch : %s", switchToClose.getId()));
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
                    Tripping task = element.toModification();
                    task.traverse(network, switchesToOpen, terminalsToDisconnect);
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

