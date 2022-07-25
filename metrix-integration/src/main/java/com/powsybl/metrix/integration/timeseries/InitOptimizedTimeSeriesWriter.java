/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.metrix.integration.timeseries;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Range;
import com.powsybl.iidm.network.*;
import com.powsybl.metrix.integration.MetrixDslData;
import com.powsybl.metrix.integration.dataGenerator.MetrixOutputData;
import com.powsybl.metrix.mapping.DefaultTimeSeriesMapperObserver;
import com.powsybl.metrix.mapping.EquipmentVariable;
import com.powsybl.metrix.mapping.MappingVariable;
import com.powsybl.metrix.mapping.TimeSeriesMapper;
import com.powsybl.timeseries.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.*;

import static com.powsybl.metrix.integration.dataGenerator.MetrixOutputData.*;

public class InitOptimizedTimeSeriesWriter extends DefaultTimeSeriesMapperObserver {

    public static final String INPUT_OPTIMIZED_FILE_NAME = "input_optimized_time_series.json";

    private Network network;
    private int length;
    private int offset;
    private TimeSeriesIndex index;
    private Writer writer;

    Map<String, MetrixOutputData.DoubleResultChunk> constantDoubleTimeSeries = new HashMap<>();
    Map<String, MetrixOutputData.DoubleResultChunk> doubleTimeSeries = new HashMap<>();
    Set<String> hvdcToInit;
    Set<String> phaseTapChangerToInit;

    public InitOptimizedTimeSeriesWriter(Network network, MetrixDslData metrixDslData, Range<Integer> pointRange, Writer writer) {
        Objects.requireNonNull(pointRange);
        this.network = Objects.requireNonNull(network);
        Objects.requireNonNull(metrixDslData);
        this.writer = Objects.requireNonNull(writer);
        this.length = pointRange.upperEndpoint() - pointRange.lowerEndpoint() + 1;
        this.offset = pointRange.lowerEndpoint();
        this.hvdcToInit = metrixDslData.getHvdcFlowResults();
        this.phaseTapChangerToInit = metrixDslData.getPstAngleTapResults();
    }

    private void addHvdcTimeSeries(int point, Identifiable<?> identifiable, double activePowerSetpoint) {
        String id = identifiable.getId();
        String timeSeriesName = HVDC_NAME + id;
        addTimeSeriesValue(point, timeSeriesName, id, HVDC_TYPE, activePowerSetpoint);
    }

    private void addPhaseTapChangerTimeSeries(int point, Identifiable<?> identifiable, int tapPosition, double alpha) {
        String id = identifiable.getId();
        String timeSeriesName = PST_NAME + id;
        addTimeSeriesValue(point, timeSeriesName, id, PST_TYPE, alpha);
        timeSeriesName = PST_TAP_NAME + id;
        addTimeSeriesValue(point, timeSeriesName, id, PST_TYPE, tapPosition);
    }

    private void addTimeSeriesValue(int point, String timeSeriesName, String id, String type, double value) {
        if (point == TimeSeriesMapper.CONSTANT_VARIANT_ID) {
            constantDoubleTimeSeries.computeIfAbsent(timeSeriesName, k -> {
                Map<String, String> tags = ImmutableMap.of(type, id, CONTINGENCY_TYPE, BASECASE_TYPE);
                return new MetrixOutputData.DoubleResultChunk(length, tags);
            });
            for (int i = 0; i < length; i++) {
                constantDoubleTimeSeries.get(timeSeriesName).insertResult(i, value);
            }
        } else {
            doubleTimeSeries.computeIfAbsent(timeSeriesName, k -> {
                Map<String, String> tags = ImmutableMap.of(type, id, CONTINGENCY_TYPE, BASECASE_TYPE);
                return new MetrixOutputData.DoubleResultChunk(length, tags);
            });
            doubleTimeSeries.get(timeSeriesName).insertResult(point - offset, value);
        }
    }

    private List<TimeSeries> createTimeSeries() {
        List<TimeSeries> timeSeries = new ArrayList<>();
        for (Map.Entry<String, MetrixOutputData.DoubleResultChunk> e : doubleTimeSeries.entrySet()) {
            String timeSeriesName = e.getKey();
            MetrixOutputData.DoubleResultChunk res = e.getValue();
            DoubleTimeSeries storedDoubleTimeSeries = new StoredDoubleTimeSeries(
                    new TimeSeriesMetadata(timeSeriesName, TimeSeriesDataType.DOUBLE, res.getTags(), index),
                    new UncompressedDoubleDataChunk(offset, res.getTimeSeries()).tryToCompress());
            timeSeries.add(storedDoubleTimeSeries);
        }
        return timeSeries;
    }

    private void writeTimeSeries(List<TimeSeries> timeSeries) {
        try {
            TimeSeries.writeJson(writer, timeSeries);
            writer.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void versionStart(int version) {
        super.versionStart(version);
        hvdcToInit.stream().forEach(id -> {
            HvdcLine hvdcLine = network.getHvdcLine(id);
            double activePowerSetpoint = hvdcLine.getActivePowerSetpoint();
            addHvdcTimeSeries(TimeSeriesMapper.CONSTANT_VARIANT_ID, hvdcLine, activePowerSetpoint);
        });
        phaseTapChangerToInit.stream().forEach(id -> {
            TwoWindingsTransformer twoWindingsTransformer = network.getTwoWindingsTransformer(id);
            PhaseTapChanger phaseTapChanger = twoWindingsTransformer.getPhaseTapChanger();
            int tapPosition = phaseTapChanger.getTapPosition();
            double alpha = phaseTapChanger.getStep(tapPosition).getAlpha();
            addPhaseTapChangerTimeSeries(TimeSeriesMapper.CONSTANT_VARIANT_ID, twoWindingsTransformer, tapPosition, alpha);
        });
    }

    @Override
    public void timeSeriesMappingStart(int point, TimeSeriesIndex index) {
        this.index = index;
        doubleTimeSeries.putAll(constantDoubleTimeSeries);
    }

    @Override
    public void timeSeriesMappedToEquipment(int point, String timeSeriesName, Identifiable<?> identifiable, MappingVariable variable, double equipmentValue) {
        if (identifiable instanceof HvdcLine && hvdcToInit.contains(identifiable.getId()) && variable == EquipmentVariable.activePowerSetpoint) {
            addHvdcTimeSeries(point, identifiable, equipmentValue);
        } else if (identifiable instanceof TwoWindingsTransformer && phaseTapChangerToInit.contains(identifiable.getId()) && variable == EquipmentVariable.phaseTapPosition) {
            int tapPosition = (int) equipmentValue;
            double alpha = ((TwoWindingsTransformer) identifiable).getPhaseTapChanger().getStep(tapPosition).getAlpha();
            addPhaseTapChangerTimeSeries(point, identifiable, tapPosition, alpha);
        }
    }

    @Override
    public void versionEnd(int version) {
        List<TimeSeries> timeSeries = createTimeSeries();
        writeTimeSeries(timeSeries);
        constantDoubleTimeSeries.clear();
        doubleTimeSeries.clear();
    }
}
