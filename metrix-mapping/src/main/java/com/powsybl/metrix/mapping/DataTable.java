/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
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
        this.tabColumns.addAll(attributeNameToValue.getAttributeNames().stream().filter(colName -> !tabColumns.contains(colName)).distinct().collect(Collectors.toList()));
    }

    private String getValue(String name, boolean isWithName) {
        return isWithName ? name + ":" : StringUtils.EMPTY;
    }

    public List<String> values(String columnName, boolean isWithName) {
        return tabValues.stream().map(attributeNameToValue -> getValue(columnName, isWithName) + attributeNameToValue.getValue(columnName))
                .collect(Collectors.toList());
    }

    private List<List<String>> values(boolean isWithName) {
        return tabValues.stream().map(values ->
                        values.getAttributeNames().stream()
                                .map(attributeName -> getValue(attributeName, isWithName) + values.getValue(attributeName))
                                .collect(Collectors.toList()))
                .collect(Collectors.toList());
    }

    public static DataTable toDataTable(List<String> header, List<List<String>> content) {
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
}
