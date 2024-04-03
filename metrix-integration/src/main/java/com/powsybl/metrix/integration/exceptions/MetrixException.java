/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package com.powsybl.metrix.integration.exceptions;

import java.text.MessageFormat;

public class MetrixException extends RuntimeException {

    public MetrixException(String message) {
        super(message);
    }

    public MetrixException(String pattern, Object... values) {
        super(MessageFormat.format(pattern, values));
    }

}
