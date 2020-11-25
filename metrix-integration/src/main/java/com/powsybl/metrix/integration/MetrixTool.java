package com.powsybl.metrix.integration;

import com.google.auto.service.AutoService;
import com.google.common.base.Stopwatch;
import com.powsybl.action.dsl.GroovyDslContingenciesProvider;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.iidm.import_.ImportConfig;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.metrix.commons.MetrixAppLogger;
import com.powsybl.metrix.mapping.timeseries.FileSystemTimeseriesStore;
import com.powsybl.metrix.mapping.timeseries.InMemoryTimeSeriesStore;
import com.powsybl.metrix.mapping.timeseries.TimeSeriesStoreUtil;
import com.powsybl.timeseries.TimeSeries;
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
import java.nio.file.Paths;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipOutputStream;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian@rte-france.com>
 */
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
                        .longOpt("input-time-series")
                        .desc("time series spaces list in the DB")
                        .hasArg()
                        .argName("SPACE1,SPACE2,...")
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

        List<String> spaceNames = Arrays.stream(line.getOptionValue("input-time-series").split(",")).map(String::valueOf).collect(Collectors.toList());

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
        store.importTimeSeries(spaceNames.stream().map(Paths::get).collect(Collectors.toList()));

        MetrixAppLogger logger = new MetrixAppLogger() {
            private String tag = "INFO";

            @Override
            public void log(String message, Object... args) {
                try {
                    IOUtils.write(String.format(tag + "\t" + message + "\n", args), context.getOutputStream(), Charset.defaultCharset());
                } catch (IOException e) {
                    e.printStackTrace();
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

        NetworkSource networkSource = new NetworkSource() {
            @Override
            public Network copy() {
                logger.tagged("info").log("Loading case ...");

                Network network = Importers.loadNetwork(caseFile, context.getShortTimeExecutionComputationManager(), ImportConfig.load(), null);

                stopwatch.stop();
                logger.tagged("performance").log("Case loaded in %d ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));

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

        Supplier<Reader> mappingReaderSupplier = () -> {
            try {
                return Files.newBufferedReader(mappingFile, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };

        ContingenciesProvider contingenciesProvider = null;
        if (contingenciesFile != null) {
            contingenciesProvider = new GroovyDslContingenciesProvider(contingenciesFile);
        }

        Supplier<Reader> metrixDslReaderSupplier = null;
        if (metrixDslFile != null) {
            metrixDslReaderSupplier = () -> {
                try {
                    return Files.newBufferedReader(metrixDslFile, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            };
        }

        Supplier<Reader> remedialActionsReaderSupplier = null;
        if (remedialActionsFile != null) {
            remedialActionsReaderSupplier = () -> {
                try {
                    return Files.newBufferedReader(remedialActionsFile, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            };
        }

        FileSystemTimeseriesStore resultStore = new FileSystemTimeseriesStore(Paths.get("metrix_results_" + UUID.randomUUID()));

        try (ZipOutputStream logArchive = createLogArchive(line, context, versions)) {
            new Metrix(networkSource, contingenciesProvider, mappingReaderSupplier, metrixDslReaderSupplier, remedialActionsReaderSupplier,
                    store, resultStore, logArchive, context.getLongTimeExecutionComputationManager(), logger, ignore -> {
            })
                    .run(new MetrixRunParameters(firstVariant, variantCount, versions, chunkSize, ignoreLimits, ignoreEmptyFilter),
                            new Metrix.DefaultResultListener() {

                                @Override
                                public void onChunkResult(int version, int chunk, List<TimeSeries> timeSeriesList) {
                                    resultStore.importTimeSeries(timeSeriesList, version, false);
                                }

                                @Override
                                public void onEnd() {
                                    // csv export
                                    if (csvResultFilePath != null) {
                                        stopwatch.reset();
                                        stopwatch.start();

                                        context.getOutputStream().println("Writing results to CSV file...");

                                        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(csvResultFilePath)), StandardCharsets.UTF_8))) {
                                            TimeSeriesStoreUtil.writeCsv(resultStore, writer, ';', ZoneId.systemDefault());
                                        } catch (IOException e) {
                                            throw new UncheckedIOException(e);
                                        }

                                        stopwatch.stop();
                                        context.getOutputStream().println("Results written to CSV file in " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
                                    }
                                }
                            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            resultStore.delete();
        }

        context.getOutputStream().println("Done in " + globalStopwatch.elapsed(TimeUnit.SECONDS) + " s");
    }

}
