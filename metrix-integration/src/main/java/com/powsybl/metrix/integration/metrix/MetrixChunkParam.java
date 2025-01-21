/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration.metrix;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.commons.json.JsonUtil;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.json.ContingencyJsonModule;
import com.powsybl.tools.ToolRunningContext;
import org.apache.commons.cli.CommandLine;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Valentin Berthault {@literal <valentin.berthault at rte-france.com>}
 */
public class MetrixChunkParam {
    public final Path caseFile;
    public final Path mappingFile;
    public final Path mappingConfigFile;
    public final Path mappingParametersFile;
    public final Path inputTimeSeriesJsonFile;
    public final Path remedialActionsFile;
    public final Path metrixDslDataFile;
    public final Path metrixDslFile;
    public final Path metrixParametersFile;
    public final Path logFile;
    public final Path logFileDetail;
    public final Path networkPointFile;
    public final Path outputTimeSeriesJsonFilePath;
    public final int firstVariant;
    public final int variantCount;
    public final int version;
    public final boolean ignoreLimits;
    public final boolean ignoreEmptyFilter;
    public final boolean writePtdfMatrix;
    public final boolean writeLodfMatrix;
    public final ContingenciesProvider contingenciesProvider;

    MetrixChunkParam(Path caseFile, Path mappingFile, Path mappingConfigFile, Path mappingParametersFile, Path inputTimeSeriesJsonFile,
                     Path remedialActionsFile, Path metrixDslDataFile, Path metrixDslFile, Path metrixParametersFile, Path logFile,
                     Path logFileDetail, Path networkPointFile, Path outputTimeSeriesJsonFilePath, int firstVariant, int variantCount,
                     int version, boolean ignoreLimits, boolean ignoreEmptyFilter, boolean writePtdfMatrix, boolean writeLodfMatrix, ContingenciesProvider contingenciesProvider) {
        this.caseFile = caseFile;
        this.mappingFile = mappingFile;
        this.mappingConfigFile = mappingConfigFile;
        this.mappingParametersFile = mappingParametersFile;
        this.inputTimeSeriesJsonFile = inputTimeSeriesJsonFile;
        this.remedialActionsFile = remedialActionsFile;
        this.metrixDslDataFile = metrixDslDataFile;
        this.metrixDslFile = metrixDslFile;
        this.metrixParametersFile = metrixParametersFile;
        this.logFile = logFile;
        this.logFileDetail = logFileDetail;
        this.networkPointFile = networkPointFile;
        this.outputTimeSeriesJsonFilePath = outputTimeSeriesJsonFilePath;
        this.firstVariant = firstVariant;
        this.variantCount = variantCount;
        this.version = version;
        this.ignoreLimits = ignoreLimits;
        this.ignoreEmptyFilter = ignoreEmptyFilter;
        this.writePtdfMatrix = writePtdfMatrix;
        this.writeLodfMatrix = writeLodfMatrix;
        this.contingenciesProvider = contingenciesProvider;
    }

    public static class MetrixChunkParamBuilder {
        private Path caseFile;
        private Path mappingFile;
        private Path mappingConfigFile;
        private Path mappingParametersFile;
        private Path inputTimeSeriesJsonFile;
        private Path remedialActionsFile;
        private Path metrixDslDataFile;
        private Path metrixDslFile;
        private Path metrixParametersFile;
        private Path logFile;
        private Path logFileDetail;
        private Path networkPointFile;
        private Path outputTimeSeriesJsonFilePath;
        private int firstVariant;
        private int variantCount;
        private int version;
        private boolean ignoreLimits;
        private boolean ignoreEmptyFilter;
        private boolean writePtdfMatrix;
        private boolean writeLodfMatrix;
        private ContingenciesProvider contingenciesProvider;

        public MetrixChunkParamBuilder simpleInit(int version, boolean ignoreLimits, boolean ignoreEmptyFilter,
                                                  ContingenciesProvider contingenciesProvider, Path networkPointFile,
                                                  Path logFile, Path logFileDetail, Path remedialActionsFile) {
            this.version = version;
            this.ignoreLimits = ignoreLimits;
            this.ignoreEmptyFilter = ignoreEmptyFilter;
            this.contingenciesProvider = contingenciesProvider;
            this.networkPointFile = networkPointFile;
            this.logFile = logFile;
            this.logFileDetail = logFileDetail;
            this.remedialActionsFile = remedialActionsFile;
            return this;
        }

        public MetrixChunkParamBuilder readCommandLine(CommandLine line, ToolRunningContext context) throws IOException {
            this.caseFile = context.getFileSystem().getPath(line.getOptionValue("case-file"));
            this.mappingFile = line.hasOption("mapping-file") ? context.getFileSystem().getPath(line.getOptionValue("mapping-file")) : null;
            this.mappingConfigFile = line.hasOption("mapping-config-file") ? context.getFileSystem().getPath(line.getOptionValue("mapping-config-file")) : null;
            this.mappingParametersFile = line.hasOption("mapping-parameters-file") ? context.getFileSystem().getPath(line.getOptionValue("mapping-parameters-file")) : null;
            this.remedialActionsFile = line.hasOption("remedial-actions-file") ? context.getFileSystem().getPath(line.getOptionValue("remedial-actions-file")) : null;
            this.metrixDslDataFile = line.hasOption("metrix-dsl-data-file") ? context.getFileSystem().getPath(line.getOptionValue("metrix-dsl-data-file")) : null;
            this.metrixDslFile = line.hasOption("metrix-dsl-file") ? context.getFileSystem().getPath(line.getOptionValue("metrix-dsl-file")) : null;
            this.metrixParametersFile = line.hasOption("metrix-parameters-file") ? context.getFileSystem().getPath(line.getOptionValue("metrix-parameters-file")) : null;
            this.logFile = line.hasOption("log-file") ? context.getFileSystem().getPath(line.getOptionValue("log-file")) : null;
            this.logFileDetail = line.hasOption("log-file-detail-format") ? context.getFileSystem().getPath(line.getOptionValue("log-file-detail-format")) : null;
            this.networkPointFile = line.hasOption("network-point-file") ? context.getFileSystem().getPath(line.getOptionValue("network-point-file")) : null;
            this.outputTimeSeriesJsonFilePath = line.hasOption("output-time-series-json-file") ? context.getFileSystem().getPath(line.getOptionValue("output-time-series-json-file")) : null;
            this.firstVariant = line.hasOption("first-variant") ? Integer.parseInt(line.getOptionValue("first-variant")) : 0;
            this.variantCount = line.hasOption("variant-count") ? Integer.parseInt(line.getOptionValue("variant-count")) : Integer.MAX_VALUE;
            this.version = Integer.parseInt(line.getOptionValue("version"));
            this.inputTimeSeriesJsonFile = context.getFileSystem().getPath(line.getOptionValue("input-time-series-json-file"));
            this.ignoreLimits = line.hasOption("ignore-limits");
            this.ignoreEmptyFilter = line.hasOption("ignore-empty-filter");
            this.writePtdfMatrix = line.hasOption("write-ptdf");
            this.writeLodfMatrix = line.hasOption("write-lodf");
            initializeContingenciesProvider(line, context);
            return this;
        }

        private void initializeContingenciesProvider(CommandLine line, ToolRunningContext context) throws IOException {
            if (!line.hasOption("contingencies-file")) {
                contingenciesProvider = network -> Collections.emptyList();
                return;
            }

            Path contingenciesFile = context.getFileSystem().getPath(line.getOptionValue("contingencies-file"));
            context.getOutputStream().println("Using contingencies file " + contingenciesFile);
            ObjectMapper objectMapper = JsonUtil.createObjectMapper().registerModule(new ContingencyJsonModule());
            List<Contingency> contingencies;
            try (Reader reader = Files.newBufferedReader(contingenciesFile, StandardCharsets.UTF_8)) {
                contingencies = objectMapper.readValue(reader, new TypeReference<ArrayList<Contingency>>() {
                });
            }
            contingenciesProvider = network -> contingencies;
        }

        public MetrixChunkParam build() {
            return new MetrixChunkParam(caseFile, mappingFile, mappingConfigFile, mappingParametersFile, inputTimeSeriesJsonFile, remedialActionsFile,
                    metrixDslDataFile, metrixDslFile, metrixParametersFile, logFile, logFileDetail, networkPointFile, outputTimeSeriesJsonFilePath,
                    firstVariant, variantCount, version, ignoreLimits, ignoreEmptyFilter, writePtdfMatrix, writeLodfMatrix, contingenciesProvider);
        }
    }
}
