/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ContentFilter {
    private String columnName;
    private List<String> values = new ArrayList<>();

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public List<String> getValues() {
        return Collections.unmodifiableList(values);
    }

    public void setValues(List<String> values) {
        this.values = new ArrayList<>(values);
    }
}
