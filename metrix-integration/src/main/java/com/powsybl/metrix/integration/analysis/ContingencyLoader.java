/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration.analysis;

import com.powsybl.contingency.BranchContingency;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyElement;
import com.powsybl.contingency.ContingencyElementType;
import com.powsybl.iidm.modification.tripping.Tripping;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Connectable;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.IdentifiableType;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Switch;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.metrix.commons.data.datatable.DataTableStore;
import com.powsybl.metrix.integration.exceptions.ContingenciesScriptLoadingException;
import com.powsybl.metrix.mapping.config.ScriptLogConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

import static com.powsybl.metrix.integration.analysis.AnalysisLogger.defaultLogger;

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
public class ContingencyLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContingencyLoader.class);

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.MetrixAnalysis");
    private static final String CONTINGENCIES_SECTION = RESOURCE_BUNDLE.getString("contingenciesSection");
    private static final String INVALID_CONTINGENCY_MESSAGE_KEY = "invalidContingency";

    private static final Map<ContingencyElementType, Set<IdentifiableType>> IDENTIFIABLE_TYPE_BY_CTY_ELEMENT_TYPE = Map.of(
        ContingencyElementType.LINE, Set.of(IdentifiableType.LINE),
        ContingencyElementType.TWO_WINDINGS_TRANSFORMER, Set.of(IdentifiableType.TWO_WINDINGS_TRANSFORMER),
        ContingencyElementType.TIE_LINE, Set.of(IdentifiableType.TIE_LINE),
        ContingencyElementType.GENERATOR, Set.of(IdentifiableType.GENERATOR),
        ContingencyElementType.HVDC_LINE, Set.of(IdentifiableType.HVDC_LINE),
        ContingencyElementType.BOUNDARY_LINE, Set.of(IdentifiableType.BOUNDARY_LINE),
        ContingencyElementType.BRANCH, Set.of(
            IdentifiableType.LINE,
            IdentifiableType.TWO_WINDINGS_TRANSFORMER,
            IdentifiableType.TIE_LINE
        )
    );

    private final ContingenciesProvider contingenciesProvider;
    private final Network network;
    private final boolean propagateBranchTripping;
    private final DataTableStore dataTableStore;
    private final ScriptLogConfig scriptLogConfig;
    private final AnalysisLogger analysisLogger;

    public ContingencyLoader(ContingenciesProvider contingenciesProvider,
                             Network network,
                             boolean propagateBranchTripping,
                             DataTableStore dataTableStore,
                             ScriptLogConfig scriptLogConfig,
                             AnalysisLogger analysisLogger) {
        this.contingenciesProvider = contingenciesProvider;
        this.network = network;
        this.propagateBranchTripping = propagateBranchTripping;
        this.dataTableStore = dataTableStore;
        this.scriptLogConfig = scriptLogConfig;
        this.analysisLogger = analysisLogger != null ? analysisLogger : defaultLogger();
    }

    /**
     * Load contingencies from ContingenciesProvider
     * <ul>
     *     <li>load contingencies from provider</li>
     *     <li>filter contingency if not valid</li>
     *     <li>if asked, apply topology propagation</li>
     *     <li>remove out of main connected component elements</li>
     *     <li>filter not empty contingency</li>
     * </ul>
     */
    public List<Contingency> load() {
        LOGGER.info("Loading contingencies from ContingenciesProvider");
        return getContingenciesFromProvider().stream()
            .filter(this::isValidContingency)
            .map(this::copyContingency)
            .map(this::propagateTopology)
            .map(this::removeOutOfMainConnectedComponentElements)
            .filter(this::hasElements)
            .collect(Collectors.toList());
    }

    private List<Contingency> getContingenciesFromProvider() {
        try {
            return contingenciesProvider.getContingencies(network, getContextObjects());
        } catch (RuntimeException e) {
            throw new ContingenciesScriptLoadingException(e);
        }
    }

    /**
     * Check the validity of each element of the contingency
     * @param contingency contingency to check
     * @return true if the all elements are valid, false otherwise
     */
    private boolean isValidContingency(Contingency contingency) {
        boolean isValid = true;
        for (ContingencyElement element : contingency.getElements()) {
            Identifiable<?> identifiable = network.getIdentifiable(element.getId());
            isValid = isValid && isValidContingencyElement(identifiable, contingency.getId(), element);
        }
        return isValid;
    }

    /**
     * Check the validity of one element of the contingency and log a warning for each invalid check.
     * <ul>
     *     <li>check that identifiable exists in the network</li>
     *     <li>check that the type of element is correct</li>
     *     <li>check that the type of element is compatible with the type of identifiable</li>
     * </ul>
     * The last check is done if the two previous are valid.
     * @param identifiable  identifiable corresponding to the contingency element
     * @param contingencyId contingency id to which the element belongs
     * @param element       contingency element to check
     * @return true if the element is valid, false otherwise
     */
    private boolean isValidContingencyElement(Identifiable<?> identifiable, String contingencyId, ContingencyElement element) {
        boolean isValid = true;
        boolean isExistingIdentifiable = identifiable != null;
        boolean isValidContingencyType = isValidContingencyType(element.getType());
        if (!isExistingIdentifiable) {
            analysisLogger.warnWithReason(CONTINGENCIES_SECTION, new Reason("invalidContingencyNetwork"),
                INVALID_CONTINGENCY_MESSAGE_KEY, contingencyId, element.getId());
            isValid = false;
        }
        if (!isValidContingencyType) {
            analysisLogger.warnWithReason(CONTINGENCIES_SECTION, new Reason("invalidContingencyType", element.getType()),
                INVALID_CONTINGENCY_MESSAGE_KEY, contingencyId, element.getId());
            isValid = false;
        }
        if (isValid && !isValidContingencyTypeForIdentifiable(identifiable.getType(), element.getType())) {
            analysisLogger.warnWithReason(CONTINGENCIES_SECTION, new Reason("invalidContingencyNetworkType", element.getType(), identifiable.getType()),
                INVALID_CONTINGENCY_MESSAGE_KEY, contingencyId, element.getId());
            isValid = false;
        }
        return isValid;
    }

    private static boolean isValidContingencyType(ContingencyElementType elementType) {
        return IDENTIFIABLE_TYPE_BY_CTY_ELEMENT_TYPE.containsKey(elementType);
    }

    private static boolean isValidContingencyTypeForIdentifiable(IdentifiableType identifiableType, ContingencyElementType elementType) {
        return IDENTIFIABLE_TYPE_BY_CTY_ELEMENT_TYPE
            .getOrDefault(elementType, Set.of())
            .contains(identifiableType);
    }

    /**
     * If asked, apply topology propagation
     * @param contingency contingency to extend
     * @return extended contingency if propagation is asked, original one if not
     */
    private Contingency propagateTopology(Contingency contingency) {
        return propagateBranchTripping ? propagateElementsToTrip(contingency) : contingency;
    }

    /**
     * Apply topology propagation
     * @param contingency contingency to extend
     * @return contingency with extended elements
     */
    private Contingency propagateElementsToTrip(Contingency contingency) {
        /*
        Set<ContingencyElement> propagated = getElementsToTrip(contingency);
        if (propagated.isEmpty()) {
            return contingency;
        }
        // sometimes propagated contains original elements, and sometimes not ... why ???
        Contingency copy = new Contingency(contingency.getId());
        propagated.forEach(copy::addElement);
        return copy;
         */
        // replace elements with new elements from propagation, this will keep the original elements
        List<ContingencyElement> extendedElements = new ArrayList<>(getElementsToTrip(contingency));
        if (!extendedElements.isEmpty()) {
            Collection<ContingencyElement> originalElements = new ArrayList<>(contingency.getElements());
            originalElements.forEach(contingency::removeElement);
            extendedElements.forEach(contingency::addElement);
        }
        return contingency;
    }

    /**
     * Apply topology propagation
     * <li>find terminals to disconnect</li>
     * <ul>
     *     <li>for each element, find all switches to open and all terminals to disconnect</li>
     *     <li>add terminals of these switches to terminals to disconnect</li>
     * </ul>
     * <li>find all impacted branches ids connected to terminals to disconnect</li>
     * <li>create one BRANCH element for each impacted branch id</li>
     * @param contingency contingency to extend
     * @return extended elements: original GENERATOR and HVDC_LINE elements plus created BRANCH elements
     */
    private Set<ContingencyElement> getElementsToTrip(Contingency contingency) {
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
        Set<String> branchIds = new HashSet<>();
        for (Terminal t : terminalsToDisconnect) {
            if (t == null) {
                continue;
            }
            Connectable<?> connectable = t.getConnectable();
            if (connectable != null && types.contains(connectable.getType())) {
                branchIds.add(connectable.getId());
            }
        }
        branchIds.forEach(id -> elementsToTrip.add(new BranchContingency(id)));
        return elementsToTrip;
    }

    /**
     * Remove elements that are not in the main connected component and log a warning for each removed element.
     * @param contingency contingency to be treated
     * @return contingency without elements that are not in the main connected component
     */
    private Contingency removeOutOfMainConnectedComponentElements(Contingency contingency) {
        List<ContingencyElement> contingencyElements = new ArrayList<>(contingency.getElements());
        for (ContingencyElement element : contingencyElements) {
            if (!isElementInMainConnectedComponent(element, contingency.getId())) {
                analysisLogger.warn(CONTINGENCIES_SECTION, "invalidElementContingency", element.getId(), contingency.getId());
                contingency.removeElement(element);
            }
        }
        return contingency;
    }

    /**
     * Check if the element of the contingency is in the main connected component.
     * @param element       element of the contingency
     * @param contingencyId contingency id
     * @return true if the element is in the main connected component, false otherwise
     */
    private boolean isElementInMainConnectedComponent(ContingencyElement element, String contingencyId) {
        Identifiable<?> identifiable = network.getIdentifiable(element.getId());
        if (identifiable instanceof Branch<?> branch) {
            return isInMain(branch.getTerminal1()) && isInMain(branch.getTerminal2());
        }
        if (identifiable instanceof Generator generator) {
            return isInMain(generator.getTerminal());
        }
        if (identifiable instanceof HvdcLine hvdcLine) {
            return isInMain(hvdcLine.getConverterStation1().getTerminal())
                && isInMain(hvdcLine.getConverterStation2().getTerminal());
        }
        analysisLogger.warnWithReason(CONTINGENCIES_SECTION, new Reason("invalidContingencyType", element.getType()),
            INVALID_CONTINGENCY_MESSAGE_KEY, contingencyId, element.getId());
        return false;
    }

    private static boolean isInMain(Terminal terminal) {
        if (!terminal.isConnected()) {
            return false;
        }
        Bus bus = terminal.getBusBreakerView().getBus();
        return bus != null && bus.isInMainConnectedComponent();
    }

    /**
     * Check if the contingency has elements and log a warning if not.
     * @param contingency contingency to check
     * @return true if the contingency has elements, false otherwise
     */
    private boolean hasElements(Contingency contingency) {
        boolean hasElements = !contingency.getElements().isEmpty();
        if (!hasElements) {
            analysisLogger.warn(CONTINGENCIES_SECTION, "invalidNumberOfElements", contingency.getId());
        }
        return hasElements;
    }

    private Map<Class<?>, Object> getContextObjects() {
        Map<Class<?>, Object> contextObjects = new HashMap<>();
        if (dataTableStore != null) {
            contextObjects.put(DataTableStore.class, dataTableStore);
        }
        if (this.scriptLogConfig != null) {
            contextObjects.put(ScriptLogConfig.class, this.scriptLogConfig);
            if (this.scriptLogConfig.getWriter() != null) {
                contextObjects.put(Writer.class, this.scriptLogConfig.getWriter());
            }
        }
        return contextObjects;
    }

    private Contingency copyContingency(Contingency contingency) {
        Contingency copy = new Contingency(contingency.getId());
        contingency.getElements().forEach(copy::addElement);
        return copy;
    }
}
