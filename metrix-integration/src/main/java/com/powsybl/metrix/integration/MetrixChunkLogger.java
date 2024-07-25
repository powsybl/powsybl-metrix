/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration;

import java.io.IOException;

/**
 * @author Paul Bui-Quang {@literal <paul.buiquang at rte-france.com>}
 */
public interface MetrixChunkLogger {

    static MetrixChunkLogger neverNull(MetrixChunkLogger metrixChunkLogger) {
        return metrixChunkLogger != null ? metrixChunkLogger : new MetrixChunkLogger() { };
    }

    default void beforeNetworkWriting() { }

    default void afterNetworkWriting() { }

    default void beforeVariantsWriting() { }

    default void afterVariantsWriting(int variantCount) { }

    default void beforeMetrixExecution() { }

    default void afterMetrixExecution() { }

    default void beforeResultParsing() { }

    default void afterResultParsing(int resultCount) { }

    interface RunWithIO {
        void run() throws IOException;
    }

    default void writeVariants(int variantCount, RunWithIO writeVariants) throws IOException {
        beforeVariantsWriting();
        writeVariants.run();
        afterVariantsWriting(variantCount);
    }

    default void writeNetwork(RunWithIO writeVariants) throws IOException {
        beforeNetworkWriting();
        writeVariants.run();
        afterNetworkWriting();
    }
}
