/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping;

import com.powsybl.metrix.mapping.exception.DataTableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
class DataTableTest {

    final List<String> columnNames = List.of("columnName1", "columnName2");
    final List<String> row1 = List.of("value1", "value2");
    final List<String> row2 = List.of("value3", "value4");
    final List<String> row3 = List.of("value1", "value4");
    final List<String> column1 = List.of("value1", "value3", "value1");
    final List<String> column2 = List.of("value2", "value4", "value4");
    final List<String> columnWithName1 = List.of("columnName1:value1", "columnName1:value3", "columnName1:value1");
    final List<String> columnWithName2 = List.of("columnName2:value2", "columnName2:value4", "columnName2:value4");
    final List<String> rowWithName1 = List.of("columnName1:value1", "columnName2:value2");
    final List<String> rowWithName2 = List.of("columnName1:value3", "columnName2:value4");
    final List<String> rowWithName3 = List.of("columnName1:value1", "columnName2:value4");
    final List<List<String>> allRows = List.of(row1, row2, row3);
    final List<List<String>> allWithNameRows = List.of(rowWithName1, rowWithName2, rowWithName3);
    DataTable dataTable;

    @BeforeEach
    void setUp() {
        dataTable = DataTable.toDataTable(columnNames, allRows);
    }

    @Test
    void dataTableTest() {
        assertThat(dataTable.columnNames()).hasSize(2);
        assertTrue(dataTable.columnNames().containsAll(columnNames));
        assertTrue(dataTable.columnExists("columnName1"));
        assertTrue(dataTable.columnExists("columnName2"));
        assertFalse(dataTable.columnExists("other"));
        assertEquals(column1, dataTable.data("columnName1"));
        assertEquals(column2, dataTable.data("columnName2"));
        assertEquals(columnWithName1, dataTable.get("columnName1"));
        assertEquals(columnWithName2, dataTable.get("columnName2"));
        assertEquals(allRows, dataTable.data());
        assertEquals(allWithNameRows, dataTable.get());
    }

    @Test
    void filterTest() {
        List<List<String>> expected = List.of(List.of("value3"), List.of("value1"));
        List<List<String>> actual = dataTable.filter(List.of("columnName1"), Map.of("columnName2", List.of("value4"))).data();
        assertEquals(expected, actual);
    }

    @Test
    void searchValueListTest() {
        List<String> expected = List.of("value3", "value1");
        List<String> actual = dataTable.searchValueList("columnName1", Map.of("columnName2", List.of("value4")));
        assertEquals(expected, actual);
    }

    @Test
    void searchFirstValueTest() {
        String expected = "value3";
        String actual = dataTable.searchFirstValue("columnName1", Map.of("columnName2", List.of("value4")));
        assertEquals(expected, actual);
    }

    @Test
    void unknownColumnNamesDataExceptionTest() {
        DataTableException e = assertThrows(DataTableException.class, () -> dataTable.data("other"));
        assertTrue(e.getMessage().contains("Unknown data table column names '[other]'"));
    }

    @Test
    void unknownColumnNamesGetExceptionTest() {
        DataTableException e = assertThrows(DataTableException.class, () -> dataTable.get("other"));
        assertTrue(e.getMessage().contains("Unknown data table column names '[other]'"));
    }

    @Test
    void emptyColumnNamesExceptionTest() {
        List<String> header = Collections.emptyList();
        List<List<String>> content = allRows;
        DataTableException e = assertThrows(DataTableException.class, () -> DataTable.toDataTable(header, content));
        assertTrue(e.getMessage().contains("Empty data table column list"));
    }

    @Test
    void sameColumnNamesExceptionTest() {
        List<String> header = List.of("columnName1", "columnName1");
        List<List<String>> content = allRows;
        DataTableException e = assertThrows(DataTableException.class, () -> DataTable.toDataTable(header, content));
        assertTrue(e.getMessage().contains("Several columns with same names '[columnName1]'"));
    }

    @Test
    void sizeColumnNamesExceptionTest() {
        List<String> header = List.of("columnName1");
        List<List<String>> content = allRows;
        DataTableException e = assertThrows(DataTableException.class, () -> DataTable.toDataTable(header, content));
        assertTrue(e.getMessage().contains("Number of columns '1' different from number of values '2' at row '1'"));
    }

    @Test
    void unknownColumnNamesSearchExceptionTest() {
        Map<String, List<String>> filter = Map.of("columnName2", List.of("value4"));
        DataTableException e = assertThrows(DataTableException.class, () -> dataTable.searchFirstValue("other", filter));
        assertTrue(e.getMessage().contains("Unknown data table column names '[other]'"));
    }
}
