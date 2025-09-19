/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.tools;

import com.google.auto.service.AutoService;
import com.google.common.base.Stopwatch;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.contingency.EmptyContingencyListProvider;
import com.powsybl.contingency.dsl.GroovyDslContingenciesProvider;
import com.powsybl.iidm.network.ImportConfig;
import com.powsybl.iidm.network.Network;
import com.powsybl.metrix.integration.Metrix;
import com.powsybl.metrix.integration.MetrixAppLogger;
import com.powsybl.metrix.integration.MetrixRunParameters;
import com.powsybl.metrix.integration.network.NetworkSource;
import com.powsybl.metrix.integration.compatibility.CsvResultListener;
import com.powsybl.metrix.integration.analysis.MetrixAnalysis;
import com.powsybl.metrix.integration.analysis.MetrixAnalysisResult;
import com.powsybl.metrix.commons.ComputationRange;
import com.powsybl.metrix.commons.data.datatable.DataTableStore;
import com.powsybl.metrix.mapping.TimeSeriesDslLoader;
import com.powsybl.metrix.commons.data.timeseries.FileSystemTimeSeriesStore;
import com.powsybl.metrix.commons.data.timeseries.InMemoryTimeSeriesStore;
import com.powsybl.tools.Command;
import com.powsybl.tools.Tool;
import com.powsybl.tools.ToolRunningContext;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UncheckedIOException;
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

/**
 * @author Paul Bui-Quang {@literal <paul.buiquang at rte-france.com>}
 */
@AutoService(Tool.class)
public class MetrixTool implements Tool {

    private static final String CONTINGENCIES_FILE = "contingencies-file";
    private static final String METRIX_DSL_FILE = "metrix-dsl-file";
    private static final String REMEDIAL_ACTIONS_FILE = "remedial-actions-file";
    private static final String FIRST_VARIANT = "first-variant";
    private static final String VARIANT_COUNT = "variant-count";
    private static final String CSV_RESULTS_FILE = "csv-results-file";
    private static final String CHUNK_SIZE = "chunk-size";
    private static final String LOG_ARCHIVE = "log-archive";

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
                        .longOpt(CONTINGENCIES_FILE)
                        .desc("Groovy DSL file that describes contingencies")
                        .hasArg()
                        .argName("FILE")
                        .build());
                options.addOption(Option.builder()
                        .longOpt(METRIX_DSL_FILE)
                        .desc("Groovy DSL file that describes the branch monitoring and the phase shifter actions")
                        .hasArg()
                        .argName("FILE")
                        .build());
                options.addOption(Option.builder()
                        .longOpt(REMEDIAL_ACTIONS_FILE)
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
                        .longOpt(FIRST_VARIANT)
                        .desc("first variant to simulate")
                        .hasArg()
                        .argName("NUM")
                        .build());
                options.addOption(Option.builder()
                        .longOpt(VARIANT_COUNT)
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
                        .longOpt(CSV_RESULTS_FILE)
                        .desc("CSV file results")
                        .hasArg()
                        .argName("FILE")
                        .build());
                options.addOption(Option.builder()
                        .longOpt(CHUNK_SIZE)
                        .desc("chunk size")
                        .hasArg()
                        .argName("SIZE")
                        .build());
                options.addOption(Option.builder()
                        .longOpt(LOG_ARCHIVE)
                        .hasArg()
                        .argName("FILE")
                        .desc("name of gzip file containing execution logs")
                        .build());
                options.addOption(Option.builder()
                        .longOpt("write-ptdf")
                        .desc("write ptdf matrix")
                        .build());
                options.addOption(Option.builder()
                        .longOpt("write-lodf")
                        .desc("write lodf matrix")
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
        if (line.hasOption(LOG_ARCHIVE)) {

            if (versions.size() > 1) {
                throw new IllegalArgumentException("Log archive option can only be used with a single version");
            }

            String logfileName = line.getOptionValue(LOG_ARCHIVE);
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

    private Path getCsvResultsFilePath(CommandLine line, ToolRunningContext context) {
        if (line.hasOption(CSV_RESULTS_FILE)) {
            String csvResultsFile = line.getOptionValue(CSV_RESULTS_FILE);
            if (!csvResultsFile.endsWith(".gz")) {
                csvResultsFile += ".gz";
            }
            return context.getFileSystem().getPath(csvResultsFile);
        } else {
            return null;
        }
    }

    @Override
    public void run(CommandLine line, ToolRunningContext context) throws IOException {
        Path caseFile = context.getFileSystem().getPath(line.getOptionValue("case-file"));

        Path mappingFile = context.getFileSystem().getPath(line.getOptionValue("mapping-file"));

        Path contingenciesFile = line.hasOption(CONTINGENCIES_FILE)
                ? context.getFileSystem().getPath(line.getOptionValue(CONTINGENCIES_FILE))
                : null;

        Path metrixDslFile = line.hasOption(METRIX_DSL_FILE)
                ? context.getFileSystem().getPath(line.getOptionValue(METRIX_DSL_FILE))
                : null;

        Path remedialActionsFile = line.hasOption(REMEDIAL_ACTIONS_FILE) ?
                context.getFileSystem().getPath(line.getOptionValue(REMEDIAL_ACTIONS_FILE))
                : null;

        boolean ignoreLimits = line.hasOption("ignore-limits");
        boolean ignoreEmptyFilter = line.hasOption("ignore-empty-filter");

        List<String> tsCsvs = Arrays.stream(line.getOptionValue("time-series").split(",")).map(String::valueOf).toList();

        int chunkSize = line.hasOption(CHUNK_SIZE) ? Integer.parseInt(line.getOptionValue(CHUNK_SIZE)) : -1;

        TreeSet<Integer> versions = Arrays.stream(line.getOptionValue("versions").split(","))
                .map(Integer::valueOf)
                .collect(Collectors.toCollection(TreeSet::new));
        if (versions.isEmpty()) {
            throw new IllegalArgumentException("Empty version list");
        }

        final Path csvResultFilePath = getCsvResultsFilePath(line, context);

        InMemoryTimeSeriesStore store = new InMemoryTimeSeriesStore();
        store.importTimeSeries(tsCsvs.stream().map(context.getFileSystem()::getPath).toList());

        MetrixAppLogger logger = new MetrixAppLogger() {
            private String tag = "INFO";

            @Override
            public void log(String message, Object... args) {
                try {
                    String stringToFormat = tag + "\t" + message + "\n";
                    IOUtils.write(String.format(stringToFormat, args), context.getOutputStream(), Charset.defaultCharset());
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
        int firstVariant = line.hasOption(FIRST_VARIANT) ? Integer.parseInt(line.getOptionValue(FIRST_VARIANT)) : 0;
        int variantCount = line.hasOption(VARIANT_COUNT) ? Integer.parseInt(line.getOptionValue(VARIANT_COUNT)) : -1;

        NetworkSource networkSource = getNetworkSource(context, caseFile, logger);
        Reader mappingReader = getReader(mappingFile);

        ContingenciesProvider contingenciesProvider;
        if (contingenciesFile != null) {
            contingenciesProvider = new GroovyDslContingenciesProvider(contingenciesFile);
        } else {
            contingenciesProvider = new EmptyContingencyListProvider();
        }

        Reader metrixDslReader = getReader(metrixDslFile);
        Reader remedialActionsReaderForAnalysis = getReader(remedialActionsFile);
        Reader remedialActionsReaderForRun = getReader(remedialActionsFile);

        FileSystemTimeSeriesStore resultStore = new FileSystemTimeSeriesStore(context.getFileSystem().getPath("metrix_results_" + UUID.randomUUID()));
        DataTableStore dataTableStore = new DataTableStore();

        try (ZipOutputStream logArchive = createLogArchive(line, context, versions)) {
            ComputationRange computationRange = new ComputationRange(versions, firstVariant, variantCount);
            MetrixRunParameters runParameters = new MetrixRunParameters(computationRange, chunkSize, ignoreLimits, ignoreEmptyFilter, false, false, false);
            TimeSeriesDslLoader timeSeriesDslLoader = new TimeSeriesDslLoader(mappingReader);
            MetrixAnalysis metrixAnalysis = new MetrixAnalysis(networkSource, timeSeriesDslLoader, metrixDslReader, remedialActionsReaderForAnalysis, contingenciesProvider,
                    store, dataTableStore, logger, computationRange);
            MetrixAnalysisResult analysisResult = metrixAnalysis.runAnalysis("extern tool");
            new Metrix(remedialActionsReaderForRun, store, resultStore, logArchive, context, logger, analysisResult)
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

                Network network = Network.read(caseFile, context.getShortTimeExecutionComputationManager(), ImportConfig.load(), null);

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
