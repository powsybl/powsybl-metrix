/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package com.powsybl.metrix.integration.io;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.metrix.integration.AbstractCompareTxt;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

class MetrixDieTest extends AbstractCompareTxt {
    private FileSystem fileSystem;

    @BeforeEach
    public void setUp() {
        this.fileSystem = Jimfs.newFileSystem(Configuration.unix());
    }

    @AfterEach
    public void tearDown() throws IOException {
        this.fileSystem.close();
    }

    @Test
    void testInvalidAttrSize() {
        MetrixDie die = new MetrixDie();
        try {
            die.setIntArray("AAA", new int[]{1});
            fail();
        } catch (Exception ignored) {
        }
    }

    @Test
    void jsonLoadSaveTest() throws IOException, URISyntaxException {
        MetrixDie die = new MetrixDie();
        Path inputFile = Paths.get(getClass().getResource("/simpleNetwork.json").toURI());
        die.loadFromJson(inputFile);
        Path outputFile = fileSystem.getPath("output.json");
        die.saveToJson(outputFile);
        assertNotNull(compareStreamTxt(Files.newInputStream(inputFile), Files.newInputStream(outputFile)));
    }
}
