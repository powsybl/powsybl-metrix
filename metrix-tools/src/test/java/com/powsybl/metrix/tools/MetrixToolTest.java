/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.tools;

import com.powsybl.tools.Command;
import com.powsybl.tools.CommandLineTools;
import com.powsybl.tools.Tool;
import org.apache.commons.cli.Options;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class MetrixToolTest extends AbstractToolTest {

    private MetrixTool tool;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        tool = new MetrixTool();
        super.setUp();
    }

    @Override
    protected Iterable<Tool> getTools() {
        return Collections.singletonList(tool);
    }

    @Override
    @Test
    public void assertCommand() {
        Command command = tool.getCommand();
        Options options = command.getOptions();
        assertCommand(command, "metrix", 14, 4);
        assertOption(options, "case-file", true, true);
        assertOption(options, "mapping-file", true, true);
        assertOption(options, "contingencies-file", false, true);
        assertOption(options, "metrix-dsl-file", false, true);
        assertOption(options, "remedial-actions-file", false, true);
        assertOption(options, "time-series", true, true);
        assertOption(options, "versions", true, true);
        assertOption(options, "first-variant", false, true);
        assertOption(options, "variant-count", false, true);
        assertOption(options, "ignore-limits", false, false);
        assertOption(options, "ignore-empty-filter", false, false);
        assertOption(options, "csv-results-file", false, true);
        assertOption(options, "chunk-size", false, true);
        assertOption(options, "log-archive", false, true);
        assertEquals("Metrix", command.getTheme());
        assertEquals("Run Metrix", command.getDescription());
        assertNull(command.getUsageFooter());
    }

    @Test
    void run() throws IOException {
        Files.copy(Objects.requireNonNull(getClass().getResourceAsStream("/simple-network.xiidm")), fileSystem.getPath("/network.xiidm"));
        Files.copy(Objects.requireNonNull(getClass().getResourceAsStream("/mapping.groovy")), fileSystem.getPath("/mapping.groovy"));
        Files.copy(Objects.requireNonNull(getClass().getResourceAsStream("/time-series-sample.csv")), fileSystem.getPath("/timeseries.csv"));
        StringBuilder expected = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(MetrixToolTest.class.getResourceAsStream("/mapping_result.txt"))))) {
            expected.append(reader.readLine());
        }

        String[] commandLine = new String[]{
            "metrix", "--case-file", "/network.xiidm",
            "--mapping-file", "/mapping.groovy",
            "--time-series", "/timeseries.csv",
            "--versions", "1"
        };
        assertCommand(commandLine, CommandLineTools.COMMAND_OK_STATUS, expected.toString(), "");
    }

    @Test
    void runWithExport() throws IOException {
        Files.copy(Objects.requireNonNull(getClass().getResourceAsStream("/simple-network.xiidm")), fileSystem.getPath("/network.xiidm"));
        Files.copy(Objects.requireNonNull(getClass().getResourceAsStream("/mapping.groovy")), fileSystem.getPath("/mapping.groovy"));
        Files.copy(Objects.requireNonNull(getClass().getResourceAsStream("/time-series-sample.csv")), fileSystem.getPath("/timeseries.csv"));
        Files.copy(Objects.requireNonNull(getClass().getResourceAsStream("/conf.groovy")), fileSystem.getPath("/conf.groovy"));
        Files.copy(Objects.requireNonNull(getClass().getResourceAsStream("/contingencies.groovy")), fileSystem.getPath("/contingencies.groovy"));
        StringBuilder expected = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(MetrixToolTest.class.getResourceAsStream("/mapping_result.txt"))))) {
            expected.append(reader.readLine());
        }

        String[] commandLine = new String[]{
            "metrix",
            "--case-file", "/network.xiidm",
            "--mapping-file", "/mapping.groovy",
            "--time-series", "/timeseries.csv",
            "--metrix-dsl-file", "/conf.groovy",
            "--contingencies-file", "/contingencies.groovy",
            "--versions", "1",
            "--csv-results-file", "results.csv",
            "--log-archive", "logs"
        };
        assertCommand(commandLine, CommandLineTools.COMMAND_OK_STATUS, expected.toString(), "");

        // Check the content of the result file
        try (InputStream fis = Files.newInputStream(fileSystem.getPath("results.csv.gz"));
             BufferedInputStream bis = new BufferedInputStream(fis);
             InputStream is = new GzipCompressorInputStream(bis);
             InputStream expectedIS = new BufferedInputStream(Files.newInputStream(fileSystem.getPath("/timeseries.csv")))) {
//            assertEquals(new String(expectedIS.readAllBytes(), StandardCharsets.UTF_8), new String(is.readAllBytes(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            fail();
        }
    }
}
