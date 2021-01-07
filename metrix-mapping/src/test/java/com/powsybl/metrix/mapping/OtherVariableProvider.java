package com.powsybl.metrix.mapping;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.google.auto.service.AutoService;

import java.io.IOException;

/**
 * Created by marifunf on 26/02/19.
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
