/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.google.auto.service.AutoService;

import java.io.IOException;

/**
 * Created by marifunf on 26/02/19.
 */
@AutoService(MappingVariableProvider.class)
public class EquipmentVariableProvider implements MappingVariableProvider<EquipmentVariable> {
    @Override
    public void writeJson(EquipmentVariable variable, JsonGenerator generator) throws IOException {
        EquipmentVariable.writeJson(variable, generator);
    }

    @Override
    public MappingVariable parseJson(JsonParser parser) throws IOException {
        return EquipmentVariable.parseJson(parser);
    }

    @Override
    public String getFieldName() {
        return EquipmentVariable.getName();
    }
}