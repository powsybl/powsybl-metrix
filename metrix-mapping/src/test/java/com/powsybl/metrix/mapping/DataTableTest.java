package com.powsybl.metrix.mapping;

import com.powsybl.metrix.mapping.exception.DataTableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class DataTableTest {

    final List<String> columnNames = List.of("columnName1", "columnName2");
    final List<String> row1 = List.of("value1", "value2");
    final List<String> row2 = List.of("value3", "value4");
    final List<String> row3 = List.of("value1", "value4");
    final List<String> rowWithName1 = List.of("columnName1:value1", "columnName2:value2");
    final List<String> rowWithName2 = List.of("columnName1:value3", "columnName2:value4");
    final List<String> rowWithName3 = List.of("columnName1:value1", "columnName2:value4");
    DataTable dataTable;

    @BeforeEach
    public void setUp() {
        dataTable = DataTable.toDataTable(columnNames, List.of(row1, row2, row3));
    }

    @Test
    void dataTableTest() {
        assertThat(dataTable.columnNames()).hasSize(2);
        assertTrue(dataTable.columnNames().containsAll(columnNames));
        assertTrue(dataTable.columnExists("columnName1"));
        assertTrue(dataTable.columnExists("columnName2"));
        assertFalse(dataTable.columnExists("other"));
        assertEquals(List.of("value1", "value3", "value1"), dataTable.data("columnName1"));
        assertEquals(List.of("value2", "value4", "value4"), dataTable.data("columnName2"));
        assertEquals(List.of("columnName1:value1", "columnName1:value3", "columnName1:value1"), dataTable.get("columnName1"));
        assertEquals(List.of("columnName2:value2", "columnName2:value4", "columnName2:value4"), dataTable.get("columnName2"));
        assertEquals(List.of(row1, row2, row3), dataTable.data());
        assertEquals(List.of(rowWithName1, rowWithName2, rowWithName3), dataTable.get());
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
    void checkTest() {
        assertThrows(DataTableException.class, () -> dataTable.data("other"));
        assertThrows(DataTableException.class, () -> dataTable.get("other"));
        assertThrows(DataTableException.class, () -> DataTable.toDataTable(Collections.emptyList(), List.of(row1, row2, row3)));
        assertThrows(DataTableException.class, () -> DataTable.toDataTable(List.of("columnName1", "columnName1"), List.of(row1, row2, row3)));
        assertThrows(DataTableException.class, () -> DataTable.toDataTable(List.of("columnName1"), List.of(row1, row2, row3)));
        assertThrows(DataTableException.class, () -> dataTable.searchFirstValue("other", Map.of("columnName2", List.of("value4"))));
    }
}
