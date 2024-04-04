/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.powsybl.timeseries.TimeSeriesException;

import java.io.IOException;
import java.util.Objects;

public class TimeSeriesDistributionKey implements DistributionKey {

    static final String NAME = "timeseries";

    private final String timeSeriesName;

    private int timeSeriesNum = -1;

    @Override
    public void writeJson(JsonGenerator generator) throws IOException {
        generator.writeStringField(NAME, timeSeriesName);
    }

    public TimeSeriesDistributionKey(String timeSeriesName) {
        this.timeSeriesName = Objects.requireNonNull(timeSeriesName);
    }

    public String getTimeSeriesName() {
        return timeSeriesName;
    }

    public int getTimeSeriesNum() {
        return timeSeriesNum;
    }

    public void setTimeSeriesNum(int timeSeriesNum) {
        this.timeSeriesNum = timeSeriesNum;
    }

    static TimeSeriesDistributionKey parseJson(JsonParser parser) throws IOException {
        JsonToken token = parser.nextToken();
        if (token != null) {
            if (token == JsonToken.VALUE_STRING) {
                return new TimeSeriesDistributionKey(parser.getValueAsString());
            } else {
                throw DistributionKey.createUnexpectedToken(token);
            }
        }
        throw new TimeSeriesException("Invalid time series name distribution key JSON");
    }

    @Override
    public int hashCode() {
        return timeSeriesName.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TimeSeriesDistributionKey other) {
            return timeSeriesName.equals(other.timeSeriesName);
        }
        return false;
    }
}
