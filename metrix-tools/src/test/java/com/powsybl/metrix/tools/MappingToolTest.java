/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.tools;

import com.powsybl.tools.AbstractToolTest;
import com.powsybl.tools.Tool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.Collections;

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
    public void assertCommand() {
        assertCommand(tool.getCommand(), "mapping", 13, 3);
        assertOption(tool.getCommand().getOptions(), "case-file", true, true);
    }

    @Test
    void run() throws IOException {
        Files.copy(MappingToolTest.class.getResourceAsStream("/simple-network.xiidm"), fileSystem.getPath("/network.xiidm"));
        Files.copy(MappingToolTest.class.getResourceAsStream("/mapping.groovy"), fileSystem.getPath("/mapping.groovy"));
        Files.copy(MappingToolTest.class.getResourceAsStream("/time-series-sample.csv"), fileSystem.getPath("/timeseries.csv"));
        StringBuilder expected = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(MappingToolTest.class.getResourceAsStream("/mapping_result.txt")))) {
            expected.append(reader.readLine());
        }

        assertCommand(new String[]{
            "mapping",
            "--case-file",
            "/network.xiidm",
            "--mapping-file",
            "/mapping.groovy",
            "--time-series",
            "/timeseries.csv"
        }, 0, expected.toString(), "Mapping is incomplete\n");
    }
}
