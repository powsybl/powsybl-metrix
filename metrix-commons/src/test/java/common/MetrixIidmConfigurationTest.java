/*
 * Copyright (c) 2024, Artelys S.A.S (https://www.artelys.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package common;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.commons.config.InMemoryPlatformConfig;
import com.powsybl.commons.config.MapModuleConfig;
import com.powsybl.iidm.serde.IidmSerDeConstants;
import com.powsybl.metrix.commons.config.MetrixIidmConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.FileSystem;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Damien Jeandemange {@literal <damien.jeandemange at artelys.com>}
 */
class MetrixIidmConfigurationTest {

    private FileSystem fileSystem;

    private InMemoryPlatformConfig platformConfig;

    @BeforeEach
    void setUp() {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        platformConfig = new InMemoryPlatformConfig(fileSystem);
    }

    @AfterEach
    void tearDown() throws IOException {
        fileSystem.close();
    }

    @Test
    void defaultConfigTest() {
        MetrixIidmConfiguration metrixIidmConfiguration = MetrixIidmConfiguration.load(platformConfig);
        assertEquals(IidmSerDeConstants.CURRENT_IIDM_VERSION.toString("."), metrixIidmConfiguration.getNetworkExportVersion());
    }

    @Test
    void camelCaseTest() {
        MapModuleConfig config = platformConfig.createModuleConfig("metrix");
        config.setStringProperty("iidmExportVersion", "1.4");
        MetrixIidmConfiguration metrixIidmConfiguration = MetrixIidmConfiguration.load(platformConfig);
        assertEquals("1.4", metrixIidmConfiguration.getNetworkExportVersion());
    }

    @Test
    void kebabCaseTest() {
        MapModuleConfig config = platformConfig.createModuleConfig("metrix");
        config.setStringProperty("iidm-export-version", "1.5");
        MetrixIidmConfiguration metrixIidmConfiguration = MetrixIidmConfiguration.load(platformConfig);
        assertEquals("1.5", metrixIidmConfiguration.getNetworkExportVersion());
    }
}
