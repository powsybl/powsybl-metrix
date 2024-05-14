/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping

import com.powsybl.metrix.mapping.exception.DataTableException

/**
 * @author marifunf {@literal <marianne.funfrock at rte-france.com>}
 */
class DataTableDslLoader {

    static class DataTableGroovyObject {

        private final DataTableStore store

        private DataTableGroovyObject(DataTableStore store) {
            assert store != null
            this.store = store
        }

        DataTable getAt(String name) {
            assert name != null
            if (exists(name)) {
                return store.get(name)
            }
            throw new DataTableException("Data table '" + name + "' not found")
        }

        Set<String> names() {
            return store.names()
        }

        boolean exists(String dataTableName) {
            return store.exists(dataTableName)
        }
    }

    static void bind(Binding binding, DataTableStore store) {
        def dt = new DataTableGroovyObject(store)
        binding.dataTable = dt
        binding.dt = dt
        binding.toDataTable = { List<String> columnNames, List<List<String>> values ->
            DataTable::toDataTable(columnNames, values)
        }
    }
}
