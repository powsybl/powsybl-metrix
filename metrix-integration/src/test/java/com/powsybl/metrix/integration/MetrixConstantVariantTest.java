/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.integration;

import com.google.common.collect.Range;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.contingency.BranchContingency;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.xml.NetworkXml;
import com.powsybl.metrix.integration.contingency.Probability;
import com.powsybl.metrix.mapping.MappingParameters;
import com.powsybl.metrix.mapping.TimeSeriesDslLoader;
import com.powsybl.metrix.mapping.TimeSeriesMappingConfig;
import com.powsybl.timeseries.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.threeten.extra.Interval;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Nicolas Lhuillier <nicolas.lhuillier@rte-france.com>
 */
public class MetrixConstantVariantTest {

    private FileSystem fileSystem;
    private Path metrixFile;
    private Path mappingFile;
    private Path variantFile;
    private Network network;

    private MappingParameters mappingParameters = MappingParameters.load();

    @Before
    public void setUp() throws IOException {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        metrixFile = fileSystem.getPath("/metrix.groovy");
        mappingFile = fileSystem.getPath("/mapping.groovy");
        variantFile = fileSystem.getPath("/variantes.csv");
        network = NetworkXml.read(getClass().getResourceAsStream("/simpleNetwork.xml"));
        network.getLoad("FVERGE11_L").getTerminal().connect(); // Connect 4th load to use it
    }

    @After
    public void tearDown() throws IOException {
        fileSystem.close();
    }

    @Test
    public void constantVariantTest() throws IOException {

        // Creates mapping file
        try (Writer writer = Files.newBufferedWriter(mappingFile, StandardCharsets.UTF_8)) {
            writer.write(String.join(System.lineSeparator(),
                "parameters {",
                    "    toleranceThreshold 0.0001f",
                    "}",
                "timeSeries['zero'] = 0",
                "timeSeries['constant_ts3'] = 500",
                "mapToGenerators {",
                "    timeSeriesName 'zero'",
                "}",
                "mapToGenerators {",
                "    timeSeriesName 'constant_ts1'",
                "    filter {",
                "        generator.id == 'FSSV.O11_G'",
                "    }",
                "}",
                "mapToGenerators {",
                "    timeSeriesName 'variable_ts1'",
                "    filter {",
                "        generator.id == 'FSSV.O12_G'",
                "    }",
                "}",
                "mapToLoads {",
                "    timeSeriesName 'constant_ts2'",
                "    filter {",
                "        load.id == 'FSSV.O11_L'",
                "    }",
                "}",
                "mapToLoads {",
                "    timeSeriesName 'variable_ts2'",
                "    filter {",
                "        load.id == 'FVALDI11_L'",
                "    }",
                "}",
                "mapToLoads {",
                "    timeSeriesName 'constant_ts1'",
                "    filter {",
                "        load.id == 'FVALDI11_L2'",
                "    }",
                "    variable fixedActivePower",
                "}",
                "mapToLoads {",
                "    timeSeriesName 'variable_ts2'",
                "    filter {",
                "        load.id == 'FVALDI11_L2'",
                "    }",
                "    variable variableActivePower",
                "}",
                "mapToLoads {",
                "    timeSeriesName 'constant_ts1'",
                "    filter {",
                "        load.id == 'FVERGE11_L'",
                "    }",
                "    variable fixedActivePower",
                "}",
                "mapToLoads {",
                "    timeSeriesName 'constant_ts2'",
                "    filter {",
                "        load.id == 'FVERGE11_L'",
                "    }",
                "    variable variableActivePower",
                "}"));
        }

        // Creates metrix file
        try (Writer writer = Files.newBufferedWriter(metrixFile, StandardCharsets.UTF_8)) {
            writer.write(String.join(System.lineSeparator(),
                "branch('FVALDI1  FTDPRA1  1') {",
                "    branchRatingsBaseCase 999",
                "}",
                "branch('FS.BIS1  FVALDI1  1') {",
                "    branchRatingsBaseCase 'constant_ts3'",
                "}",
                "branch('FP.AND1  FVERGE1  2') {",
                "    branchRatingsBaseCase 'variable_ts3'",
                "}"
                ));
        }

        // create time series space mock
        TimeSeriesIndex index = RegularTimeSeriesIndex.create(Interval.parse("2015-01-01T00:00:00Z/2015-07-20T00:00:00Z"), Duration.ofDays(200));
        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache(
            TimeSeries.createDouble("constant_ts1", index, 100d, 100d),
            TimeSeries.createDouble("variable_ts1", index, 200d, 201d),
            TimeSeries.createDouble("constant_ts2", index, 300d, 300d),
            TimeSeries.createDouble("variable_ts2", index, 400d, 401d),
            TimeSeries.createDouble("variable_ts3", index, 600d, 601d)
        );

        ContingenciesProvider contingenciesProvider = new ContingenciesProvider() {
            @Override
            public List<Contingency> getContingencies(Network network) {
                Contingency a = new Contingency("a", Collections.singletonList(new BranchContingency("FVALDI1  FTDPRA1  1")));
                a.addExtension(Probability.class, new Probability(0.002d, null));
                Contingency b = new Contingency("b", Arrays.asList(new BranchContingency("FS.BIS1  FVALDI1  1"), new BranchContingency("FP.AND1  FVERGE1  2")));
                b.addExtension(Probability.class, new Probability(null, "variable_ts1"));
                return Arrays.asList(a, b);
            }
        };

        MetrixParameters metrixParameters = MetrixParameters.load();
        MetrixNetwork metrixNetwork = MetrixNetwork.create(network, contingenciesProvider, null, new MetrixParameters(), (Path) null);

        TimeSeriesMappingConfig mappingConfig;
        try (Reader mappingReader = Files.newBufferedReader(mappingFile, StandardCharsets.UTF_8)) {
            mappingConfig = TimeSeriesDslLoader.load(mappingReader, network, mappingParameters, store, null, null);
        }

        try (Reader metrixDslReader = Files.newBufferedReader(metrixFile, StandardCharsets.UTF_8)) {
            MetrixDslDataLoader.load(metrixDslReader, network, metrixParameters, store, mappingConfig);
        }

        Range<Integer> variantRange = Range.closed(0, 1);

        MetrixVariantProvider variantProvider = new MetrixTimeSeriesVariantProvider(network, store, mappingParameters,
                mappingConfig, network -> Collections.emptyList(), 1, variantRange, false, false, System.err);

        // write variants
        MetrixVariantsWriter variantsWriter = new MetrixVariantsWriter(variantProvider, metrixNetwork);
        variantsWriter.write(Range.closed(0, 1), variantFile);

        assertEquals(String.join(System.lineSeparator(),
            "NT;2;",
            "-1;CONELE;2;FSSV.O11_L;300;FVERGE11_L;400;",
            "-1;QATI00MN;2;FVALDI1  FTDPRA1  1;999;FS.BIS1  FVALDI1  1;500;",
            "0;PRODIM;2;FSSV.O11_G;100;FSSV.O12_G;200;",
            "0;CONELE;2;FVALDI11_L;400;FVALDI11_L2;500;",
            "0;QATI00MN;1;FP.AND1  FVERGE1  2;600;",
            "0;PROBABINC;2;b;200;a;0.002;",
            "1;PRODIM;2;FSSV.O11_G;100;FSSV.O12_G;201;",
            "1;CONELE;2;FVALDI11_L;401;FVALDI11_L2;501;",
            "1;QATI00MN;1;FP.AND1  FVERGE1  2;601;",
            "1;PROBABINC;2;b;201;a;0.002;") + System.lineSeparator(),
            new String(Files.readAllBytes(variantFile), StandardCharsets.UTF_8));
    }
}