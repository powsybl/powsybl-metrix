/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.tools;

import com.google.auto.service.AutoService;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.contingency.dsl.GroovyDslContingenciesProvider;
import com.powsybl.iidm.import_.ImportConfig;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.metrix.integration.*;
import com.powsybl.metrix.integration.dataGenerator.MetrixInputData;
import com.powsybl.metrix.mapping.TimeSeriesMappingConfig;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStore;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStoreCache;
import com.powsybl.tools.Command;
import com.powsybl.tools.Tool;
import com.powsybl.tools.ToolRunningContext;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.nio.file.Path;

@AutoService(Tool.class)
public class MetrixDieTool implements Tool {

    @Override
    public Command getCommand() {
        return new Command() {
            @Override
            public String getName() {
                return "metrix-die";
            }

            @Override
            public String getTheme() {
                return "Metrix";
            }

            @Override
            public String getDescription() {
                return "Generate Metrix DIE files";
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
                        .desc("name of the remedial actions file")
                        .hasArg()
                        .argName("FILE")
                        .build());
                options.addOption(Option.builder()
                        .longOpt("output-dir")
                        .desc("Output directory")
                        .hasArg()
                        .argName("DIR")
                        .build());
                return options;
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

        Path contingenciesFile = line.hasOption("contingencies-file")
                ? context.getFileSystem().getPath(line.getOptionValue("contingencies-file"))
                : null;

        Path metrixDslFile = line.hasOption("metrix-dsl-file")
                ? context.getFileSystem().getPath(line.getOptionValue("metrix-dsl-file"))
                : null;

        Path remedialActionFile = line.hasOption("remedial-actions-file")
                ? context.getFileSystem().getPath(line.getOptionValue("remedial-actions-file"))
                : null;

        Path outputDir = line.hasOption("output-dir")
                ? context.getFileSystem().getPath(line.getOptionValue("output-dir"))
                : null;

        context.getOutputStream().println("Loading case ...");

        Network network = Importers.loadNetwork(caseFile, context.getShortTimeExecutionComputationManager(), ImportConfig.load(), null);

        MetrixParameters parameters = MetrixParameters.load();

        MetrixConfig config = MetrixConfig.load();

        ContingenciesProvider contingenciesProvider = null;
        if (contingenciesFile != null) {
            contingenciesProvider = new GroovyDslContingenciesProvider(contingenciesFile);
        }

        TimeSeriesMappingConfig mappingConfig = new TimeSeriesMappingConfig(network);

        MetrixDslData metrixDslData = null;
        if (metrixDslFile != null) {
            ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache() {
                @Override
                public boolean timeSeriesExists(String timeSeriesName) {
                    return true;
                }
            };
            metrixDslData = MetrixDslDataLoader.load(metrixDslFile, network, parameters, store, mappingConfig);
        }

        context.getOutputStream().println("Writing DIE ...");

        // write DIE
        new MetrixInputData(MetrixNetwork.create(network, contingenciesProvider, null, parameters, remedialActionFile), metrixDslData, parameters)
                .write(outputDir, true, config.isConstantLossFactor());
    }
}
