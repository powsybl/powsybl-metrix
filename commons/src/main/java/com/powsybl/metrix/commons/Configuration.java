/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.commons;

import com.powsybl.commons.config.ModuleConfig;
import com.powsybl.commons.config.PlatformConfig;

/**
 * @author Paul Bui-Quang <paul.buiquang at rte-france.com>
 */
public final class Configuration {

    private static final String IIDM_EXPORT_VERSION = "1.0";
    private final String networkExportVersion;

    public static Configuration load() {
        return load(PlatformConfig.defaultConfig());
    }

    public static Configuration load(PlatformConfig platformConfig) {
        ModuleConfig moduleConfig = platformConfig.getModuleConfig("metrix");

        String iidmExportVersion = moduleConfig.getStringProperty("iidmExportVersion", IIDM_EXPORT_VERSION);
        return new Configuration(iidmExportVersion);
    }

    private Configuration(String networkExportVersion) {
        this.networkExportVersion = networkExportVersion;
    }

    public String getNetworkExportVersion() {
        return networkExportVersion;
    }
}
