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
import com.powsybl.commons.json.JsonUtil;
import com.powsybl.timeseries.TimeSeriesException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;

public interface DistributionKey {
    void writeJson(JsonGenerator generator) throws IOException;

    static void writeJson(DistributionKey key, JsonGenerator generator) {
        try {
            generator.writeStartObject();
            key.writeJson(generator);
            generator.writeEndObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static String toJson(DistributionKey key) {
        Objects.requireNonNull(key);
        return JsonUtil.toJson(generator -> {
            try {
                generator.writeStartObject();
                key.writeJson(generator);
                generator.writeEndObject();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    static DistributionKey parseJson(String json) {
        return JsonUtil.parseJson(json, DistributionKey::parseJson);
    }

    static DistributionKey parseJson(JsonParser parser) {
        Objects.requireNonNull(parser);
        try {
            JsonToken token;
            while ((token = parser.nextToken()) != null) {
                if (token == JsonToken.START_OBJECT) {
                    // skip
                } else {
                    return parseJson(parser, token);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        throw new TimeSeriesException("Invalid distribution key JSON");
    }

    static TimeSeriesException createUnexpectedToken(JsonToken token) {
        return new TimeSeriesException("Unexpected JSON token " + token);
    }

    static DistributionKey parseJson(JsonParser parser, JsonToken token) throws IOException {
        Objects.requireNonNull(parser);
        Objects.requireNonNull(token);
        if (token == JsonToken.FIELD_NAME) {
            String fieldName = parser.getCurrentName();
            switch (fieldName) {
                case NumberDistributionKey.NAME:
                    return NumberDistributionKey.parseJson(parser);

                case TimeSeriesDistributionKey.NAME:
                    return TimeSeriesDistributionKey.parseJson(parser);

                default:
                    break;
            }
        }
        throw createUnexpectedToken(token);
    }
}
