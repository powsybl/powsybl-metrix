/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration.network;

import com.google.common.collect.Range;
import com.powsybl.metrix.integration.MetrixVariable;
import com.powsybl.metrix.mapping.EquipmentVariable;
import com.powsybl.metrix.mapping.MappableEquipmentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author Paul Bui-Quang {@literal <paul.buiquang at rte-france.com>}
 */
public class MetrixVariantsWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetrixVariantsWriter.class);

    private static final char SEPARATOR = ';';

    private final MetrixVariantProvider variantProvider;
    private final MetrixNetwork metrixNetwork;

    public static String getMetrixKey(EquipmentVariable variable, MappableEquipmentType equipmentType) {
        return switch (equipmentType) {
            case GENERATOR -> getGeneratorKey(variable);
            case LOAD -> getLoadKey(variable);
            case HVDC_LINE -> getHvdcKey(variable);
            case PHASE_TAP_CHANGER -> getPhaseTapChangerKey(variable);
            default -> {
                LOGGER.warn("Unhandled variable {}", variable);
                yield null;
            }
        };
    }

    public static String getMetrixVariableKey(MetrixVariable variable) {
        return switch (variable) {
            case OFF_GRID_COST_DOWN -> "COUBHR";
            case OFF_GRID_COST_UP -> "CTORDR";
            case ON_GRID_COST_DOWN -> "COUBAR";
            case ON_GRID_COST_UP -> "COUHAR";
            case THRESHOLD_N -> "QATI00MN";
            case THRESHOLD_N1 -> "QATI5MNS";
            case THRESHOLD_NK -> "QATI20MN";
            case THRESHOLD_ITAM -> "QATITAMN";
            case THRESHOLD_ITAM_NK -> "QATITAMK";
            case THRESHOLD_N_END_OR -> "QATI00MN2";
            case THRESHOLD_N1_END_OR -> "QATI5MNS2";
            case THRESHOLD_NK_END_OR -> "QATI20MN2";
            case THRESHOLD_ITAM_END_OR -> "QATITAMN2";
            case THRESHOLD_ITAM_NK_END_OR -> "QATITAMK2";
            case CURATIVE_COST_DOWN -> "COUEFF";
            default -> {
                LOGGER.debug("Unhandled variable {}", variable);
                yield null;
            }
        };
    }

    private static String getPhaseTapChangerKey(EquipmentVariable variable) {
        if (variable == EquipmentVariable.PHASE_TAP_POSITION) {
            return "DTVALDEP";
        }
        return null;
    }

    private static String getHvdcKey(EquipmentVariable variable) {
        return switch (variable) {
            case ACTIVE_POWER_SETPOINT -> "DCIMPPUI";
            case MIN_P -> "DCMINPUI";
            case MAX_P -> "DCMAXPUI";
            default -> null;
        };
    }

    private static String getLoadKey(EquipmentVariable variable) {
        if (variable == EquipmentVariable.P0) {
            return "CONELE";
        }
        return null;
    }

    private static String getGeneratorKey(EquipmentVariable variable) {
        return switch (variable) {
            case TARGET_P -> "PRODIM";
            case MIN_P -> "TRPUIMIN";
            case MAX_P -> "TRVALPMD";
            default -> null;
        };
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
