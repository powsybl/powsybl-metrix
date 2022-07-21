/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping;

import com.google.common.io.ByteStreams;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class AbstractCompareTxt {

    public static Object compareStreamTxt(byte[] stream, String directoryName, String fileName) throws Exception {
        try (InputStream expected = AbstractCompareTxt.class.getResourceAsStream(directoryName + fileName)) {
            try (InputStream actual = new ByteArrayInputStream(stream)) {
                return compareStreamTxt(expected, actual);
            }
        }
    }

    public static Object compareStreamTxt(InputStream expected, InputStream actual) {
        try {
            return compareStreamTxt(expected, new String(ByteStreams.toByteArray(actual), StandardCharsets.UTF_8));
        } catch (IOException var3) {
            throw new UncheckedIOException(var3);
        }
    }

    public static Object compareStreamTxt(InputStream expected, String actual) {
        try {
            String expectedStr = normalizeLineSeparator(new String(ByteStreams.toByteArray(expected), StandardCharsets.UTF_8));
            String actualStr = normalizeLineSeparator(actual);
            assertEquals(expectedStr, actualStr);
            return "";
        } catch (IOException var4) {
            throw new UncheckedIOException(var4);
        }
    }

    private static String normalizeLineSeparator(String str) {
        return str.replace("\r\n", "\n").replace("\r", "\n");
    }
}
