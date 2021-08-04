package com.powsybl.metrix.integration.network;

import com.powsybl.iidm.network.*;
import com.powsybl.metrix.mapping.TimeSeriesMapper;
import com.powsybl.timeseries.*;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public final class MetrixNetworkPoint {

    private static final String ID_TO_CLOSE = "+";
    private static final String ID_SEPARATOR = " / ";

    private MetrixNetworkPoint() throws IllegalAccessException {
        throw new IllegalAccessException();
    }

    public static void addTimeSeriesValues(int version, int point, boolean isCurativeMode, String defaultId, List<TimeSeries> timeSeries, Network networkPoint) {
        final boolean isCurativeTimeSeriesAdding = false;

        // Add double time series to network
        List<DoubleTimeSeries> doubleTimeSeries = timeSeries.stream()
                .filter(ts -> ts.getMetadata().getDataType() == TimeSeriesDataType.DOUBLE)
                .map(ts -> (DoubleTimeSeries) ts)
                .collect(Collectors.toList());
        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache(doubleTimeSeries);
        addTimeSeriesValues(version, point, isCurativeTimeSeriesAdding, defaultId, store, networkPoint);
        if (isCurativeMode) {
            addTimeSeriesValues(version, point, !isCurativeTimeSeriesAdding, defaultId, store, networkPoint);
        }

        // For curative mode, add topology time series to network
        List<StringTimeSeries> stringTimeSeries = timeSeries.stream()
                .filter(ts -> isCurativeMode && ts.getMetadata().getDataType() == TimeSeriesDataType.STRING && ts.getMetadata().getName().compareTo("TOPOLOGY_" + defaultId) == 0)
                .map(ts -> (StringTimeSeries) ts)
                .collect(Collectors.toList());
        addTopologyTimeSeries(point, stringTimeSeries, networkPoint);
    }

    private static String getPrefix(String prefix, boolean isCurativeMode) {
        return isCurativeMode ? prefix + "CUR_" : prefix;
    }

    private static String getSuffix(boolean isCurativeMode, String defaultId) {
        return isCurativeMode ? "_" + defaultId : "";
    }

    private static void addTimeSeriesValues(int version, int point, boolean isCurativeMode, String defaultId, ReadOnlyTimeSeriesStore store, Network networkPoint) {

        networkPoint.getGenerators().forEach(generator -> addTimeSeriesToGenerator(generator, store, version, point,
                getPrefix("GEN_", isCurativeMode),
                getSuffix(isCurativeMode, defaultId)
        ));

        networkPoint.getLoads().forEach(load -> addTimeSeriesToLoad(load, store, version, point,
                getPrefix("LOAD_", isCurativeMode),
                getSuffix(isCurativeMode, defaultId)
        ));

        networkPoint.getHvdcLines().forEach(hvdcLine -> addTimeSeriesToHvdcLine(hvdcLine, store, version, point,
                getPrefix("HVDC_", isCurativeMode),
                getSuffix(isCurativeMode, defaultId)
        ));

        networkPoint.getTwoWindingsTransformerStream().filter(transformer -> transformer.getPhaseTapChanger() != null).forEach(transformer -> addTimeSeriesToPhaseTapChanger(transformer, store, version, point,
                isCurativeMode ? "PST_CUR_TAP_" : "PST_TAP_",
                getSuffix(isCurativeMode, defaultId)
        ));
    }

    private static double getTimeSeriesValue(Identifiable identifiable, ReadOnlyTimeSeriesStore store, int version, int point, String prefix, String suffix) {
        AtomicReference<Double> value = new AtomicReference<>(Double.NaN);
        String timeSeriesName = prefix + identifiable.getId() + suffix;
        if (store.timeSeriesExists(timeSeriesName)) {
            Optional<DoubleTimeSeries> timeSeries = store.getDoubleTimeSeries(timeSeriesName, version);
            timeSeries.ifPresent(timeSeriesList -> {
                double[] values = timeSeriesList.toArray();
                value.set(values[point]);
            });
        }
        return value.get();
    }

    private static void addTimeSeriesToGenerator(Generator generator, ReadOnlyTimeSeriesStore store, int version, int point, String prefix, String suffix) {
        double value = getTimeSeriesValue(generator, store, version, point, prefix, suffix);
        if (!Double.isNaN(value)) {
            generator.setTargetP(generator.getTargetP() + value);
        }
    }

    private static void addTimeSeriesToLoad(Load load, ReadOnlyTimeSeriesStore store, int version, int point, String prefix, String suffix) {
        double value = getTimeSeriesValue(load, store, version, point, prefix, suffix);
        if (!Double.isNaN(value)) {
            load.setP0(load.getP0() - value);
        }
    }

    private static void addTimeSeriesToHvdcLine(HvdcLine hvdcLine, ReadOnlyTimeSeriesStore store, int version, int point, String prefix, String suffix) {
        double value = getTimeSeriesValue(hvdcLine, store, version, point, prefix, suffix);
        if (!Double.isNaN(value)) {
            TimeSeriesMapper.setHvdcLineSetPoint(hvdcLine, value);
        }
    }

    private static void addTimeSeriesToPhaseTapChanger(TwoWindingsTransformer transformer, ReadOnlyTimeSeriesStore store, int version, int point, String prefix, String suffix) {
        double value = getTimeSeriesValue(transformer, store, version, point, prefix, suffix);
        if (!Double.isNaN(value)) {
            transformer.getPhaseTapChanger().setTapPosition((int) value);
        }
    }

    private static void addTopologyTimeSeries(int point, List<StringTimeSeries> stringTimeSeries, Network networkPoint) {
        if (stringTimeSeries.isEmpty()) {
            return;
        }
        StringTimeSeries topologyTimeSeries = stringTimeSeries.get(0);
        String[] values = topologyTimeSeries.toArray();
        String value = values[point];
        String[] ids = value.split(ID_SEPARATOR);
        for (int i = 0; i < ids.length; i++) {
            String id = ids[i];
            boolean isToOpen = true;
            if (id.startsWith(ID_TO_CLOSE)) {
                isToOpen = false;
                id = id.substring(1);
            }
            Identifiable identifiable = networkPoint.getIdentifiable(id);
            if (identifiable instanceof Branch) {
                Branch branch = (Branch) identifiable;
                if (isToOpen) {
                    branch.getTerminal1().disconnect();
                    branch.getTerminal2().disconnect();
                } else {
                    branch.getTerminal1().connect();
                    branch.getTerminal2().connect();
                }
            } else if (identifiable instanceof Switch) {
                Switch sw = (Switch) identifiable;
                sw.setOpen(isToOpen);
            }
        }
    }
}
