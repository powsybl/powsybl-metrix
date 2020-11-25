/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.commons;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

/**
 * @author Paul Bui-Quang <paul.buiquang at rte-france.com>
 */
public final class Utils {

    private Utils() throws IllegalAccessException {
        throw new IllegalAccessException("Cannot instanciate utility class");
    }

    public static void deleteRecursive(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            for (Path child : Files.list(path).collect(Collectors.toList())) {
                deleteRecursive(child);
            }
        }
        Files.delete(path);
    }
}
