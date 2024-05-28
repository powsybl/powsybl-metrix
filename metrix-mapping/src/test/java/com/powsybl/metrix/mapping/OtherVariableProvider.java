/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.google.auto.service.AutoService;

import java.io.IOException;

/**
 * @author Paul Bui-Quang {@literal <paul.buiquang at rte-france.com>}
 */
@AutoService(MappingVariableProvider.class)
public class OtherVariableProvider implements MappingVariableProvider<OtherVariable> {
    @Override
    public void writeJson(OtherVariable variable, JsonGenerator generator) throws IOException {
        OtherVariable.writeJson(variable, generator);
    }

    @Override
    public MappingVariable parseJson(JsonParser parser) throws IOException {
        return OtherVariable.parseJson(parser);
    }

    @Override
    public String getFieldName() {
        return OtherVariable.getName();
    }
}
