/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.commons.extensions.iidm;

import com.google.auto.service.AutoService;
import com.powsybl.commons.extensions.ExtensionXmlSerializer;
import com.powsybl.commons.xml.XmlReaderContext;
import com.powsybl.commons.xml.XmlUtil;
import com.powsybl.commons.xml.XmlWriterContext;
import com.powsybl.iidm.network.HvdcLine;

import javax.xml.stream.XMLStreamException;
import java.io.InputStream;

/**
 * @author Mathieu Bague <mathieu.bague at rte-france.com>
 */
@AutoService(ExtensionXmlSerializer.class)
public class HvdcOperatorActivePowerRangeXmlSerializer implements ExtensionXmlSerializer<HvdcLine, HvdcOperatorActivePowerRange> {

    @Override
    public String getExtensionName() {
        return "hvdcOperatorActivePowerRange";
    }

    @Override
    public String getCategoryName() {
        return "network";
    }

    @Override
    public Class<? super HvdcOperatorActivePowerRange> getExtensionClass() {
        return HvdcOperatorActivePowerRange.class;
    }

    @Override
    public boolean hasSubElements() {
        return false;
    }

    @Override
    public InputStream getXsdAsStream() {
        return getClass().getResourceAsStream("/xsd/hvdcOperatorActivePowerRange.xsd");
    }

    @Override
    public String getNamespaceUri() {
        return "http://www.itesla_project.eu/schema/iidm/ext/hvdc_operator_active_power_range/1_0";
    }

    @Override
    public String getNamespacePrefix() {
        return "hopr";
    }

    @Override
    public void write(HvdcOperatorActivePowerRange opr, XmlWriterContext context) throws XMLStreamException {
        XmlUtil.writeFloat("fromCS1toCS2", opr.getOprFromCS1toCS2(), context.getExtensionsWriter());
        XmlUtil.writeFloat("fromCS2toCS1", opr.getOprFromCS2toCS1(), context.getExtensionsWriter());
    }

    @Override
    public HvdcOperatorActivePowerRange read(HvdcLine hvdcLine, XmlReaderContext context) {
        float oprFromCS1toCS2 = XmlUtil.readFloatAttribute(context.getReader(), "fromCS1toCS2");
        float oprFromCS2toCS1 = XmlUtil.readFloatAttribute(context.getReader(), "fromCS2toCS1");
        hvdcLine.newExtension(HvdcOperatorActivePowerRangeAdder.class)
                .withOprFromCS1toCS2(oprFromCS1toCS2)
                .withOprFromCS2toCS1(oprFromCS2toCS1)
                .add();

        return hvdcLine.getExtension(HvdcOperatorActivePowerRange.class);
    }
}
