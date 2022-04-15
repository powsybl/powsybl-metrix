/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.metrix.mapping.log;

import com.google.common.base.Strings;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public interface LogDescriptionBuilder {
    String LABEL_SEPARATOR = " / ";
    String IGNORE_LIMITS_DISABLED = LABEL_SEPARATOR + "IL disabled";
    String SCALING_DOWN_PROBLEM = "scaling down" + LABEL_SEPARATOR;
    String TS_SYNTHESIS = LABEL_SEPARATOR + "TS synthesis";
    int N = 1;
    DecimalFormat FORMATTER = new DecimalFormat("0." + Strings.repeat("#", N), new DecimalFormatSymbols(Locale.US));

    default String formatDouble(double value) {
        return FORMATTER.format(value);
    }
}
