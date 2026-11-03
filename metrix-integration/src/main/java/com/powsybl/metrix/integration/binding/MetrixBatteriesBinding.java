/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration.binding;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collection;

/**
 * @author Matthieu SAUR {@literal <matthieu.saur at rte-france.com>}
 */
public class MetrixBatteriesBinding extends AbstractMetrixGroupBinding {

    MetrixBatteriesBinding() {
        super();
    }

    public MetrixBatteriesBinding(String name, Collection<String> ids) {
        this(name, ids, ReferenceVariable.PMAX);
    }

    @JsonCreator
    public MetrixBatteriesBinding(@JsonProperty("name") String name,
                @JsonProperty("ids") Collection<String> ids,
                @JsonProperty("reference") ReferenceVariable reference) {
        super(name, ids, reference);
    }

    @Override
    protected String getGroupName() {
        return "Batteries";
    }

    @Override
    public String getGroupNameSingular() {
        return "Battery";
    }

}
