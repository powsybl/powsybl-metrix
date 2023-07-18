/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping;

import com.powsybl.commons.TestUtil;
import com.powsybl.iidm.network.Network;
import com.powsybl.metrix.mapping.exception.DataTableException;
import com.powsybl.timeseries.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DataTableDslLoaderTest {

    private final MappingParameters parameters = MappingParameters.load();
    private final Network network = MappingTestNetwork.create();
    private final ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache();
    private final DataTable dataTable = DataTable.toDataTable(List.of("columnName"), List.of(List.of("value")));
    private final DataTableStore dataTableStore = new DataTableStore();

    @BeforeEach
    public void setUp() {
        dataTableStore.addTable("tableName", dataTable);
    }

    @Test
    void dataTableDslLoaderTest() throws IOException {
        DataTableStore dataTableStore = new DataTableStore();
        dataTableStore.addTable("tableName", dataTable);

        // mapping script
        String script = String.join(System.lineSeparator(),
                "exists = dataTable.exists('tableName')",
                "notExists = dataTable.exists('other')",
                "names = dt.names()",
                "newTable = toDataTable(['newColumnName'], [['newValue']])",
                "dataTable['tableName']",
                "println exists",
                "println notExists",
                "println names",
                "println newTable.data()"
        );

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (Writer out = new BufferedWriter(new OutputStreamWriter(outputStream))) {
            new TimeSeriesDslLoader(script).load(network, parameters, store, dataTableStore, out, null);
        }

        String output = TestUtil.normalizeLineSeparator(outputStream.toString());
        assertEquals("true\nfalse\n[tableName]\n[[newValue]]\n", output);
    }

    @Test
    void notFoundExceptionTest() {
        // mapping script
        String script = String.join(System.lineSeparator(),
                "dataTable['other']"
        );
        TimeSeriesDslLoader timeSeriesDslLoader = new TimeSeriesDslLoader(script);
        DataTableException e = assertThrows(DataTableException.class, () -> timeSeriesDslLoader.load(network, parameters, store, dataTableStore, null));
        assertTrue(e.getMessage().contains("Data table 'other' not found"));
    }

    @Test
    void sameNameExceptionTest() {
        DataTableException e = assertThrows(DataTableException.class, () -> dataTableStore.addTable("tableName", dataTable));
        assertTrue(e.getMessage().contains("A data table with the name 'tableName' is already loaded"));
    }
}
