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
import com.powsybl.metrix.mapping.MappingVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian@rte-france.com>
 */
public class MetrixVariantsWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetrixVariantsWriter.class);

    private static final char SEPARATOR = ';';

    private final MetrixVariantProvider variantProvider;
    private final MetrixNetwork metrixNetwork;

    public static String getMetrixKey(MappingVariable variable, String equipmentType) {
        String key = null;
        switch (equipmentType) {
            case "generator":
                if (variable == EquipmentVariable.targetP) {
                    key = "PRODIM";
                } else if (variable == EquipmentVariable.minP) {
                    key = "TRPUIMIN";
                } else if (variable == EquipmentVariable.maxP) {
                    key = "TRVALPMD";
                }
                break;
            case "load":
                if (variable == EquipmentVariable.p0) {
                    key = "CONELE";
                }
                break;
            case "hvdcLine":
                if (variable == EquipmentVariable.activePowerSetpoint) {
                    key = "DCIMPPUI";
                } else if (variable == EquipmentVariable.minP) {
                    key = "DCMINPUI";
                } else if (variable == EquipmentVariable.maxP) {
                    key = "DCMAXPUI";
                }
                break;
            case "pst":
                if (variable == EquipmentVariable.currentTap) {
                    key = "DTVALDEP";
                }
                break;
            default:
                if (variable == MetrixVariable.offGridCostDown) {
                    key = "COUBHR";
                } else if (variable == MetrixVariable.offGridCostUp) {
                    key = "CTORDR";
                } else if (variable == MetrixVariable.onGridCostDown) {
                    key = "COUBAR";
                } else if (variable == MetrixVariable.onGridCostUp) {
                    key = "COUHAR";
                } else if (variable == MetrixVariable.thresholdN) {
                    key = "QATI00MN";
                } else if (variable == MetrixVariable.thresholdN1) {
                    key = "QATI5MNS";
                } else if (variable == MetrixVariable.thresholdNk) {
                    key = "QATI20MN";
                } else if (variable == MetrixVariable.thresholdITAM) {
                    key = "QATITAMN";
                } else if (variable == MetrixVariable.thresholdITAMNk) {
                    key = "QATITAMK";
                } else if (variable == MetrixVariable.thresholdNEndOr) {
                    key = "QATI00MN2";
                } else if (variable == MetrixVariable.thresholdN1EndOr) {
                    key = "QATI5MNS2";
                } else if (variable == MetrixVariable.thresholdNkEndOr) {
                    key = "QATI20MN2";
                } else if (variable == MetrixVariable.thresholdITAMEndOr) {
                    key = "QATITAMN2";
                } else if (variable == MetrixVariable.thresholdITAMNkEndOr) {
                    key = "QATITAMK2";
                } else if (variable == MetrixVariable.curativeCostDown) {
                    key = "COUEFF";
                } else {
                    LOGGER.warn("Unhandled variable {}", variable);
                }
        }
        return key;
    }

    public MetrixVariantsWriter(MetrixVariantProvider variantProvider, MetrixNetwork metrixNetwork) {
        this.variantProvider = variantProvider;
        this.metrixNetwork = metrixNetwork;
    }

    public void write(Range<Integer> variantRange, Path file) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            write(variantRange, writer);
        }
    }

    public void writeGzip(Range<Integer> variantRange, Path fileGz) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(fileGz)), StandardCharsets.UTF_8))) {
            write(variantRange, writer);
        }
    }

    public void write(Range<Integer> variantRange, BufferedWriter writer) throws IOException {
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
            variantProvider.readVariants(variantRange, new MetrixVariantReaderImpl(metrixNetwork, writer, SEPARATOR));
        }

    }
}
