/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */
package com.powsybl.metrix.mapping.common.iidm.extensions;

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
public class HvdcAngleDroopActivePowerControlXmlSerializer implements ExtensionXmlSerializer<HvdcLine, HvdcAngleDroopActivePowerControl> {

    @Override
    public String getExtensionName() {
        return "hvdcAngleDroopActivePowerControl";
    }

    @Override
    public String getCategoryName() {
        return "network";
    }

    @Override
    public Class<? super HvdcAngleDroopActivePowerControl> getExtensionClass() {
        return HvdcAngleDroopActivePowerControl.class;
    }

    @Override
    public boolean hasSubElements() {
        return false;
    }

    @Override
    public InputStream getXsdAsStream() {
        return getClass().getResourceAsStream("/xsd/hvdcAngleDroopActivePowerControl.xsd");
    }

    @Override
    public String getNamespaceUri() {
        return "http://www.itesla_project.eu/schema/iidm/ext/hvdc_angle_droop_active_power_control/1_0";
    }

    @Override
    public String getNamespacePrefix() {
        return "hapc";
    }

    @Override
    public void write(HvdcAngleDroopActivePowerControl extension, XmlWriterContext context) throws XMLStreamException {
        context.getExtensionsWriter().writeAttribute("p0", Float.toString(extension.getP0()));
        context.getExtensionsWriter().writeAttribute("droop", Float.toString(extension.getDroop()));
        context.getExtensionsWriter().writeAttribute("enabled", Boolean.toString(extension.isEnabled()));
    }

    @Override
    public HvdcAngleDroopActivePowerControlImpl read(HvdcLine hvdcLine, XmlReaderContext context) throws XMLStreamException {
        float p0 = XmlUtil.readFloatAttribute(context.getReader(), "p0");
        float droop = XmlUtil.readFloatAttribute(context.getReader(), "droop");
        boolean enabled = XmlUtil.readBoolAttribute(context.getReader(), "enabled");

        return new HvdcAngleDroopActivePowerControlImpl(hvdcLine, p0, droop, enabled);
    }
}
