/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.integration;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian@rte-france.com>
 */
public interface MetrixChunkLogger {

    void beforeNetworkWriting();

    void afterNetworkWriting();

    void beforeVariantsWriting();

    void afterVariantsWriting(int variantCount);

    void beforeMetrixExecution();

    void afterMetrixExecution();

    void beforeResultParsing();

    void afterResultParsing(int resultCount);
}
