/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping;

import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.NetworkListener;
import com.powsybl.metrix.mapping.log.Log;
import com.powsybl.metrix.mapping.log.LogBuilder;
import com.powsybl.metrix.mapping.log.LogContent;
import com.powsybl.metrix.mapping.log.NetworkMappingBuilder;

import java.util.Collections;
import java.util.Set;

import static com.powsybl.metrix.mapping.TimeSeriesMapper.CONSTANT_VARIANT_ID;

public class NetworkTopographyChangeNotifier implements NetworkListener {

    protected boolean alreadyNotify = false;
    protected TimeSeriesMappingLogger logger;
    protected final String id;
    private static final Set<String> ATTRIBUTE_BLACK_LIST = Collections.singleton("open");

    protected enum NotificationType {
        Creation,
        Remove,
        Update
    }

    public NetworkTopographyChangeNotifier(String id, TimeSeriesMappingLogger logger) {
        this.id = id;
        this.logger = logger;
    }

    protected void sendNotification(NotificationType type, Identifiable identifiable) {
        LogBuilder logBuilder = new LogBuilder().level(System.Logger.Level.WARNING).version(0).point(CONSTANT_VARIANT_ID);
        LogContent logContent = new NetworkMappingBuilder().notificationTypeName(type.name()).id(identifiable.getNameOrId()).build();
        Log log = logBuilder.logDescription(logContent).build();
        logger.addLog(log);
        alreadyNotify = true;
    }

    @Override
    public void onCreation(Identifiable identifiable) {
        this.sendNotification(NotificationType.Creation, identifiable);
    }

    @Override
    public void beforeRemoval(Identifiable identifiable) {
        this.sendNotification(NotificationType.Remove, identifiable);
    }

    @Override
    public void afterRemoval(String s) {
        // Do nothing
    }

    @Override
    public void onUpdate(Identifiable identifiable, String attribute, String variantId, Object oldValue, Object newValue) {
        if (ATTRIBUTE_BLACK_LIST.contains(attribute)) {
            this.sendNotification(NotificationType.Update, identifiable);
        }
    }

    @Override
    public void onUpdate(Identifiable identifiable, String s, Object o, Object o1) {
        // Do nothing
    }
}
