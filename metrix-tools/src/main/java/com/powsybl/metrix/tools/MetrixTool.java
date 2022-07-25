/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.tools;

import com.google.auto.service.AutoService;
import com.google.common.base.Stopwatch;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.contingency.EmptyContingencyListProvider;
import com.powsybl.contingency.dsl.GroovyDslContingenciesProvider;
import com.powsybl.iidm.import_.ImportConfig;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.metrix.integration.*;
import com.powsybl.metrix.integration.compatibility.CsvResultListener;
import com.powsybl.metrix.integration.metrix.MetrixAnalysis;
import com.powsybl.metrix.integration.metrix.MetrixAnalysisResult;
import com.powsybl.metrix.mapping.ComputationRange;
import com.powsybl.metrix.mapping.timeseries.FileSystemTimeseriesStore;
import com.powsybl.metrix.mapping.timeseries.InMemoryTimeSeriesStore;
import com.powsybl.tools.Command;
import com.powsybl.tools.Tool;
import com.powsybl.tools.ToolRunningContext;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.ZipOutputStream;

@AutoService(Tool.class)
public class MetrixTool implements Tool {

    @Override
    public Command getCommand() {
        return new Command() {
            @Override
            public String getName() {
                return "metrix";
            }

            @Override
            public String getTheme() {
                return "Metrix";
            }

            @Override
            public String getDescription() {
                return "Run Metrix";
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
                        .longOpt("mapping-file")
                        .desc("Groovy DSL file that describes the mapping")
                        .hasArg()
                        .argName("FILE")
                        .required()
                        .build());
                options.addOption(Option.builder()
                        .longOpt("contingencies-file")
                        .desc("Groovy DSL file that describes contingencies")
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
                        .longOpt("remedial-actions-file")
                        .desc("Name of the remedial actions file")
                        .hasArg()
                        .argName("FILE")
                        .build());
                options.addOption(Option.builder()
                        .longOpt("time-series")
                        .desc("time series csv list")
                        .hasArg()
                        .argName("FILE1,FILE2,...")
                        .required()
                        .build());
                options.addOption(Option.builder()
                        .longOpt("versions")
                        .desc("time series versions")
                        .hasArg()
                        .argName("NUM1,NUM2,...")
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
                        .longOpt("ignore-limits")
                        .desc("ignore generator limits")
                        .build());
                options.addOption(Option.builder()
                        .longOpt("ignore-empty-filter")
                        .desc("ignore empty filter with non zero time series value")
                        .build());
                options.addOption(Option.builder()
                        .longOpt("csv-results-file")
                        .desc("CSV file results")
                        .hasArg()
                        .argName("FILE")
                        .build());
                options.addOption(Option.builder()
                        .longOpt("chunk-size")
                        .desc("chunk size")
                        .hasArg()
                        .argName("SIZE")
                        .build());
                options.addOption(Option.builder()
                        .longOpt("log-archive")
                        .hasArg()
                        .argName("FILE")
                        .desc("name of gzip file containing execution logs")
                        .build());
                options.addOption(Option.builder()
                        .longOpt("network-point-file")
                        .desc("output network point file")
                        .hasArg()
                        .argName("FILE")
                        .build());
                return options;
            }

            @Override
            public String getUsageFooter() {
                return null;
            }
        };
    }

    private static ZipOutputStream createLogArchive(CommandLine line, ToolRunningContext context, TreeSet<Integer> versions) {
        if (line.hasOption("log-archive")) {

            if (versions.size() > 1) {
                throw new IllegalArgumentException("Log archive option can only be used with a single version");
            }

            String logfileName = line.getOptionValue("log-archive");
            if (!logfileName.endsWith(".zip")) {
                logfileName += ".zip";
            }
            Path logZipFile = context.getFileSystem().getPath(logfileName);
            try {
                return new ZipOutputStream(Files.newOutputStream(logZipFile));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return null;
    }

    @Override
    public void run(CommandLine line, ToolRunningContext context) throws IOException {
        Path caseFile = context.getFileSystem().getPath(line.getOptionValue("case-file"));

        Path mappingFile = context.getFileSystem().getPath(line.getOptionValue("mapping-file"));

        Path contingenciesFile = line.hasOption("contingencies-file")
                ? context.getFileSystem().getPath(line.getOptionValue("contingencies-file"))
                : null;

        Path metrixDslFile = line.hasOption("metrix-dsl-file")
                ? context.getFileSystem().getPath(line.getOptionValue("metrix-dsl-file"))
                : null;

        Path remedialActionsFile = line.hasOption("remedial-actions-file") ?
                context.getFileSystem().getPath(line.getOptionValue("remedial-actions-file"))
                : null;

        boolean ignoreLimits = line.hasOption("ignore-limits");
        boolean ignoreEmptyFilter = line.hasOption("ignore-empty-filter");

        List<String> tsCsvs = Arrays.stream(line.getOptionValue("time-series").split(",")).map(String::valueOf).collect(Collectors.toList());

        int chunkSize = line.hasOption("chunk-size") ? Integer.parseInt(line.getOptionValue("chunk-size")) : -1;

        TreeSet<Integer> versions = Arrays.stream(line.getOptionValue("versions").split(","))
                .map(Integer::valueOf)
                .collect(Collectors.toCollection(TreeSet::new));
        if (versions.isEmpty()) {
            throw new IllegalArgumentException("Empty version list");
        }

        final Path csvResultFilePath;
        if (line.hasOption("csv-results-file")) {
            String csvResultsFile = line.getOptionValue("csv-results-file");
            if (!csvResultsFile.endsWith(".gz")) {
                csvResultsFile += ".gz";
            }
            csvResultFilePath = context.getFileSystem().getPath(csvResultsFile);
        } else {
            csvResultFilePath = null;
        }

        InMemoryTimeSeriesStore store = new InMemoryTimeSeriesStore();
        store.importTimeSeries(tsCsvs.stream().map(context.getFileSystem()::getPath).collect(Collectors.toList()));

        MetrixAppLogger logger = new MetrixAppLogger() {
            private String tag = "INFO";

            @Override
            public void log(String message, Object... args) {
                try {
                    IOUtils.write(String.format(tag + "\t" + message + "\n", args), context.getOutputStream(), Charset.defaultCharset());
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }

            @Override
            public MetrixAppLogger tagged(String tag) {
                this.tag = tag;
                return this;
            }
        };

        Stopwatch globalStopwatch = Stopwatch.createStarted();

        Stopwatch stopwatch = Stopwatch.createStarted();

        // define variant range to compute
        int firstVariant = line.hasOption("first-variant") ? Integer.parseInt(line.getOptionValue("first-variant")) : 0;
        int variantCount = line.hasOption("variant-count") ? Integer.parseInt(line.getOptionValue("variant-count")) : -1;

        NetworkSource networkSource = getNetworkSource(context, caseFile, logger);
        Reader mappingReader = getReader(mappingFile);

        ContingenciesProvider contingenciesProvider = null;
        if (contingenciesFile != null) {
            contingenciesProvider = new GroovyDslContingenciesProvider(contingenciesFile);
        } else {
            contingenciesProvider = new EmptyContingencyListProvider();
        }

        Reader metrixDslReader = getReader(metrixDslFile);
        Reader remedialActionsReaderForAnalysis = getReader(remedialActionsFile);
        Reader remedialActionsReaderForRun = getReader(remedialActionsFile);

        FileSystemTimeseriesStore resultStore = new FileSystemTimeseriesStore(context.getFileSystem().getPath("metrix_results_" + UUID.randomUUID()));

        try (ZipOutputStream logArchive = createLogArchive(line, context, versions)) {
            MetrixRunParameters runParameters = new MetrixRunParameters(firstVariant, variantCount, versions, chunkSize, ignoreLimits, ignoreEmptyFilter, false);
            ComputationRange computationRange = new ComputationRange(runParameters.getVersions(), runParameters.getFirstVariant(), runParameters.getVariantCount());
            MetrixAnalysis metrixAnalysis = new MetrixAnalysis(networkSource, mappingReader, metrixDslReader, remedialActionsReaderForAnalysis,
                    store, logger, computationRange);
            MetrixAnalysisResult analysisResult = metrixAnalysis.runAnalysis("extern tool");
            new Metrix(contingenciesProvider, remedialActionsReaderForRun,
                    store, resultStore, logArchive, context.getLongTimeExecutionComputationManager(), logger, analysisResult)
                    .run(runParameters, new CsvResultListener(csvResultFilePath, resultStore, stopwatch, context), null);

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            resultStore.delete();
        }

        context.getOutputStream().println("Done in " + globalStopwatch.elapsed(TimeUnit.SECONDS) + " s");
    }

    private Reader getReader(Path filePath) {
        if (filePath != null) {
            try {
                return Files.newBufferedReader(filePath, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return null;
    }

    private NetworkSource getNetworkSource(ToolRunningContext context, Path caseFile, MetrixAppLogger logger) {
        return new NetworkSource() {
            @Override
            public Network copy() {
                Stopwatch networkLoadingStopwatch = Stopwatch.createStarted();
                logger.tagged("info").log("Loading case ...");

                Network network = Importers.loadNetwork(caseFile, context.getShortTimeExecutionComputationManager(), ImportConfig.load(), null);

                networkLoadingStopwatch.stop();
                logger.tagged("performance").log("Case loaded in %d ms", networkLoadingStopwatch.elapsed(TimeUnit.MILLISECONDS));

                return network;
            }

            @Override
            public void write(OutputStream os) {
                try {
                    Files.copy(caseFile, os);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        };
    }
}
