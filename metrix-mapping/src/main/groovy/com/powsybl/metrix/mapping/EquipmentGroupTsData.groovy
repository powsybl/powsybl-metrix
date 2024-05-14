/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping

/**
 * @author marifunf {@literal <marianne.funfrock at rte-france.com>}
 */
class EquipmentGroupTsData extends FilteredData {

    EquipmentGroupType group

    String name

    void group(EquipmentGroupType group) {
        this.group = group
    }

    void withName(String name) {
        this.name = name
    }
}
