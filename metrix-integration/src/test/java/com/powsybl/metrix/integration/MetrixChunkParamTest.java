/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration;

import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.contingency.EmptyContingencyListProvider;
import com.powsybl.metrix.integration.metrix.MetrixChunkParam;
import com.powsybl.tools.ToolRunningContext;
import org.apache.commons.cli.CommandLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */

class MetrixChunkParamTest {

    private CommandLine commandLine = mock(CommandLine.class);

    private ToolRunningContext toolRunningContext = mock(ToolRunningContext.class);

    private FileSystem fileSystem = mock(FileSystem.class);

    @BeforeEach
    public void setUp() {
        when(toolRunningContext.getFileSystem()).thenReturn(fileSystem);
    }

    private void mockPathOption(String option) {
        String optionPath = option + "-path";
        when(commandLine.getOptionValue(option)).thenReturn(optionPath);
        when(fileSystem.getPath(optionPath)).thenReturn(Path.of(optionPath));
    }

    private void mockHasOption(String option) {
        when(commandLine.hasOption(option)).thenReturn(true);
    }

    private void mockOptionalPathOption(String option) {
        mockHasOption(option);
        mockPathOption(option);
    }

    private void mockOptionalValueOption(String option, String value) {
        mockHasOption(option);
        when(commandLine.getOptionValue(option)).thenReturn(value);
    }

    private void mockCommandLineMandatoryParameters() {
        when(commandLine.getOptionValue("version")).thenReturn("1");
        mockPathOption("case-file");
        mockPathOption("input-time-series-json-file");
    }

    private void mockCommandLineOptionalParameters() {
        mockOptionalPathOption("mapping-file");
        mockOptionalPathOption("mapping-config-file");
        mockOptionalPathOption("mapping-parameters-file");
        mockOptionalPathOption("remedial-actions-file");
        mockOptionalPathOption("metrix-dsl-data-file");
        mockOptionalPathOption("metrix-dsl-file");
        mockOptionalPathOption("metrix-parameters-file");
        mockOptionalPathOption("log-file");
        mockOptionalPathOption("log-file-detail-format");
        mockOptionalPathOption("network-point-file");
        mockOptionalPathOption("output-time-series-json-file");
    }

    @Test
    void simpleInitTest() {
        //GIVEN
        ContingenciesProvider contingenciesProvider = new EmptyContingencyListProvider();
        Path networkPointFilePath = Path.of("network-point-file");
        Path logFilePath = Path.of("log-file");
        Path logFileDetailPath = Path.of("log-file-detail-format");
        Path remedialActionsFilePath = Path.of("remedial-actions-file");

        //WHEN
        MetrixChunkParam metrixChunkParam = new MetrixChunkParam.MetrixChunkParamBuilder().simpleInit(0,
                true, false, contingenciesProvider, networkPointFilePath,
                logFilePath, logFileDetailPath, remedialActionsFilePath).build();

        //THEN
        assertEquals(0, metrixChunkParam.version);
        assertTrue(metrixChunkParam.ignoreLimits);
        assertFalse(metrixChunkParam.ignoreEmptyFilter);
        assertEquals(contingenciesProvider, metrixChunkParam.contingenciesProvider);
        assertEquals(networkPointFilePath, metrixChunkParam.networkPointFile);
        assertEquals(logFilePath, metrixChunkParam.logFile);
        assertEquals(logFileDetailPath, metrixChunkParam.logFileDetail);
        assertEquals(remedialActionsFilePath, metrixChunkParam.remedialActionsFile);
    }

    @Test
    void readCommandLineDefaultParametersTest() throws IOException {
        //GIVEN
        mockCommandLineMandatoryParameters();

        //WHEN
        MetrixChunkParam metrixChunkParam = new MetrixChunkParam.MetrixChunkParamBuilder().readCommandLine(commandLine, toolRunningContext).build();

        //THEN
        assertNull(metrixChunkParam.mappingFile);
        assertNull(metrixChunkParam.mappingConfigFile);
        assertNull(metrixChunkParam.mappingParametersFile);
        assertNull(metrixChunkParam.remedialActionsFile);
        assertNull(metrixChunkParam.metrixDslDataFile);
        assertNull(metrixChunkParam.metrixDslFile);
        assertNull(metrixChunkParam.metrixParametersFile);
        assertNull(metrixChunkParam.logFile);
        assertNull(metrixChunkParam.logFileDetail);
        assertNull(metrixChunkParam.networkPointFile);
        assertNull(metrixChunkParam.outputTimeSeriesJsonFilePath);
        assertEquals(0, metrixChunkParam.firstVariant);
        assertEquals(Integer.MAX_VALUE, metrixChunkParam.variantCount);
        assertNotNull(metrixChunkParam.contingenciesProvider);
    }

    @Test
    void readCommandLineMandatoryParametersTest() throws IOException {
        //GIVEN
        mockCommandLineMandatoryParameters();

        //WHEN
        MetrixChunkParam metrixChunkParam = new MetrixChunkParam.MetrixChunkParamBuilder().readCommandLine(commandLine, toolRunningContext).build();

        //THEN
        assertEquals(1, metrixChunkParam.version);
        assertEquals(Path.of("case-file-path"), metrixChunkParam.caseFile);
        assertEquals(Path.of("input-time-series-json-file-path"), metrixChunkParam.inputTimeSeriesJsonFile);

        assertNull(metrixChunkParam.mappingFile);
    }

    @Test
    void readCommandLineOptionalParametersTest() throws IOException {
        //GIVEN
        mockCommandLineMandatoryParameters();
        mockCommandLineOptionalParameters();
        mockOptionalValueOption("first-variant", "5");
        mockOptionalValueOption("variant-count", "10");
        mockHasOption("ignore-limits");
        mockHasOption("ignore-empty-filter");
        mockHasOption("write-ptdf");
        mockHasOption("write-lodf");

        //WHEN
        MetrixChunkParam metrixChunkParam = new MetrixChunkParam.MetrixChunkParamBuilder().readCommandLine(commandLine, toolRunningContext).build();

        //THEN
        assertEquals(1, metrixChunkParam.version);
        assertEquals(Path.of("mapping-file-path"), metrixChunkParam.mappingFile);
        assertEquals(Path.of("mapping-config-file-path"), metrixChunkParam.mappingConfigFile);
        assertEquals(Path.of("mapping-parameters-file-path"), metrixChunkParam.mappingParametersFile);
        assertEquals(Path.of("remedial-actions-file-path"), metrixChunkParam.remedialActionsFile);
        assertEquals(Path.of("metrix-dsl-data-file-path"), metrixChunkParam.metrixDslDataFile);
        assertEquals(Path.of("metrix-dsl-file-path"), metrixChunkParam.metrixDslFile);
        assertEquals(Path.of("metrix-parameters-file-path"), metrixChunkParam.metrixParametersFile);
        assertEquals(Path.of("log-file-path"), metrixChunkParam.logFile);
        assertEquals(Path.of("log-file-detail-format-path"), metrixChunkParam.logFileDetail);
        assertEquals(Path.of("network-point-file-path"), metrixChunkParam.networkPointFile);
        assertEquals(Path.of("output-time-series-json-file-path"), metrixChunkParam.outputTimeSeriesJsonFilePath);
        assertEquals(5, metrixChunkParam.firstVariant);
        assertEquals(10, metrixChunkParam.variantCount);
        assertTrue(metrixChunkParam.ignoreLimits);
        assertTrue(metrixChunkParam.ignoreEmptyFilter);
        assertTrue(metrixChunkParam.writePtdfMatrix);
        assertTrue(metrixChunkParam.writeLodfMatrix);
    }
}
