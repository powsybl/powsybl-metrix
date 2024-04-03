/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package com.powsybl.metrix.mapping;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.powsybl.metrix.mapping.exception.DataTableException;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

public class DataTable {
    @JsonProperty("tabColumns")
    private List<String> tabColumns;
    @JsonProperty("tabValues")
    private List<AttributeNameToValue> tabValues;

    @JsonCreator
    public DataTable(List<String> tabColumns, List<AttributeNameToValue> tabValues) {
        this.tabColumns = new ArrayList<>(tabColumns);
        this.tabValues = new ArrayList<>(tabValues);
    }

    public DataTable() {
        this.tabColumns = new ArrayList<>();
        this.tabValues = new ArrayList<>();
    }

    public List<String> columnNames() {
        return tabColumns;
    }

    public boolean columnExists(String columnName) {
        return tabColumns.contains(columnName);
    }

    public List<List<String>> data() {
        return values(false);
    }

    public List<List<String>> get() {
        return values(true);
    }

    public List<String> data(String columnName) {
        return values(columnName, false);
    }

    public List<String> get(String columnName) {
        return values(columnName, true);
    }

    private void addAttributeNameToValue(AttributeNameToValue attributeNameToValue) {
        addValues(attributeNameToValue);
        addColumns(attributeNameToValue);
    }

    private void addValues(AttributeNameToValue attributeNameToValue) {
        this.tabValues.add(attributeNameToValue);
    }

    private void addColumns(AttributeNameToValue attributeNameToValue) {
        this.tabColumns.addAll(attributeNameToValue.getAttributeNames().stream().filter(colName -> !tabColumns.contains(colName)).distinct().toList());
    }

    private String getValue(String name, boolean isWithName) {
        return isWithName ? name + ":" : StringUtils.EMPTY;
    }

    public List<String> values(String columnName, boolean isWithName) {
        checkColumnNames(columnName);
        return tabValues.stream().map(attributeNameToValue -> getValue(columnName, isWithName) + attributeNameToValue.getValue(columnName))
            .toList();
    }

    private List<List<String>> values(boolean isWithName) {
        return tabValues.stream().map(values ->
                values.getAttributeNames().stream()
                    .map(attributeName -> getValue(attributeName, isWithName) + values.getValue(attributeName))
                    .toList())
            .toList();
    }

    public static DataTable toDataTable(List<String> header, List<List<String>> content) {
        checkData(header, content);
        DataTable dataTable = new DataTable();
        content.forEach(line -> {
            AttributeNameToValue attributeNameToValue = new AttributeNameToValue();
            for (int column = 0; column < header.size(); column++) {
                attributeNameToValue.put(header.get(column), line.get(column));
            }
            dataTable.addAttributeNameToValue(attributeNameToValue);
        });
        return dataTable;
    }

    private static void checkNbValues(int nbColumns, List<String> values, int row) {
        if (nbColumns != values.size()) {
            throw new DataTableException(String.format("Number of columns '%s' different from number of values '%s' at row '%s'", nbColumns, values.size(), row + 1));
        }
    }

    private static void checkData(List<String> header, List<List<String>> content) {
        checkEmptyColumnNames(header);
        checkDuplicateColumnNames(header);
        int nbColumns = header.size();
        content.forEach(values -> checkNbValues(nbColumns, values, content.indexOf(values)));
    }

    private void removeNotSelectedColumns(List<String> selectedColumns) {
        this.tabValues = this.tabValues.stream()
            .map(attributeNameToValue -> attributeNameToValue.filterSelectedColumns(selectedColumns))
            .toList();
    }

    private void filterOnParameter(String parameter, List<String> tabValueToFilter) {
        this.tabValues = this.tabValues.stream()
            .filter(attributeNameToValue -> tabValueToFilter.stream().anyMatch(filterValue -> StringUtils.containsIgnoreCase(attributeNameToValue.getValue(parameter), filterValue)))
            .toList();
    }

    private void filterInfos(QueryFilter filter) {
        filter.getContentFilters().forEach(contentFilter -> filterOnParameter(contentFilter.getColumnName(), contentFilter.getValues()));
        removeNotSelectedColumns(filter.getSelectedColumns());
        tabColumns = new ArrayList<>(filter.getSelectedColumns());
    }

    private static void checkEmptyColumnNames(List<String> columns) {
        if (columns.isEmpty()) {
            throw new DataTableException("Empty data table column list");
        }
    }

    private static void checkDuplicateColumnNames(List<String> columns) {
        Set<String> duplicateColumnNames = columns.stream().distinct()
            .filter(i -> Collections.frequency(columns, i) > 1)
            .collect(Collectors.toSet());
        if (!duplicateColumnNames.isEmpty()) {
            throw new DataTableException(String.format("Several columns with same names '%s'", duplicateColumnNames));
        }
    }

    private void checkColumnNames(List<String> columns) {
        checkEmptyColumnNames(columns);
        Set<String> unknownColumns = new HashSet<>(columns);
        columnNames().forEach(unknownColumns::remove);
        if (!unknownColumns.isEmpty()) {
            throw new DataTableException(String.format("Unknown data table column names '%s'", unknownColumns));
        }
    }

    private void checkColumnNames(String column) {
        checkColumnNames(List.of(column));
    }

    public DataTable filter(List<String> selectedColumns, Map<String, List<String>> filter) {
        checkColumnNames(selectedColumns);
        QueryFilter queryFilter = new QueryFilter();
        queryFilter.setSelectedColumns(selectedColumns);
        List<ContentFilter> contentFilters = new ArrayList<>();
        filter.forEach((columnName, values) -> {
            ContentFilter contentFilter = new ContentFilter();
            contentFilter.setColumnName(columnName);
            contentFilter.setValues(filter.get(columnName));
            contentFilters.add(contentFilter);
        });
        queryFilter.setContentFilters(contentFilters);
        DataTable filteredDataTable = new DataTable(tabColumns, tabValues);
        filteredDataTable.filterInfos(queryFilter);
        return filteredDataTable;
    }

    public String searchFirstValue(String selectedColumn, Map<String, List<String>> filter) {
        checkColumnNames(selectedColumn);
        DataTable filteredDataTable = filter(List.of(selectedColumn), filter);
        List<String> values = filteredDataTable.data(selectedColumn);
        return values.isEmpty() ? null : values.get(0);
    }

    public List<String> searchValueList(String selectedColumn, Map<String, List<String>> filter) {
        checkColumnNames(selectedColumn);
        DataTable filteredDataTable = filter(List.of(selectedColumn), filter);
        return filteredDataTable.data(selectedColumn);
    }
}
