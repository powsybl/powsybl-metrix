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
 * @author berthault {@literal <valentinberthault at outlook.fr>}
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
                LOGGER.error("Error encountered while reading results for variant " + varNum, e);
                getDoubleTimeSeries(ERROR_CODE_NAME).insertResult(varNum - offset, ERROR_CODE);
            }
        }
    }

    public void read(BufferedReader reader, int varNum) throws IOException {
        String line;
        String[] chunks;
        DoubleResultChunk ts;
        StringResultChunk sts;
        Map<Integer, String> outageNames = new HashMap<>();
        String outageName;

        boolean empty = true;
        while ((line = reader.readLine()) != null) {
            empty = false;
            chunks = line.split(";", -1);
            int outageId;
            switch (chunks[0]) {
                case "C1 " -> {
                    if ("COMPTE RENDU".equals(chunks[1])) {
                        continue; // header
                    }
                    ts = getDoubleTimeSeries(ERROR_CODE_NAME);
                    ts.insertResult(varNum - offset, Double.parseDouble(chunks[2]));
                }
                case "C2 " -> {
                    if ("NON CONNEXITE".equals(chunks[1])) {
                        continue; // header
                    }
                    if (!EMPTY_STRING.equals(chunks[4])) {
                        ts = getDoubleTimeSeries("LOST_GEN_", GENERATOR, chunks[2]);
                        ts.insertResult(varNum - offset, Double.parseDouble(chunks[4]));
                    }
                    if (!EMPTY_STRING.equals(chunks[5])) {
                        ts = getDoubleTimeSeries("LOST_LOAD_", "load", chunks[2]);
                        ts.insertResult(varNum - offset, Double.parseDouble(chunks[5]));
                    }
                }
                case "C2B " -> {
                    if ("NON CONNEXITE".equals(chunks[1])) {
                        continue; // header
                    }
                    if (!EMPTY_STRING.equals(chunks[4])) {
                        ts = getDoubleTimeSeries("LOST_LOAD_", "load", chunks[3], chunks[2]);
                        ts.insertResult(varNum - offset, Double.parseDouble(chunks[4]));
                    }
                }
                case "C4 " -> {
                    // outage ids
                    if ("INCIDENTS".equals(chunks[1])) {
                        continue; // header
                    }
                    outageNames.put(Integer.parseInt(chunks[2].trim()), chunks[4]);
                }
                case "C5 " -> {
                    // initial balancing for synchronous areas
                    if ("ZONE SYNC".equals(chunks[1])) {
                        continue; // header
                    }
                    ts = getDoubleTimeSeries("INIT_BAL_AREA_" + chunks[2]);
                    ts.insertResult(varNum - offset, Double.parseDouble(chunks[3]));
                }
                case "R1 " -> {
                    // Preventive load shedding
                    if ("PAR CONSO".equals(chunks[1])) {
                        continue; // header
                    }
                    if (!EMPTY_STRING.equals(chunks[4])) {
                        ts = getDoubleTimeSeries("INIT_BAL_LOAD_", "load", chunks[2]);
                        ts.insertResult(varNum - offset, Double.parseDouble(chunks[4]));
                    }
                    if (!EMPTY_STRING.equals(chunks[5])) {
                        ts = getDoubleTimeSeries("LOAD_", "load", chunks[2]);
                        ts.insertResult(varNum - offset, Double.parseDouble(chunks[5]));
                    }
                }
                case "R1B " -> {
                    // Curative Loads
                    if (INCIDENT.equals(chunks[1])) {
                        continue; // header
                    }
                    outageName = Optional.ofNullable(outageNames.get(Integer.parseInt(chunks[1]))).orElseThrow(() -> new PowsyblException(UNKNOWN_OUTAGE));
                    ts = getDoubleTimeSeries("LOAD_CUR_", "load", chunks[2], outageName);
                    ts.insertResult(varNum - offset, Double.parseDouble(chunks[3]));
                }
                case "R1C " -> {
                    // Loads bindings
                    if ("NOM REGROUPEMENT".equals(chunks[1])) {
                        continue; // header
                    }
                    ts = getDoubleTimeSeries("LOAD_", "load binding", chunks[1]);
                    ts.insertResult(varNum - offset, Double.parseDouble(chunks[2]));
                }
                case "R2 " -> {
                    // Preventive redispatching
                    if ("PAR GROUPE".equals(chunks[1])) {
                        continue; // header
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
                case "R2B " -> {
                    // Generator Curative
                    if (INCIDENT.equals(chunks[1])) {
                        continue; // header
                    }
                    outageName = Optional.ofNullable(outageNames.get(Integer.parseInt(chunks[1]))).orElseThrow(() -> new PowsyblException(UNKNOWN_OUTAGE));
                    ts = getDoubleTimeSeries("GEN_CUR_", GENERATOR, chunks[2], outageName);
                    ts.insertResult(varNum - offset, Double.parseDouble(chunks[3]));
                }
                case "R2C " -> {
                    // Generators bindings
                    if ("NOM REGROUPEMENT".equals(chunks[1])) {
                        continue; // header
                    }
                    ts = getDoubleTimeSeries("GEN_", "generator binding", chunks[1]);
                    ts.insertResult(varNum - offset, Double.parseDouble(chunks[2]));
                }
                case "R3 " -> {
                    // Basecase flows
                    if (PAR_LIGNE.equals(chunks[1])) {
                        continue; // header
                    }
                    ts = getDoubleTimeSeries(FLOW_NAME, BRANCH, chunks[2]);
                    ts.insertResult(varNum - offset, Double.parseDouble(chunks[3]));
                }
                case "R3B " -> {
                    // max outage flows
                    if (PAR_LIGNE.equals(chunks[1])) {
                        continue; // header
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
                case "R3C " -> {
                    // Detailed outage flows
                    if (PAR_LIGNE.equals(chunks[1])) {
                        continue; // header
                    }
                    outageName = Optional.ofNullable(outageNames.get(Integer.parseInt(chunks[3]))).orElseThrow(() -> new PowsyblException(UNKNOWN_OUTAGE));
                    ts = getDoubleTimeSeries(FLOW_NAME, BRANCH, chunks[2], outageName);
                    ts.insertResult(varNum - offset, Double.parseDouble(chunks[4]));
                }
                case "R4 " -> {
                    // Marginal costs
                    if ("VAR. MARGINALES".equals(chunks[1])) {
                        continue; // header
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
                case "R4B " -> {
                    // Detailed marginal costs
                    if ("VAR. MARGINALES".equals(chunks[1])) {
                        continue; // header
                    }

                    outageId = Integer.parseInt(chunks[3]);
                    outageName = outageId == 0 ? null : Optional.ofNullable(outageNames.get(outageId)).orElseThrow(() -> new PowsyblException(UNKNOWN_OUTAGE));
                    ts = getDetailedMVTimeSeries("MV_POW", chunks[2], chunks[5], outageName);
                    ts.insertResult(varNum - offset, Double.parseDouble(chunks[6]));
                    ts = getDetailedMVTimeSeries("MV_COST", chunks[2], chunks[5], outageName);
                    ts.insertResult(varNum - offset, Double.parseDouble(chunks[7]));
                }
                case "R5 " -> {
                    // PST Basecase
                    if ("PAR TD".equals(chunks[1])) {
                        continue; // header
                    }
                    ts = getDoubleTimeSeries(PST_NAME, PST_TYPE, chunks[2]);
                    ts.insertResult(varNum - offset, Double.parseDouble(chunks[3]));
                    ts = getDoubleTimeSeries(PST_TAP_NAME, PST_TYPE, chunks[2]);
                    ts.insertResult(varNum - offset, Integer.parseInt(chunks[4]));
                }
                case "R5B " -> {
                    // PST Curative
                    if (INCIDENT.equals(chunks[1])) {
                        continue; // header
                    }
                    outageName = Optional.ofNullable(outageNames.get(Integer.parseInt(chunks[1]))).orElseThrow(() -> new PowsyblException(UNKNOWN_OUTAGE));
                    ts = getDoubleTimeSeries(PST_CUR_NAME, PST_TYPE, chunks[2], outageName);
                    ts.insertResult(varNum - offset, Double.parseDouble(chunks[3]));
                    ts = getDoubleTimeSeries(PST_CUR_TAP_NAME, PST_TYPE, chunks[2], outageName);
                    ts.insertResult(varNum - offset, Integer.parseInt(chunks[4]));
                }
                case "R6 " -> {
                    // HVDC Basecase
                    if (" PAR LCC".equals(chunks[1])) {
                        continue; // header
                    }
                    ts = getDoubleTimeSeries(HVDC_NAME, HVDC_TYPE, chunks[2]);
                    ts.insertResult(varNum - offset, Double.parseDouble(chunks[3]));

                    if (!EMPTY_STRING.equals(chunks[4])) {
                        ts = getDoubleTimeSeries("MV_", HVDC_TYPE, chunks[2]);
                        ts.insertResult(varNum - offset, Double.parseDouble(chunks[4]));
                    }
                }
                case "R6B " -> {
                    // HVDC Curative
                    if (INCIDENT.equals(chunks[1])) {
                        continue; // header
                    }
                    outageName = Optional.ofNullable(outageNames.get(Integer.parseInt(chunks[1]))).orElseThrow(() -> new PowsyblException(UNKNOWN_OUTAGE));
                    ts = getDoubleTimeSeries("HVDC_CUR_", HVDC_TYPE, chunks[2], outageName);
                    ts.insertResult(varNum - offset, Double.parseDouble(chunks[3]));
                }
                case "R7 " -> {
                    // Redispatching by generator types
                    if ("PAR FILIERE".equals(chunks[1])) {
                        continue; // header
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
                case "R8 " -> {
                    // Losses
                    if ("PERTES".equals(chunks[1])) {
                        continue; // header
                    }
                    ts = getDoubleTimeSeries(LOSSES);
                    ts.insertResult(varNum - offset, Double.parseDouble(chunks[2]));
                }
                case "R8B " -> {
                    // Pertes
                    if ("PERTES".equals(chunks[1])) {
                        continue; // header
                    }
                    ts = getDoubleTimeSeries(LOSSES_BY_COUNTRY, "losses", chunks[2]);
                    ts.insertResult(varNum - offset, Double.parseDouble(chunks[3]));
                }
                case "R9 " -> {
                    // Fonction objectif de l'optimisation
                    if ("FCT OBJECTIF".equals(chunks[1])) {
                        continue; // header
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
                case "R10" -> {
                    // Topological remedial actions
                    if (INCIDENT.equals(chunks[1])) {
                        continue; // header
                    }
                    sts = getStringTimeSeries("TOPOLOGY_", CONTINGENCY_TYPE, chunks[2]);
                    sts.insertResult(varNum - offset, chunks[4]);
                }
                default -> LOGGER.error("Unexpected content for variant {} ({})", varNum, line);
            }
        }
        if (empty) {
            LOGGER.error("Empty Metrix result file");
        }
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

    private void completeOptimizedTimeSeries(List<TimeSeries> initOptimizedTimeSeriesList) {

        if (initOptimizedTimeSeriesList.isEmpty()) {
            return;
        }

        // complete preventive time series of hvdc and pst optimized results with initial values (mapped or base case)
        for (TimeSeries initTimeSeries : initOptimizedTimeSeriesList) {
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

        // complete curative time series of hvdc and pst optimized results with preventive results
        for (TimeSeries initTimeSeries : initOptimizedTimeSeriesList) {
            String timeSeriesName = initTimeSeries.getMetadata().getName();
            Map<String, String> tags = initTimeSeries.getMetadata().getTags();
            if (doubleTimeSeries.containsKey(timeSeriesName)) {
                double[] preventiveValues = doubleTimeSeries.get(timeSeriesName).getTimeSeries();
                // find variants with no metrix curative optimized result, put preventive values instead of NaN
                doubleTimeSeries.entrySet().stream()
                        .filter(ts -> isCurativeTimeSeries(timeSeriesName, tags, ts.getKey(), ts.getValue().getTags()))
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
