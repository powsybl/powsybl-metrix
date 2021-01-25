/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.integration.exceptions;

import java.util.concurrent.ExecutionException;

/**
 * Created by funfrockmar on 03/11/20.
 */
public class MappingScriptLoadingException extends RuntimeException {

    public MappingScriptLoadingException(ExecutionException exception) {
        super(exception);
    }
}
