/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration.analysis;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.contingency.EmptyContingencyListProvider;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.serde.NetworkSerDe;
import com.powsybl.metrix.integration.MetrixAppLogger;
import com.powsybl.metrix.data.datatable.DataTableStore;
import com.powsybl.metrix.integration.network.NetworkSource;
import com.powsybl.metrix.mapping.TimeSeriesDslLoader;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStoreCache;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
class MetrixAnalysisTest {
    private FileSystem fileSystem;
    private Network network;
    private NetworkSource networkSource;
    private MetrixAppLogger appLogger;
    private ByteArrayOutputStream appLoggerOutputStream;

    private MetrixAnalysis metrixAnalysis() {
        return metrixAnalysis("", "");
    }

    private MetrixAnalysis metrixAnalysis(String mappingScript, String remedialScript) {
        return new MetrixAnalysis(
                networkSource,
                new TimeSeriesDslLoader(mappingScript),
                null,
                new StringReader(remedialScript),
                new EmptyContingencyListProvider(),
                new ReadOnlyTimeSeriesStoreCache(),
                new DataTableStore(),
                appLogger,
                null);
    }

    @BeforeEach
    void setUp() {
        this.fileSystem = Jimfs.newFileSystem(Configuration.unix());
        this.network = NetworkSerDe.read(Objects.requireNonNull(getClass().getResourceAsStream("/simpleNetwork.xml")));
        this.networkSource = new NetworkSource() {
            @Override
            public Network copy() {
                return network;
            }

            @Override
            public void write(OutputStream os) {
                // Nothing to do here
            }
        };
        appLoggerOutputStream = new ByteArrayOutputStream();
        appLogger = new MetrixAppLogger() {
            @Override
            public void log(String message, Object... args) {
                try {
                    IOUtils.write(String.format("INFO" + "\t" + message + "\n", args), appLoggerOutputStream, Charset.defaultCharset());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public MetrixAppLogger tagged(String tag) {
                return this;
            }
        };
    }

    @AfterEach
    void tearDown() throws IOException {
        this.fileSystem.close();
    }

    @Test
    void updateTaskTest() {
        AtomicBoolean updated = new AtomicBoolean(false);
        Consumer<Future<?>> taskUpdater = future -> updated.set(true);
        MetrixAnalysis metrixAnalysis = metrixAnalysis();
        metrixAnalysis.setUpdateTask(taskUpdater);
        metrixAnalysis.runAnalysis("");
        assertTrue(updated.get());
    }

    @Test
    void scriptLogWriterTest() throws IOException {
        String script = "println(\"log\")";
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (Writer out = new BufferedWriter(new OutputStreamWriter(outputStream))) {
            MetrixAnalysis metrixAnalysis = metrixAnalysis(script, "");
            metrixAnalysis.setScriptLogWriter(out);
            metrixAnalysis.runAnalysis("");
            String output = outputStream.toString();
            assertFalse(output.isEmpty());
        }
    }

    @Test
    void inputLogWriterTest() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (Writer out = new BufferedWriter(new OutputStreamWriter(outputStream))) {
            MetrixAnalysis metrixAnalysis = metrixAnalysis("", "WRONG");
            metrixAnalysis.setInputLogWriter(out);
            metrixAnalysis.runAnalysis("");
            String output = outputStream.toString();
            assertFalse(output.isEmpty());
        }
    }

    @Test
    void schemaNameTest() {
        MetrixAnalysis metrixAnalysis = metrixAnalysis();
        metrixAnalysis.setSchemaName("schemaName");
        metrixAnalysis.runAnalysis("");
        String output = appLoggerOutputStream.toString();
        assertTrue(output.contains("schemaName"));
    }
}
