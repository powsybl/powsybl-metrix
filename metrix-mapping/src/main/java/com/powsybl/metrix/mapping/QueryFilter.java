/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author marifunf {@literal <marianne.funfrock at rte-france.com>}
 */
public class QueryFilter {
    private List<String> selectedColumns = new ArrayList<>();
    private List<ContentFilter> contentFilters = new ArrayList<>();

    public List<String> getSelectedColumns() {
        return Collections.unmodifiableList(selectedColumns);
    }

    public void setSelectedColumns(List<String> selectedColumns) {
        this.selectedColumns = selectedColumns;
    }

    public List<ContentFilter> getContentFilters() {
        return Collections.unmodifiableList(contentFilters);
    }

    public void setContentFilters(List<ContentFilter> contentFilters) {
        this.contentFilters = new ArrayList<>(contentFilters);
    }
}
