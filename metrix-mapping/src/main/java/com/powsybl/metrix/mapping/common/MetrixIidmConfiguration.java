/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping.common;

import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.iidm.serde.IidmSerDeConstants;

/**
 * @author Paul Bui-Quang {@literal <paul.buiquang at rte-france.com>}
 */
public final class MetrixIidmConfiguration {

    private static final String DEFAULT_IIDM_EXPORT_VERSION = IidmSerDeConstants.CURRENT_IIDM_VERSION.toString(".");
    private String networkExportVersion = DEFAULT_IIDM_EXPORT_VERSION;

    public static MetrixIidmConfiguration load() {
        return load(PlatformConfig.defaultConfig());
    }

    public static MetrixIidmConfiguration load(PlatformConfig platformConfig) {
        MetrixIidmConfiguration metrixIidmConfiguration = new MetrixIidmConfiguration();
        platformConfig.getOptionalModuleConfig("metrix")
                .ifPresent(moduleConfig -> metrixIidmConfiguration
                        .setNetworkExportVersion(moduleConfig.getStringProperty("iidm-export-version", moduleConfig.getStringProperty("iidmExportVersion", DEFAULT_IIDM_EXPORT_VERSION))));
        return metrixIidmConfiguration;
    }

    private MetrixIidmConfiguration() {
    }

    public String getNetworkExportVersion() {
        return networkExportVersion;
    }

    public MetrixIidmConfiguration setNetworkExportVersion(String networkExportVersion) {
        this.networkExportVersion = networkExportVersion;
        return this;
    }
}
