/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping;

import com.powsybl.metrix.mapping.exception.DataTableException;

import java.util.*;

public class DataTableStore {

    private final Map<String, DataTable> tables = new HashMap<>();

    public DataTableStore() {
    }

    public Set<String> names() {
        return tables.keySet();
    }

    public boolean exists(String dataTableName) {
        return names().contains(dataTableName);
    }

    public void addTable(String name, DataTable table) {
        if (tables.putIfAbsent(name, table) != null) {
            throw new DataTableException(String.format("A data table with the name '%s' is already loaded", name));
        }
    }

    public DataTable get(String name) {
        return tables.get(name);
    }
}
