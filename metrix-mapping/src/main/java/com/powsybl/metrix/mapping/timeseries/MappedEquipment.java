/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping.timeseries;

import com.powsybl.iidm.network.Identifiable;
import com.powsybl.metrix.mapping.DistributionKey;

import java.util.Objects;

/**
 * @author Valentin Berthault {@literal <valentin.berthault at rte-france.com>}
 */
public class MappedEquipment {

    private final Identifiable<?> identifiable;

    private final DistributionKey distributionKey;

    public MappedEquipment(Identifiable<?> identifiable, DistributionKey distributionKey) {
        this.identifiable = Objects.requireNonNull(identifiable);
        this.distributionKey = distributionKey;
    }

    public Identifiable<?> getIdentifiable() {
        return identifiable;
    }

    public String getId() {
        return identifiable.getId();
    }

    public DistributionKey getDistributionKey() {
        return distributionKey;
    }
}
