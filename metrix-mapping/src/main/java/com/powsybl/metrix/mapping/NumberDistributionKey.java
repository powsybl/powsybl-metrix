/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.powsybl.timeseries.TimeSeriesException;

import java.io.IOException;

public class NumberDistributionKey implements DistributionKey {

    static final String NAME = "number";

    public static final NumberDistributionKey ONE = new NumberDistributionKey(1);

    private final double value;

    @Override
    public void writeJson(JsonGenerator generator) throws IOException {
        generator.writeNumberField(NAME, value);
    }

    public NumberDistributionKey(double value) {
        this.value = value;
    }

    public double getValue() {
        return value;
    }

    static NumberDistributionKey parseJson(JsonParser parser) throws IOException {
        JsonToken token = parser.nextToken();
        if (token != null) {
            if (token == JsonToken.VALUE_NUMBER_FLOAT) {
                return new NumberDistributionKey(parser.getDoubleValue());
            } else if (token == JsonToken.VALUE_NUMBER_INT) {
                return new NumberDistributionKey(parser.getIntValue());
            } else {
                throw DistributionKey.createUnexpectedToken(token);
            }
        }
        throw new TimeSeriesException("Invalid double distribution key JSON");
    }

    @Override
    public int hashCode() {
        return Double.hashCode(value);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof NumberDistributionKey) {
            NumberDistributionKey other = (NumberDistributionKey) obj;
            return value == other.value;
        }
        return false;
    }
}
