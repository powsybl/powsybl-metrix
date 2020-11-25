/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auto.service.AutoService;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Range;
import com.powsybl.commons.json.JsonUtil;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.json.ContingencyJsonModule;
import com.powsybl.iidm.import_.ImportConfig;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.metrix.mapping.MappingParameters;
import com.powsybl.metrix.mapping.TimeSeriesDslLoader;
import com.powsybl.metrix.mapping.TimeSeriesMappingConfig;
import com.powsybl.metrix.mapping.json.TimeSeriesMappingConfigJsonModule;
import com.powsybl.timeseries.*;
import com.powsybl.tools.Command;
import com.powsybl.tools.Tool;
import com.powsybl.tools.ToolRunningContext;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian@rte-france.com>
 */
@AutoService(Tool.class)
public class MetrixChunkTool implements Tool {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetrixChunkTool.class);

    @Override
    public Command getCommand() {
        return new Command() {
            @Override
            public String getName() {
                return "metrix-chunk";
            }

            @Override
            public String getTheme() {
                return "Metrix";
            }

            @Override
            public String getDescription() {
                return "Internal command to run metrix on a variant chunk";
            }

            @Override
            public Options getOptions() {
                Options options = new Options();
                options.addOption(Option.builder()
                        .longOpt("case-file")
                        .desc("the base case file")
                        .hasArg()
                        .argName("FILE")
                        .required()
                        .build());
                options.addOption(Option.builder()
                        .longOpt("mapping-parameters-file")
                        .desc("mapping parameters json file")
                        .hasArg()
                        .argName("FILE")
                        .build());
                options.addOption(Option.builder()
                        .longOpt("mapping-file")
                        .desc("Groovy DSL file that describes the mapping")
                        .hasArg()
                        .argName("FILE")
                        .build());
                options.addOption(Option.builder()
                        .longOpt("mapping-config-file")
                        .desc("mapping config json file")
                        .hasArg()
                        .argName("FILE")
                        .build());
                options.addOption(Option.builder()
                        .longOpt("input-time-series-json-file")
                        .desc("input time series json file")
                        .hasArg()
                        .argName("FILE")
                        .required()
                        .build());
                options.addOption(Option.builder()
                        .longOpt("version")
                        .desc("time series version")
                        .hasArg()
                        .argName("NUM")
                        .required()
                        .build());
                options.addOption(Option.builder()
                        .longOpt("first-variant")
                        .desc("first variant to simulate")
                        .hasArg()
                        .argName("NUM")
                        .build());
                options.addOption(Option.builder()
                        .longOpt("variant-count")
                        .desc("number of variants simulated")
                        .hasArg()
                        .argName("COUNT")
                        .build());
                options.addOption(Option.builder()
                        .longOpt("output-time-series-json-file")
                        .desc("output time series json file")
                        .hasArg()
                        .argName("FILE")
                        .build());
                options.addOption(Option.builder()
                        .longOpt("metrix-dsl-file")
                        .desc("Groovy DSL file that describes the branch monitoring and the phase shifter actions")
                        .hasArg()
                        .argName("FILE")
                        .build());
                options.addOption(Option.builder()
                        .longOpt("metrix-dsl-data-file")
                        .desc("metrix dsl data json file")
                        .hasArg()
                        .argName("FILE")
                        .build());
                options.addOption(Option.builder()
                        .longOpt("metrix-parameters-file")
                        .desc("metrix parameters json file")
                        .hasArg()
                        .argName("FILE")
                        .build());
                options.addOption(Option.builder()
                        .longOpt("ignore-limits")
                        .desc("ignore generator limits")
                        .build());
                options.addOption(Option.builder()
                        .longOpt("ignore-empty-filter")
                        .desc("ignore empty filter with non zero time series value")
                        .build());
                options.addOption(Option.builder()
                        .longOpt("contingencies-file")
                        .desc("Groovy DSL file that describes contingencies")
                        .hasArg()
                        .argName("FILE")
                        .build());
                options.addOption(Option.builder()
                        .longOpt("remedial-actions-file")
                        .desc("name of the remedial actions file")
                        .hasArg()
                        .argName("FILE")
                        .build());
                options.addOption(Option.builder()
                        .longOpt("log-file")
                        .desc("Metrix log file")
                        .hasArg()
                        .argName("FILE")
                        .build());
                return options;
            }

            @Override
            public boolean isHidden() {
                return true;
            }

            @Override
            public String getUsageFooter() {
                return null;
            }
        };
    }

    @Override
    public void run(CommandLine line, ToolRunningContext context) throws Exception {
        Path caseFile = context.getFileSystem().getPath(line.getOptionValue("case-file"));

        Path mappingFile = null;
        if (line.hasOption("mapping-file")) {
            mappingFile = context.getFileSystem().getPath(line.getOptionValue("mapping-file"));
        }
        Path mappingConfigFile = null;
        if (line.hasOption("mapping-config-file")) {
            mappingConfigFile = context.getFileSystem().getPath(line.getOptionValue("mapping-config-file"));
        }
        Path mappingParametersFile = null;
        if (line.hasOption("mapping-parameters-file")) {
            mappingParametersFile = context.getFileSystem().getPath(line.getOptionValue("mapping-parameters-file"));
        }

        int firstVariant = line.hasOption("first-variant") ? Integer.parseInt(line.getOptionValue("first-variant")) : 0;
        int variantCount = line.hasOption("variant-count") ? Integer.parseInt(line.getOptionValue("variant-count")) : Integer.MAX_VALUE;

        int version = Integer.parseInt(line.getOptionValue("version"));

        Path inputTimeSeriesJsonFile = context.getFileSystem().getPath(line.getOptionValue("input-time-series-json-file"));

        boolean ignoreLimits = line.hasOption("ignore-limits");
        boolean ignoreEmptyFilter = line.hasOption("ignore-empty-filter");

        ContingenciesProvider contingenciesProvider;
        if (line.hasOption("contingencies-file")) {
            Path contingenciesFile = context.getFileSystem().getPath(line.getOptionValue("contingencies-file"));
            context.getOutputStream().println("Using contingencies file " + contingenciesFile);
            ObjectMapper objectMapper = JsonUtil.createObjectMapper()
                    .registerModule(new ContingencyJsonModule());
            List<Contingency> contingencies;
            try (Reader reader = Files.newBufferedReader(contingenciesFile, StandardCharsets.UTF_8)) {
                contingencies = objectMapper.readValue(reader, new TypeReference<ArrayList<Contingency>>() {
                });
            }
            contingenciesProvider = network -> contingencies;
        } else {
            contingenciesProvider = network -> Collections.emptyList();
        }

        Path remedialActionsFile = null;
        if (line.hasOption("remedial-actions-file")) {
            remedialActionsFile = context.getFileSystem().getPath(line.getOptionValue("remedial-actions-file"));
        }

        MetrixConfig metrixConfig = MetrixConfig.load();

        MetrixParameters metrixParameters = MetrixParameters.load();
        MappingParameters mappingParameters = MappingParameters.load();

        context.getOutputStream().println("Version " + version);
        context.getOutputStream().println("Variants [" + firstVariant + ", " + (firstVariant + variantCount - 1) + "]");

        context.getOutputStream().println("Loading case " + caseFile + "...");
        Stopwatch stopwatch = Stopwatch.createStarted();

        Network network = Importers.loadNetwork(caseFile, context.getShortTimeExecutionComputationManager(), ImportConfig.load(), null);

        stopwatch.stop();
        context.getOutputStream().println("Case loaded in " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");

        context.getOutputStream().println("Loading time series " + inputTimeSeriesJsonFile + "...");
        stopwatch.reset();
        stopwatch.start();

        List<DoubleTimeSeries> inputTimeSeries = TimeSeries.parseJson(inputTimeSeriesJsonFile).stream()
                .map(DoubleTimeSeries.class::cast)
                .collect(Collectors.toList());
        Collections.reverse(inputTimeSeries);

        ReadOnlyTimeSeriesStoreCache cache = new ReadOnlyTimeSeriesStoreCache(new ArrayList<>(inputTimeSeries.stream()
                .collect(Collectors.toMap(tsx -> tsx.getMetadata().getName(), Function.identity(), (ts1, ts2) -> {
                    LOGGER.warn("Multiple timeseries with same name found : {}", ts1.getMetadata().getName());
                    if (StoredDoubleTimeSeries.class.isAssignableFrom(ts1.getClass())) {
                        return ts1;
                    }
                    return ts2;
                }))
                .values())
        );
        TimeSeriesNameResolver resolver = new FromStoreTimeSeriesNameResolver(cache, version);

        // resolve calculated time series with stored time series
        for (DoubleTimeSeries ts : inputTimeSeries) {
            // Stored ts doesn't need a resolver
            if (ts.getClass().isAssignableFrom(CalculatedTimeSeries.class)) {
                ts.setTimeSeriesNameResolver(resolver);
            }
        }
        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache(inputTimeSeries);

        stopwatch.stop();
        context.getOutputStream().println(inputTimeSeries.size() + " time series loaded in " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");

        if (mappingParametersFile != null) {
            context.getOutputStream().println("Loading json mapping parameters file " + mappingParametersFile + "...");
            ObjectMapper objectMapper = new ObjectMapper();
            try (Reader reader = Files.newBufferedReader(mappingParametersFile, StandardCharsets.UTF_8)) {
                mappingParameters = objectMapper.readValue(reader, MappingParameters.class);
            }
        }
        TimeSeriesMappingConfig mappingConfig;
        if (mappingConfigFile != null || mappingFile != null) {
            stopwatch.reset();
            stopwatch.start();
            if (mappingConfigFile != null) {
                context.getOutputStream().println("Loading time series mapping config json " + mappingConfigFile + "...");
                ObjectMapper objectMapper = JsonUtil.createObjectMapper()
                        .registerModule(new TimeSeriesMappingConfigJsonModule());
                try (Reader reader = Files.newBufferedReader(mappingConfigFile, StandardCharsets.UTF_8)) {
                    mappingConfig = objectMapper.readValue(reader, TimeSeriesMappingConfig.class);
                }
            } else {
                context.getOutputStream().println("Loading time series mapping groovy " + mappingFile + "...");
                context.getOutputStream().println("Warning this is a legacy mapping process (now the correct way is to use the serialized mapping result). The stats (sum, max, mean, etc.) won't be consistent with the new way.");
                // there is a problem here. Computation range will differ from computation range used in the mapping executed on the server
                mappingConfig = TimeSeriesDslLoader.load(mappingFile, network, mappingParameters, store, null);
            }
            stopwatch.stop();
            context.getOutputStream().println("Time series mapping loaded in " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
        } else {
            mappingConfig = new TimeSeriesMappingConfig(network);
        }

        Path metrixDslDataFile = null;
        if (line.hasOption("metrix-dsl-data-file")) {
            metrixDslDataFile = context.getFileSystem().getPath(line.getOptionValue("metrix-dsl-data-file"));
        }
        Path metrixDslFile = null;
        if (line.hasOption("metrix-dsl-file")) {
            metrixDslFile = context.getFileSystem().getPath(line.getOptionValue("metrix-dsl-file"));
        }
        Path metrixParametersFile = null;
        if (line.hasOption("metrix-parameters-file")) {
            metrixParametersFile = context.getFileSystem().getPath(line.getOptionValue("metrix-parameters-file"));
        }

        MetrixDslData metrixDslData = null;
        if (metrixDslDataFile != null) {
            context.getOutputStream().println("Loading json dsl data file " + metrixDslDataFile + "...");
            ObjectMapper objectMapper = new ObjectMapper();
            try (Reader reader = Files.newBufferedReader(metrixDslDataFile, StandardCharsets.UTF_8)) {
                metrixDslData = objectMapper.readValue(reader, MetrixDslData.class);
            }
        } else if (metrixDslFile != null) {
            context.getOutputStream().println("Loading groovy dsl data file " + metrixDslFile + "...");
            metrixDslData = MetrixDslDataLoader.load(metrixDslFile, network, metrixParameters, store, mappingConfig);
        }
        if (metrixParametersFile != null) {
            context.getOutputStream().println("Loading json metrix parameters file " + metrixParametersFile + "...");
            ObjectMapper objectMapper = new ObjectMapper();
            try (Reader reader = Files.newBufferedReader(metrixParametersFile, StandardCharsets.UTF_8)) {
                metrixParameters = objectMapper.readValue(reader, MetrixParameters.class);
            }
        }

        Range<Integer> variantRange = Range.closed(firstVariant, firstVariant + variantCount - 1);

        MetrixVariantProvider variantProvider = new MetrixTimeSeriesVariantProvider(network, store, mappingParameters,
                mappingConfig, contingenciesProvider, version, variantRange,  ignoreLimits, ignoreEmptyFilter, context.getErrorStream());

        MetrixLogger logger = new MetrixToolLogger(context.getOutputStream());

        Path logFile = line.hasOption("log-file") ? context.getFileSystem().getPath(line.getOptionValue("log-file")) : null;

        // run metrix
        List<TimeSeries> outputTimeSeries = new MetrixChunk(network, context.getShortTimeExecutionComputationManager(), metrixConfig,
                                                            remedialActionsFile, logFile, logger)
                .run(metrixParameters, contingenciesProvider, metrixDslData, variantProvider)
                .join();

        // write output time series to json
        if (line.hasOption("output-time-series-json-file")) {
            String outputTimeSeriesJsonFile = line.getOptionValue("output-time-series-json-file");
            Path outputTimeSeriesJsonFilePath = context.getFileSystem().getPath(outputTimeSeriesJsonFile);

            context.getOutputStream().println("Writing output time series to " + outputTimeSeriesJsonFilePath + "...");
            stopwatch.reset();
            stopwatch.start();

            try (BufferedWriter writer = Files.newBufferedWriter(outputTimeSeriesJsonFilePath, StandardCharsets.UTF_8)) {
                TimeSeries.writeJson(writer, outputTimeSeries);
            }

            stopwatch.stop();
            context.getOutputStream().println("Output time series written in " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
        }
    }
}
