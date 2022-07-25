/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.integration.io;

import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.io.LittleEndianDataInputStream;
import com.google.common.io.LittleEndianDataOutputStream;

import java.io.IOException;
import java.io.PrintStream;

interface Attribute {

    String getName();

    AttributeType getType();

    void read(LittleEndianDataInputStream is, int i, int j) throws IOException;

    void write(LittleEndianDataOutputStream os) throws IOException;

    int getFirstIndexMaxValue();

    int getSecondIndexMaxValue();

    int getValueCount();

    int getSize();

    void print(PrintStream out);

    void writeJson(JsonGenerator generator) throws IOException;
}
