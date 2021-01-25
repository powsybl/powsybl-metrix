/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.integration;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.commons.config.InMemoryPlatformConfig;
import com.powsybl.commons.config.MapModuleConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.FileSystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian@rte-france.com>
 */
public class MetrixConfigTest {

    private FileSystem fileSystem;

    private InMemoryPlatformConfig platformConfig;

    @Before
    public void setUp() {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        platformConfig = new InMemoryPlatformConfig(fileSystem);
    }

    @After
    public void tearDown() throws IOException {
        fileSystem.close();
    }

    @Test
    public void test() {
        MapModuleConfig config = platformConfig.createModuleConfig("metrix");
        config.setStringProperty("home-dir", "/home");
        config.setStringProperty("debug", "true");
        config.setStringProperty("constant-loss-factor", "true");
        config.setStringProperty("chunkSize", "333");
        config.setStringProperty("resultLimit", "20000");
        MetrixConfig metrixConfig = MetrixConfig.load(platformConfig);
        assertEquals(fileSystem.getPath("/home"), metrixConfig.getHomeDir());
        assertTrue(metrixConfig.isDebug());
        assertTrue(metrixConfig.isConstantLossFactor());
        assertEquals(333, metrixConfig.getChunkSize());
        assertEquals(20000, metrixConfig.getResultNumberLimit());
    }

    @Test
    public void snakeCaseTest() {
        MapModuleConfig config = platformConfig.createModuleConfig("metrix");
        config.setStringProperty("home-dir", "/home");
        config.setStringProperty("debug", "true");
        config.setStringProperty("constant-loss-factor", "true");
        config.setStringProperty("chunk-size", "333");
        config.setStringProperty("result-limit", "20000");
        MetrixConfig metrixConfig = MetrixConfig.load(platformConfig);
        assertTrue(metrixConfig.isConstantLossFactor());
        assertEquals(333, metrixConfig.getChunkSize());
        assertEquals(20000, metrixConfig.getResultNumberLimit());
    }
}
