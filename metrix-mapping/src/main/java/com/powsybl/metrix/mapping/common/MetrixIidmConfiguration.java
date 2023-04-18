/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping.common;

import com.powsybl.commons.config.ModuleConfig;
import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.iidm.xml.IidmXmlConstants;

public final class MetrixIidmConfiguration {

    private static final String IIDM_EXPORT_VERSION = IidmXmlConstants.CURRENT_IIDM_XML_VERSION.toString(".");
    private final String networkExportVersion;

    public static MetrixIidmConfiguration load() {
        return load(PlatformConfig.defaultConfig());
    }

    public static MetrixIidmConfiguration load(PlatformConfig platformConfig) {
        ModuleConfig moduleConfig = platformConfig.getOptionalModuleConfig("metrix")
                .orElseThrow(() -> new IllegalStateException("Metrix module configuration could not be found"));
        String iidmExportVersion = moduleConfig.getOptionalStringProperty("iidm-export-version")
                .orElseGet(() -> moduleConfig.getStringProperty("iidmExportVersion", IIDM_EXPORT_VERSION));
        return new MetrixIidmConfiguration(iidmExportVersion);
    }

    private MetrixIidmConfiguration(String networkExportVersion) {
        this.networkExportVersion = networkExportVersion;
    }

    public String getNetworkExportVersion() {
        return networkExportVersion;
    }
}
