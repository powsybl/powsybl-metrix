/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.integration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author Paul Bui-Quang <paul.buiquang at rte-france.com>
 */
public class BatchCompletableFuture extends CompletableFuture {
    private final List<CompletableFuture> children;

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        children.forEach(child -> child.cancel(mayInterruptIfRunning));
        return super.cancel(mayInterruptIfRunning);
    }

    @Override
    public Object join() {
        List<Object> values = new ArrayList<>();
        for (CompletableFuture child : children) {
            values.add(child.join());
        }
        return values;
    }

    public BatchCompletableFuture(CompletableFuture<?>... cfs) {
        children = Arrays.asList(cfs);
    }
}
