/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.integration;

import com.google.common.collect.Range;
import com.powsybl.metrix.mapping.EquipmentVariable;
import com.powsybl.metrix.mapping.MappableEquipmentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class MetrixVariantsWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetrixVariantsWriter.class);

    private static final char SEPARATOR = ';';

    private final MetrixVariantProvider variantProvider;
    private final MetrixNetwork metrixNetwork;

    public static String getMetrixKey(EquipmentVariable variable, MappableEquipmentType equipmentType) {
        switch (equipmentType) {
            case GENERATOR:
                return getGeneratorKey(variable);
            case LOAD:
                return getLoadKey(variable);
            case HVDC_LINE:
                return getHvdcKey(variable);
            case PHASE_TAP_CHANGER:
                return getPhaseTapChangerKey(variable);
            default:
                LOGGER.warn("Unhandled variable {}", variable);
                return null;
        }
    }

    public static String getMetrixVariableKey(MetrixVariable variable) {
        switch (variable) {
            case offGridCostDown:
                return "COUBHR";
            case offGridCostUp:
                return "CTORDR";
            case onGridCostDown:
                return "COUBAR";
            case onGridCostUp:
                return "COUHAR";
            case thresholdN:
                return "QATI00MN";
            case thresholdN1:
                return "QATI5MNS";
            case thresholdNk:
                return "QATI20MN";
            case thresholdITAM:
                return "QATITAMN";
            case thresholdITAMNk:
                return "QATITAMK";
            case thresholdNEndOr:
                return "QATI00MN2";
            case thresholdN1EndOr:
                return "QATI5MNS2";
            case thresholdNkEndOr:
                return "QATI20MN2";
            case thresholdITAMEndOr:
                return "QATITAMN2";
            case thresholdITAMNkEndOr:
                return "QATITAMK2";
            case curativeCostDown:
                return "COUEFF";
            default:
                LOGGER.debug("Unhandled variable {}", variable);
                return null;
        }
    }

    private static String getPhaseTapChangerKey(EquipmentVariable variable) {
        if (variable == EquipmentVariable.phaseTapPosition) {
            return "DTVALDEP";
        }
        return null;
    }

    private static String getHvdcKey(EquipmentVariable variable) {
        switch (variable) {
            case activePowerSetpoint:
                return "DCIMPPUI";
            case minP:
                return "DCMINPUI";
            case maxP:
                return "DCMAXPUI";
            default:
                return null;
        }
    }

    private static String getLoadKey(EquipmentVariable variable) {
        if (variable == EquipmentVariable.p0) {
            return "CONELE";
        }
        return null;
    }

    private static String getGeneratorKey(EquipmentVariable variable) {
        switch (variable) {
            case targetP:
                return "PRODIM";
            case minP:
                return "TRPUIMIN";
            case maxP:
                return "TRVALPMD";
            default:
                return null;
        }
    }

    public MetrixVariantsWriter(MetrixVariantProvider variantProvider, MetrixNetwork metrixNetwork) {
        this.variantProvider = variantProvider;
        this.metrixNetwork = metrixNetwork;
    }

    public void write(Range<Integer> variantRange, Path file, Path workingDir) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            write(variantRange, writer, workingDir);
        }
    }

    public void write(Range<Integer> variantRange, BufferedWriter writer, Path workingDir) throws IOException {
        writer.write("NT");
        writer.write(SEPARATOR);
        if (variantProvider == null) {
            writer.write("1");
            writer.write(SEPARATOR);
            writer.newLine();
            writer.write("0");
            writer.write(SEPARATOR);
            writer.newLine();
        } else {
            int variantCount = variantRange.upperEndpoint() - variantRange.lowerEndpoint() + 1;
            writer.write(Integer.toString(variantCount));
            writer.write(SEPARATOR);
            writer.newLine();
            variantProvider.readVariants(variantRange, new MetrixVariantReaderImpl(metrixNetwork, writer, SEPARATOR), workingDir);
        }

    }
}
