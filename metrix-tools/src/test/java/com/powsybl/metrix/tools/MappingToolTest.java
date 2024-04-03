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
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MappingToolTest extends AbstractToolTest {

    private MappingTool tool;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        tool = new MappingTool();
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
        assertCommand(command, "mapping", 13, 3);
        assertOption(options, "case-file", true, true);
        assertOption(options, "mapping-file", true, true);
        assertOption(options, "time-series", true, true);
        assertOption(options, "mapping-synthesis-dir", false, true);
        assertOption(options, "check-equipment-time-series", false, false);
        assertOption(options, "check-versions", false, true);
        assertOption(options, "mapping-status-file", false, true);
        assertOption(options, "network-output-dir", false, true);
        assertOption(options, "equipment-time-series-dir", false, true);
        assertOption(options, "first-variant", false, true);
        assertOption(options, "max-variant-count", false, true);
        assertOption(options, "ignore-limits", false, false);
        assertOption(options, "ignore-empty-filter", false, false);
        assertEquals("Metrix", command.getTheme());
        assertEquals("Time serie to network mapping tool", command.getDescription());
        assertNull(command.getUsageFooter());
    }

    @Test
    void run() throws IOException {
        Files.copy(Objects.requireNonNull(getClass().getResourceAsStream("/simple-network.xiidm")), fileSystem.getPath("/network.xiidm"));
        Files.copy(Objects.requireNonNull(getClass().getResourceAsStream("/mapping.groovy")), fileSystem.getPath("/mapping.groovy"));
        Files.copy(Objects.requireNonNull(getClass().getResourceAsStream("/time-series-sample.csv")), fileSystem.getPath("/timeseries.csv"));
        StringBuilder expected = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream("/mapping_result.txt"))))) {
            expected.append(reader.readLine());
        }

        String[] commandLine = new String[] {
            "mapping", "--case-file", "/network.xiidm",
            "--mapping-file", "/mapping.groovy",
            "--time-series", "/timeseries.csv"
        };
        assertCommand(commandLine, CommandLineTools.COMMAND_OK_STATUS, expected.toString(), StringUtils.EMPTY);
    }

    @Test
    void runMappingIsIncomplete() throws IOException {
        Files.copy(Objects.requireNonNull(getClass().getResourceAsStream("/simple-network.xiidm")), fileSystem.getPath("/network.xiidm"));
        Files.copy(Objects.requireNonNull(getClass().getResourceAsStream("/incomplete_mapping.groovy")), fileSystem.getPath("/mapping.groovy"));
        Files.copy(Objects.requireNonNull(getClass().getResourceAsStream("/time-series-sample.csv")), fileSystem.getPath("/timeseries.csv"));
        StringBuilder expected = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream("/mapping_result.txt"))))) {
            expected.append(reader.readLine());
        }

        String[] commandLine = new String[] {
            "mapping", "--case-file", "/network.xiidm",
            "--mapping-file", "/mapping.groovy",
            "--time-series", "/timeseries.csv"
        };
        assertCommand(commandLine, CommandLineTools.COMMAND_OK_STATUS, expected.toString(), "Mapping is incomplete\n");
    }

    @Test
    void runCheckVersions() throws IOException {
        Files.copy(Objects.requireNonNull(getClass().getResourceAsStream("/simple-network.xiidm")), fileSystem.getPath("/network.xiidm"));
        Files.copy(Objects.requireNonNull(getClass().getResourceAsStream("/mapping.groovy")), fileSystem.getPath("/mapping.groovy"));
        Files.copy(Objects.requireNonNull(getClass().getResourceAsStream("/time-series-sample.csv")), fileSystem.getPath("/timeseries.csv"));
        StringBuilder expected = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream("/mapping_result.txt"))))) {
            expected.append(reader.readLine());
        }

        String[] commandLine = new String[] {
            "mapping", "--case-file", "/network.xiidm",
            "--mapping-file", "/mapping.groovy",
            "--time-series", "/timeseries.csv",
            "--check-versions", "1"
        };
        assertCommand(commandLine, CommandLineTools.COMMAND_OK_STATUS, expected.toString(), StringUtils.EMPTY);
    }

    @Test
    void runCheckEquipmentTSError() throws IOException {
        Files.copy(Objects.requireNonNull(getClass().getResourceAsStream("/simple-network.xiidm")), fileSystem.getPath("/network.xiidm"));
        Files.copy(Objects.requireNonNull(getClass().getResourceAsStream("/mapping.groovy")), fileSystem.getPath("/mapping.groovy"));
        Files.copy(Objects.requireNonNull(getClass().getResourceAsStream("/time-series-sample.csv")), fileSystem.getPath("/timeseries.csv"));
        StringBuilder expected = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream("/mapping_result.txt"))))) {
            expected.append(reader.readLine());
        }

        String[] commandLine = new String[] {
            "mapping", "--case-file", "/network.xiidm",
            "--mapping-file", "/mapping.groovy",
            "--time-series", "/timeseries.csv",
            "--check-equipment-time-series"
        };
        // Command seems OK but no result + error message
        assertCommand(commandLine, CommandLineTools.COMMAND_OK_STATUS, "", "check-versions has to be set when check-equipment-time-series is set");
    }

    @Test
    void runFullOptions() throws IOException {
        Files.copy(Objects.requireNonNull(getClass().getResourceAsStream("/simple-network.xiidm")), fileSystem.getPath("/network.xiidm"));
        Files.copy(Objects.requireNonNull(getClass().getResourceAsStream("/mapping.groovy")), fileSystem.getPath("/mapping.groovy"));
        Files.copy(Objects.requireNonNull(getClass().getResourceAsStream("/time-series-sample.csv")), fileSystem.getPath("/timeseries.csv"));
        StringBuilder expected = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream("/mapping_result.txt"))))) {
            expected.append(reader.readLine());
        }

        String[] commandLine = new String[] {
            "mapping", "--case-file", "/network.xiidm",
            "--mapping-file", "/mapping.groovy",
            "--time-series", "/timeseries.csv",
            "--first-variant", "0",
            "--max-variant-count", "9999",
            "--mapping-synthesis-dir", "./temp_mapping_dir/",
            "--mapping-status-file", "./temp_mapping_file/",
            "--check-versions", "1",
            "--check-equipment-time-series",
            "--equipment-time-series-dir", "./temp_mapping_dir/",
            "--network-output-dir", "./temp_mapping_dir/"
        };
        assertCommand(commandLine, CommandLineTools.COMMAND_OK_STATUS, expected.toString(), StringUtils.EMPTY);
    }
}
