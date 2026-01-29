/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.commons.data.datatable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.powsybl.metrix.commons.exception.DataTableException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
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

    /**
     * Create a table
     * @param header  list of column names
     * @param content list of values for the lines
     */
    public static DataTable toDataTable(List<String> header, List<List<String>> content) {
        checkData(header, content);
        DataTable dataTable = new DataTable();
        content.forEach(line -> {
            AttributeNameToValue attributeNameToValue = new AttributeNameToValue();
            int headerSize = header.size();
            for (int column = 0; column < headerSize; column++) {
                attributeNameToValue.put(header.get(column), line.get(column));
            }
            dataTable.addAttributeNameToValue(attributeNameToValue);
        });
        return dataTable;
    }

    /**
     * Retrieve the list of column names
     */
    public List<String> columnNames() {
        return tabColumns;
    }

    /**
     * Check for the existence of a column
     * @param columnName column name to check
     */
    public boolean columnExists(String columnName) {
        return tabColumns.contains(columnName);
    }

    /**
     * Retrieve the list of value lists for all lines
     */
    public List<List<String>> data() {
        return values(false);
    }

    /**
     * Retrieve the list of value lists for all lines, values are prefixed by the attribute names
     */
    public List<List<String>> get() {
        return values(true);
    }

    /**
     * Retrieve the list of values from a column
     * @param columnName column name to retrieve
     */
    public List<String> data(String columnName) {
        return values(columnName, false);
    }

    /**
     * Retrieve the list of values from a column, values are prefixed by the attribute names
     * @param columnName column name to retrieve
     */
    public List<String> get(String columnName) {
        return values(columnName, true);
    }

    /**
     * Retrieve the list of values from a column, with or without prefix by the attribute names
     * @param columnName column name to retrieve
     * @param isWithName values are prefixed by the attribute names if true
     */
    public List<String> values(String columnName, boolean isWithName) {
        checkColumnNames(columnName);
        return tabValues.stream().map(attributeNameToValue -> getValue(columnName, isWithName) + attributeNameToValue.getValue(columnName))
            .collect(Collectors.toList());
    }

    /**
     * Iterate through the lines of a table and retrieve the column values
     */
    public List<LinkedHashMap<String, String>> getLines() {
        return tabValues.stream()
            .map(attributeNameToValue -> new LinkedHashMap<>(attributeNameToValue.getAttributeNameToValue()))
            .collect(Collectors.toList());
    }

    /**
     * Filtering a table by applying a map of selection criteria
     * @param selectedColumns list of column names to keep
     * @param filter          map of selection criteria
     */
    public DataTable filter(List<String> selectedColumns, Map<String, List<String>> filter) {
        checkColumnNames(selectedColumns);
        QueryFilter queryFilter = queryFilter(selectedColumns, filter);
        DataTable filteredDataTable = new DataTable(tabColumns, tabValues);
        filteredDataTable.filterInfos(queryFilter);
        return filteredDataTable;
    }

    /**
     * Find the first value contained in a column by applying a map of selection criteria
     * @param selectedColumn column name
     * @param filter         map of selection criteria
     */
    public String searchFirstValue(String selectedColumn, Map<String, List<String>> filter) {
        checkColumnNames(selectedColumn);
        QueryFilter queryFilter = queryFilter(List.of(selectedColumn), filter);
        return this.tabValues.stream()
            .filter(attributeNameToValue -> matchesQueryFilter(attributeNameToValue, queryFilter))
            .map(attributeNameToValue -> attributeNameToValue.getValue(selectedColumn))
            .findFirst()
            .orElse(null);
    }

    /**
     * Find the list of values contained in a column by applying a map of selection criteria
     * @param selectedColumn column name
     * @param filter         map of selection criteria
     */
    public List<String> searchValueList(String selectedColumn, Map<String, List<String>> filter) {
        checkColumnNames(selectedColumn);
        QueryFilter queryFilter = queryFilter(List.of(selectedColumn), filter);
        return this.tabValues.stream()
            .filter(attributeNameToValue -> matchesQueryFilter(attributeNameToValue, queryFilter))
            .map(attributeNameToValue -> attributeNameToValue.getValue(selectedColumn))
            .toList();
    }

    /**
     * Delete lines by applying a map of selection criteria
     * @param filter map of selection criteria
     */
    public DataTable removeLines(Map<String, List<String>> filter) {
        QueryFilter queryFilter = queryFilter(columnNames(), filter);
        this.tabValues.removeIf(attributeNameToValue -> matchesQueryFilter(attributeNameToValue, queryFilter));
        return this;
    }

    /**
     * Add lines
     * @param header  list of column names
     * @param content list of values for the lines to be added
     */
    public DataTable addLines(List<String> header, List<List<String>> content) {
        checkHeaders(columnNames(), header);
        checkData(header, content);
        content.forEach(line -> {
            AttributeNameToValue attributeNameToValue = new AttributeNameToValue();
            int headerSize = header.size();
            for (int column = 0; column < headerSize; column++) {
                attributeNameToValue.put(header.get(column), line.get(column));
            }
            this.addAttributeNameToValue(attributeNameToValue);
        });
        return this;
    }

    /**
     * Replace values by applying a map of selection criteria
     * @param selectedColumns list of column names to replace
     * @param filter          map of selection criteria
     * @param values          list of new values
     */
    public DataTable replaceValues(List<String> selectedColumns, Map<String, List<String>> filter, List<String> values) {
        checkColumnNames(selectedColumns);
        checkValuesSize(selectedColumns, values);
        QueryFilter queryFilter = queryFilter(selectedColumns, filter);
        this.tabValues.stream()
            .filter(attributeNameToValue -> matchesQueryFilter(attributeNameToValue, queryFilter))
            .forEach(attributeNameToValue -> applyReplacement(attributeNameToValue, selectedColumns, values));
        return this;
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

    private List<List<String>> values(boolean isWithName) {
        return tabValues.stream().map(values ->
                values.getAttributeNames().stream()
                    .map(attributeName -> getValue(attributeName, isWithName) + values.getValue(attributeName))
                    .collect(Collectors.toList()))
            .collect(Collectors.toList());
    }

    private void filterInfos(QueryFilter filter) {
        this.tabValues = this.tabValues.stream()
            .filter(attributeNameToValue -> matchesQueryFilter(attributeNameToValue, filter))
            .map(attributeNameToValue -> attributeNameToValue.filterSelectedColumns(filter.getSelectedColumns()))
            .toList();
        tabColumns = new ArrayList<>(filter.getSelectedColumns());
    }

    private void checkColumnNames(List<String> columns) {
        Set<String> unknownColumns = new HashSet<>(columns);
        columnNames().forEach(unknownColumns::remove);
        if (!unknownColumns.isEmpty()) {
            throw new DataTableException(String.format("Unknown data table column names '%s'", unknownColumns));
        }
    }

    private void checkColumnNames(String column) {
        checkColumnNames(List.of(column));
    }

    private static void checkData(List<String> header, List<List<String>> content) {
        checkDuplicateColumnNames(header);
        int nbColumns = header.size();
        content.forEach(values -> checkNbValues(nbColumns, values, content.indexOf(values)));
    }

    private static void checkDuplicateColumnNames(List<String> columns) {
        Set<String> duplicateColumnNames = columns.stream().distinct()
                .filter(i -> Collections.frequency(columns, i) > 1)
                .collect(Collectors.toSet());
        if (!duplicateColumnNames.isEmpty()) {
            throw new DataTableException(String.format("Several columns with same names '%s'", duplicateColumnNames));
        }
    }

    private static void checkHeaders(List<String> existingHeader, List<String> header) {
        if (!new HashSet<>(existingHeader).equals(new HashSet<>(header))) {
            throw new DataTableException(String.format("Headers are different: existing header=%s header to add=%s", existingHeader, header));
        }
    }

    private static void checkValuesSize(List<String> selectedColumns, List<String> values) {
        if (selectedColumns.size() != values.size()) {
            throw new DataTableException(String.format("Number of selected columns '%s' different from number of values '%s'", selectedColumns.size(), values.size()));
        }
    }

    private boolean matchesFilter(AttributeNameToValue attributeNameToValue, String columnName, List<String> filterValues) {
        String value = attributeNameToValue.getValue(columnName);
        if (value == null) {
            return false;
        }
        return filterValues.stream().anyMatch(filterValue -> Strings.CI.equals(value, filterValue));
    }

    private boolean matchesQueryFilter(AttributeNameToValue attributeNameToValue, QueryFilter queryFilter) {
        return queryFilter.getContentFilters().stream()
            .allMatch(contentFilter -> matchesFilter(attributeNameToValue, contentFilter.getColumnName(), contentFilter.getValues()));
    }

    private void applyReplacement(AttributeNameToValue attributeNameToValue, List<String> selectedColumns, List<String> values) {
        for (int i = 0; i < selectedColumns.size(); i++) {
            attributeNameToValue.put(selectedColumns.get(i), values.get(i));
        }
    }

    private static void checkNbValues(int nbColumns, List<String> values, int row) {
        if (nbColumns != values.size()) {
            throw new DataTableException(String.format("Number of columns '%s' different from number of values '%s' at row '%s'", nbColumns, values.size(), row + 1));
        }
    }

    private static QueryFilter queryFilter(List<String> selectedColumns, Map<String, List<String>> filter) {
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
        return queryFilter;
    }
}
