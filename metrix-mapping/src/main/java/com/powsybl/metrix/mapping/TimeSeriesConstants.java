/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping;

public interface TimeSeriesConstants {

    char CSV_SEPARATOR = ';';
    String GENERATORS = "Generators";
    String LOADS = "Loads";
    String BOUNDARY_LINES = "BoundaryLines";
    String HVDC_LINES = "HvdcLines";
    String PSTS = "Psts";
    String BREAKERS = "Breakers";
    String MAPPED = "Mapped";
    String UNMAPPED = "Unmapped";
    String MULTI_MAPPED = "Multi-mapped";
    String IGNORED_UNMAPPED = "Ignored unmapped";
    String DISCONNECTED = "Disconnected";
    String OUT_OF_MAIN_CC = "Out of main cc";

    String CS12 = "CS1toCS2";
    String MINUS_CS21 = "-CS2toCS1";
    String MINUS_MAXP = "-maxP";
}
