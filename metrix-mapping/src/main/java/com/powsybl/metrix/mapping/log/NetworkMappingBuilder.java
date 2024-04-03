/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping.log;

public class NetworkMappingBuilder implements LogDescriptionBuilder {

    private String notificationTypeName;

    private String id;

    public NetworkMappingBuilder notificationTypeName(String notificationTypeName) {
        this.notificationTypeName = notificationTypeName;
        return this;
    }

    public NetworkMappingBuilder id(String id) {
        this.id = id;
        return this;
    }

    public LogContent build() {
        LogContent log = new LogContent();
        log.message = String.format("Network update not applied : %s of item %s",
                notificationTypeName, id);
        log.label = "network update";
        return log;
    }
}
