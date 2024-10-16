/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.commons.config.InMemoryPlatformConfig;
import com.powsybl.commons.config.MapModuleConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Paul Bui-Quang {@literal <paul.buiquang at rte-france.com>}
 */
class MetrixConfigTest {

    private FileSystem fileSystem;

    private InMemoryPlatformConfig platformConfig;

    @BeforeEach
    public void setUp() {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        platformConfig = new InMemoryPlatformConfig(fileSystem);
    }

    @AfterEach
    public void tearDown() throws IOException {
        fileSystem.close();
    }

    @Test
    void test() {
        MapModuleConfig config = platformConfig.createModuleConfig("metrix");
        config.setStringProperty("home-dir", "/home");
        config.setStringProperty("command", "metrix-simulator");
        config.setStringProperty("debug", "true");
        config.setStringProperty("constant-loss-factor", "true");
        config.setStringProperty("chunkSize", "333");
        config.setStringProperty("resultLimit", "20000");
        MetrixConfig metrixConfig = MetrixConfig.load(platformConfig);
        assertEquals(fileSystem.getPath("/home"), metrixConfig.getHomeDir());
        assertEquals("metrix-simulator", metrixConfig.getCommand());
        assertTrue(metrixConfig.isDebug());
        assertTrue(metrixConfig.isConstantLossFactor());
        assertEquals(333, metrixConfig.getChunkSize());
        assertEquals(20000, metrixConfig.getResultNumberLimit());
    }

    @Test
    void kebabCaseTest() {
        MapModuleConfig config = platformConfig.createModuleConfig("metrix");
        config.setStringProperty("home-dir", "/home");
        config.setStringProperty("command", "metrix-simulator");
        config.setStringProperty("debug", "true");
        config.setStringProperty("constant-loss-factor", "true");
        config.setStringProperty("chunk-size", "333");
        config.setStringProperty("result-limit", "20000");
        MetrixConfig metrixConfig = MetrixConfig.load(platformConfig);
        assertTrue(metrixConfig.isConstantLossFactor());
        assertEquals(333, metrixConfig.getChunkSize());
        assertEquals(20000, metrixConfig.getResultNumberLimit());
    }

    @Test
    void testDefaultConfig() {
        MetrixConfig metrixConfig = MetrixConfig.load(platformConfig);
        assertEquals(Paths.get(System.getProperty("user.home")).resolve(".local/opt/powsybl-metrix").toString(), metrixConfig.getHomeDir().toString());
        assertEquals("metrix-simulator", metrixConfig.getCommand());
        assertFalse(metrixConfig.isDebug());
        assertFalse(metrixConfig.isConstantLossFactor());
        assertEquals(10, metrixConfig.getChunkSize());
        assertEquals(10000, metrixConfig.getResultNumberLimit());
        assertEquals(0, metrixConfig.getDebugLogLevel());
        assertEquals(2, metrixConfig.getLogLevel());
    }
}
