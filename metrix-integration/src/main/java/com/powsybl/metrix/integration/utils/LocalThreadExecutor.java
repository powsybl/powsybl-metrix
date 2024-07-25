/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration.utils;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
public class LocalThreadExecutor<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(LocalThreadExecutor.class);

    private final String threadName;

    public LocalThreadExecutor(String threadName) {
        if (StringUtils.isEmpty(threadName)) {
            throw new IllegalArgumentException("threadName cannot be null");
        }
        this.threadName = threadName;
    }

    public CompletableFuture<T> supplyAsync(Supplier<T> command) {
        return CompletableFuture.supplyAsync(() -> {
            long begin = System.currentTimeMillis();
            T result = command.get();
            long elapsed = System.currentTimeMillis() - begin;
            LOGGER.info("{} execution time {} ms", threadName, elapsed);
            return result;
        }, r -> new Thread(r, threadName + "_" + Instant.now()).start());
    }

    public CompletableFuture<Void> runAsync(Runnable command) {
        return CompletableFuture.runAsync(() -> {
            long begin = System.currentTimeMillis();
            command.run();
            long elapsed = System.currentTimeMillis() - begin;
            LOGGER.info("{} execution time {} ms", threadName, elapsed);
        }, r -> new Thread(r, threadName + "_" + Instant.now()).start());
    }
}
