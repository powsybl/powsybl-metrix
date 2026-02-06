/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.commons.data.datatable;

import com.powsybl.metrix.commons.exception.DataTableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.powsybl.metrix.commons.data.datatable.DataTable.toDataTable;
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
        dataTable = toDataTable(columnNames, allRows);
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
        assertEquals(Map.of("columnName1", "value1", "columnName2", "value2"), dataTable.getLines().get(0));
        assertEquals(Map.of("columnName1", "value3", "columnName2", "value4"), dataTable.getLines().get(1));
        assertEquals(Map.of("columnName1", "value1", "columnName2", "value4"), dataTable.getLines().get(2));
    }

    @Test
    void emptyDataTableTest() {
        // GIVEN
        List<String> header = Collections.emptyList();
        List<List<String>> content = Collections.emptyList();
        // WHEN
        DataTable actualEmptyDataTable = toDataTable(header, content);
        // THEN
        assertThat(actualEmptyDataTable).isNotNull();
        assertThat(actualEmptyDataTable.columnNames()).isEmpty();
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
    void searchValueListEqualsTest() {
        List<String> actual = dataTable.searchValueList("columnName1", Map.of("columnName2", List.of("value")));
        assertTrue(actual.isEmpty());
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
    void sameColumnNamesExceptionTest() {
        List<String> header = List.of("columnName1", "columnName1");
        List<List<String>> content = allRows;
        DataTableException e = assertThrows(DataTableException.class, () -> toDataTable(header, content));
        assertTrue(e.getMessage().contains("Several columns with same names '[columnName1]'"));
    }

    @Test
    void sizeColumnNamesExceptionTest() {
        List<String> header = List.of("columnName1");
        List<List<String>> content = allRows;
        DataTableException e = assertThrows(DataTableException.class, () -> toDataTable(header, content));
        assertTrue(e.getMessage().contains("Number of columns '1' different from number of values '2' at row '1'"));
    }

    @Test
    void unknownColumnNamesSearchExceptionTest() {
        Map<String, List<String>> filter = Map.of("columnName2", List.of("value4"));
        DataTableException e = assertThrows(DataTableException.class, () -> dataTable.searchFirstValue("other", filter));
        assertTrue(e.getMessage().contains("Unknown data table column names '[other]'"));
    }

    @Test
    void removeLinesTest() {
        // GIVEN
        // WHEN
        dataTable.removeLines(Map.of("columnName2", List.of("value2")));
        // THEN
        List<List<String>> expected = List.of(List.of("value3", "value4"), List.of("value1", "value4"));
        assertEquals(expected, dataTable.data());
    }

    @Test
    void removeLinesNoMatchTest() {
        // GIVEN
        // WHEN
        dataTable.removeLines(Map.of("columnName2", List.of("noMatch"))).data();
        // THEN
        List<List<String>> expected = List.of(row1, row2, row3);
        assertEquals(expected, dataTable.data());
    }

    @Test
    void removeMultipleLinesTest() {
        // GIVEN
        // WHEN
        dataTable.removeLines(Map.of("columnName2", List.of("value4")));
        // THEN
        List<List<String>> expected = List.of(List.of("value1", "value2"));
        assertEquals(expected, dataTable.data());
    }

    @Test
    void removeAllLinesTest() {
        // GIVEN
        DataTable otherDataTable = toDataTable(List.of("column"), List.of(List.of("value")));
        // WHEN
        otherDataTable.removeLines(Map.of("column", List.of("value")));
        // THEN
        assertTrue(otherDataTable.data().isEmpty());
    }

    @Test
    void addLinesTest() {
        // GIVEN
        List<String> header = List.of("columnName1", "columnName2");
        List<String> row4 = List.of("value5", "value6");
        List<List<String>> content = List.of(row4);
        // WHEN
        dataTable.addLines(header, content);
        // THEN
        List<List<String>> expected = List.of(row1, row2, row3, row4);
        assertEquals(expected, dataTable.data());
    }

    @Test
    void addLinesMultipleTest() {
        // GIVEN
        List<String> header = List.of("columnName1", "columnName2");
        List<String> row4 = List.of("value5", "value6");
        List<String> row5 = List.of("value7", "value8");
        List<List<String>> content = List.of(row4, row5);
        // WHEN
        dataTable.addLines(header, content);
        // THEN
        List<List<String>> expected = List.of(row1, row2, row3, row4, row5);
        assertEquals(expected, dataTable.data());
    }

    @Test
    void addLinesOtherOrderTest() {
        // GIVEN
        List<String> header = List.of("columnName2", "columnName1");
        List<String> row4 = List.of("value6", "value5");
        List<List<String>> content = List.of(row4);
        // WHEN
        dataTable.addLines(header, content);
        // THEN
        List<List<String>> expected = List.of(row1, row2, row3, row4);
        assertEquals(expected, dataTable.data());
    }

    @Test
    void addLinesHeaderExceptionTest() {
        // GIVEN
        List<String> header = List.of("columnName1");
        List<String> row4 = List.of("value5");
        List<List<String>> content = List.of(row4);
        // WHEN
        DataTableException e = assertThrows(DataTableException.class, () -> dataTable.addLines(header, content));
        // THEN
        assertTrue(e.getMessage().contains("Headers are different: existing header=[columnName1, columnName2] header to add=[columnName1]"));
    }

    @Test
    void replaceValuesTest() {
        // GIVEN
        List<String> selectedColumns = List.of("columnName2");
        Map<String, List<String>> filter = Map.of("columnName2", List.of("value4"));
        List<String> values = List.of("value5");
        // WHEN
        dataTable.replaceValues(selectedColumns, filter, values);
        // THEN
        List<List<String>> expected = List.of(row1, List.of("value3", "value5"), List.of("value1", "value5"));
        assertEquals(expected, dataTable.data());
    }

    @Test
    void replaceValuesNoMatchTest() {
        // GIVEN
        List<String> selectedColumns = List.of("columnName2");
        Map<String, List<String>> filter = Map.of("columnName2", List.of("noMatch"));
        List<String> values = List.of("value5");
        // WHEN
        dataTable.replaceValues(selectedColumns, filter, values);
        // THEN
        List<List<String>> expected = List.of(row1, row2, row3);
        assertEquals(expected, dataTable.data());
    }

    @Test
    void replaceValuesMultipleColumnsTest() {
        // GIVEN
        List<String> selectedColumns = List.of("columnName1", "columnName2");
        Map<String, List<String>> filter = Map.of("columnName2", List.of("value4"));
        List<String> values = List.of("value5", "value6");
        // WHEN
        dataTable.replaceValues(selectedColumns, filter, values);
        // THEN
        List<List<String>> expected = List.of(row1, List.of("value5", "value6"), List.of("value5", "value6"));
        assertEquals(expected, dataTable.data());
    }

    @Test
    void replaceValuesSizeExceptionTest() {
        // GIVEN
        List<String> selectedColumns = List.of("columnName1", "columnName2");
        Map<String, List<String>> filter = Map.of("columnName2", List.of("value4"));
        List<String> values = List.of("value5");
        // WHEN
        DataTableException e = assertThrows(DataTableException.class, () -> dataTable.replaceValues(selectedColumns, filter, values));
        // THEN
        assertTrue(e.getMessage().contains("Number of selected columns '2' different from number of values '1'"));
    }
}
