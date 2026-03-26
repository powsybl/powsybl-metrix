/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration.analysis;

import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyElement;
import com.powsybl.contingency.dsl.GroovyDslContingenciesProvider;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.serde.NetworkSerDe;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
class MetrixContingencyAnalysisRealNetworkTest {

    private Network network;

    @BeforeEach
    void setUp() throws IOException {
        InputStream inputStream = Objects.requireNonNull(getClass().getResourceAsStream("/realNetwork.xiidm.gz"));
        byte[] data = IOUtils.toByteArray(inputStream);
        network = NetworkSerDe.gunzip(data);
    }

    @Test
    void loadContingencyTest() {
        StringWriter writer = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(writer)) {
            AnalysisLogger logger = new AnalysisLogger(bufferedWriter);
            ContingenciesProvider provider = new GroovyDslContingenciesProvider(Objects.requireNonNull(getClass().getResourceAsStream("/defaultScript.groovy")));
            ContingencyLoader contingencyLoader = new ContingencyLoader(provider, network, true, null, null, logger);
            List<Contingency> actualContingencies = contingencyLoader.load();
            actualContingencies.sort(Comparator.comparing(Contingency::getId));
            bufferedWriter.flush();

            Map<String, String> actualMap = new TreeMap<>();
            actualContingencies.forEach(contingency -> {
                String ids = contingency.getElements().stream()
                    .map(ContingencyElement::getId)
                    .collect(Collectors.joining(","));
                actualMap.put(contingency.getId(), ids);
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
