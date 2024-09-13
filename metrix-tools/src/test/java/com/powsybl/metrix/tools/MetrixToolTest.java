/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.tools;

import com.powsybl.tools.Command;
import com.powsybl.tools.CommandLineTools;
import com.powsybl.tools.Tool;
import org.apache.commons.cli.Options;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Paul Bui-Quang {@literal <paul.buiquang at rte-france.com>}
 */
class MetrixToolTest extends AbstractToolTest {

    private MetrixTool tool;

    @Override
    @BeforeEach
    public void setUp() {
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
        Files.copy(Objects.requireNonNull(getClass().getResourceAsStream("/expected_results.csv")), fileSystem.getPath("/expected_results.csv"));
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
        assertTimeSeriesCsvEquals(fileSystem.getPath("/expected_results.csv"), fileSystem.getPath("results.csv.gz"));
    }

    /**
     * Compare TimeSeries CSV by comparing line-by-line the index as datetime first (since the datetime formatting in
     * the CSV depends on <code>ZoneId.systemDefault()</code>, it is timezone dependent) and then the rest of the line.
     */
    private void assertTimeSeriesCsvEquals(Path expectedCsv, Path actualCsv) {
        // Date-time formatter
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.systemDefault());

        // Open the two files
        try (BufferedReader expectedReader = Files.newBufferedReader(expectedCsv);
             InputStream fis = Files.newInputStream(actualCsv);
             GzipCompressorInputStream gis = new GzipCompressorInputStream(fis);
             InputStreamReader inputStreamReader = new InputStreamReader(gis);
             BufferedReader actualReader = new BufferedReader(inputStreamReader)) {
            // Compare the first lines
            String csvLineExpected = expectedReader.readLine();
            String csvLineActual = actualReader.readLine();
            assertEquals(csvLineExpected, csvLineActual);

            // Read the next line
            csvLineExpected = expectedReader.readLine();
            csvLineActual = actualReader.readLine();

            // Read line by line
            while (csvLineExpected != null && csvLineActual != null) {
                // Split the lines on separator
                String[] dataExpected = csvLineExpected.split(";", 2);
                String[] dataActual = csvLineActual.split(";", 2);

                // Compare the datetime elements (the value should be the same but formatting can differ due to different timezone)
                ZonedDateTime dateTimeExpected = ZonedDateTime.parse(dataExpected[0]);
                ZonedDateTime dateTimeActual = ZonedDateTime.parse(dataActual[0], dateTimeFormatter);
                assertEquals(dateTimeExpected.toInstant(), dateTimeActual.toInstant());

                // Compare the rest of the line as a single string
                assertEquals(dataExpected[1], dataActual[1]);

                // Read the next line
                csvLineExpected = expectedReader.readLine();
                csvLineActual = actualReader.readLine();
            }
            // Check same number of lines
            assertTrue(csvLineExpected == null && csvLineActual == null);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
