/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping;

public final class TimeSeriesConstants {
    private TimeSeriesConstants() {
        // utilitary class
    }

    public static final char CSV_SEPARATOR = ';';
    public static final String GENERATORS = "Generators";
    public static final String LOADS = "Loads";
    public static final String BOUNDARY_LINES = "BoundaryLines";
    public static final String HVDC_LINES = "HvdcLines";
    public static final String PSTS = "Psts";
    public static final String BREAKERS = "Breakers";
    public static final String MAPPED = "Mapped";
    public static final String UNMAPPED = "Unmapped";
    public static final String MULTI_MAPPED = "Multi-mapped";
    public static final String IGNORED_UNMAPPED = "Ignored unmapped";
    public static final String DISCONNECTED = "Disconnected";
    public static final String OUT_OF_MAIN_CC = "Out of main cc";
    public static final String CS12 = "CS1toCS2";
    public static final String MINUS_CS21 = "-CS2toCS1";
    public static final String MINUS_MAXP = "-maxP";
}
