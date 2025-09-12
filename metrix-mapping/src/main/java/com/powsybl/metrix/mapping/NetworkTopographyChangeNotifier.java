/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping;

import com.powsybl.commons.extensions.Extension;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.NetworkListener;
import com.powsybl.metrix.mapping.log.Log;
import com.powsybl.metrix.mapping.log.LogBuilder;
import com.powsybl.metrix.mapping.log.LogContent;
import com.powsybl.metrix.mapping.log.NetworkMappingBuilder;

import java.util.Collections;
import java.util.Set;

import static com.powsybl.metrix.mapping.TimeSeriesMapper.CONSTANT_VARIANT_ID;

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
public class NetworkTopographyChangeNotifier implements NetworkListener {

    protected boolean alreadyNotify = false;
    protected TimeSeriesMappingLogger logger;
    protected final String id;
    private static final Set<String> ATTRIBUTE_BLACK_LIST = Collections.singleton("open");

    protected enum NotificationType {
        CREATION("Creation"),
        REMOVE("Remove"),
        UPDATE("Update");

        NotificationType(String name) {
            this.name = name;
        }

        final String getName() {
            return name;
        }

        private final String name;
    }

    public NetworkTopographyChangeNotifier(String id, TimeSeriesMappingLogger logger) {
        this.id = id;
        this.logger = logger;
    }

    protected void sendNotification(NotificationType type, Identifiable<?> identifiable) {
        LogBuilder logBuilder = new LogBuilder().level(System.Logger.Level.WARNING).version(0).point(CONSTANT_VARIANT_ID);
        LogContent logContent = new NetworkMappingBuilder().notificationTypeName(type.getName()).id(identifiable.getNameOrId()).build();
        Log log = logBuilder.logDescription(logContent).build();
        logger.addLog(log);
        alreadyNotify = true;
    }

    @Override
    public void onCreation(Identifiable<?> identifiable) {
        this.sendNotification(NotificationType.CREATION, identifiable);
    }

    @Override
    public void beforeRemoval(Identifiable<?> identifiable) {
        this.sendNotification(NotificationType.REMOVE, identifiable);
    }

    @Override
    public void afterRemoval(String s) {
        // Do nothing
    }

    @Override
    public void onUpdate(Identifiable<?> identifiable, String attribute, String variantId, Object oldValue, Object newValue) {
        if (ATTRIBUTE_BLACK_LIST.contains(attribute)) {
            this.sendNotification(NotificationType.UPDATE, identifiable);
        }
    }

    @Override
    public void onExtensionCreation(Extension<?> extension) {
        // Do nothing
    }

    @Override
    public void onExtensionAfterRemoval(Identifiable<?> identifiable, String s) {
        // Do nothing
    }

    @Override
    public void onExtensionBeforeRemoval(Extension<?> extension) {
        // Do nothing
    }

    @Override
    public void onExtensionUpdate(Extension<?> extendable, String attribute, String variantId, Object oldValue, Object newValue) {
        // Do nothing
    }

    @Override
    public void onPropertyAdded(Identifiable<?> identifiable, String key, Object newValue) {
        // Do nothing
    }

    @Override
    public void onPropertyReplaced(Identifiable<?> identifiable, String key, Object oldValue, Object newValue) {
        // Do nothing
    }

    @Override
    public void onPropertyRemoved(Identifiable<?> identifiable, String key, Object oldValue) {
        // Do nothing
    }

    @Override
    public void onVariantCreated(String sourceVariantId, String targetVariantId) {
        // Do nothing
    }

    @Override
    public void onVariantOverwritten(String sourceVariantId, String targetVariantId) {
        // Do nothing
    }

    @Override
    public void onVariantRemoved(String variantId) {
        // Do nothing
    }
}
