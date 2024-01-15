/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.tools;

import com.powsybl.tools.Command;
import com.powsybl.tools.Tool;
import org.apache.commons.cli.Options;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Objects;

import static com.powsybl.tools.CommandLineTools.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MetrixDieToolTest extends AbstractToolTest {

    private MetrixDieTool tool;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        tool = new MetrixDieTool();
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
        assertCommand(command, "metrix-die", 5, 1);
        assertOption(options, "case-file", true, true);
        assertOption(options, "contingencies-file", false, true);
        assertOption(options, "metrix-dsl-file", false, true);
        assertOption(options, "remedial-actions-file", false, true);
        assertOption(options, "output-dir", false, true);
        assertEquals("Metrix", command.getTheme());
        assertEquals("Generate Metrix DIE files", command.getDescription());
        assertNull(command.getUsageFooter());
    }

    @Test
    void run() throws IOException {
        Files.copy(Objects.requireNonNull(MetrixDieToolTest.class.getResourceAsStream("/simple-network.xiidm")), fileSystem.getPath("/network.xiidm"));
        StringBuilder expected = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(MetrixDieToolTest.class.getResourceAsStream("/mapping_result.txt")))) {
            expected.append(reader.readLine());
        }

        String[] commandLine = new String[] {
            "metrix-die", "--case-file", "/network.xiidm"
        };
        assertCommand(commandLine, COMMAND_OK_STATUS, expected.toString(), "");
    }
}
