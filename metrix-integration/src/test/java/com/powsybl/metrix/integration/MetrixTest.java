/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.integration;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Paul Bui-Quang <paul.buiquang at rte-france.com>
 */
public class MetrixTest {

    private static final FileSystem JIMFS = Jimfs.newFileSystem(Configuration.unix());
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool();

    static {
        try {
            Files.createDirectory(JIMFS.getPath("/tmp"));
            Files.createDirectories(JIMFS.getPath("/tmp", "foo"));
            Files.copy(MetrixTest.class.getResourceAsStream("/empty_result.json.gz"), JIMFS.getPath("/tmp", "foo", "output_time_series_2_0.json.gz"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
