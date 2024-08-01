/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration.dataGenerator;

import com.powsybl.commons.PowsyblException;
import com.powsybl.timeseries.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Valentin Berthault {@literal <valentin.berthault at rte-france.com>}
 */
public class MetrixOutputData {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetrixOutputData.class);

    static final String FILE_NAME_PREFIX = "result";

    private static final String EMPTY_STRING = "";

    public static final String ERROR_CODE_NAME = "ERROR_CODE";
    public static final String OVERLOAD_BASECASE = "OVERLOAD_BASECASE";
    public static final String OVERLOAD_OUTAGES = "OVERLOAD_OUTAGES";
    public static final String GEN_COST = "GEN_COST";
    public static final String LOAD_COST = "LOAD_COST";
    public static final String FLOW_NAME = "FLOW_";
    public static final String MAX_THREAT_NAME = "MAX_THREAT_";
    public static final String MAX_TMP_THREAT_FLOW = "MAX_TMP_THREAT_FLOW_";
    public static final String GEN_VOL_UP = "GEN_VOL_UP_";
    public static final String GEN_VOL_DOWN = "GEN_VOL_DOWN_";
    public static final String LOSSES = "LOSSES";
    public static final String LOSSES_BY_COUNTRY = "LOSSES_";
    public static final String HVDC_NAME = "HVDC_";
    public static final String PST_NAME = "PST_";
    public static final String PST_TAP_NAME = "PST_TAP_";
    public static final String PST_CUR_NAME = "PST_CUR_";
    public static final String PST_CUR_TAP_NAME = "PST_CUR_TAP_";
    public static final String HVDC_TYPE = "hvdc";
    public static final String PST_TYPE = "pst";
    public static final String GEN_TYPE = "generator-type";
    public static final String GENERATOR = "generator";
    public static final String BRANCH = "branch";
    public static final String CONTINGENCY_TYPE = "contingency";
    public static final String BASECASE_TYPE = "basecase";
    private static final String UNKNOWN_OUTAGE = "Unknown outage";

    private static final String INCIDENT = "INCIDENT";
    private static final String PAR_LIGNE = "PAR LIGNE";
    private static final Double ERROR_CODE = 1d;

    private final Map<String, DoubleResultChunk> doubleTimeSeries = new ConcurrentHashMap<>();

    private final Map<String, StringResultChunk> stringTimeSeries = new ConcurrentHashMap<>();

    private final int offset;

    private final int length;

    public static final class DoubleResultChunk {

        private final double[] timeSeries;
        private final Map<String, String> tags = new HashMap<>();

        public double[] getTimeSeries() {
            return timeSeries;
        }

        public Map<String, String> getTags() {
            return tags;
        }

        DoubleResultChunk(int length) {
            timeSeries = new double[length];
            Arrays.fill(timeSeries, Double.NaN);
        }

        public DoubleResultChunk(int length, Map<String, String> metadata) {
            this(length);
            Objects.requireNonNull(metadata);
            tags.putAll(metadata);
        }

        public void insertResult(int pos, double value) {
            timeSeries[pos] = value;
        }
    }

    private static final class StringResultChunk {

        private final String[] timeSeries;
        private final Map<String, String> tags = new HashMap<>();

        String[] getTimeSeries() {
            return timeSeries;
        }

        Map<String, String> getTags() {
            return tags;
        }

        StringResultChunk(int length) {
            timeSeries = new String[length];
            Arrays.fill(timeSeries, "");
        }

        StringResultChunk(int length, Map<String, String> metadata) {
            this(length);
            Objects.requireNonNull(metadata);
            tags.putAll(metadata);
        }

        void insertResult(int pos, String value) {
            timeSeries[pos] = value;
        }
    }

    /**
     * @param length number of data in each time series
     */
    public MetrixOutputData(int offset, int length) {
        this.offset = offset;
        this.length = length;
    }

    static String getFileName(int variantNum) {
        return FILE_NAME_PREFIX + "_s" + variantNum;
    }

    private StringResultChunk getStringTimeSeries(String prefix, String type, String id) {
        String name = prefix + id;
        return stringTimeSeries.computeIfAbsent(name, k -> {
            Map<String, String> tags = Map.of(type, id);
            return new StringResultChunk(length, tags);
        });
    }

    private DoubleResultChunk getDoubleTimeSeries(String name) {
        return doubleTimeSeries.computeIfAbsent(name, k -> new DoubleResultChunk(length));
    }

    private DoubleResultChunk getDoubleTimeSeries(String prefix, String id) {
        String name = prefix + id;
        return doubleTimeSeries.computeIfAbsent(name, k -> {
            Map<String, String> tags = Map.of(BRANCH, id);
            return new DoubleResultChunk(length, tags);
        });
    }

    private DoubleResultChunk getDoubleTimeSeries(String prefix, String type, String id) {
        return getDoubleTimeSeries(prefix, type, id, null);
    }

    private DoubleResultChunk getDoubleTimeSeries(String prefix, String type, String id, String outage) {
        Optional<String> optOutage = Optional.ofNullable(outage);
        String name = prefix + id + optOutage.map(s -> "_" + s).orElse(EMPTY_STRING);
        return doubleTimeSeries.computeIfAbsent(name, k -> {
            Map<String, String> tags = Map.of(type, id, CONTINGENCY_TYPE, optOutage.orElse(BASECASE_TYPE));
            return new DoubleResultChunk(length, tags);
        });
    }

    private DoubleResultChunk getDetailedMVTimeSeries(String prefix, String id, String element, String outage) {
        Optional<String> optOutage = Optional.ofNullable(outage);
        String name = prefix + id + optOutage.map(s -> "_" + s).orElse(EMPTY_STRING) + "_" + element;
        return doubleTimeSeries.computeIfAbsent(name, k -> {
            Map<String, String> tags = Map.of(BRANCH, id,
                    "action", element,
                        CONTINGENCY_TYPE, optOutage.orElse(BASECASE_TYPE));
            return new DoubleResultChunk(length, tags);
        });
    }

    public void readFile(Path workingDir, int varNum) {
        Path resultFilePath = workingDir.resolve(getFileName(varNum));
        if (!Files.exists(resultFilePath)) {
            LOGGER.error("Result file not found for variant {}", varNum);
            getDoubleTimeSeries(ERROR_CODE_NAME).insertResult(varNum - offset, ERROR_CODE);
        } else {
            try (BufferedReader reader = Files.newBufferedReader(resultFilePath, StandardCharsets.UTF_8)) {
                read(reader, varNum);
            } catch (Exception e) {
                LOGGER.error("Error encountered while reading results for variant {}", varNum, e);
                getDoubleTimeSeries(ERROR_CODE_NAME).insertResult(varNum - offset, ERROR_CODE);
            }
        }
    }

    public void read(BufferedReader reader, int varNum) throws IOException {
        String line;
        String[] chunks;
        Map<Integer, String> outageNames = new HashMap<>();

        boolean empty = true;
        while ((line = reader.readLine()) != null) {
            empty = false;
            chunks = line.split(";", -1);
            switch (chunks[0]) {
                case "C1 " -> readC1(varNum, chunks);
                case "C2 " -> readC2(varNum, chunks);
                case "C2B " -> readC2B(varNum, chunks);
                case "C4 " -> readC4(outageNames, chunks);
                case "C5 " -> readC5(varNum, chunks);
                case "R1 " -> readR1(varNum, chunks);
                case "R1B " -> readR1B(varNum, outageNames, chunks);
                case "R1C " -> readR1C(varNum, chunks);
                case "R2 " -> readR2(varNum, chunks);
                case "R2B " -> readR2B(varNum, chunks, outageNames);
                case "R2C " -> readR2C(varNum, chunks);
                case "R3 " -> readR3(varNum, chunks);
                case "R3B " -> readR3B(varNum, chunks, outageNames);
                case "R3C " -> readR3C(varNum, chunks, outageNames);
                case "R4 " -> readR4(varNum, chunks, outageNames);
                case "R4B " -> readR4B(varNum, chunks, outageNames);
                case "R5 " -> readR5(varNum, chunks);
                case "R5B " -> readR5B(varNum, chunks, outageNames);
                case "R6 " -> readR6(varNum, chunks);
                case "R6B " -> readR6B(varNum, chunks, outageNames);
                case "R7 " -> readR7(varNum, chunks);
                case "R8 " -> readR8(varNum, chunks);
                case "R8B " -> readR8B(varNum, chunks);
                case "R9 " -> readR9(varNum, chunks);
                case "R10" -> readR10(varNum, chunks);
                default -> LOGGER.error("Unexpected content for variant {} ({})", varNum, line);
            }
        }
        if (empty) {
            LOGGER.error("Empty Metrix result file");
        }
    }

    /**
     * Topological remedial actions
     */
    private void readR10(int varNum, String[] chunks) {
        StringResultChunk sts;
        // Check that it's not the header
        if (INCIDENT.equals(chunks[1])) {
            return;
        }
        sts = getStringTimeSeries("TOPOLOGY_", CONTINGENCY_TYPE, chunks[2]);
        sts.insertResult(varNum - offset, chunks[4]);
    }

    /**
     * Objective method for the optimization
     */
    private void readR9(int varNum, String[] chunks) {
        DoubleResultChunk ts;
        // Check that it's not the header
        if ("FCT OBJECTIF".equals(chunks[1])) {
            return;
        }
        ts = getDoubleTimeSeries(GEN_COST);
        ts.insertResult(varNum - offset, Double.parseDouble(chunks[2]));

        ts = getDoubleTimeSeries(LOAD_COST);
        ts.insertResult(varNum - offset, Double.parseDouble(chunks[3]));

        ts = getDoubleTimeSeries(OVERLOAD_OUTAGES);
        ts.insertResult(varNum - offset, Double.parseDouble(chunks[4]));

        ts = getDoubleTimeSeries(OVERLOAD_BASECASE);
        ts.insertResult(varNum - offset, Double.parseDouble(chunks[5]));
        if (!EMPTY_STRING.equals(chunks[6])) {
            ts = getDoubleTimeSeries("GEN_CUR_COST");
            ts.insertResult(varNum - offset, Double.parseDouble(chunks[6]));
        }
        if (!EMPTY_STRING.equals(chunks[7])) {
            ts = getDoubleTimeSeries("LOAD_CUR_COST");
            ts.insertResult(varNum - offset, Double.parseDouble(chunks[7]));
        }
    }

    /**
     * Losses by country
     */
    private void readR8B(int varNum, String[] chunks) {
        DoubleResultChunk ts;
        // Check that it's not the header
        if ("PERTES".equals(chunks[1])) {
            return;
        }
        ts = getDoubleTimeSeries(LOSSES_BY_COUNTRY, "losses", chunks[2]);
        ts.insertResult(varNum - offset, Double.parseDouble(chunks[3]));
    }

    /**
     * Losses
     */
    private void readR8(int varNum, String[] chunks) {
        DoubleResultChunk ts;
        // Check that it's not the header
        if ("PERTES".equals(chunks[1])) {
            return;
        }
        ts = getDoubleTimeSeries(LOSSES);
        ts.insertResult(varNum - offset, Double.parseDouble(chunks[2]));
    }

    /**
     * Redispatching by generator types
     */
    private void readR7(int varNum, String[] chunks) {
        DoubleResultChunk ts;
        // Check that it's not the header
        if ("PAR FILIERE".equals(chunks[1])) {
            return;
        }
        if (!EMPTY_STRING.equals(chunks[3])) {
            ts = getDoubleTimeSeries(GEN_VOL_DOWN, GEN_TYPE, chunks[2]);
            ts.insertResult(varNum - offset, Double.parseDouble(chunks[3]));
        }
        if (!EMPTY_STRING.equals(chunks[4])) {
            ts = getDoubleTimeSeries(GEN_VOL_UP, GEN_TYPE, chunks[2]);
            ts.insertResult(varNum - offset, Double.parseDouble(chunks[4]));
        }
        if (!EMPTY_STRING.equals(chunks[5])) {
            ts = getDoubleTimeSeries("GEN_CUR_VOL_DOWN_", GEN_TYPE, chunks[2]);
            ts.insertResult(varNum - offset, Double.parseDouble(chunks[5]));
        }
        if (!EMPTY_STRING.equals(chunks[6])) {
            ts = getDoubleTimeSeries("GEN_CUR_VOL_UP_", GEN_TYPE, chunks[2]);
            ts.insertResult(varNum - offset, Double.parseDouble(chunks[6]));
        }
    }

    /**
     * HVDC Curative
     */
    private void readR6B(int varNum, String[] chunks, Map<Integer, String> outageNames) {
        String outageName;
        DoubleResultChunk ts;
        // Check that it's not the header
        if (INCIDENT.equals(chunks[1])) {
            return;
        }
        outageName = Optional.ofNullable(outageNames.get(Integer.parseInt(chunks[1]))).orElseThrow(() -> new PowsyblException(UNKNOWN_OUTAGE));
        ts = getDoubleTimeSeries("HVDC_CUR_", HVDC_TYPE, chunks[2], outageName);
        ts.insertResult(varNum - offset, Double.parseDouble(chunks[3]));
    }

    /**
     * HVDC Basecase
     */
    private void readR6(int varNum, String[] chunks) {
        DoubleResultChunk ts;
        // Check that it's not the header
        if (" PAR LCC".equals(chunks[1])) {
            return;
        }
        ts = getDoubleTimeSeries(HVDC_NAME, HVDC_TYPE, chunks[2]);
        ts.insertResult(varNum - offset, Double.parseDouble(chunks[3]));

        if (!EMPTY_STRING.equals(chunks[4])) {
            ts = getDoubleTimeSeries("MV_", HVDC_TYPE, chunks[2]);
            ts.insertResult(varNum - offset, Double.parseDouble(chunks[4]));
        }
    }

    /**
     * PST Curative
     */
    private void readR5B(int varNum, String[] chunks, Map<Integer, String> outageNames) {
        DoubleResultChunk ts;
        String outageName;
        // Check that it's not the header
        if (INCIDENT.equals(chunks[1])) {
            return;
        }
        outageName = Optional.ofNullable(outageNames.get(Integer.parseInt(chunks[1]))).orElseThrow(() -> new PowsyblException(UNKNOWN_OUTAGE));
        ts = getDoubleTimeSeries(PST_CUR_NAME, PST_TYPE, chunks[2], outageName);
        ts.insertResult(varNum - offset, Double.parseDouble(chunks[3]));
        ts = getDoubleTimeSeries(PST_CUR_TAP_NAME, PST_TYPE, chunks[2], outageName);
        ts.insertResult(varNum - offset, Integer.parseInt(chunks[4]));
    }

    /**
     * PST Basecase
     */
    private void readR5(int varNum, String[] chunks) {
        DoubleResultChunk ts;
        // Check that it's not the header
        if ("PAR TD".equals(chunks[1])) {
            return;
        }
        ts = getDoubleTimeSeries(PST_NAME, PST_TYPE, chunks[2]);
        ts.insertResult(varNum - offset, Double.parseDouble(chunks[3]));
        ts = getDoubleTimeSeries(PST_TAP_NAME, PST_TYPE, chunks[2]);
        ts.insertResult(varNum - offset, Integer.parseInt(chunks[4]));
    }

    /**
     * Detailed marginal costs
     */
    private void readR4B(int varNum, String[] chunks, Map<Integer, String> outageNames) {
        String outageName;
        DoubleResultChunk ts;
        int outageId;
        // Check that it's not the header
        if ("VAR. MARGINALES".equals(chunks[1])) {
            return;
        }

        outageId = Integer.parseInt(chunks[3]);
        outageName = outageId == 0 ? null : Optional.ofNullable(outageNames.get(outageId)).orElseThrow(() -> new PowsyblException(UNKNOWN_OUTAGE));
        ts = getDetailedMVTimeSeries("MV_POW", chunks[2], chunks[5], outageName);
        ts.insertResult(varNum - offset, Double.parseDouble(chunks[6]));
        ts = getDetailedMVTimeSeries("MV_COST", chunks[2], chunks[5], outageName);
        ts.insertResult(varNum - offset, Double.parseDouble(chunks[7]));
    }

    /**
     * Marginal costs
     */
    private void readR4(int varNum, String[] chunks, Map<Integer, String> outageNames) {
        int outageId;
        String outageName;
        DoubleResultChunk ts;
        // Check that it's not the header
        if ("VAR. MARGINALES".equals(chunks[1])) {
            return;
        }
        outageId = Integer.parseInt(chunks[3]);
        if (outageId == 0) {
            ts = getDoubleTimeSeries("MV_", BRANCH, chunks[2]);
        } else {
            outageName = Optional.ofNullable(outageNames.get(outageId)).orElseThrow(() -> new PowsyblException(UNKNOWN_OUTAGE));
            ts = getDoubleTimeSeries("MV_", BRANCH, chunks[2], outageName);
        }
        ts.insertResult(varNum - offset, Double.parseDouble(chunks[4]));
    }

    /**
     * Detailed outage flows
     */
    private void readR3C(int varNum, String[] chunks, Map<Integer, String> outageNames) {
        DoubleResultChunk ts;
        String outageName;
        // Check that it's not the header
        if (PAR_LIGNE.equals(chunks[1])) {
            return;
        }
        outageName = Optional.ofNullable(outageNames.get(Integer.parseInt(chunks[3]))).orElseThrow(() -> new PowsyblException(UNKNOWN_OUTAGE));
        ts = getDoubleTimeSeries(FLOW_NAME, BRANCH, chunks[2], outageName);
        ts.insertResult(varNum - offset, Double.parseDouble(chunks[4]));
    }

    /**
     * max outage flows
     */
    private void readR3B(int varNum, String[] chunks, Map<Integer, String> outageNames) {
        StringResultChunk sts;
        DoubleResultChunk ts;
        String outageName;
        // Check that it's not the header
        if (PAR_LIGNE.equals(chunks[1])) {
            return;
        }
        if (!EMPTY_STRING.equals(chunks[3])) {
            sts = getStringTimeSeries("MAX_TMP_THREAT_NAME_", BRANCH, chunks[2]);
            outageName = Optional.ofNullable(outageNames.get(Integer.parseInt(chunks[3]))).orElseThrow(() -> new PowsyblException(UNKNOWN_OUTAGE));
            sts.insertResult(varNum - offset, outageName);

            ts = getDoubleTimeSeries(MAX_TMP_THREAT_FLOW, chunks[2]);
            ts.insertResult(varNum - offset, Double.parseDouble(chunks[4]));
        }
        int i = 0;
        int chunkNum;
        while ((chunkNum = 5 + 2 * i) < chunks.length &&
            !EMPTY_STRING.equals(chunks[chunkNum])) {
            i++;
            sts = getStringTimeSeries(MAX_THREAT_NAME + i + "_NAME_", BRANCH, chunks[2]);
            sts.insertResult(varNum - offset, outageNames.get(Integer.parseInt(chunks[chunkNum])));

            ts = getDoubleTimeSeries(MAX_THREAT_NAME + i + "_" + FLOW_NAME, chunks[2]);
            ts.insertResult(varNum - offset, Double.parseDouble(chunks[chunkNum + 1]));
        }
    }

    /**
     * Basecase flows
     */
    private void readR3(int varNum, String[] chunks) {
        DoubleResultChunk ts;
        // Check that it's not the header
        if (PAR_LIGNE.equals(chunks[1])) {
            return;
        }
        ts = getDoubleTimeSeries(FLOW_NAME, BRANCH, chunks[2]);
        ts.insertResult(varNum - offset, Double.parseDouble(chunks[3]));
    }

    /**
     * Generators bindings
     */
    private void readR2C(int varNum, String[] chunks) {
        DoubleResultChunk ts;
        // Check that it's not the header
        if ("NOM REGROUPEMENT".equals(chunks[1])) {
            return;
        }
        ts = getDoubleTimeSeries("GEN_", "generator binding", chunks[1]);
        ts.insertResult(varNum - offset, Double.parseDouble(chunks[2]));
    }

    /**
     * Generator Curative
     */
    private void readR2B(int varNum, String[] chunks, Map<Integer, String> outageNames) {
        String outageName;
        DoubleResultChunk ts;
        // Check that it's not the header
        if (INCIDENT.equals(chunks[1])) {
            return;
        }
        outageName = Optional.ofNullable(outageNames.get(Integer.parseInt(chunks[1]))).orElseThrow(() -> new PowsyblException(UNKNOWN_OUTAGE));
        ts = getDoubleTimeSeries("GEN_CUR_", GENERATOR, chunks[2], outageName);
        ts.insertResult(varNum - offset, Double.parseDouble(chunks[3]));
    }

    /**
     * Preventive redispatching
     */
    private void readR2(int varNum, String[] chunks) {
        DoubleResultChunk ts;
        // Check that it's not the header
        if ("PAR GROUPE".equals(chunks[1])) {
            return;
        }
        if (!EMPTY_STRING.equals(chunks[5])) {
            ts = getDoubleTimeSeries("INIT_BAL_GEN_", GENERATOR, chunks[2]);
            ts.insertResult(varNum - offset, Double.parseDouble(chunks[5]));
        }
        if (!EMPTY_STRING.equals(chunks[6])) {
            ts = getDoubleTimeSeries("GEN_", GENERATOR, chunks[2]);
            ts.insertResult(varNum - offset, Double.parseDouble(chunks[6]));
        }
    }

    /**
     * Loads bindings
     */
    private void readR1C(int varNum, String[] chunks) {
        DoubleResultChunk ts;
        // Check that it's not the header
        if ("NOM REGROUPEMENT".equals(chunks[1])) {
            return;
        }
        ts = getDoubleTimeSeries("LOAD_", "load binding", chunks[1]);
        ts.insertResult(varNum - offset, Double.parseDouble(chunks[2]));
    }

    /**
     * Curative Loads
     */
    private void readR1B(int varNum, Map<Integer, String> outageNames, String[] chunks) {
        // Check that it's not the header
        if (INCIDENT.equals(chunks[1])) {
            return;
        }
        String outageName = Optional.ofNullable(outageNames.get(Integer.parseInt(chunks[1]))).orElseThrow(() -> new PowsyblException(UNKNOWN_OUTAGE));
        DoubleResultChunk ts = getDoubleTimeSeries("LOAD_CUR_", "load", chunks[2], outageName);
        ts.insertResult(varNum - offset, Double.parseDouble(chunks[3]));
    }

    /**
     * Preventive load shedding
     */
    private void readR1(int varNum, String[] chunks) {
        // Check that it's not the header
        if ("PAR CONSO".equals(chunks[1])) {
            return;
        }
        DoubleResultChunk ts;
        if (!EMPTY_STRING.equals(chunks[4])) {
            ts = getDoubleTimeSeries("INIT_BAL_LOAD_", "load", chunks[2]);
            ts.insertResult(varNum - offset, Double.parseDouble(chunks[4]));
        }
        if (!EMPTY_STRING.equals(chunks[5])) {
            ts = getDoubleTimeSeries("LOAD_", "load", chunks[2]);
            ts.insertResult(varNum - offset, Double.parseDouble(chunks[5]));
        }
    }

    /**
     * Initial balancing for synchronous areas
     */
    private void readC5(int varNum, String[] chunks) {
        // Check that it's not the header
        if ("ZONE SYNC".equals(chunks[1])) {
            return;
        }
        DoubleResultChunk ts = getDoubleTimeSeries("INIT_BAL_AREA_" + chunks[2]);
        ts.insertResult(varNum - offset, Double.parseDouble(chunks[3]));
    }

    /**
     * Outage ids
     */
    private void readC4(Map<Integer, String> outageNames, String[] chunks) {
        // Check that it's not the header
        if ("INCIDENTS".equals(chunks[1])) {
            return;
        }
        outageNames.put(Integer.parseInt(chunks[2].trim()), chunks[4]);
    }

    /**
     * Non connectivity
     */
    private void readC2B(int varNum, String[] chunks) {
        // Check that it's not the header
        if ("NON CONNEXITE".equals(chunks[1])) {
            return;
        }
        if (!EMPTY_STRING.equals(chunks[4])) {
            DoubleResultChunk ts = getDoubleTimeSeries("LOST_LOAD_", "load", chunks[3], chunks[2]);
            ts.insertResult(varNum - offset, Double.parseDouble(chunks[4]));
        }
    }

    /**
     * Non connectivity
     */
    private void readC2(int varNum, String[] chunks) {
        // Check that it's not the header
        if ("NON CONNEXITE".equals(chunks[1])) {
            return;
        }
        DoubleResultChunk ts;
        if (!EMPTY_STRING.equals(chunks[4])) {
            ts = getDoubleTimeSeries("LOST_GEN_", GENERATOR, chunks[2]);
            ts.insertResult(varNum - offset, Double.parseDouble(chunks[4]));
        }
        if (!EMPTY_STRING.equals(chunks[5])) {
            ts = getDoubleTimeSeries("LOST_LOAD_", "load", chunks[2]);
            ts.insertResult(varNum - offset, Double.parseDouble(chunks[5]));
        }
    }

    /**
     * Resume
     */
    private void readC1(int varNum, String[] chunks) {
        // Check that it's not the header
        if ("COMPTE RENDU".equals(chunks[1])) {
            return;
        }
        DoubleResultChunk ts = getDoubleTimeSeries(ERROR_CODE_NAME);
        ts.insertResult(varNum - offset, Double.parseDouble(chunks[2]));
    }

    public static boolean isCurativeTimeSeries(String preventiveTimeSeriesName, Map<String, String> preventiveTags, String timeSeriesName, Map<String, String> tags) {
        if (!tags.containsKey(HVDC_TYPE) && !tags.containsKey(PST_TYPE)) {
            return false;
        }
        String preventiveType = getType(preventiveTags);
        String type = getType(tags);
        if (preventiveType.equals(EMPTY_STRING) || type.equals(EMPTY_STRING) || !type.equals(preventiveType)) {
            return false;
        }
        String preventiveId = getId(preventiveTags);
        String id = getId(tags);
        if (preventiveId.equals(EMPTY_STRING) || id.equals(EMPTY_STRING) || !id.equals(preventiveId)) {
            return false;
        }
        if (!tags.containsKey(CONTINGENCY_TYPE)) {
            return false;
        }
        if (tags.get(CONTINGENCY_TYPE).equals(BASECASE_TYPE)) {
            return false;
        }
        // Nothing else to check fot hvdc : HVDC_ <-> HVDC_CUR_
        if (type.equals(PST_TYPE)) {
            if (preventiveTimeSeriesName.startsWith(PST_TAP_NAME)) {
                // PST_TAP_ <-> PST_CUR_TAP_
                return timeSeriesName.startsWith(PST_CUR_TAP_NAME);
            } else if (preventiveTimeSeriesName.startsWith(PST_NAME)) {
                // PST_  <-> PST_CUR_
                return !timeSeriesName.startsWith(PST_CUR_TAP_NAME) && timeSeriesName.startsWith(PST_CUR_NAME);
            }
        }
        return true;
    }

    public static boolean isPreventiveTimeSeries(Map<String, String> tags) {
        return (tags.containsKey(HVDC_TYPE) || tags.containsKey(PST_TYPE)) && tags.containsKey(CONTINGENCY_TYPE) && tags.get(CONTINGENCY_TYPE).equals(BASECASE_TYPE);
    }

    public static String getId(Map<String, String> tags) {
        if (tags.containsKey(HVDC_TYPE)) {
            return tags.get(HVDC_TYPE);
        } else if (tags.containsKey(PST_TYPE)) {
            return tags.get(PST_TYPE);
        }
        return EMPTY_STRING;
    }

    public static String getType(Map<String, String> tags) {
        if (tags.containsKey(HVDC_TYPE)) {
            return HVDC_TYPE;
        } else if (tags.containsKey(PST_TYPE)) {
            return PST_TYPE;
        }
        return EMPTY_STRING;
    }

    public static String getTimeSeriesPrefix(String timeSeriesName, Map<String, String> tags) {
        if (tags.containsKey(HVDC_TYPE)) {
            return HVDC_NAME;
        } else if (tags.containsKey(PST_TYPE)) {
            if (timeSeriesName.startsWith(PST_TAP_NAME)) {
                return PST_TAP_NAME;
            } else if (timeSeriesName.startsWith(PST_NAME)) {
                return PST_NAME;
            } else if (timeSeriesName.startsWith(PST_CUR_TAP_NAME)) {
                return PST_CUR_TAP_NAME;
            } else if (timeSeriesName.startsWith(PST_CUR_NAME)) {
                return PST_CUR_NAME;
            }
        }
        return EMPTY_STRING;
    }

    private void completePreventiveTimeSeries(List<TimeSeries> initOptimizedTimeSeriesList) {
        for (TimeSeries<?, ?> initTimeSeries : initOptimizedTimeSeriesList) {
            String timeSeriesName = initTimeSeries.getMetadata().getName();
            double[] initValues = ((StoredDoubleTimeSeries) initTimeSeries).toArray();
            if (doubleTimeSeries.containsKey(timeSeriesName)) {
                // find variants with no metrix optimized result, put initial values instead of NaN
                DoubleResultChunk res = doubleTimeSeries.get(timeSeriesName);
                for (int i = 0; i < res.timeSeries.length; i++) {
                    if (Double.isNaN(res.timeSeries[i])) {
                        res.insertResult(i, initValues[i + offset]);
                    }
                }
            } else {
                // any optimized results for the chunk, put initial values for all variants
                Map<String, String> tags = initTimeSeries.getMetadata().getTags();
                DoubleResultChunk ts = getDoubleTimeSeries(getTimeSeriesPrefix(timeSeriesName, tags), getType(tags), getId(tags));
                for (int i = 0; i < ts.timeSeries.length; i++) {
                    ts.insertResult(i, initValues[i + offset]);
                }
            }
        }
    }

    private void completeCurativeTimeSeries(List<TimeSeries> initOptimizedTimeSeriesList) {
        for (TimeSeries<?, ?> initTimeSeries : initOptimizedTimeSeriesList) {
            String preventiveTimeSeriesName = initTimeSeries.getMetadata().getName();
            Map<String, String> preventiveTags = initTimeSeries.getMetadata().getTags();
            if (doubleTimeSeries.containsKey(preventiveTimeSeriesName)) {
                double[] preventiveValues = doubleTimeSeries.get(preventiveTimeSeriesName).getTimeSeries();
                // find variants with no metrix curative optimized result, put preventive values instead of NaN
                doubleTimeSeries.entrySet().stream()
                    .filter(ts -> isCurativeTimeSeries(preventiveTimeSeriesName, preventiveTags, ts.getKey(), ts.getValue().getTags()))
                    .forEach(curativeTs -> {
                        DoubleResultChunk res = curativeTs.getValue();
                        double[] curativeValues = res.getTimeSeries();
                        for (int i = 0; i < curativeValues.length; i++) {
                            if (Double.isNaN(curativeValues[i])) {
                                res.insertResult(i, preventiveValues[i]);
                            }
                        }
                    });
            }
        }
    }

    private void completeOptimizedTimeSeries(List<TimeSeries> initOptimizedTimeSeriesList) {

        if (initOptimizedTimeSeriesList.isEmpty()) {
            return;
        }

        // complete preventive time series of hvdc and pst optimized results with initial values (mapped or base case)
        completePreventiveTimeSeries(initOptimizedTimeSeriesList);

        // complete curative time series of hvdc and pst optimized results with preventive results
        completeCurativeTimeSeries(initOptimizedTimeSeriesList);
    }

    public void createTimeSeries(TimeSeriesIndex index, List<TimeSeries> initOptimizedTimeSeriesList, List<TimeSeries> timeSeriesList) {

        // complete hvdc and pst optimized results with initial values (mapped or base case)
        completeOptimizedTimeSeries(initOptimizedTimeSeriesList);

        // write double time series
        for (Map.Entry<String, DoubleResultChunk> e : doubleTimeSeries.entrySet()) {
            String timeSeriesName = e.getKey();
            DoubleResultChunk res = e.getValue();
            timeSeriesList.add(new StoredDoubleTimeSeries(new TimeSeriesMetadata(timeSeriesName, TimeSeriesDataType.DOUBLE, res.getTags(), index),
                    new UncompressedDoubleDataChunk(offset, res.getTimeSeries()).tryToCompress()));
        }

        // write string time series
        for (Map.Entry<String, StringResultChunk> e : stringTimeSeries.entrySet()) {
            String timeSeriesName = e.getKey();
            StringResultChunk res = e.getValue();
            timeSeriesList.add(new StringTimeSeries(new TimeSeriesMetadata(timeSeriesName, TimeSeriesDataType.STRING, res.getTags(), index),
                    new UncompressedStringDataChunk(offset, res.getTimeSeries()).tryToCompress()));
        }
    }
}
