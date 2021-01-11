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
import com.powsybl.iidm.xml.IidmXmlVersion;

/**
 * @author Paul Bui-Quang <paul.buiquang at rte-france.com>
 */
public final class MetrixIIDMConfiguration {

    private static final String IIDM_EXPORT_VERSION = IidmXmlConstants.CURRENT_IIDM_XML_VERSION.toString(".");
    private final String networkExportVersion;

    public static MetrixIIDMConfiguration load() {
        return load(PlatformConfig.defaultConfig());
    }

    public static MetrixIIDMConfiguration load(PlatformConfig platformConfig) {
        ModuleConfig moduleConfig = platformConfig.getModuleConfig("metrix");

        String iidmExportVersion = moduleConfig.getStringProperty("iidmExportVersion", IIDM_EXPORT_VERSION);
        return new MetrixIIDMConfiguration(iidmExportVersion);
    }

    private MetrixIIDMConfiguration(String networkExportVersion) {
        this.networkExportVersion = networkExportVersion;
    }

    public String getNetworkExportVersion() {
        return networkExportVersion;
    }
}
